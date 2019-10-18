/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.access.internal.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Using the Mock Class in order to simulate Access Client Resource if config file does not exist
 *
 */
@Path("/accessMock")
@Consumes("application/json")
@Produces("application/json")
public class AccessResourceMock implements AccessInternalResource {

    final String queryDsqlForGot =
            "{ \"$query\" : [ { \"$exists\": \"#id\" } ], " +
                    " \"$filter\": { \"$orderby\": \"#id\" }, " +
                    " \"$projection\" : { } " +
                    " }";

    /**
     * Empty Constructor
     */
    public AccessResourceMock() {
        // Empty Constructor
    }

    /**
     * get units list
     */
    @GET
    @Path("/units")
    public Response getUnits(JsonNode dslQuery) {
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }

    /**
     * Select units with inherited rules
     */
    @Override
    @GET
    @Path("/unitsWithInheritedRules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnitsWithInheritedRules(JsonNode queryDsl) {
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }

    @Override
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportDIP(JsonNode dslRequest) {
        return null;
    }

    @Override
    @POST
    @Path("/dipexport/usagefilter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportDIPByUsageFilter(DipExportRequest dipExportRequest) {
        return null;
    }

    @Override
    @GET
    @Path("/dipexport/{id}/dip")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response findDIPByID(@PathParam("id") String id) {
        return null;
    }

    @Override
    @POST
    @Path("/reclassification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startReclassificationWorkflow(JsonNode reclassificationRequestJson) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Override
    @POST
    @Path("/elimination/analysis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEliminationAnalysisWorkflow(EliminationRequestBody eliminationRequestBody) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Override
    @POST
    @Path("/elimination/action")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEliminationActionWorkflow(EliminationRequestBody eliminationRequestBody) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * get unit list by unit id
     */
    @GET
    @Path("/units/{id_unit}")
    public Response getUnitById(JsonNode dslQuery, @PathParam("id_unit") String id_unit) {
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }

    @GET
    @Path("/units/{id_unit}")
    public Response getUnitByIdWithXMLFormat(JsonNode dslQuery, String unitId) {
        // TODO revoyer du xml
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }

    @Override
    @GET
    @Path("/objects/{id_object}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    public Response getObjectByIdWithXMLFormat(JsonNode dslQuery, String objectId) {
        return Response.status(200).entity("{\"objectGroupById\":\"OK_MockObjectGroup\"}").build();
    }

    @Override
    @GET
    @Path("/units/{id_unit}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    public Response getObjectByUnitIdWithXMLFormat(JsonNode dslQuery, String unitId) {
        return Response.status(200).entity("{\"objectGroupByUnitId\":\"OK_MockObjectGroup\"}").build();
    }

    /**
     * update archive units by Id with Json query
     *
     * @param dslQuery DSK, null not allowed
     * @param unit_id units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{id_unit}")
    public Response updateUnitById(JsonNode dslQuery, @PathParam("id_unit") String unit_id,
        @HeaderParam(GlobalDataRest.X_REQUEST_ID) String requestId) {
        return Response.status(200).entity("{\"unit\" = \"OK_MockUnits\"}").build();
    }

    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, JsonNode query) {
        return Response.status(200).entity("{\"objectGroup\":\"OK_MockObjectGroup\"}").build();
    }

    public Response getObjectStreamAsync(HttpHeaders headers, String idObjectGroup, String unitId) {
        return Response.status(200).entity("{\"objectGroup\":\"OK_MockObjectGroup\"}").build();
    }

    @Override public Response getAccessLogStreamAsync(HttpHeaders headers, JsonNode params) {
        return Response.status(200).entity("{\"accessLog\":\"OK_AccessLog\"}").build();
    }

    /**
     * Mass update of archive units with Json query
     *
     * @param dslQuery  DSL, null not allowed
     * @return the response
     */
    @POST
    @Path("/units")
    public Response massUpdateUnits(JsonNode dslQuery) {
        return Response.status(200).entity("{\"units\" = \"OK_MockUnits\"}").build();
    }

    /**
     * Mass update of archive units rules
     * @param massUpdateUnitRuleRequest wrapper for {DSL, RuleActions}, null not allowed
     * @return the response
     */
    @Override public Response massUpdateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest) {
        return null;
    }

    /**
     * get units list
     */
    @GET
    @Path("/objects")
    public Response getObjects(JsonNode dslQuery) {
        return Response.status(200).entity(queryDsqlForGot).build();
    }

    @POST
    @Path("/transfers/reply")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response transferReply(InputStream transferReply) {
        return Response.status(200).entity(queryDsqlForGot).build();
    }
}
