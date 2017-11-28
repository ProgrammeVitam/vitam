/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.security.internal.rest.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;

import java.security.cert.CertificateException;
import java.util.Optional;

public class PersonalCertificateService {

    private static final String INVALID_CERTIFICATE = "Invalid certificate";
    private final static String NO_CERTIFICATE_MESSAGE = "No certificate transmitted";
    private static final String PERSONAL_LOGBOOK_EVENT = "STP_PERSONAL_CHECK";

    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IdentityService.class);
    private PersonalRepository personalRepository;

    public PersonalCertificateService(LogbookOperationsClientFactory logbookOperationsClientFactory,
        PersonalRepository personalRepository) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.personalRepository = personalRepository;
    }

    /**
     * create certificate
     *
     * @throws CertificateException
     * @throws InvalidParseOperationException
     */
    public void createPersonalCertificateIfNotPresent(byte[] certificate)
        throws PersonalCertificateException, InvalidParseOperationException {

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        if (personalRepository.findPersonalCertificateByHash(parsedCertificate.getCertificateHash()).isPresent()) {
            LOGGER.info("Personal certificate already exists {0}", parsedCertificate.getCertificateHash());
            return;
        }

        LOGGER.info("Personal certificate does not exist {0}. Creating it...", parsedCertificate.getCertificateHash());

        PersonalCertificateModel personalModel = new PersonalCertificateModel();
        personalModel.setId(GUIDFactory.newGUID().toString());

        personalModel.setCertificate(parsedCertificate.getRawCertificate());

        personalModel.setSubjectDN(parsedCertificate.getX509Certificate().getSubjectDN().getName());
        personalModel.setIssuerDN(parsedCertificate.getX509Certificate().getIssuerDN().getName());
        personalModel.setSerialNumber(parsedCertificate.getX509Certificate().getSerialNumber());

        personalModel.setCertificateHash(parsedCertificate.getCertificateHash());

        personalRepository.createPersonalCertificate(personalModel);
    }

    public void deletePersonalCertificateIfPresent(byte[] certificate)
        throws PersonalCertificateException {

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        personalRepository.deletePersonalCertificate(parsedCertificate.getCertificateHash());
    }

    public void checkPersonalCertificateExistence(byte[] certificate, String permission)
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException {

        if (certificate == null) {
            createNoPersonalCertificateLogbook();
            throw new PersonalCertificateException(NO_CERTIFICATE_MESSAGE);
        }

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        Optional<PersonalCertificateModel> model
            = personalRepository.findPersonalCertificateByHash(parsedCertificate.getCertificateHash());

        if (model.isPresent()) {
            // Access  OK
            // FIXME story a creer : tracer certificat corrects identifiés.
            LOGGER.debug("Access OK for permission {0} with valid personal certificate {1}",
                permission, parsedCertificate.getCertificateHash());
            return;
        }

        createInvalidPersonalCertificateLogbook(parsedCertificate, permission);

        throw new PersonalCertificateException(INVALID_CERTIFICATE);
    }

    private void createNoPersonalCertificateLogbook()
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        InvalidParseOperationException {

        final LogbookOperationParameters logbookParameters = getLogbookParametersKo();

        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("Certificate", "change it ");
        msg.put("Permissions", "Change It");
        evDetData.set("Context", msg);

        final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);

        logbookOperationsClientFactory.getClient().create(logbookParameters);
    }

    private void createInvalidPersonalCertificateLogbook(ParsedCertificate parsedCertificate, String permission)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        InvalidParseOperationException {


        final LogbookOperationParameters logbookParameters = getLogbookParametersKo();

        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("CertificateSn", parsedCertificate.getX509Certificate().getSerialNumber().toString());
        msg.put("CertificateSubjectDN", parsedCertificate.getX509Certificate().getSubjectDN().toString());
        msg.put("Permission", permission);

        evDetData.set("Context", msg);
        final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);

        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);

        logbookOperationsClientFactory.getClient().create(logbookParameters);
    }

    private LogbookOperationParameters getLogbookParametersKo() {
        GUID eip = GUIDFactory.newGUID();

        return LogbookParametersFactory
            .newLogbookOperationParameters(eip, PERSONAL_LOGBOOK_EVENT, eip, LogbookTypeProcess.CHECK,
                StatusCode.KO, VitamLogbookMessages.getCodeOp(PERSONAL_LOGBOOK_EVENT, StatusCode.KO), eip);
    }


}
