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
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Path("/home")
public class SimpleJerseyMetricsResource {

    static final public Set<String> expectedNames = new HashSet<>(Arrays.asList(
        "/home/:PUT:*:*:timer",
        "/home/:GET:*:*:timer",
        "/home/:POST:*:*:meter",
        "/home/:HEAD:*:*:timer",
        "/home/:DELETE:*:*:timer",
        "/home/:DELETE:*:*:meter",
        "/home/:PUT:*:*:meter",
        "/home/:GET:*:*:meter",
        "/home/:OPTIONS:*:*:meter",
        "/home/:POST:*:*:timer",
        "/home/:OPTIONS:*:*:timer",
        "/home/:HEAD:*:*:meter"
    ));

    @GET
    public Response simpleGet() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PUT
    public Response simplePut() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    public Response simplePost() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @DELETE
    public Response simpleDelete() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @HEAD
    public Response simpleHead() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @OPTIONS
    public Response simpleOptions() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
