/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.common.security.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response.Status;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthorizationFilterTest {

    private static HttpServletRequest httpServletRequest;
    private static HttpServletResponse httpServletResponse;
    private static FilterChain filterChain;
    private static AuthorizationFilter filter;
    private static Map<String, String> headersMap;
    private static Enumeration<String> httpServletRequestHeaders;

    @Before
    public void before() throws ServletException {

        filter = new AuthorizationFilter();

        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        final VitamConfiguration vitamConfiguration = VitamConfiguration.getConfiguration();
        VitamConfiguration.setSecret("vitamsecret");

        headersMap = AuthorizationFilterHelper.getAuthorizationHeaders(HttpMethod.GET, "/containers/continerid");
        final Vector<String> authorizationHeaders = new Vector<>();
        authorizationHeaders.add(GlobalDataRest.X_TIMESTAMP);
        authorizationHeaders.add(GlobalDataRest.X_PLATFORM_ID);
        httpServletRequestHeaders = authorizationHeaders.elements();
    }

    @Test
    public void testDoFilterOK() throws Exception {

        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TIMESTAMP))
            .thenReturn(headersMap.get(GlobalDataRest.X_TIMESTAMP));
        when(httpServletRequest.getHeader(GlobalDataRest.X_PLATFORM_ID))
            .thenReturn(headersMap.get(GlobalDataRest.X_PLATFORM_ID));
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }


    @Test
    public void testDoFilterStatusOK() throws Exception {

        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.STATUS_URL);
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testDoFilterAdminStatusOK() throws Exception {

        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.ADMIN_PATH);
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testDoFilterNotAcceptableNoHeaders() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(null);
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(httpServletResponse).setStatus(Status.UNAUTHORIZED.getStatusCode());
    }

}
