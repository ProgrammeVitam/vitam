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
package fr.gouv.vitam.common.tenant.filter;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantFilterTest {

    private static HttpServletRequest httpServletRequest;
    private static HttpServletResponse httpServletResponse;
    private static FilterChain filterChain;
    private static TenantFilter filter;
    private static Map<String, String> headersMap;

    private static FilterConfig mockFilterConfig;
    private static ServletContext mockedContext;
    private static Enumeration<String> httpServletRequestHeaders;

    private static final String NOT_STATUS_ADMIN_URI = "/containers/containerid";

    @Before
    public void before() throws ServletException, InvalidParseOperationException {
        filter = new TenantFilter();
        mockFilterConfig = mock(FilterConfig.class);
        mockedContext = mock(ServletContext.class);

        List<String> tenants = new ArrayList<>();
        tenants.add("0");
        tenants.add("1");

        JsonNode node = JsonHandler.toJsonNode(tenants);

        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        when(mockedContext.getInitParameter(GlobalDataRest.TENANT_LIST)).thenReturn(node.toString());
        when(mockFilterConfig.getServletContext()).thenReturn(mockedContext);

        filter.init(mockFilterConfig);

        headersMap = new HashMap<>();
        headersMap.put(GlobalDataRest.X_TENANT_ID, "0");
        headersMap.put("FakeTenantId", "25");

        final Vector<String> authorizationHeaders = new Vector<>();
        authorizationHeaders.add(GlobalDataRest.X_TENANT_ID);
        httpServletRequestHeaders = authorizationHeaders.elements();
    }

    @Test
    public void testTenantFilterOK() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(
            headersMap.get(GlobalDataRest.X_TENANT_ID)
        );
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testNotExistingTenantFilterThenUnauthorized() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(headersMap.get("FakeTenantId"));
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).sendError(
            eq(Status.UNAUTHORIZED.getStatusCode()),
            eq("{\"Error\":\"X-Tenant-Id check failed!\"}")
        );
    }

    @Test
    public void testIncorrectTenantsListFilterThenPreconditionFailed() throws Exception {
        when(mockedContext.getInitParameter(GlobalDataRest.TENANT_LIST)).thenReturn("0,1,2");
        when(mockFilterConfig.getServletContext()).thenReturn(mockedContext);

        filter.init(mockFilterConfig);

        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(headersMap.get("FakeTenantId"));
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).sendError(
            eq(Status.PRECONDITION_FAILED.getStatusCode()),
            eq("{\"Error\":\"X-Tenant-Id check failed!\"}")
        );
    }

    @Test
    public void testTenantsListAsIntegerFilterThenOK() throws Exception {
        List<Integer> tenants = new ArrayList<>();
        tenants.add(0);
        tenants.add(1);

        JsonNode node = JsonHandler.toJsonNode(tenants);

        when(mockedContext.getInitParameter(GlobalDataRest.TENANT_LIST)).thenReturn(node.toString());
        when(mockFilterConfig.getServletContext()).thenReturn(mockedContext);

        filter.init(mockFilterConfig);

        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(
            headersMap.get(GlobalDataRest.X_TENANT_ID)
        );
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testTenantStatusFilterOK() throws Exception {
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.STATUS_URL);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testTenantAdminFilterOK() throws Exception {
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.ADMIN_PATH);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    public void testTenantFilterEmptyHeadersPreconditionFailed() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(null);
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        when(httpServletResponse.getWriter()).thenReturn(mock(PrintWriter.class));
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).sendError(
            eq(Status.PRECONDITION_FAILED.getStatusCode()),
            eq("{\"Error\":\"X-Tenant-Id check failed!\"}")
        );
    }

    @Test
    public void testTenantFilterEmptyXTenantIdHeaderPreconditionFailed() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn(null);
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).sendError(
            eq(Status.PRECONDITION_FAILED.getStatusCode()),
            eq("{\"Error\":\"X-Tenant-Id check failed!\"}")
        );
    }

    @Test
    public void testTenantFilterXTenantIdAsStringHeaderPreconditionFailed() throws Exception {
        when(httpServletRequest.getHeaderNames()).thenReturn(httpServletRequestHeaders);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID)).thenReturn("thisIsAString");
        when(httpServletRequest.getRequestURI()).thenReturn(NOT_STATUS_ADMIN_URI);
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        verify(httpServletResponse).sendError(
            eq(Status.PRECONDITION_FAILED.getStatusCode()),
            eq("{\"Error\":\"X-Tenant-Id check failed!\"}")
        );
    }
}
