package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class BasicAuthenticationFilterTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BasicAuthenticationFilterTest.class);

    private static final AuthenticationLevel AUTHENTICATION_LEVEL = AuthenticationLevel.BASIC_AUTHENT;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ResourceInfo resourceInfo;

    @Mock
    private FeatureContext featureContext;

    @Mock
    private AdminManagementConfiguration configuration;

    @Captor
    private ArgumentCaptor<EndpointAuthenticationFilter> endpointAuthenticationFilter;

    @Before
    public void setup() {
        when(resourceInfo.getResourceClass())
            .thenReturn((Class) BasicAuthenticationFilterTest.class);
    }

    @Test
    public void checkEndpointAuthenticationFilterRegistration() throws Exception {

        // mock resource info.
        when(resourceInfo.getResourceMethod())
            .thenReturn(BasicAuthenticationFilterTest.class.getMethod("basedVitamAuthenticationMethod"));

        // instanciate BasicAuthenticationFilter class
        BasicAuthenticationFilter instance = new BasicAuthenticationFilter(configuration);
        instance.configure(resourceInfo, featureContext);

        verify(featureContext)
            .register(endpointAuthenticationFilter.capture(), eq(Priorities.AUTHORIZATION + 10));
        verifyNoMoreInteractions(featureContext);
        Assert.assertEquals(AUTHENTICATION_LEVEL, endpointAuthenticationFilter.getValue().getAuthentLevel());
    }

    @Test
    public void checkEndpointAuthenticationFilterRegistrationUnusedVitamAuthentication() throws Exception {

        // mock resource info.
        when(resourceInfo.getResourceMethod())
            .thenReturn(this.getClass().getMethod("UnusedVitamAuthenticationMethod"));

        // instanciate BasicAuthenticationFilter class
        BasicAuthenticationFilter instance = new BasicAuthenticationFilter(configuration);
        instance.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(any(), anyInt());
    }

    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public void basedVitamAuthenticationMethod() {
        LOGGER.debug("Method using Vitam basic authentication annotation.");
    }

    public void UnusedVitamAuthenticationMethod() {
        LOGGER.debug("No use of the Vitam basic authentication annotation.");
    }

}
