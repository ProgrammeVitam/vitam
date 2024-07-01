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
package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.security.rest.Secured;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.SECURITYPROFILES_CREATE_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AuthorizationFilterTest {

    private static final String MY_PERMISSION = "securityprofiles:create:json";

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

        ArgumentCaptor<
            EndpointPersonalCertificateAuthorizationFilter
        > endpointPersonalCertificateAuthorizationFilterArgumentCaptor = ArgumentCaptor.forClass(
            EndpointPersonalCertificateAuthorizationFilter.class
        );

        ArgumentCaptor<EndpointAdminOnlyAuthorizationFilter> endpointAdminOnlyAuthorizationFilterArgumentCaptor =
            ArgumentCaptor.forClass(EndpointAdminOnlyAuthorizationFilter.class);

        verify(context).register(
            endpointAdminOnlyAuthorizationFilterArgumentCaptor.capture(),
            eq(Priorities.AUTHORIZATION + 10)
        );
        verify(context).register(
            endpointAuthorizationFilterArgumentCaptor.capture(),
            eq(Priorities.AUTHORIZATION + 20)
        );
        verify(context).register(
            endpointPersonalCertificateAuthorizationFilterArgumentCaptor.capture(),
            eq(Priorities.AUTHORIZATION + 30)
        );
        verifyNoMoreInteractions(context);
        Assert.assertEquals(MY_PERMISSION, endpointAuthorizationFilterArgumentCaptor.getValue().getPermission());
        Assert.assertEquals(
            MY_PERMISSION,
            endpointPersonalCertificateAuthorizationFilterArgumentCaptor.getValue().getPermission()
        );
    }

    @Test
    public void checkNoEndpointAuthorizationFilterRegistrationForUnsecuredMethod() throws Exception {
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(this.getClass().getMethod("MyUnsecuredMethod"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) AuthorizationFilterTest.class);

        FeatureContext context = mock(FeatureContext.class);

        AuthorizationFilter instance = new AuthorizationFilter();
        instance.configure(resourceInfo, context);

        verify(context, never()).register(any(), anyInt());
    }

    @Secured(permission = SECURITYPROFILES_CREATE_JSON, description = "description")
    public void MySecuredMethod() {
        // NOP
    }

    public void MyUnsecuredMethod() {
        // NOP
    }
}
