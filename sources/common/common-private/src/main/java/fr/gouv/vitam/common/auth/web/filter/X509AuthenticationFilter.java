/**
 * Copyright Paul Merlin 2011 (Apache Licence v2.0)
 * 
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
package fr.gouv.vitam.common.auth.web.filter;

import java.security.cert.X509Certificate;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import fr.gouv.vitam.common.auth.core.authc.X509AuthenticationToken;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
public class X509AuthenticationFilter extends AuthenticatingFilter {

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
        throws Exception {

        if (!executeLogin(request, response)) {
            ((HttpServletResponse) response).sendError(403, "Access Denied . Need a valid TLS client certificate");
            return false;
        }
        return true;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response)
        throws Exception {

        X509Certificate[] clientCertChain =
            (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (clientCertChain == null || clientCertChain.length < 1) {
            throw new ShiroException("Request do not contain any X509Certificate ");
        }
        return new X509AuthenticationToken(clientCertChain, getHost(request));

    }


}
