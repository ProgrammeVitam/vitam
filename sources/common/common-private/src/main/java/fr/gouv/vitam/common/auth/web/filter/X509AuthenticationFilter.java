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
/*
 * Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
package fr.gouv.vitam.common.auth.web.filter;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;

import static fr.gouv.vitam.common.auth.web.filter.CertUtils.REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 *
 *
 * This implementation enable authentication by header if no certificate found in the request attribute
 * and the useHeader parameter is true
 * The checked header is "X-SSL-CLIENT-CERT" and the value must be a valid public certificate as pem formatted string
 *
 * To enable this filter, replace in shiro.ini the key x509 to be equal to the current filter as follow.
 * x509 = fr.gouv.vitam.common.auth.web.filter.X509AuthenticationFilter
 *
 * To enable use if header check in shiro.ini add the following
 * x509.useHeader = true
 * Be careful, passing a header certificate is not fully secure (Possible injection during the routing).
 *
 * We recommend the use of request attribute instead of header.
 */
public class X509AuthenticationFilter extends AuthenticatingFilter {

    private boolean useHeader = false;

    public void setUseHeader(boolean useHeader) {
        this.useHeader = useHeader;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        if (!executeLogin(request, response)) {
            ((HttpServletResponse) response).sendError(403, "Access Denied . Need a valid TLS client certificate");
            return false;
        }
        return true;
    }

    protected boolean onLoginSuccess(
        AuthenticationToken token,
        Subject subject,
        ServletRequest request,
        ServletResponse response
    ) throws Exception {
        X509AuthenticationToken x509Token = (X509AuthenticationToken) token;
        X509Certificate x509Certificate = x509Token.getX509Certificate();
        byte[] derEncodedCertificate = x509Certificate.getEncoded();
        String base64Certificate = BaseXx.getBase64(derEncodedCertificate);
        request.setAttribute(REQUEST_PERSONAL_CERTIFICATE_ATTRIBUTE, base64Certificate);

        return true;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        X509Certificate[] clientCertChain = CertUtils.extractCert(request, useHeader);

        if (clientCertChain == null || clientCertChain.length < 1) {
            throw new ShiroException("Request do not contain any X509Certificate ");
        }

        return new X509AuthenticationToken(clientCertChain, getHost(request));
    }
}
