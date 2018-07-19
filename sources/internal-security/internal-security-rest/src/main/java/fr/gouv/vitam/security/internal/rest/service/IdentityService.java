/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * manage certificate.
 */
public class IdentityService {

    private IdentityRepository identityRepository;

    public IdentityService(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    /**
     * create certificate
     *
     * @param identityInsertModel
     * @throws CertificateException
     * @throws InvalidParseOperationException
     */
    public void createIdentity(IdentityInsertModel identityInsertModel)
        throws CertificateException, InvalidParseOperationException {

        IdentityModel identityModel = new IdentityModel();
        identityModel.setId(GUIDFactory.newGUID().toString());
        identityModel.setContextId(identityInsertModel.getContextId());
        identityModel.setCertificate(identityInsertModel.getCertificate());

        X509Certificate certificate = parseCertificate(identityInsertModel.getCertificate());

        identityModel.setSubjectDN(certificate.getSubjectDN().getName());
        identityModel.setIssuerDN(certificate.getIssuerDN().getName());
        identityModel.setSerialNumber(certificate.getSerialNumber());

        identityRepository.createIdentity(identityModel);
    }

    /**
     * @param identityInsertModel
     * @return
     * @throws CertificateException
     * @throws InvalidParseOperationException
     */
    public Optional<IdentityModel> linkContextToIdentity(IdentityInsertModel identityInsertModel)
        throws CertificateException, InvalidParseOperationException {
        X509Certificate x509Certificate = parseCertificate(identityInsertModel.getCertificate());

        Optional<IdentityModel> identityModel = identityRepository.findIdentity(
            x509Certificate.getSubjectDN().getName(), x509Certificate.getSerialNumber());

        identityModel.ifPresent(identity -> {
            identity.setContextId(identityInsertModel.getContextId());
            identityRepository.linkContextToIdentity(identity.getSubjectDN(), identity.getContextId(),
                identity.getSerialNumber());
        });
        return identityModel;
    }

    /**
     * @param certificate
     * @return
     * @throws CertificateException
     * @throws InvalidParseOperationException
     */
    public Optional<IdentityModel> findIdentity(byte[] certificate)
        throws CertificateException, InvalidParseOperationException {
        X509Certificate x509Certificate = parseCertificate(certificate);
        return identityRepository
            .findIdentity(x509Certificate.getSubjectDN().getName(), x509Certificate.getSerialNumber());
    }

    private X509Certificate parseCertificate(byte[] certificate) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificate));
    }

    /**
     * @param contextId
     * @return true if the context is used by an identity
     */
    public boolean contextIsUsed(String contextId) {
        return identityRepository.contextIsUsed(contextId);
    }

}
