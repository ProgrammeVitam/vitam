package fr.gouv.vitam.common.xsrf.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;

import org.glassfish.grizzly.servlet.FilterConfigImpl;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.GlobalDataRest;

public class XSRFFilterTest {

    private static HttpServletRequest httpServletRequest;
    private static HttpServletResponse httpServletResponse;
    private static FilterChain filterChain;
    private static XSRFFilter filter;
    private static String SESSIONID = "sessionId";
    private static final String JSESSIONID = "JSESSIONID";

    @Before
    public void before() throws ServletException {

        filter = new XSRFFilter();
        filter.init(new FilterConfigImpl(null));

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
