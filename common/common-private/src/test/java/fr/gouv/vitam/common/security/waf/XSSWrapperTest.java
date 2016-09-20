package fr.gouv.vitam.common.security.waf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

public class XSSWrapperTest {
    
    private static HttpServletRequest httpServletRequest;
    private static XSSWrapper wrapper;
    
    @Before
    public void before() throws ServletException {
        httpServletRequest = mock(HttpServletRequest.class);
    }
    
    @Test
    public void testSanitize() throws Exception {
        Enumeration<String> headers;
        Vector<String> header = new Vector<String>();
        header.add("test");
        headers = header.elements();
        
        wrapper = new XSSWrapper(httpServletRequest);
        assertFalse(wrapper.sanitize());
        
        when(httpServletRequest.getHeaderNames()).thenReturn(headers);
        when(httpServletRequest.getHeader("test")).thenReturn("<?php echo\" Hello \" ?>");
        
        when(httpServletRequest.getParameterNames()).thenReturn(headers);
        when(httpServletRequest.getParameter("test")).thenReturn("<script>(.*?)</script>");
        
        wrapper = new XSSWrapper(httpServletRequest);
        assertTrue(wrapper.sanitize());
    }
}
