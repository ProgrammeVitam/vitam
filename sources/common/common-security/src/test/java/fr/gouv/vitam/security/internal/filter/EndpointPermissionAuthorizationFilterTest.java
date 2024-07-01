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

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class of {@link EndpointPermissionAuthorizationFilter}
 */
@RunWithCustomExecutor
public class EndpointPermissionAuthorizationFilterTest {

    public static final String PERMISSION = "my_permission";
    public static final String SECURITY_PROFILE_IDENTIFIER = "MY_PROFILE_000001";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    public void testAccessDeniedToUnknownSecurityProfile() throws Exception {
        VitamThreadUtils.getVitamSession().setSecurityProfileIdentifier(SECURITY_PROFILE_IDENTIFIER);

        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        when(adminManagementClient.findSecurityProfileByIdentifier(SECURITY_PROFILE_IDENTIFIER)).thenReturn(
            new VitamError("ERROR").setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
        );

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointPermissionAuthorizationFilter instance = new EndpointPermissionAuthorizationFilter(
            PERMISSION,
            adminManagementClient
        );
        instance.filter(containerRequestContext);

        verify(containerRequestContext, only()).abortWith(any());
    }

    @Test
    public void testAccessGrantedToSecurityProfileWithFullAccess() throws Exception {
        VitamThreadUtils.getVitamSession().setSecurityProfileIdentifier(SECURITY_PROFILE_IDENTIFIER);

        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        boolean fullAccess = true;
        Set<String> permissions = Collections.EMPTY_SET;
        SecurityProfileModel securityProfile = new SecurityProfileModel(
            "guid",
            SECURITY_PROFILE_IDENTIFIER,
            "Sec profile",
            fullAccess,
            permissions
        );
        when(adminManagementClient.findSecurityProfileByIdentifier(SECURITY_PROFILE_IDENTIFIER)).thenReturn(
            new RequestResponseOK().addResult(securityProfile)
        );

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointPermissionAuthorizationFilter instance = new EndpointPermissionAuthorizationFilter(
            PERMISSION,
            adminManagementClient
        );
        instance.filter(containerRequestContext);

        verify(containerRequestContext, never()).abortWith(any());
    }

    @Test
    public void testAccessGrantedToSecurityProfileWithMatchingPermission() throws Exception {
        VitamThreadUtils.getVitamSession().setSecurityProfileIdentifier(SECURITY_PROFILE_IDENTIFIER);

        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        boolean fullAccess = false;
        Set<String> permissions = new HashSet<>(Arrays.asList("some_permission", PERMISSION, "some_other_permission"));
        SecurityProfileModel securityProfile = new SecurityProfileModel(
            "guid",
            SECURITY_PROFILE_IDENTIFIER,
            "Sec profile",
            fullAccess,
            permissions
        );
        when(adminManagementClient.findSecurityProfileByIdentifier(SECURITY_PROFILE_IDENTIFIER)).thenReturn(
            new RequestResponseOK().addResult(securityProfile)
        );

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointPermissionAuthorizationFilter instance = new EndpointPermissionAuthorizationFilter(
            PERMISSION,
            adminManagementClient
        );
        instance.filter(containerRequestContext);

        verify(containerRequestContext, never()).abortWith(any());
    }

    @Test
    public void testAccessDeniedToSecurityProfileWithoutMatchingPermission() throws Exception {
        VitamThreadUtils.getVitamSession().setSecurityProfileIdentifier(SECURITY_PROFILE_IDENTIFIER);

        AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
        boolean fullAccess = false;
        Set<String> permissions = new HashSet<>(Arrays.asList("some_permission", "some_other_permission"));
        SecurityProfileModel securityProfile = new SecurityProfileModel(
            "guid",
            SECURITY_PROFILE_IDENTIFIER,
            "Sec profile",
            fullAccess,
            permissions
        );
        when(adminManagementClient.findSecurityProfileByIdentifier(SECURITY_PROFILE_IDENTIFIER)).thenReturn(
            new RequestResponseOK().addResult(securityProfile)
        );

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointPermissionAuthorizationFilter instance = new EndpointPermissionAuthorizationFilter(
            PERMISSION,
            adminManagementClient
        );
        instance.filter(containerRequestContext);

        verify(containerRequestContext, only()).abortWith(any());
    }
}
