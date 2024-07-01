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
package fr.gouv.vitam.common.security.filter;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AuthorizationFilterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private RequestAuthorizationValidator requestAuthorizationValidator;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthorizationFilter filter;

    @Test
    public void testDoFilterWhenValidationOk() throws Exception {
        // Given
        doReturn(true).when(requestAuthorizationValidator).checkAuthorizationHeaders(httpServletRequest);

        // When
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Then
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
        verifyNoMoreInteractions(filterChain);
        verifyNoMoreInteractions(httpServletResponse);
    }

    @Test
    public void testDenyRequestWhenValidationFails() throws Exception {
        // Given
        doReturn(false).when(requestAuthorizationValidator).checkAuthorizationHeaders(httpServletRequest);

        // When
        filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Then
        verify(httpServletResponse).sendError(
            eq(Status.UNAUTHORIZED.getStatusCode()),
            eq("{\"Error\":\"Authorization headers check failed!\"}")
        );
        verifyNoMoreInteractions(filterChain);
    }
}
