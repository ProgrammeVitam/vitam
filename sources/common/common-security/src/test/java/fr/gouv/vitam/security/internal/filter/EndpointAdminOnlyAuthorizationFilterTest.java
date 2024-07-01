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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Test class of {@link fr.gouv.vitam.security.internal.filter.EndpointPermissionAuthorizationFilter}
 */
@RunWithCustomExecutor
public class EndpointAdminOnlyAuthorizationFilterTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    public void testAccessDeniedToAPIAdmin() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamConfiguration.setAdminTenant(1);

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointAdminOnlyAuthorizationFilter instance = new EndpointAdminOnlyAuthorizationFilter(true);
        instance.filter(containerRequestContext);

        verify(containerRequestContext, only()).abortWith(any());
    }

    @Test
    public void testAccessGrantedToAPIAdmin() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(1);
        VitamConfiguration.setAdminTenant(1);

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointAdminOnlyAuthorizationFilter instance = new EndpointAdminOnlyAuthorizationFilter(true);
        instance.filter(containerRequestContext);

        verify(containerRequestContext, never()).abortWith(any());
    }

    @Test
    public void testAccessGrantedToNotAPIAdmin() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamConfiguration.setAdminTenant(1);

        ContainerRequestContext containerRequestContext = spy(ContainerRequestContext.class);

        EndpointAdminOnlyAuthorizationFilter instance = new EndpointAdminOnlyAuthorizationFilter(false);
        instance.filter(containerRequestContext);

        verify(containerRequestContext, never()).abortWith(any());
    }
}
