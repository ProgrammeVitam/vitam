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
package fr.gouv.vitam.functional.administration.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

/**
 * This resource manage Agencys create, update, find, ...
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class AgenciesResourceMock {

    static final String AGENCIES_URI = "/agencies";
    static final String AGENCIES_IMPORT = "/agencies/import";
    static final String AGENCIES_CHECK = "/agencies/check";
    private final ResteasyTestApplication.ExpectedResults mock;

    public AgenciesResourceMock(ResteasyTestApplication.ExpectedResults mock) {
        this.mock = mock;
    }

    @Path(AGENCIES_IMPORT)
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAgencies(@Context HttpHeaders headers, InputStream inputStream, @Context UriInfo uri) {
        consumeAndCloseStream(inputStream);
        return mock.post();
    }

    @Path(AGENCIES_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgencies(JsonNode queryDsl) {
        return mock.get();
    }

    @Path(AGENCIES_CHECK)
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response checkAgenciesFile(InputStream agencyStream) {
        consumeAndCloseStream(agencyStream);
        return mock.post();
    }

    @Path(AGENCIES_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAgencies(@PathParam("id") String agencyId, JsonNode queryDsl) {
        return Response.status(Status.NOT_IMPLEMENTED).entity(null).build();
    }

    protected void consumeAndCloseStream(InputStream stream) {
        try {
            if (null != stream) {
                while (stream.read() > 0) {}
                stream.close();
            }
        } catch (IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }
}
