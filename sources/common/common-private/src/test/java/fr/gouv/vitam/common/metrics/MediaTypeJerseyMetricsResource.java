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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Path("/home4")
public class MediaTypeJerseyMetricsResource {

    public static final Set<String> expectedNames = new HashSet<>(
        Arrays.asList(
            "/home4/:POST:multipart/form-data:*:meter",
            "/home4//users:GET:*:application/json:timer",
            "/home4/:GET:*:application/json:meter",
            "/home4/:POST:application/svg+xml,application/atom+xml:*:meter",
            "/home4/:DELETE:multipart/form-data:application/json,text/plain:meter",
            "/home4//users:GET:*:application/json:meter",
            "/home4/:POST:application/xhtml+xml:*:timer",
            "/home4//users:POST:multipart/form-data:*:meter",
            "/home4/:GET:*:application/xml:meter",
            "/home4/:GET:*:application/json:timer",
            "/home4/:GET:*:application/octet-stream,text/plain:meter",
            "/home4/:POST:application/xhtml+xml:*:meter",
            "/home4/:POST:application/svg+xml,application/atom+xml:*:timer",
            "/home4/:POST:multipart/form-data:*:timer",
            "/home4/:DELETE:multipart/form-data:application/json,text/plain:timer",
            "/home4//users:POST:multipart/form-data:*:timer",
            "/home4/:GET:*:application/octet-stream,text/plain:timer",
            "/home4/:GET:*:application/xml:timer"
        )
    );

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getXML() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersJSON() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN })
    public Response getTextOrOctetStream() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postFormData() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    @Consumes(MediaType.APPLICATION_XHTML_XML)
    public Response postXhtmlXml() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    @Path("/users")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postUsersFormData() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @POST
    @Consumes({ MediaType.APPLICATION_SVG_XML, MediaType.APPLICATION_ATOM_XML })
    public Response postSvgXmlOrAtomXml() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @DELETE
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response delete() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
