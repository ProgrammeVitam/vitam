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
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.core.griffin.GriffinService;
import fr.gouv.vitam.functional.administration.core.griffin.PreservationScenarioService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static fr.gouv.vitam.common.error.VitamCodeHelper.getCode;
import static javax.ws.rs.core.Response.status;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class PreservationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationResource.class);

    private final PreservationScenarioService preservationScenarioService;

    private final GriffinService griffinService;

    PreservationResource(PreservationScenarioService preservationScenarioService, GriffinService griffinService) {
        this.preservationScenarioService = preservationScenarioService;
        this.griffinService = griffinService;
    }

    @POST
    @Path("/importGriffins")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importGriffin(List<GriffinModel> griffinModelList, @Context UriInfo uri) {
        try {
            RequestResponse<GriffinModel> requestResponse = griffinService.importGriffin(griffinModelList);
            return status(Status.CREATED).entity(requestResponse).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        }
    }

    @POST
    @Path("/importPreservationScenarios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPreservationScenario(
        List<PreservationScenarioModel> preservationScenarioModel,
        @Context UriInfo uri
    ) {
        try {
            RequestResponse<PreservationScenarioModel> requestResponse = preservationScenarioService.importScenarios(
                preservationScenarioModel
            );
            return status(Status.CREATED).entity(requestResponse).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected server error {}", e);
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        }
    }

    private Response buildErrorResponse(VitamCode vitamCode, String message) {
        return Response.status(vitamCode.getStatus())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError<JsonNode>(getCode(vitamCode))
                            .setContext(vitamCode.getService().getName())
                            .setState(vitamCode.getDomain().getName())
                            .setMessage(vitamCode.getMessage())
                            .setDescription(vitamCode.getMessage())
                    )
                    .toString() +
                message
            )
            .build();
    }

    @GET
    @Path("/griffin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findGriffin(JsonNode queryDsl) {
        try {
            RequestResponse<GriffinModel> requestResponse = griffinService.findGriffin(queryDsl);

            return Response.status(Status.OK).entity(requestResponse).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Unexpected server error {}", e);
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/preservationScenario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findPreservation(JsonNode queryDsl) {
        try {
            RequestResponse<PreservationScenarioModel> requestResponse =
                preservationScenarioService.findPreservationScenario(queryDsl);

            return Response.status(Status.OK).entity(requestResponse).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Unexpected server error {}", e);
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
