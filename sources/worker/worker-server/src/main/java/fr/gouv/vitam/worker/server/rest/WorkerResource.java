package fr.gouv.vitam.worker.server.rest;

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

import javax.ws.rs.Consumes;
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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.VitamError;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server2.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.impl.WorkerImplFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Worker Resource implementation
 */
@Path("/worker/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class WorkerResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerResource.class);

    private static final String WORKER_MODULE = "WORKER";
    private static final String CODE_VITAM = "code_vitam";
    private final Worker worker;

    /**
     * Constructor
     *
     * @param configuration the worker configuration to be applied
     */
    public WorkerResource(WorkerConfiguration configuration) {
        LOGGER.info("init Worker Resource server");
        DbConfigurationImpl databaseConfiguration;
        if (configuration.isDbAuthentication()) {
            databaseConfiguration =
                new DbConfigurationImpl(configuration.getDbHost(), configuration.getDbPort(),
                    configuration.getDbName(),
                    true, configuration.getDbUserName(), configuration.getDbPassword());
        } else {
            databaseConfiguration = new DbConfigurationImpl(configuration.getDbHost(), configuration.getDbPort(),
                configuration.getDbName());
        }
        this.worker =
            WorkerImplFactory.create(LogbookMongoDbAccessFactory.create(databaseConfiguration));
    }


    /**
     * Constructor for tests
     *
     * @param configuration the worker configuration to be applied
     * @param worker the worker service be applied
     */
    WorkerResource(WorkerConfiguration configuration, Worker worker) {
        LOGGER.info("init Worker Resource server");
        this.worker = worker;
    }

    /**
     * Get a list of running steps
     *
     * @return Response containing the list of steps
     */
    @Path("tasks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getStepsList() {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Submit a step to be launched
     *
     * @param headers http header
     * @param descriptionStep the description of the step as a {fr.gouv.vitam.worker.common.DescriptionStep}
     * @return Response containing the status of the step
     */
    @Path("tasks")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitStep(@Context HttpHeaders headers, DescriptionStep descriptionStep) {
        HttpHeaderHelper.checkVitamHeaders(headers);
        try {
            ParametersChecker.checkParameter("Must have a step description", descriptionStep);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(descriptionStep));
            final CompositeItemStatus responses =
                worker.run(descriptionStep.getWorkParams(), descriptionStep.getStep());
            return Response.status(Status.OK).entity(responses).build();
        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(getErrorEntity(Status.PRECONDITION_FAILED))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST)).build();
        } catch (final HandlerNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST)).build();
        } catch (final ProcessingException | ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST)).build();
        }
    }


    /**
     * Get the status of a step
     *
     * @param idAsync the id of the Async
     * @return Response containing the status of a specific step
     */
    @Path("tasks/{id_async}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStepStatus(@PathParam("id_async") String idAsync) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Modifying a step (pausing, resuming, prioritizing)
     *
     * @param idAsync the id of the Async
     * @return Response containing the status of the step
     */
    @Path("tasks/{id_async}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyStep(@PathParam("id_async") String idAsync) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    private RequestResponseError getErrorEntity(Status status) {
        return new RequestResponseError().setError(new VitamError(status.getStatusCode()).setContext(WORKER_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase()));
    }

}
