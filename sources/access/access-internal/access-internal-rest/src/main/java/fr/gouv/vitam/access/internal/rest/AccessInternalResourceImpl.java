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

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessInternalResourceImpl extends ApplicationStatusResource implements AccessInternalResource {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalResourceImpl.class);

    private static final String END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS = "End of execution of DSL Vitam from Access";
    private static final String EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING =
        "Execution of DSL Vitam from Access ongoing...";
    private static final String BAD_REQUEST_EXCEPTION = "Bad request Exception ";
    private static final String ACCESS_MODULE = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";
    private static final String ACCESS_RESOURCE_INITIALIZED = "AccessResource initialized";

    private final AccessInternalModule accessModule;

    /**
     *
     * @param configuration to associate with AccessResourceImpl
     */
    public AccessInternalResourceImpl(AccessInternalConfiguration configuration) {
        accessModule = new AccessInternalModuleImpl(configuration);
        WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
        LOGGER.debug(ACCESS_RESOURCE_INITIALIZED);
    }

    /**
     * Test constructor
     *
     * @param accessModule
     */
    AccessInternalResourceImpl(AccessInternalModule accessModule) {
        this.accessModule = accessModule;
        LOGGER.debug(ACCESS_RESOURCE_INITIALIZED);
    }


    /**
     * get Archive Unit list by query based on identifier
     * 
     * @param queryDsl as JsonNode
     * @return an archive unit result list
     */
    @Override
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(JsonNode queryDsl) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);
        Status status;
        JsonNode result = null;
        
        try {
            SanityChecker.checkJsonAll(queryDsl);
            result = accessModule.selectUnit(addProdServicesToQuery(queryDsl));
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * get Archive Unit list by query based on identifier
     * 
     * @param queryDsl as JsonNode
     * @param idUnit identifier
     * @return an archive unit result list
     */


    @Override
    @GET
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode queryDsl,
        @PathParam("id_unit") String idUnit) {
        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);

        Status status;
        JsonNode result = null;
        try {

            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(idUnit);
            result = accessModule.selectUnitbyId(addProdServicesToQuery(queryDsl), idUnit);
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * update archive units by Id with Json query
     *
     * @param requestId request identifier
     * @param queryDsl DSK, null not allowed
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @Override
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(JsonNode queryDsl,
        @PathParam("id_unit") String idUnit, @HeaderParam(GlobalDataRest.X_REQUEST_ID) String requestId) {

        LOGGER.debug(EXECUTION_OF_DSL_VITAM_FROM_ACCESS_ONGOING);

        Status status;
        JsonNode result = null;
        try {
            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(idUnit);
            SanityChecker.checkParameter(requestId);
            if (!VitamThreadUtils.getVitamSession().isWritingPermission()){
                status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status)).build();
            }
            result = accessModule.updateUnitbyId(queryDsl, idUnit, requestId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION, e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        LOGGER.debug(END_OF_EXECUTION_OF_DSL_VITAM_FROM_ACCESS);
        return Response.status(Status.OK).entity(result).build();
    }

    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, JsonNode query) {
        JsonNode result;
        Status status;
        try {
            SanityChecker.checkJsonAll(query);
            SanityChecker.checkParameter(idObjectGroup);
            final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
            Set<String> prodServices = vitamSession.getProdServices();        
            if (prodServices == null || prodServices.isEmpty()){
                result = accessModule.selectObjectGroupById(query, idObjectGroup);
            } else {
                final SelectParserMultiple parser = new SelectParserMultiple();
                parser.parse(query);
                parser.getRequest().addQueries(
                    QueryHelper.in(SedaConstants.TAG_ORIGINATINGAGENCY, prodServices.stream().toArray(String[]::new))
                    .setDepthLimit(0));
                result = accessModule.selectObjectGroupById(parser.getRequest().getFinalSelect(), idObjectGroup);
            }
            
        } catch (final InvalidParseOperationException | IllegalArgumentException | InvalidCreateOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    private void asyncObjectStream(AsyncResponse asyncResponse, HttpHeaders headers, String idObjectGroup,
        JsonNode query, boolean post) {

        if (post) {
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
                return;
            }
            final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
            if (!HttpMethod.GET.equalsIgnoreCase(xHttpOverride)) {
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.METHOD_NOT_ALLOWED).entity(getErrorEntity(Status.METHOD_NOT_ALLOWED)
                        .toString()).build());
                return;
            }
        }
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
            LOGGER.error("At least one required header is missing. Required headers: (" + VitamHttpHeader.TENANT_ID
                .name() + ", " + VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString()).build());
            return;
        }
        final String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        
        if (!validUsage(xQualifier.split("_")[0])){
            final Response errorResponse = Response.status(Status.UNAUTHORIZED)
                .entity(getErrorEntity(Status.UNAUTHORIZED).toString())
                .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
            return;
        }
        
        try {
            SanityChecker.checkHeaders(headers);
            HttpHeaderHelper.checkVitamHeaders(headers);
            SanityChecker.checkJsonAll(query);
            SanityChecker.checkParameter(idObjectGroup);
            accessModule.getOneObjectFromObjectGroup(asyncResponse, idObjectGroup, query, xQualifier,
                Integer.valueOf(xVersion));
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED).toString())
                .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (final AccessInternalExecutionException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse =
                Response.status(Status.INTERNAL_SERVER_ERROR).entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR)
                    .toString()).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (MetaDataNotFoundException | StorageNotFoundException exc) {
            LOGGER.error(exc);
            final Response errorResponse =
                Response.status(Status.NOT_FOUND).entity(getErrorEntity(Status.NOT_FOUND).toString()).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        }
    }
    
    private boolean validUsage(String s){        
        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        Set<String> versions = vitamSession.getUsages();
        
        if (versions == null || versions.isEmpty()){
            return true;
        }            
        for (String version : versions){
            if (version.equals(s)){
                return true;
            }
        }
        return false;
    }

    private JsonNode addProdServicesToQuery(JsonNode queryDsl) throws InvalidParseOperationException, InvalidCreateOperationException {        
        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        Set<String> prodServices = vitamSession.getProdServices();        
        if (prodServices == null || prodServices.isEmpty()){
            return queryDsl; 
        } else {
            final SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(queryDsl);
            parser.getRequest().addQueries(QueryHelper.in(
                PROJECTIONARGS.MANAGEMENT.exactToken() + "." + SedaConstants.TAG_ORIGINATINGAGENCY, 
                prodServices.stream().toArray(String[]::new)).setDepthLimit(0));
            return parser.getRequest().getFinalSelect();
        }
    }
    
    @Override
    @GET
    @Path("/objects/{id_object_group}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectStreamAsync(@Context HttpHeaders headers, @PathParam("id_object_group") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {

        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                asyncObjectStream(asyncResponse, headers, idObjectGroup, query, false);
            }
        });
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setContext(ACCESS_MODULE)

            .setHttpCode(status.getStatusCode()).setState(CODE_VITAM).setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }
}
