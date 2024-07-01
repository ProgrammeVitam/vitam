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
package fr.gouv.vitam.worker.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.common.WorkerAccessRequest;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.impl.WorkerFactory;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;

@Path("/worker/v1")
@Tag(name = "Worker")
public class WorkerResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerResource.class);
    private static final String WORKER_MODULE = "WORKER";
    private static final String CODE_VITAM = "code_vitam";

    private final WorkerFactory workerFactory;
    private final Worker workerMocked;

    /**
     * Constructor
     *
     * @param pluginLoader the plugin loader
     */
    WorkerResource(PluginLoader pluginLoader) {
        LOGGER.info("init Worker Resource server");
        workerMocked = null;
        workerFactory = WorkerFactory.getInstance(pluginLoader);
    }

    /**
     * Constructor for tests
     *
     * @param pluginLoader the plugin loader
     * @param worker the worker service be applied
     */
    WorkerResource(PluginLoader pluginLoader, Worker worker) {
        LOGGER.info("init Worker Resource server");
        workerMocked = worker;
        workerFactory = WorkerFactory.getInstance(pluginLoader);
    }

    /**
     * Submit a step to be launched
     *
     * @param headers http header
     * @param descriptionStepJson the description of the step as a {fr.gouv.vitam.worker.common.DescriptionStep}
     * @return Response containing the status of the step
     */
    @Path("tasks")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "submit a step to be launched",
        description = "Permet de soumettre une tâche (steps + contexte + item)"
    )
    public Response submitStep(@Context HttpHeaders headers, JsonNode descriptionStepJson) {
        HttpHeaderHelper.checkVitamHeaders(headers);
        try {
            ParametersChecker.checkParameter("Must have a step description", descriptionStepJson);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(descriptionStepJson));
            final ItemStatus responses;
            DescriptionStep descriptionStep = JsonHandler.getFromJsonNode(descriptionStepJson, DescriptionStep.class);
            if (workerMocked == null) {
                try (Worker worker = workerFactory.create()) {
                    responses = worker.run(descriptionStep.getWorkParams(), descriptionStep.getStep());
                    return Response.status(Status.OK).entity(responses).build();
                }
            } else {
                responses = workerMocked.run(descriptionStep.getWorkParams(), descriptionStep.getStep());
                return Response.status(Status.OK).entity(responses).build();
            }
        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED))
                .build();
        } catch (final ProcessingRetryAsyncException retryExc) {
            LOGGER.warn(retryExc);
            List<WorkerAccessRequest> entity = new ArrayList<>();
            retryExc
                .getAccessRequestIdByContext()
                .forEach(
                    (key, value) ->
                        value.forEach(
                            accessRequestId ->
                                entity.add(
                                    new WorkerAccessRequest(accessRequestId, key.getStrategyId(), key.getOfferId())
                                )
                        )
                );
            return Response.status(CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode())
                .entity(entity)
                .build();
        } catch (final IllegalArgumentException | ProcessingException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST)).build();
        }
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(WORKER_MODULE)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }
}
