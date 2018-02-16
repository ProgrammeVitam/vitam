package fr.gouv.vitam.security.internal.filter;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import fr.gouv.vitam.common.security.rest.Secured;

public class AuthorizationFilterTest {

    private static final String MY_PERMISSION = "my_permission";

    @Test
    public void checkEndpointAuthorizationFilterRegistrationForSecuredMethod() throws Exception {

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(AuthorizationFilterTest.class.getMethod("MySecuredMethod"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) AuthorizationFilterTest.class);

        FeatureContext context = mock(FeatureContext.class);

        AuthorizationFilter instance = new AuthorizationFilter();
        instance.configure(resourceInfo, context);

        ArgumentCaptor<EndpointPermissionAuthorizationFilter> endpointAuthorizationFilterArgumentCaptor =
            ArgumentCaptor.forClass(EndpointPermissionAuthorizationFilter.class);

        ArgumentCaptor<EndpointPersonalCertificateAuthorizationFilter>
            endpointPersonalCertificateAuthorizationFilterArgumentCaptor =
            ArgumentCaptor.forClass(EndpointPersonalCertificateAuthorizationFilter.class);

        ArgumentCaptor<EndpointAdminOnlyAuthorizationFilter>
            endpointAdminOnlyAuthorizationFilterArgumentCaptor =
            ArgumentCaptor.forClass(EndpointAdminOnlyAuthorizationFilter.class);

        verify(context)
            .register(endpointAdminOnlyAuthorizationFilterArgumentCaptor.capture(), Matchers.eq(Priorities.AUTHORIZATION + 10));
        verify(context)
            .register(endpointAuthorizationFilterArgumentCaptor.capture(), Matchers.eq(Priorities.AUTHORIZATION + 20));
        verify(context).register(endpointPersonalCertificateAuthorizationFilterArgumentCaptor.capture(),
            Matchers.eq(Priorities.AUTHORIZATION + 30));
        verifyNoMoreInteractions(context);
        Assert.assertEquals(MY_PERMISSION, endpointAuthorizationFilterArgumentCaptor.getValue().getPermission());
        Assert.assertEquals(MY_PERMISSION,
            endpointPersonalCertificateAuthorizationFilterArgumentCaptor.getValue().getPermission());
    }

    @Test
    public void checkNoEndpointAuthorizationFilterRegistrationForUnsecuredMethod() throws Exception {

        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(this.getClass().getMethod("MyUnsecuredMethod"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) AuthorizationFilterTest.class);

        FeatureContext context = mock(FeatureContext.class);

        AuthorizationFilter instance = new AuthorizationFilter();
        instance.configure(resourceInfo, context);

        verify(context, never()).register(anyObject(), anyInt());
    }

    @Secured(permission = MY_PERMISSION, description = "description")
    public void MySecuredMethod() {
        // NOP
    }

    public void MyUnsecuredMethod() {
        // NOP
    }
}
