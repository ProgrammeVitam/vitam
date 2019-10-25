/*
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
 */

package fr.gouv.vitam.processing.distributor.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;

/**
 * Process Distributor Resource implementation
 */
@Path("/processing/v1/worker_family")
public class ProcessDistributorResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorResource.class);
    private static final String PROCESSING_MODULE = "PROCESSING";
    private static final String CODE_VITAM = "code_vitam";

    private final IWorkerManager workerManager;

    /**
     * Constructor
     *
     * @param workerManager
     */
    public ProcessDistributorResource(IWorkerManager workerManager) {
        this.workerManager = workerManager;

        LOGGER.info("init Process Distributor Resource server");
    }

    /**
     * Get the list of worker families
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkerFamilies(@Context HttpHeaders headers) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Interact with worker families
     *
     * @param headers http header
     * @param query the query
     * @return Response NOT_IMPLEMENTED
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putWorkerFamilies(@Context HttpHeaders headers, JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Get the list of worker families
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkerFamilyStatus(@Context HttpHeaders headers, @PathParam("id_family") String idFamily) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Add a new worker family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param query the query describing the worker family to be created
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkerFamily(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Update a specific worker family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param query the query describing the worker family to be updated
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkerFamily(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Delete a specific worker family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param query the query describing the worker family to be deleted
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteWorkerFamily(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }


    /**
     * Get the list of workers for a specific family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}/workers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFamilyWorkersList(@Context HttpHeaders headers, @PathParam("id_family") String idFamily) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Delete workers for a specific family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param query the query describing the workers to be deleted
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}/workers")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFamilyWorkers(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }


    /**
     * Get status of a specific worker
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param idWorker the id of the worker
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}/workers/{id_worker}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkerStatus(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Register a new worker
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param idWorker the id of the worker
     * @param workerInformation information describing the worker to be registered
     * @return Response the status of the registering
     */
    @Path("/{id_family}/workers/{id_worker}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerWorker(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker, String workerInformation) {
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(workerInformation));
            GlobalDatasParser.sanityRequestCheck(workerInformation);
            workerManager.registerWorker(idFamily, idWorker, workerInformation);

        } catch (ProcessingBadRequestException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity("{\"error\":\"" + exc.getMessage() + "\"}")
                .build();
        } catch (final WorkerAlreadyExistsException exc) {
            LOGGER.warn(exc);
            return Response.status(Status.CONFLICT).entity("{\"error\":\"" + exc.getMessage() + "\"}")
                .build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn(e);
            return Response.status(Status.CONFLICT).entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
        return Response.status(Status.OK).entity("{\"success\" :\"Worker " + idWorker + " created \"}").build();
    }

    /**
     * Update a specific worker
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param idWorker the id of the worker
     * @param query the query describing the worker to be updated
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/{id_family}/workers/{id_worker}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorker(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker, JsonNode query) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Unregister a specific worker family
     *
     * @param headers http header
     * @param idFamily the id of the family
     * @param idWorker the id of the worker
     * @return Response the status of the unregistering
     */
    @Path("/{id_family}/workers/{id_worker}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregisterWorker(@Context HttpHeaders headers, @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker) {
        try {
            workerManager.unregisterWorker(idFamily, idWorker);
        } catch (WorkerFamilyNotFoundException | WorkerNotFoundException | InterruptedException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity("{\"error\":\"" + exc.getMessage() + "\"}")
                .build();
        }
        return Response.status(Status.OK).entity("{\"success\" :\"Worker " + idWorker + " deleted \"}").build();
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(PROCESSING_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
    }

}
