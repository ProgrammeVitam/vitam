/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.auth.web.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import org.apache.shiro.ShiroException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 */
public class CertUtils {

    public static final String REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE = "Personae";


    /**
     * Extract certificate from request or from header
     *
     * @param request
     * @param useHeader
     * @return the extracted certificate
     */
    public static X509Certificate[] extractCert(ServletRequest request, boolean useHeader) {
        Object attribute = request.getAttribute("javax.servlet.request.X509Certificate");

        X509Certificate[] clientCertChain = (null == attribute) ? null : (X509Certificate[]) attribute;
        //If request attribute certificate not found, try to get certificate from header
        if ((clientCertChain == null || clientCertChain.length < 1) && useHeader) {

            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            String pem = httpRequest.getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
            if (null != pem) {

                try {

                    /*
                     * TODO use instead the official PemReader that implement RFC 7468 when released
                     * Implementation of RFC 7468 (PEM can have empty space, an other characters
                     * PemReader is RFC 1421 and do not handle character other than \n
                     * This implementation replace those characters with \n
                     */
                    pem = pem.replaceAll("-----BEGIN CERTIFICATE-----", "-----BEGIN_CERTIFICATE-----");
                    pem = pem.replaceAll("-----END CERTIFICATE-----", "-----END_CERTIFICATE-----");
                    pem = pem.replace((char) 0x09, (char) 0x0A); // Line Feed
                    pem = pem.replace((char) 0x0B, (char) 0x0A); // Vertical Tab
                    pem = pem.replace((char) 0x0C, (char) 0x0A); // Form Feed
                    pem = pem.replace((char) 0x20, (char) 0x0A); // Space
                    pem = pem.replace((char) 0x0D, (char) 0x0A); // Carriage Return
                    pem = pem.replaceAll("-----BEGIN_CERTIFICATE-----", "-----BEGIN CERTIFICATE-----");
                    pem = pem.replaceAll("-----END_CERTIFICATE-----", "-----END CERTIFICATE-----");

                    final InputStream pemStream = new ByteArrayInputStream(pem.getBytes());
                    final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    final X509Certificate cert = (X509Certificate) cf.generateCertificate(pemStream);
                    clientCertChain = new X509Certificate[] {cert};
                } catch (Exception ce) {
                    throw new ShiroException(ce);
                }
            }


        }

        return clientCertChain;
    }
}
