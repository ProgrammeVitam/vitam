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
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class PersonalCertificateService {

    public static DigestType digestType = DigestType.SHA256;

    private static final String INVALID_CERTIFICATE = "Invalid certificate";
    private final static String NO_CERTIFICTATE_MESSAGE = "No certificate transmitted";
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
    public void createIdentity(byte[] certificate)
        throws CertificateException, InvalidParseOperationException {

        PersonalCertificateModel personalModel = new PersonalCertificateModel();
        personalModel.setId(GUIDFactory.newGUID().toString());

        X509Certificate x509certificate = parseCertificate(certificate);

        byte[] rawDerEncodedCertificate = x509certificate.getEncoded();

        personalModel.setCertificate(rawDerEncodedCertificate);

        personalModel.setSubjectDN(x509certificate.getSubjectDN().getName());
        personalModel.setIssuerDN(x509certificate.getIssuerDN().getName());
        personalModel.setSerialNumber(x509certificate.getSerialNumber());

        Digest digest = new Digest(DigestType.SHA256).update(rawDerEncodedCertificate);

        personalModel.setCertificateHash(digest.toString());

        personalRepository.createPersonalCertificate(personalModel);
    }


    public void checkPersonalCertificateExistence(byte[] certificate)
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException {

        X509Certificate x509certificate;
        if (certificate == null) {
            LOGGER.error(INVALID_CERTIFICATE);

            logNoPersonalCertificate();
            throw new PersonalCertificateException(NO_CERTIFICTATE_MESSAGE);
        }

        Optional<PersonalCertificateModel> model;

        try {

            x509certificate = parseCertificate(certificate);
            byte[] rawDerEncodedCertificate = x509certificate.getEncoded();

            Digest digest = new Digest(digestType).update(rawDerEncodedCertificate);

            model = personalRepository.findIPersonalCertificateByHash(digest.toString());
        } catch (CertificateException e) {
            //FIX ME HERE Logbase 64 tuncated
            LOGGER.error(INVALID_CERTIFICATE + certificate, e);
            logNoPersonalCertificate();
            throw new PersonalCertificateException(INVALID_CERTIFICATE);
        }

        if (model.isPresent()) {
            // FIXE ME story a creer : tracer certificat corrects identifiés.
            LOGGER.debug("Good message change it");
            return;
        }
        logInvalidPersonalCertificate(x509certificate);

        throw new PersonalCertificateException(INVALID_CERTIFICATE);
    }


    private void logNoPersonalCertificate()
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

    private void logInvalidPersonalCertificate(X509Certificate x509certificate)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        InvalidParseOperationException {


        final LogbookOperationParameters logbookParameters = getLogbookParametersKo();

        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("CertificateSn", x509certificate.getSerialNumber().toString());
        msg.put("CertificateSubjectDN", x509certificate.getSubjectDN().toString());

        msg.put("Permissions", "Change It");
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

    private X509Certificate parseCertificate(byte[] certificate) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificate));
    }

}
