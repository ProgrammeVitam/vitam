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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.service.CRLService;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.repository.CertificateCRLCheckStateUpdater;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import org.bson.Document;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * {@inheritDoc}
 */
public class CRLServiceImpl implements CRLService {

    private final IdentityRepository identityRepository;
    private final PersonalRepository personalRepository;
    private final AlertService alertService;

    public CRLServiceImpl(IdentityRepository identityRepository, PersonalRepository personalRepository) {
        this(identityRepository, personalRepository, new AlertServiceImpl());
    }

    public CRLServiceImpl(
        IdentityRepository identityRepository,
        PersonalRepository personalRepository,
        AlertService alertService
    ) {
        this.identityRepository = identityRepository;
        this.personalRepository = personalRepository;
        this.alertService = alertService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkIdentityWithCRL(byte[] crlCert)
        throws CertificateException, InvalidParseOperationException, CRLException {
        X509CRL crl = X509PKIUtil.parseX509CRLCertificate(crlCert);

        if (!X509PKIUtil.validateX509CRL(crl)) {
            String alertMessage =
                "CRL issued by " +
                crl.getIssuerDN().getName() +
                " has invalid dates (issued after now or expired before now) : thisUpdate=" +
                crl.getThisUpdate() +
                " ,nextUpdate=" +
                crl.getNextUpdate();
            alertService.createAlert(alertMessage);
            throw new CRLException(alertMessage);
        }

        checkAndUpdateCertificatesByType(crl, identityRepository);
        checkAndUpdateCertificatesByType(crl, personalRepository);
    }

    private void checkAndUpdateCertificatesByType(
        X509CRL crl,
        CertificateCRLCheckStateUpdater<?> crlCheckerRepositoryImplementer
    ) throws InvalidParseOperationException, CertificateException {
        FindIterable<Document> crlCAIdentitiesDocs = crlCheckerRepositoryImplementer.findCertificate(
            crl.getIssuerDN().getName(),
            CertificateStatus.VALID
        );
        MongoCursor<Document> crlCAIdentities = crlCAIdentitiesDocs.iterator();

        List<String> crtRevocatedList = new ArrayList<>();
        List<String> crtExpiredList = new ArrayList<>();

        while (crlCAIdentities.hasNext()) {
            CertificateBaseModel certificateModel = BsonHelper.fromDocumentToObject(
                crlCAIdentities.next(),
                crlCheckerRepositoryImplementer.getEntityModelType()
            );

            try {
                X509Certificate cert = X509PKIUtil.parseX509Certificate(certificateModel.getCertificate());
                if (crl.isRevoked(cert)) {
                    crtRevocatedList.add(certificateModel.getId());
                    alertService.createAlert(
                        VitamLogLevel.WARN,
                        "Certificate " + certificateModel.getSubjectDN() + " was revoked by CRL"
                    );
                }
            } catch (CertificateExpiredException e) {
                crtExpiredList.add(certificateModel.getId());
                alertService.createAlert(
                    VitamLogLevel.WARN,
                    "Certificate " + certificateModel.getSubjectDN() + " is expired"
                );
            }
        }

        if (!crtExpiredList.isEmpty()) {
            crlCheckerRepositoryImplementer.updateCertificateState(crtExpiredList, CertificateStatus.EXPIRED);
        }

        if (!crtRevocatedList.isEmpty()) {
            crlCheckerRepositoryImplementer.updateCertificateState(crtRevocatedList, CertificateStatus.REVOKED);
        }
    }
}
