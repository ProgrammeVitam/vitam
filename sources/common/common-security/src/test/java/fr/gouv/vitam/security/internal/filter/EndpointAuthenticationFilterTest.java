package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.BasicAuthModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enpoint authentication filter tests.
 */
@RunWithCustomExecutor
@RunWith(MockitoJUnitRunner.class)
public class EndpointAuthenticationFilterTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EndpointAuthenticationFilterTest.class);

    private static final AuthenticationLevel AUTHENTICATION_LEVEL = AuthenticationLevel.BASIC_AUTHENT;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private AdminManagementConfiguration configuration;

    @Mock
    private ContainerRequestContext containerRequestContext;

    @Captor
    private ArgumentCaptor<ContainerRequestContext> contextArgumentCaptor;

    @InjectMocks
    @Spy
    private EndpointAuthenticationFilter instance;

    @Before
    public void setup() {

        // Instanciate Vitam configuration credentials.
        List<BasicAuthModel> basicAuthConfig = Arrays.asList(new BasicAuthModel("adminUserName", "adminPassword"));

        // mock admin basic authentication informations.
        when(configuration.getAdminBasicAuth())
            .thenReturn(basicAuthConfig);
    }

    @Test
    @RunWithCustomExecutor
    public void testBasicAuthentication_Success() throws Exception {

        // Encode to Base64 format of (adminUserName:adminPassword) -> Basic YWRtaW5Vc2VyTmFtZTphZG1pblBhc3N3b3Jk
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic YWRtaW5Vc2VyTmFtZTphZG1pblBhc3N3b3Jk");

        // mock context headers.
        when(containerRequestContext.getHeaders())
            .thenReturn(headers);

        LOGGER.debug(String.format("headers informations : %s", headers));
        instance.filter(containerRequestContext);

        // verify number of the filter method call and the value of the parameter.
        verify(instance, times(1))
            .filter(contextArgumentCaptor.capture());
        Assert.assertEquals(headers, contextArgumentCaptor.getValue().getHeaders());
    }

    @Test
    @RunWithCustomExecutor
    public void testBasicAuthenticationFailed_WrongCredentials() throws Exception {

        // Wrong Encode to Base64 format of (adminUserName:adminPassword)
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.AUTHORIZATION.toString(), "Basic YWRtaW5Vc2VyTmFtZTphZG1pblBhc3N3bXXX");
        LOGGER.debug(String.format("headers informations : %s", headers));

        // mock context headers.
        when(containerRequestContext.getHeaders())
            .thenReturn(headers);

        // verify type and message of the thrown Exception.
        assertThatThrownBy(() -> instance.filter(containerRequestContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("VitamAuthentication failed: Wrong credentials");
    }

    @Test
    @RunWithCustomExecutor
    public void testBasicAuthenticationFailed_MissingInfos() throws Exception {

        // Wrong Encode to Base64 format of (adminUserName:adminPassword)
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.AUTHORIZATION.toString(), "XXX YWRtaW5Vc2VyTmFtZTphZG1pblBhc3N3bXXX");
        LOGGER.debug(String.format("headers informations : %s", headers));

        // mock context headers.
        when(containerRequestContext.getHeaders())
            .thenReturn(headers);

        // verify type and message of the thrown Exception.
        assertThatThrownBy(() -> instance.filter(containerRequestContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("VitamAuthentication failed: VitamAuthentication informations are missing.");
    }

}
