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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.common.service.X509PKIUtil;
import fr.gouv.vitam.security.internal.rest.repository.CertificateRepository;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * manage certificate.
 */
public class IdentityService extends SecurityService {

    private final IdentityRepository identityRepository;

    public IdentityService(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    /**
     * create certificate
     *
     * @param identityInsertModel Identity certificate to insert
     * @throws CertificateException thrown if certificate parse fail
     * @throws InvalidParseOperationException thrown if insertion fail
     */
    public void createIdentity(IdentityInsertModel identityInsertModel)
        throws CertificateException, InvalidParseOperationException {
        IdentityModel identityModel = new IdentityModel();
        identityModel.setId(GUIDFactory.newGUID().toString());
        identityModel.setContextId(identityInsertModel.getContextId());
        identityModel.setCertificate(identityInsertModel.getCertificate());
        identityModel.setCertificateStatus(CertificateStatus.VALID);

        X509Certificate certificate = X509PKIUtil.parseX509Certificate(identityInsertModel.getCertificate());

        identityModel.setSubjectDN(certificate.getSubjectDN().getName());
        identityModel.setIssuerDN(certificate.getIssuerDN().getName());
        identityModel.setSerialNumber(String.valueOf(certificate.getSerialNumber()));
        identityModel.setExpirationDate(
            LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.fromDate(certificate.getNotAfter()))
        );

        identityRepository.createIdentity(identityModel);
    }

    /**
     * @param identityInsertModel Identity certificate to insert
     * @return the identity model if exists
     * @throws CertificateException thrown if certificate parse fail
     * @throws InvalidParseOperationException thrown retrieving identity fail
     */
    public Optional<IdentityModel> linkContextToIdentity(IdentityInsertModel identityInsertModel)
        throws CertificateException, InvalidParseOperationException {
        X509Certificate x509Certificate = X509PKIUtil.parseX509Certificate(identityInsertModel.getCertificate());

        Optional<IdentityModel> identityModel = identityRepository.findIdentity(
            x509Certificate.getSubjectDN().getName(),
            String.valueOf(x509Certificate.getSerialNumber())
        );

        identityModel.ifPresent(identity -> {
            identity.setContextId(identityInsertModel.getContextId());
            identityRepository.linkContextToIdentity(
                identity.getSubjectDN(),
                identity.getContextId(),
                identity.getSerialNumber()
            );
        });
        return identityModel;
    }

    /**
     * @param certificate the certificate to find
     * @return the identity model if exists
     * @throws CertificateException thrown if certificate parse fail
     * @throws InvalidParseOperationException thrown retrieving certificate fail
     */
    public Optional<IdentityModel> findIdentity(byte[] certificate)
        throws CertificateException, InvalidParseOperationException {
        X509Certificate x509Certificate = X509PKIUtil.parseX509Certificate(certificate);

        Optional<IdentityModel> identityModelOptional = identityRepository.findIdentity(
            x509Certificate.getSubjectDN().getName(),
            String.valueOf(x509Certificate.getSerialNumber())
        );

        // check validity of the retrieved certificate from VITAM DB
        if (identityModelOptional.isPresent()) {
            X509PKIUtil.parseX509Certificate(identityModelOptional.get().getCertificate());
        }

        return identityModelOptional;
    }

    /**
     * @return list of identity models
     * @throws InvalidParseOperationException thrown retrieving certificates fail
     */
    public List<IdentityModel> findAllIdentities() throws InvalidParseOperationException {
        return identityRepository.findAll();
    }

    /**
     * @param contextId the context Id
     * @return true if the context is used by an identity
     */
    public boolean contextIsUsed(String contextId) {
        return identityRepository.contextIsUsed(contextId);
    }

    @Override
    public CertificateRepository getRepository() {
        return identityRepository;
    }
}
