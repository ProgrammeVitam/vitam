package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.security.rest.Secured;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthorizationFilterTest {

    @Test
    public void checkEndpointAuthorizationFilterRegistrationForSecuredMethod() throws Exception {

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(AuthorizationFilterTest.class.getMethod("MySecuredMethod"));
        when(resourceInfo.getResourceClass()).thenReturn((Class)AuthorizationFilterTest.class);

        FeatureContext context = mock(FeatureContext.class);

        AuthorizationFilter instance = new AuthorizationFilter();
        instance.configure(resourceInfo, context);

        ArgumentCaptor<EndpointAuthorizationFilter> endpointAuthorizationFilterArgumentCaptor = ArgumentCaptor.forClass(EndpointAuthorizationFilter.class);
        verify(context, only()).register(endpointAuthorizationFilterArgumentCaptor.capture(), Matchers.eq(Priorities.AUTHORIZATION));
        Assert.assertEquals("my_permission", endpointAuthorizationFilterArgumentCaptor.getValue().getPermission());
    }

    @Test
    public void checkNoEndpointAuthorizationFilterRegistrationForUnsecuredMethod() throws Exception {

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(this.getClass().getMethod("MyUnsecuredMethod"));
        when(resourceInfo.getResourceClass()).thenReturn((Class)AuthorizationFilterTest.class);

        FeatureContext context = mock(FeatureContext.class);

        AuthorizationFilter instance = new AuthorizationFilter();
        instance.configure(resourceInfo, context);

        verify(context, never()).register(any(EndpointAuthorizationFilter.class), anyInt());
    }

    @Secured(permission = "my_permission", description = "description")
    public void MySecuredMethod() {
        // NOP
    }

    public void MyUnsecuredMethod() {
        // NOP
    }
}
