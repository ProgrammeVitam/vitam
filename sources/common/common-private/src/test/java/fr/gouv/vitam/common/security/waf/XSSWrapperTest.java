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
