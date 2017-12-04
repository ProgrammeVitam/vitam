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
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;

import java.security.cert.CertificateException;
import java.util.Optional;

/**
 * manage certificate.
 */
public class IdentityService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IdentityService.class);

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

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(identityInsertModel.getCertificate());

        if (identityRepository.findIdentity(parsedCertificate.getCertificateHash()).isPresent()) {
            LOGGER.info("Identity certificate already exists {0}", parsedCertificate.getCertificateHash());
            return;
        }

        LOGGER.info("Identity certificate does not exist {0}. Creating it...", parsedCertificate.getCertificateHash());

        IdentityModel identityModel = new IdentityModel();
        identityModel.setId(GUIDFactory.newGUID().toString());
        identityModel.setContextId(identityInsertModel.getContextId());
        identityModel.setCertificate(parsedCertificate.getRawCertificate());

        identityModel.setSubjectDN(parsedCertificate.getX509Certificate().getSubjectDN().getName());
        identityModel.setIssuerDN(parsedCertificate.getX509Certificate().getIssuerDN().getName());
        identityModel.setSerialNumber(parsedCertificate.getX509Certificate().getSerialNumber());

        identityModel.setCertificateHash(parsedCertificate.getCertificateHash());

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

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(identityInsertModel.getCertificate());

        Optional<IdentityModel> identityModel = identityRepository.findIdentity(
            parsedCertificate.getCertificateHash());

        identityModel.ifPresent(identity -> {
            identity.setContextId(identityInsertModel.getContextId());
            identityRepository.linkContextToIdentity(parsedCertificate.getCertificateHash(), identity.getContextId());
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

        ParsedCertificate parsedCertificate = ParsedCertificate.parseCertificate(certificate);

        return identityRepository
            .findIdentity(parsedCertificate.getCertificateHash());
    }
}
