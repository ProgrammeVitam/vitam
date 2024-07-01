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
package fr.gouv.vitam.processing.distributor.rest;

import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

@Path("/processing/v1/worker_family")
@Tag(name = "Processing")
public class ProcessDistributorResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDistributorResource.class);

    private final IWorkerManager workerManager;

    /**
     * Constructor
     *
     * @param workerManager a WorkerManager instance
     */
    public ProcessDistributorResource(IWorkerManager workerManager) {
        this.workerManager = workerManager;

        LOGGER.info("init Process Distributor Resource server");
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
    public Response registerWorker(
        @Context HttpHeaders headers,
        @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker,
        WorkerBean workerInformation
    ) {
        try {
            String asString = JsonHandler.unprettyPrint(workerInformation);
            SanityChecker.checkJsonAll(asString);
            GlobalDatasParser.sanityRequestCheck(asString);
            workerManager.registerWorker(idFamily, idWorker, workerInformation);
        } catch (ProcessingBadRequestException | InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity("{\"error\":\"" + exc.getMessage() + "\"}").build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
        return Response.status(Status.OK).entity("{\"success\" :\"Worker " + idWorker + " created \"}").build();
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
    public Response unregisterWorker(
        @Context HttpHeaders headers,
        @PathParam("id_family") String idFamily,
        @PathParam("id_worker") String idWorker
    ) {
        try {
            workerManager.unregisterWorker(idFamily, idWorker);
        } catch (WorkerFamilyNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity("{\"error\":\"" + exc.getMessage() + "\"}").build();
        } catch (IOException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
        return Response.status(Status.OK).entity("{\"success\" :\"Worker " + idWorker + " deleted \"}").build();
    }
}
