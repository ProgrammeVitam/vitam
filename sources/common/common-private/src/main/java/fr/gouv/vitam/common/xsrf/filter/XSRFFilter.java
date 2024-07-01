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
package fr.gouv.vitam.common.xsrf.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XSRFFilter implements Filter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(XSRFFilter.class);

    private static final String CSRF_STATE_TOKEN_DOES_NOT_MATCH_ONE_PROVIDED =
        "CSRF state token does not match one provided";
    private static Map<String, String> tokenMap = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestURI = req.getRequestURI();
        if (isUriNotProtected(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        String xhrToken = req.getHeader(GlobalDataRest.X_CSRF_TOKEN);
        String sessionId = req.getRequestedSessionId();
        String token = tokenMap.get(sessionId);

        if (sessionId != null && token != null && token.equals(xhrToken)) {
            deleteTokenWhenLogout(req, sessionId);
            chain.doFilter(request, response);
        } else {
            deleteTokenWhenLogout(req, sessionId);
            LOGGER.error(CSRF_STATE_TOKEN_DOES_NOT_MATCH_ONE_PROVIDED);
            final HttpServletResponse newResponse = (HttpServletResponse) response;
            newResponse.sendError(Status.FORBIDDEN.getStatusCode(), CSRF_STATE_TOKEN_DOES_NOT_MATCH_ONE_PROVIDED);
            StreamUtils.closeSilently(request.getInputStream());
        }
    }

    @Override
    public void destroy() {
        // Empty
    }

    public static void addToken(String sessionId, String token) {
        tokenMap.put(sessionId, token);
    }

    private void deleteTokenWhenLogout(HttpServletRequest req, String sessionId) {
        if (req.getRequestURI().contains(VitamConfiguration.LOGOUT_URL)) {
            tokenMap.remove(sessionId);
        }
    }

    private boolean isUriNotProtected(String requestURI) {
        return (
            requestURI.contains(VitamConfiguration.LOGIN_URL) ||
            requestURI.contains(VitamConfiguration.LOGOUT_URL) ||
            requestURI.contains(VitamConfiguration.TENANTS_URL) ||
            requestURI.contains(VitamConfiguration.MESSAGES_LOGBOOK_URL) ||
            requestURI.contains(VitamConfiguration.OBJECT_DOWNLOAD_URL) ||
            requestURI.contains(VitamConfiguration.DIP_EXPORT_URL) ||
            requestURI.contains(VitamConfiguration.SECURE_MODE_URL) ||
            requestURI.contains(VitamConfiguration.ADMIN_TENANT_URL) ||
            requestURI.contains(VitamConfiguration.PERMISSIONS_URL)
        );
    }
}
