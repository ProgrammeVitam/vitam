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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.api.model.RequestResponseError;
import fr.gouv.vitam.api.model.RequestResponseOK;
import fr.gouv.vitam.api.model.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.core.MetaDataImpl;
import fr.gouv.vitam.core.MongoDbAccessFactory;
import fr.gouv.vitam.core.database.collections.DbRequest;

/**
 * Units resource REST API
 */
@Path("/metadata/v1")
public class MetaDataResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataResource.class);

    private static final String X_HTTP_METHOD = "X-Http-Method-Override";



    private final MetaDataImpl metaDataImpl;

    /**
     * MetaDataResource constructor
     * 
     * @param configuration {@link MetaDataConfiguration}
     */

    // TODO: comment
    public MetaDataResource(MetaDataConfiguration configuration) {
        metaDataImpl = new MetaDataImpl(configuration, new MongoDbAccessFactory(), DbRequest::new);
        LOGGER.info("init MetaData Resource server");
    }

    /**
     * Get unit status
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * Insert or Select unit with json request
     * 
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataExecutionException
     */

    @Path("units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertOrSelectUnit(String request, @HeaderParam(X_HTTP_METHOD) String xhttpOverride) {

        if (xhttpOverride != null) {
            if ("GET".equals(xhttpOverride)) {
                return selectUnitsByQuery(request);
            }

        } else {
            return insertUnit(request);
        }
        return Response.status(Status.METHOD_NOT_ALLOWED).build();

    }

    /**
     * Create unit with json request
     */
    private Response insertUnit(String insertRequest) {
        Status status;
        JsonNode queryJson;

        try {
            queryJson = JsonHandler.getFromString(insertRequest);
            metaDataImpl.insertUnit(queryJson);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
            // Unprocessable Entity not implemented by Jersey
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage());
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e.getMessage());
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e.getMessage());
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e.getMessage());
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        }
        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .setQuery(queryJson))
            .build();
    }

    /**
     * select units list by query
     * 
     * @param selectRequest
     * @return
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataExecutionException
     * @throws InvalidParseOperationException
     */
    private Response selectUnitsByQuery(String selectRequest) {
        Status status;
        JsonNode jsonResultNode;
        try {
            jsonResultNode = metaDataImpl.selectUnitsByQuery(selectRequest);

        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();

        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e.getMessage());
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e.getMessage());
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();

        }

        return Response.status(Status.FOUND).entity(jsonResultNode).build();
    }

    /**
     * Select unit by query and path parameter unit_id
     * 
     * @param selectRequest
     * @param unitId
     * @return {@link Response} will be contains an json filled by unit result
     * @see #entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     */
    @Path("units/{id_unit}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnitById(String selectRequest, @PathParam("id_unit") String unitId,
        @HeaderParam(X_HTTP_METHOD) String xhttpOverride) {
        if (!"GET".equals(xhttpOverride)) {
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        }
        return selectUnitById(selectRequest, unitId);
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
    public Response getUnitById(String selectRequest, @PathParam("id_unit") String unitId) {
        return selectUnitById(selectRequest, unitId);
    }

    /**
     * Select unit by request and unit id
     */
    private Response selectUnitById(String selectRequest, String unitId) {
        Status status;
        JsonNode jsonResultNode;
        try {
            jsonResultNode = metaDataImpl.selectUnitsById(selectRequest, unitId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e.getMessage());
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e.getMessage());
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e.getMessage());
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ACCESS")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        }
        return Response.status(Status.FOUND).entity(jsonResultNode).build();
    }


    /**
     * Create unit with json request
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
            LOGGER.error(e.getMessage());
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e.getMessage());
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e.getMessage());
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e.getMessage());
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e.getMessage());
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        }

        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .setQuery(queryJson))
            .build();
    }
}
