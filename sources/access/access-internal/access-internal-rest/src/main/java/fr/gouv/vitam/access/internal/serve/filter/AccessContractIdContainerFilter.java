/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.access.internal.serve.filter;

import fr.gouv.vitam.access.internal.serve.exception.MissingAccessContractIdException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manage the X_ACCESS_CONTRACT_ID header from the server-side perspective.
 */
@PreMatching
@Priority(Priorities.AUTHORIZATION)
public class AccessContractIdContainerFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContractIdContainerFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getUriInfo().getPath().contains("/status")
            || requestContext.getUriInfo().getPath().contains("/slicedOperations")
            || requestContext.getUriInfo().getPath().contains("/operations")) {
            return;
        }
        try {
            AccessContractIdHeaderHelper
                .manageAccessContractFromHeader(requestContext.getHeaders(), AdminManagementClientFactory
                    .getInstance());
        } catch (MissingAccessContractIdException e) {
            LOGGER.error(e);

            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(JsonHandler.createObjectNode().put("Error", e.getMessage())).type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

}
