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
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.core.context.ContextService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class ContextResource {

    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    static final String CONTEXTS_URI = "/contexts";
    static final String UPDATE_CONTEXT_URI = "/context";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextResource.class);
    private static final String CONTEXTS_JSON_IS_MANDATORY_PATAMETER = "The json input of contexts is mandatory";

    private final ContextService contextService;

    /**
     * @param contextService
     */
    public ContextResource(ContextService contextService) {
        this.contextService = contextService;
        LOGGER.debug("init Admin Management Resource server");
    }

    @Path(CONTEXTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContexts(List<ContextModel> ContextModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(CONTEXTS_JSON_IS_MANDATORY_PATAMETER, ContextModelList);

        try {
            RequestResponse<ContextModel> requestResponse = contextService.createContexts(ContextModelList);

            if (!requestResponse.isOk()) {
                ((VitamError<ContextModel>) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (ReferentialException exp) {
            LOGGER.error(exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        } catch (VitamException exp) {
            // FIXME Proper exception handling
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Construct the error following input
     *
     * @param status Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @param code The functional error code, if absent the http code will be used instead
     * @return
     */
    private VitamError getErrorEntity(Status status, String message, String code) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode)
            .setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState("ko")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    /**
     * Find contexts by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(CONTEXTS_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findContexts(JsonNode queryDsl) {
        try {
            SanityChecker.checkJsonAll(queryDsl);
            try (DbRequestResult result = contextService.findContexts(queryDsl)) {
                RequestResponseOK<ContextModel> response = result.getRequestResponseOK(
                    queryDsl,
                    fr.gouv.vitam.functional.administration.common.Context.class,
                    ContextModel.class
                );
                return Response.status(Status.OK).entity(response).build();
            }
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    /**
     * Update contexts
     *
     * @param contextId
     * @param queryDsl
     * @return Response
     */
    @Path(UPDATE_CONTEXT_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateContexts(@PathParam("id") String contextId, JsonNode queryDsl) {
        try {
            RequestResponse<ContextModel> requestResponse = contextService.updateContext(contextId, queryDsl);
            if (!requestResponse.isOk()) {
                ((VitamError<ContextModel>) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (ReferentialNotFoundException exp) {
            LOGGER.error(exp);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorEntity(Status.NOT_FOUND, exp.getMessage(), null))
                .build();
        } catch (VitamException exp) {
            // FIXME : Proper error management
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Delate contexts
     *
     * @param contextId
     * @return Response
     */
    Response deleteContext(String contextId, boolean force) {
        try {
            RequestResponse<ContextModel> requestResponse = contextService.deleteContext(contextId, force);
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                return Response.status(Response.Status.NOT_FOUND).entity(requestResponse).build();
            }
            if (Response.Status.FORBIDDEN.getStatusCode() == requestResponse.getHttpCode()) {
                return Response.status(Response.Status.FORBIDDEN).entity(requestResponse).build();
            }
            if (!requestResponse.isOk()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(requestResponse).build();
            }

            return Response.status(Status.NO_CONTENT).entity(requestResponse).build();
        } catch (ReferentialNotFoundException exp) {
            LOGGER.error(exp);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorEntity(Status.NOT_FOUND, exp.getMessage(), null))
                .build();
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }
}
