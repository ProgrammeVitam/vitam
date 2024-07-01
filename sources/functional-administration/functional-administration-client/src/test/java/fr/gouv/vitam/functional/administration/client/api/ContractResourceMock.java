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
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/adminmanagement/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class ContractResourceMock {

    private final ResteasyTestApplication.ExpectedResults mock;
    static final String INGEST_CONTRACTS_URI = "/ingestcontracts";
    static final String ACCESS_CONTRACTS_URI = "/accesscontracts";
    static final String MANAGEMENT_CONTRACTS_URI = "/managementcontracts";
    static final String UPDATE_ACCESS_CONTRACT_URI = "/accesscontracts";
    static final String UPDATE_INGEST_CONTRACTS_URI = "/ingestcontracts";
    static final String UPDATE_MANAGEMENT_CONTRACTS_URI = "/managementcontracts";

    public ContractResourceMock(ResteasyTestApplication.ExpectedResults mock) {
        this.mock = mock;
    }

    @Path(INGEST_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContracts(List<IngestContractModel> ingestContractModelList, @Context UriInfo uri) {
        return mock.post();
    }

    @GET
    @Path(INGEST_CONTRACTS_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findIngestContracts(JsonNode queryDsl) {
        return mock.get();
    }

    @Path(ACCESS_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAccessContracts(List<AccessContractModel> accessContractModelList, @Context UriInfo uri) {
        return mock.post();
    }

    @Path(UPDATE_ACCESS_CONTRACT_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccessContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        return mock.put();
    }

    @Path(UPDATE_INGEST_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateIngestContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        return mock.put();
    }

    @Path(ACCESS_CONTRACTS_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessContracts(JsonNode queryDsl) {
        return mock.get();
    }

    @Path(MANAGEMENT_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importManagementContracts(
        List<ManagementContractModel> managementContractModelList,
        @Context UriInfo uri
    ) {
        return mock.post();
    }

    @Path(MANAGEMENT_CONTRACTS_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findManagementContracts(JsonNode queryDsl) {
        return mock.get();
    }

    @Path(UPDATE_MANAGEMENT_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateManagementContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        return mock.put();
    }
}
