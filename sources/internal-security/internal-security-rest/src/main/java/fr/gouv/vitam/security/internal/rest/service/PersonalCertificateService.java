/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.security.internal.rest.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import fr.gouv.vitam.security.internal.common.model.PersonalCertificateModel;
import fr.gouv.vitam.security.internal.common.service.ParsedCertificate;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.repository.CertificateRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;

import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;

/**
 * Manages personal certificates
 */
public class PersonalCertificateService extends SecurityService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PersonalCertificateService.class);

    private static final String INVALID_CERTIFICATE = "Invalid certificate";
    private static final String NO_CERTIFICATE_MESSAGE = "No certificate transmitted";
    private static final String PERSONAL_LOGBOOK_EVENT = "STP_PERSONAL_CERTIFICATE_CHECK";

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final PersonalRepository personalRepository;

    public PersonalCertificateService(
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        PersonalRepository personalRepository
    ) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.personalRepository = personalRepository;
    }

    /**
     * Create certificate if not present in DB.
     *
     * @throws PersonalCertificateException thrown if certificate parse fail
     * @throws InvalidParseOperationException thrown if creation fail
     */
    public void createPersonalCertificateIfNotPresent(byte[] certificate)
        throws PersonalCertificateException, InvalidParseOperationException {
        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        if (findPersonalCertificateByHash(parsedCertificate.getCertificateHash()).isPresent()) {
            LOGGER.info("Personal certificate already exists {0}", parsedCertificate.getCertificateHash());
            return;
        }

        LOGGER.info("Personal certificate does not exist {0}. Creating it...", parsedCertificate.getCertificateHash());

        PersonalCertificateModel personalModel = new PersonalCertificateModel();
        personalModel.setId(GUIDFactory.newGUID().toString());

        personalModel.setCertificate(parsedCertificate.getRawCertificate());

        personalModel.setSubjectDN(parsedCertificate.getX509Certificate().getSubjectDN().getName());
        personalModel.setIssuerDN(parsedCertificate.getX509Certificate().getIssuerDN().getName());
        personalModel.setSerialNumber(String.valueOf(parsedCertificate.getX509Certificate().getSerialNumber()));

        personalModel.setCertificateHash(parsedCertificate.getCertificateHash());
        personalModel.setExpirationDate(
            LocalDateUtil.getFormattedDateForMongo(
                LocalDateUtil.fromDate(parsedCertificate.getX509Certificate().getNotAfter())
            )
        );

        personalRepository.createPersonalCertificate(personalModel);
    }

    /**
     * Delete certificate if present.
     *
     * @param certificate the certificate to delete
     * @throws PersonalCertificateException thrown if certificate parse fail
     */
    public void deletePersonalCertificateIfPresent(byte[] certificate) throws PersonalCertificateException {
        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        personalRepository.deletePersonalCertificate(parsedCertificate.getCertificateHash());
    }

    /**
     * Checks if the personal certificate if valid.
     *
     * @param certificate the certificate to check
     * @param permission the permission for which access if checked (required for logbook logging)
     * @throws LogbookClientException thrown if logbook creation fail
     * @throws InvalidParseOperationException thrown if logbook or certificate parse fail
     * @throws PersonalCertificateException thrown if certificate parse fail
     */
    public void checkPersonalCertificateExistence(byte[] certificate, String permission)
        throws LogbookClientException, InvalidParseOperationException, PersonalCertificateException {
        ParsedCertificate parsedCertificate;
        if (certificate == null) {
            createNoPersonalCertificateLogbook(permission);
            throw new PersonalCertificateException(NO_CERTIFICATE_MESSAGE);
        }

        try {
            parsedCertificate = ParsedCertificate.parseCertificate(certificate);
        } catch (PersonalCertificateException e) {
            createInvalidPersonalCertificateLogbook(permission);
            throw e;
        }
        Optional<PersonalCertificateModel> model = findPersonalCertificateByHash(
            parsedCertificate.getCertificateHash()
        );

        if (model.isPresent()) {
            // Access  OK
            // FIXME story a creer : tracer certificat corrects identifiés.
            LOGGER.debug(
                "Access OK for permission {0} with valid personal certificate {1}",
                permission,
                parsedCertificate.getCertificateHash()
            );
            return;
        }

        createInvalidPersonalCertificateLogbook(parsedCertificate, permission);

        throw new PersonalCertificateException(INVALID_CERTIFICATE);
    }

    private Optional<PersonalCertificateModel> findPersonalCertificateByHash(String certificateHash)
        throws InvalidParseOperationException, PersonalCertificateException {
        //check validity of the  retrieved certificate from VITAM DB
        Optional<PersonalCertificateModel> personalCertificateModelOptional =
            personalRepository.findPersonalCertificateByHash(certificateHash);

        //check certificate validity
        if (personalCertificateModelOptional.isPresent()) {
            try {
                X509PKIUtil.parseX509Certificate(personalCertificateModelOptional.get().getCertificate());
            } catch (CertificateException e) {
                throw new PersonalCertificateException(INVALID_CERTIFICATE);
            }
        }

        return personalCertificateModelOptional;
    }

    /**
     * @return list of identity models
     * @throws InvalidParseOperationException thrown retrieving certificates fail
     */
    public List<PersonalCertificateModel> findAllPersonalCertificates() throws InvalidParseOperationException {
        return personalRepository.findAll();
    }

    private void createNoPersonalCertificateLogbook(String permission)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, InvalidParseOperationException {
        final LogbookOperationParameters logbookParameters = getLogbookParametersKo();

        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("Certificate", "No certificate");
        msg.put("Permission", permission);
        evDetData.set("Context", msg);

        final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
        logbookOperationsClientFactory.getClient().create(logbookParameters);
    }

    private void createInvalidPersonalCertificateLogbook(String permission)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, InvalidParseOperationException {
        final LogbookOperationParameters logbookParameters = getLogbookParametersKo();

        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("Certificate", "Invalid certificate");
        msg.put("Permission", permission);
        evDetData.set("Context", msg);
        final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
        logbookOperationsClientFactory.getClient().create(logbookParameters);
    }

    private void createInvalidPersonalCertificateLogbook(ParsedCertificate parsedCertificate, String permission)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, InvalidParseOperationException {
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

        return LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            PERSONAL_LOGBOOK_EVENT,
            eip,
            LogbookTypeProcess.CHECK,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(PERSONAL_LOGBOOK_EVENT, StatusCode.KO),
            eip
        );
    }

    @Override
    public CertificateRepository getRepository() {
        return personalRepository;
    }
}
