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
package fr.gouv.vitam.metadata.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;

/**
 * Units resource REST API
 */
@Path("/metadata/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class MetaDataResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataResource.class);
    private final MetaData metaDataImpl;

    /**
     * MetaDataResource constructor
     *
     * @param configuration {@link MetaDataConfiguration}
     */
    public MetaDataResource(MetaDataConfiguration configuration) {
        metaDataImpl = MetaDataImpl.newMetadata(configuration, new MongoDbAccessMetadataFactory());
        LOGGER.info("init MetaData Resource server");
    }

    MongoDbAccessMetadataImpl getMongoDbAccess() {
        return ((MetaDataImpl) metaDataImpl).getMongoDbAccess();
    }

    /**
     * Insert unit with json request
     * 
     * @param insertRequest
     * @param xhttpOverride
     * @return Response
     */
    @Path("units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertUnit(JsonNode insertRequest) {
        Status status;

        try {
            SanityChecker.checkJsonAll(insertRequest);
            metaDataImpl.insertUnit(insertRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .setQuery(insertRequest))
            .build();
    }

    /**
     * Select unit with json request
     * 
     * @param request
     * @return Response
     */
    @Path("units")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnit(JsonNode request) {
        return selectUnitsByQuery(request);
    }

    /**
     * select units list by query
     *
     * @param selectRequest
     * @return
     */
    private Response selectUnitsByQuery(JsonNode selectRequest) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectUnitsByQuery(selectRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();

        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();

        }

        return Response.status(Status.FOUND).entity(new RequestResponseOK()
                .setHits(arrayNodeResults.size(), 0, 1)
                .setQuery(selectRequest)
                .addAllResults(arrayNodeResults)).build();
    }

    /**
     *
     * @param selectRequest
     * @param unitId
     * @return {@link Response} will be contains an json filled by unit result
     * @see #entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     */
    @Path("units/{id_unit}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode selectRequest, @PathParam("id_unit") String unitId) {
        return selectUnitById(selectRequest, unitId);
    }

    /**
     * Update unit by query and path parameter unit_id
     *
     * @param updateRequest
     * @param unitId
     * @return {@link Response} will be contains an json filled by unit result
     * @see #entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     */
    @Path("units/{id_unit}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitbyId(JsonNode updateRequest, @PathParam("id_unit") String unitId) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.updateUnitbyId(updateRequest, unitId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.FOUND).entity(new RequestResponseOK()
                .setHits(arrayNodeResults.size(), 0, 1)
                .setQuery(updateRequest)
                .addAllResults(arrayNodeResults)).build();
    }

    /**
     * Selects unit by request and unit id
     */
    // FIXME P1 : maybe produces NOT_FOUND when unit is not found?
    private Response selectUnitById(JsonNode selectRequest, String unitId) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectUnitsById(selectRequest, unitId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.FOUND).entity(new RequestResponseOK()
                .setHits(arrayNodeResults.size(), 0, 1)
                .setQuery(selectRequest)
                .addAllResults(arrayNodeResults)).build();
    }

    private Response metadataExecutionExceptionTrace(final MetaDataExecutionException e) {
        Status status;
        LOGGER.error(e);
        status = Status.INTERNAL_SERVER_ERROR;
        return Response.status(status)
            .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("ACCESS")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
            .build();
    }

    // OBJECT GROUP RESOURCE. TODO P1 see to externalize it (one resource for units, one resource for object group) to
    // avoid so much lines and complex maintenance
    /**
     * Create unit with json request
     * 
     * @param insertRequest
     * @return the Response
     *
     * @throws InvalidParseOperationException
     */
    @Path("objectgroups")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertObjectGroup(String insertRequest) throws InvalidParseOperationException {
        Status status;
        JsonNode queryJson;
        try {
            queryJson = JsonHandler.getFromString(insertRequest);
            metaDataImpl.insertObjectGroup(queryJson);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }

        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .setQuery(queryJson))
            .build();
    }

    /**
     * Get ObjectGroup
     *
     * @param selectRequest the request
     * @param objectGroupId the objectGroup ID to get
     * @return a response with the select query and the required object group (can be empty)
     */
    @Path("objectgroups/{id_og}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupById(JsonNode selectRequest, @PathParam("id_og") String objectGroupId) {
    	Status status;
        try {
            ParametersChecker.checkParameter("Request select required", selectRequest);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(Status.PRECONDITION_FAILED).entity(
            		new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState("code_vitam")
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(Status.PRECONDITION_FAILED.getReasonPhrase()))
                .build();
        }
        return selectObjectGroupById(selectRequest, objectGroupId);
    }

    /**
     * Selects unit by request and unit id
     */
    // FIXME P1 : maybe produce NOT_FOUND when objectGroup is not found?
    private Response selectObjectGroupById(JsonNode selectRequest, String objectGroupId) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectObjectGroupById(selectRequest, objectGroupId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("METADATA")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("METADATA")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.OK).entity(new RequestResponseOK()
                .setHits(arrayNodeResults.size(), 0, 1)
                .setQuery(selectRequest)
                .addAllResults(arrayNodeResults)).build();
    }

}
