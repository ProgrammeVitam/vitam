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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RequestAuthorizationValidatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private RequestAuthorizationValidator requestAuthorizationValidator;

    @Before
    public void before() throws ServletException {
        VitamConfiguration.setSecret("vitamsecret");
        VitamConfiguration.setRequestTimeAlertThrottlingDelay(60);
    }

    @Test
    public void testDoFilterOK() throws Exception {
        // Given
        setRequestHeaders(httpServletRequest);
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isTrue();
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testDoFilterStatusOK() {
        // Given
        setRequestHeaders(httpServletRequest);
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.STATUS_URL);
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isTrue();
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testDoFilterAdminStatusOK() throws Exception {
        // Given
        setRequestHeaders(httpServletRequest);
        when(httpServletRequest.getHeader(GlobalDataRest.X_TIMESTAMP)).thenReturn(null);
        when(httpServletRequest.getHeader(GlobalDataRest.X_PLATFORM_ID)).thenReturn(null);
        when(httpServletRequest.getRequestURI()).thenReturn(VitamConfiguration.ADMIN_PATH);
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isTrue();
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testDoFilterNotAcceptableNoHeaders() throws Exception {
        // Given no request headers
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isFalse();
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testOldButNonExpiredRequestThenOK() {
        // Given
        setRequestHeaders(httpServletRequest);
        logicalClock.logicalSleep(50, ChronoUnit.SECONDS);

        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isTrue();
        verify(alertService).createAlert(contains("Timestamp check failed"));
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testExpiredRequestThenUnauthorized() {
        // Given
        setRequestHeaders(httpServletRequest);
        logicalClock.logicalSleep(61, ChronoUnit.SECONDS);
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);

        // Then
        assertThat(result).isFalse();
        verify(alertService).createAlert(contains("Critical Timestamp check failed"));
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testMultipleTooManyExpiredRequestsThenLimitAlertsPerPeriod() {
        // Given
        setRequestHeaders(httpServletRequest);
        when(httpServletRequest.getRequestURI()).thenReturn("/containers/continerid");
        when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET);

        // When
        logicalClock.logicalSleep(60, ChronoUnit.SECONDS);

        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.SECONDS);
            boolean result = requestAuthorizationValidator.checkAuthorizationHeaders(httpServletRequest);
            results.add(result);
        }

        // Then
        assertThat(results).allMatch(result -> !result);
        verify(alertService, times(4)).createAlert(contains("Critical Timestamp check failed"));
        verifyNoMoreInteractions(alertService);
    }

    private void setRequestHeaders(HttpServletRequest httpServletRequest) {
        Map<String, String> headersMap = AuthorizationFilterHelper.getAuthorizationHeaders(
            HttpMethod.GET,
            "/containers/continerid"
        );
        when(httpServletRequest.getHeader(GlobalDataRest.X_TIMESTAMP)).thenReturn(
            headersMap.get(GlobalDataRest.X_TIMESTAMP)
        );
        when(this.httpServletRequest.getHeader(GlobalDataRest.X_PLATFORM_ID)).thenReturn(
            headersMap.get(GlobalDataRest.X_PLATFORM_ID)
        );
    }
}
