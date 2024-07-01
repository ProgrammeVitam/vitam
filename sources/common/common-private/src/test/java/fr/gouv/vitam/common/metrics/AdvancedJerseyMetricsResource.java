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
package fr.gouv.vitam.common.metrics;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Path("/home2")
public class AdvancedJerseyMetricsResource {

    public static final Set<String> expectedNames = new HashSet<>(
        Arrays.asList(
            "/home2//users:GET:*:*:timer",
            "/home2//users/{id}:DELETE:*:*:timer",
            "/home2//options:OPTIONS:*:*:timer",
            "/home2//users/{id}:PUT:*:*:meter",
            "/home2//users/{id}:DELETE:*:*:meter",
            "/home2//head:HEAD:*:*:meter",
            "/home2//users/{id}:POST:*:*:timer",
            "/home2//users:GET:*:*:meter",
            "/home2//head:HEAD:*:*:timer",
            "/home2//options:OPTIONS:*:*:meter",
            "/home2//users/{id}:PUT:*:*:timer",
            "/home2//users/{id}:POST:*:*:meter"
        )
    );

    @GET
    @Path("/users")
    public Response advancedGet() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PUT
    @Path("/users/{id}")
    public Response advancedPut(@PathParam("id_op") String operationId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    @Path("/users/{id}")
    public Response advancedPost(@PathParam("id_op") String operationId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @DELETE
    @Path("/users/{id}")
    public Response advancedDelete(@PathParam("id_op") String operationId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @HEAD
    @Path("/head")
    public Response advancedHead() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @OPTIONS
    @Path("/options")
    public Response advancdeOptions() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
