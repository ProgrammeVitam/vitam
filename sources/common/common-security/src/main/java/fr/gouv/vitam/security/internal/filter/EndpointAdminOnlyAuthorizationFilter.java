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

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Handles permission based access authorization for REST endpoints.
 */
public class EndpointAdminOnlyAuthorizationFilter implements ContainerRequestFilter {

    private static final String TENANT_ADMINISTRATION_IS_REQUIRED = "Tenant administration is required";

    private final boolean isAdminOnly;

    /**
     * Contructor with isAdminOnly option
     *
     * @param isAdminOnly
     */
    public EndpointAdminOnlyAuthorizationFilter(boolean isAdminOnly) {
        this.isAdminOnly = isAdminOnly;
    }

    /**
     * Checks authorization filter based of the current security profile permission set.
     *
     * @param requestContext the invocation context
     * @throws java.io.IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        if (isAdminOnly && tenantId != VitamConfiguration.getAdminTenant()) {
            final VitamError vitamError = generateVitamError();
            requestContext.abortWith(
                Response.status(vitamError.getHttpCode())
                    .entity(vitamError)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            );
        }
    }

    /**
     * Generate Vitam Error
     *
     * @return Vitam Error
     */
    private VitamError generateVitamError() {
        final VitamError vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED));

        vitamError
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getMessage())
            .setDescription(TENANT_ADMINISTRATION_IS_REQUIRED)
            .setState(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.name())
            .setHttpCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getStatus().getStatusCode());
        return vitamError;
    }
}
