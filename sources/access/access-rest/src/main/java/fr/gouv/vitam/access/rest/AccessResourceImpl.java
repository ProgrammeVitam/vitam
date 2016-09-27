/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.api.AccessResource;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.access.core.AccessModuleImpl;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.VitamError;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.InternalVitamResources;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;


/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access/v1")
@Consumes("application/json")
@Produces("application/json")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessResourceImpl extends InternalVitamResources implements AccessResource {

    private static final String ACCESS_MODULE = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceImpl.class);


    private AccessModule accessModule;

    /**
     *
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessResourceImpl(AccessConfiguration configuration) {
        super(new BasicVitamStatusServiceImpl());
        accessModule = new AccessModuleImpl(configuration);
        LOGGER.info("AccessResource initialized");
    }

    /**
     * Test constructor
     *
     * @param accessModule
     */
    AccessResourceImpl(AccessModule accessModule) {
        super(new BasicVitamStatusServiceImpl());
        this.accessModule = accessModule;
        LOGGER.info("AccessResource initialized");
    }

    /**
     * get units list by query
     */
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        LOGGER.info("Execution of DSL Vitam from Access ongoing...");
        Status status;
        JsonNode result = null;
        JsonNode queryJson = null;
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
                GlobalDatasParser.sanityRequestCheck(queryDsl);
                queryJson = JsonHandler.getFromString(queryDsl);
                result = accessModule.selectUnit(queryJson);
            } else {
                throw new AccessExecutionException("There is no 'X-Http-Method-Override:GET' as a header");
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (final AccessExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        LOGGER.info("End of execution of DSL Vitam from Access");
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * get units list by query based on identifier
     */
    @POST
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("id_unit") String id_unit) {
        LOGGER.info("Execution of DSL Vitam from Access ongoing...");

        Status status;
        JsonNode queryJson = null;
        JsonNode result = null;
        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
                GlobalDatasParser.sanityRequestCheck(queryDsl);
                queryJson = JsonHandler.getFromString(queryDsl);
                result = accessModule.selectUnitbyId(queryJson, id_unit);
            } else {
                throw new AccessExecutionException("There is no 'X-Http-Method-Override:GET' as a header");
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (final AccessExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        LOGGER.info("End of execution of DSL Vitam from Access");
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryDsl DSK, null not allowed
     * @param id_unit units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(String queryDsl, @PathParam("id_unit") String id_unit) {

        LOGGER.info("Execution of DSL Vitam from Access ongoing...");

        Status status;
        JsonNode queryJson = null;
        JsonNode result = null;
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(queryDsl));
            GlobalDatasParser.sanityRequestCheck(queryDsl);
            queryJson = JsonHandler.getFromString(queryDsl);
            result = accessModule.updateUnitbyId(queryJson, id_unit);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (final AccessExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        LOGGER.info("End of execution of DSL Vitam from Access");
        return Response.status(Status.OK).entity(result).build();
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, String query) {
        JsonNode result;
        Status status;
        try {
            ParametersChecker.checkParameter("Must have a dsl query", query);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(query));
            JsonNode queryJson = JsonHandler.getFromString(query);
            result = accessModule.selectObjectGroupById(queryJson, idObjectGroup);
        } catch (InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (AccessExecutionException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    @Override
    @POST
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xHttpOverride,
        @PathParam("id_object_group") String idObjectGroup, String query) {
        if (!"GET".equalsIgnoreCase(xHttpOverride)) {
            Status status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        return getObjectGroup(idObjectGroup, query);
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectStream(@Context HttpHeaders headers, @PathParam("id_object_group") String idObjectGroup,
        String query) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build();
        }
        String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        String xTenantId = headers.getRequestHeader(GlobalDataRest.X_TENANT_ID).get(0);
        InputStream result;
        try {
            HttpHeaderHelper.checkVitamHeaders(headers);
            ParametersChecker.checkParameter("Must have a dsl query", query);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(query));
            JsonNode queryJson = JsonHandler.getFromString(query);
            result = accessModule.getOneObjectFromObjectGroup(idObjectGroup, queryJson, xQualifier,
                Integer.valueOf(xVersion), xTenantId);
        } catch (InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST).toString()).build();
        } catch (IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build();
        } catch (AccessExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR)
                .toString()).build();
        } catch (MetaDataNotFoundException | StorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity(getErrorEntity(Status.NOT_FOUND).toString()).build();
        }
        return Response.status(Status.OK).header("X-Qualifier", xQualifier).header("X-Version", xVersion).entity(result)
            .build();
    }

    @Override
    @POST
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectStreamPost(@Context HttpHeaders headers,
        @PathParam("id_object_group") String idObjectGroup, String query) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build();
        }
        String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
        if (!"GET".equalsIgnoreCase(xHttpOverride)) {
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(getErrorEntity(Status.METHOD_NOT_ALLOWED)
                .toString()).build();
        }
        return getObjectStream(headers, idObjectGroup, query);
    }

    private RequestResponseError getErrorEntity(Status status) {
        return new RequestResponseError().setError(new VitamError(status.getStatusCode()).setContext(ACCESS_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase()));
    }
}
