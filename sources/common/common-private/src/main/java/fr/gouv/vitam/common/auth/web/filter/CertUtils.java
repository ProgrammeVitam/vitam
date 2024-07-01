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
package fr.gouv.vitam.common.auth.web.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.ShiroException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;

/**
 *
 */
public class CertUtils {

    public static final String REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE = "Personae";
    public static final String JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE = "javax.servlet.request.X509Certificate";
    private static final String NGINX_URL_ENCODED_PEM_HEADER_SIGNATURE = "-----BEGIN%20CERTIFICATE-----";
    private static final String BEGIN_CERT_PREFIX = "-----BEGIN";
    private static final String END_CERT_PREFIX = "-----END";
    private static final String NULL_HTTPD_HEADER = "(null)";

    private static final AlertService alertService = new AlertServiceImpl();

    /**
     * Extract certificate from request or from header
     *
     * @param request
     * @param useHeader
     * @return the extracted certificate
     */
    public static X509Certificate[] extractCert(ServletRequest request, boolean useHeader) {
        Object attribute = request.getAttribute(JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);

        X509Certificate[] clientCertChain = (null == attribute) ? null : (X509Certificate[]) attribute;
        //If request attribute certificate not found, try to get certificate from header
        if (ArrayUtils.isNotEmpty(clientCertChain)) {
            return clientCertChain;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        String pem = httpRequest.getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        if (null != pem && !NULL_HTTPD_HEADER.equals(pem)) {
            if (!useHeader) {
                alertService.createAlert(
                    "SEVERE. Illegal access with " + GlobalDataRest.X_SSL_CLIENT_CERT + " header. Forged header attack?"
                );
                return null;
            }

            try {
                if (hasNginxSignature(pem)) {
                    pem = extractNginxEncodedCertificateAsPEM(pem);
                } else {
                    pem = extractHttpdEncodedCertificateAsPEM(pem);
                }

                final InputStream pemStream = new ByteArrayInputStream(pem.getBytes());
                final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                final X509Certificate cert = (X509Certificate) cf.generateCertificate(pemStream);
                return new X509Certificate[] { cert };
            } catch (Exception ce) {
                alertService.createAlert("Could not read certificate" + ce.getMessage());
                throw new ShiroException(ce);
            }
        }

        return null;
    }

    private static boolean hasNginxSignature(String pem) {
        return pem.startsWith(NGINX_URL_ENCODED_PEM_HEADER_SIGNATURE);
    }

    private static String extractNginxEncodedCertificateAsPEM(String pem) {
        // NGINX header: proxy_set_header X-SSL-CLIENT-CERT $ssl_client_escaped_cert;
        // Format : url-encoded PEM certificate (url-encoded base64)
        // Ex: "-----BEGIN%20CERTIFICATE-----%0AMIIGejCCBGKgAwIBAgIBBTANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJmcjEM%0AMAoGA1U...Vsh8vQ%2FAbF3aYeWcKOa%0A-----END%20CERTIFICATE-----%0A"
        pem = URLDecoder.decode(pem, StandardCharsets.UTF_8);
        return pem;
    }

    private static String extractHttpdEncodedCertificateAsPEM(String pem) {
        // Apache httpd header: RequestHeader set X-SSL-CLIENT-CERT "%{SSL_CLIENT_CERT}s"
        // Expected format : PEM certificate (base64 charset + spacing)
        // Ex: "-----BEGIN CERTIFICATE----- MIIGfzCCBGegAwIBAgIBCDANBgkqhkiG9w0BAQsFADB7MQswCQYDVQQGEwJmcjEM s5pz...N7zg= -----END CERTIFICATE-----"

        StringTokenizer stringTokenizer = new StringTokenizer(pem, " ", false);
        StringBuilder stringBuilder = new StringBuilder(pem.length());
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            switch (token) {
                case BEGIN_CERT_PREFIX:
                case END_CERT_PREFIX:
                    // "-----BEGIN CERTIFICATE----- ==> needs a spacing here
                    // -----END CERTIFICATE-----    ==> needs a spacing here
                    stringBuilder.append(token).append(' ');
                    break;
                default:
                    // Needs a new line here
                    stringBuilder.append(token).append('\n');
                    break;
            }
        }
        pem = stringBuilder.toString();
        return pem;
    }
}
