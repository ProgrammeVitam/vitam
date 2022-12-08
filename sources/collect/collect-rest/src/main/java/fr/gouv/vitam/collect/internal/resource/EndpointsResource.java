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

package fr.gouv.vitam.collect.internal.resource;

import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/collect-external/v1")
public class EndpointsResource extends ApplicationStatusResource {
    private final SecureEndpointRegistry secureEndpointRegistry;

    public EndpointsResource(SecureEndpointRegistry secureEndpointRegistry) {
        this.secureEndpointRegistry = secureEndpointRegistry;
    }

    /**
     * Get all Endpoints
     * @return Response of EndpointInfo
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response listResourceEndpoints() {
        final List<EndpointInfo> allCollectSecuredEndpoints =
            Stream.of(
                    this.secureEndpointRegistry.getEndPointsByResourcePath(
                        ProjectResource.class.getAnnotation(Path.class).value()),
                    this.secureEndpointRegistry.getEndPointsByResourcePath(
                        TransactionResource.class.getAnnotation(Path.class).value()),
                    this.secureEndpointRegistry.getEndPointsByResourcePath(
                        CollectMetadataResource.class.getAnnotation(Path.class).value())
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return Response.status(Response.Status.OK).entity(allCollectSecuredEndpoints).build();
    }
}
