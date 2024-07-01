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

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import org.apache.commons.codec.binary.Hex;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Helper class for certificate parsing X509 certificates.
 */
public class ParsedCertificate {

    private static final int MAX_CERTIFICATE_LOG_LENGTH = 10240;
    private static final DigestType digestType = DigestType.SHA256;

    private final X509Certificate x509Certificate;
    private final byte[] rawCertificate;
    private final String certificateHash;

    /**
     * Constructor
     *
     * @param x509Certificate
     * @param rawCertificate
     * @param certificateHash
     */
    public ParsedCertificate(X509Certificate x509Certificate, byte[] rawCertificate, String certificateHash) {
        this.x509Certificate = x509Certificate;
        this.rawCertificate = rawCertificate;
        this.certificateHash = certificateHash;
    }

    /**
     * Get x509Certificate
     *
     * @return x509Certificate
     */
    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    /**
     * Get rawCertificate
     *
     * @return rawCertificate
     */
    public byte[] getRawCertificate() {
        return rawCertificate;
    }

    /**
     * Get certificateHash
     *
     * @return certificateHash
     */
    public String getCertificateHash() {
        return certificateHash;
    }

    /**
     * Parses a certificate
     *
     * @param certificate
     * @return the ParsedCertificate
     * @throws PersonalCertificateException
     */
    public static ParsedCertificate parseCertificate(byte[] certificate) throws PersonalCertificateException {
        try {
            X509Certificate x509certificate = X509PKIUtil.parseX509Certificate(certificate);

            byte[] rawCertificate = x509certificate.getEncoded();
            String certificateHash = getCertificateHash(rawCertificate);

            return new ParsedCertificate(x509certificate, rawCertificate, certificateHash);
        } catch (CertificateException ex) {
            throw new PersonalCertificateException(
                "Could not parse certificate. " + toCertificateHexString(certificate),
                ex
            );
        }
    }

    private static String getCertificateHash(byte[] rawDerEncodedCertificate) {
        Digest digest = new Digest(digestType).update(rawDerEncodedCertificate);
        return digest.digestHex();
    }

    /**
     * Converts a certificate to Hex for logging (Truncated to MAX_CERTIFICATE_LOG_LENGTH)
     *
     * @param certificate
     * @return
     */
    public static String toCertificateHexString(byte[] certificate) {
        if (certificate.length > MAX_CERTIFICATE_LOG_LENGTH) {
            byte[] truncatedCertificateToLog = Arrays.copyOf(certificate, MAX_CERTIFICATE_LOG_LENGTH);
            return Hex.encodeHexString(truncatedCertificateToLog) + "... [truncated]";
        } else {
            return Hex.encodeHexString(certificate);
        }
    }
}
