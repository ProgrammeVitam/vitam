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
package fr.gouv.vitam.security.internal.common.service;

import java.io.ByteArrayInputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Utility class for X.509 PKI certificate and CRL profile.
 */
public class X509PKIUtil {

    /**
     * generate cerificate using X.509 Certifcate factory
     *
     * @param certificate array of byte representing the certificate file
     * @return
     * @throws CertificateException thrown if certificate parse fail
     */
    public static X509Certificate parseX509Certificate(byte[] certificate) throws CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(
            new ByteArrayInputStream(certificate)
        );

        x509Certificate.checkValidity();

        return x509Certificate;
    }

    public static boolean validateX509CRL(X509CRL crl) throws CRLException {
        Date now = new Date();
        return (
            (crl.getThisUpdate() != null && now.after(crl.getThisUpdate())) &&
            (crl.getNextUpdate() != null && now.before(crl.getNextUpdate()))
        );
    }

    public static X509CRL parseX509CRLCertificate(byte[] crlCertificate) throws CertificateException, CRLException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509CRL) certificateFactory.generateCRL(new ByteArrayInputStream(crlCertificate));
    }
}
