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
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XSRFFilterTest {

    private static HttpServletRequest httpServletRequest;
    private static HttpServletResponse httpServletResponse;
    private static FilterChain filterChain;
    private static XSRFFilter filter;
    private static String SESSIONID = "sessionId";

    @Before
    public void before() throws ServletException {
        filter = new XSRFFilter();

        httpServletRequest = mock(HttpServletRequest.class);
        httpServletResponse = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    public void testDoFilterOK() throws Exception {
        String tokenCSRF = XSRFHelper.generateCSRFToken();

        when(httpServletRequest.getHeader(GlobalDataRest.X_CSRF_TOKEN)).thenReturn(tokenCSRF);
        when(httpServletRequest.getRequestedSessionId()).thenReturn(SESSIONID);
        XSRFFilter.addToken(SESSIONID, tokenCSRF);

        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }
}
