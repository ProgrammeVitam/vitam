/**
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
package fr.gouv.vitam.processing.management.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.api.model.RequestResponseError;
import fr.gouv.vitam.api.model.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.management.core.ProcessManagementImpl;

/**
 * This class is resource provider of ProcessManagement
 */
@Path("/processing/api/v0.0.3")
public class ProcessManagementResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementResource.class);
    private final ProcessManagementImpl processManagement;
    private final ServerConfiguration config;

    /**
     * ProcessManagementResource : initiate the ProcessManagementResource resources
     *
     * @param configuration
     */
    public ProcessManagementResource(ServerConfiguration configuration) {
        processManagement = new ProcessManagementImpl(configuration);
        config = configuration;
        LOGGER.info("init Process Management Resource server");
    }

    /**
     * check the status of server
     *
     * @return Response with OK status
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // Should returns 200
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * Execute the process as a set of operations.
     *
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @return http response
     */
    @Path("operations")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeVitamProcess(ProcessingEntry process) {
        Status status;
        final WorkParams workParam = new WorkParams();

        workParam.setContainerName(process.getContainer());
        workParam.setServerConfiguration(config);
        ProcessResponse resp = new ProcessResponse();

        try {
            resp = (ProcessResponse) processManagement.submitWorkflow(workParam, process.getWorkflow());
        } catch (WorkflowNotFoundException | HandlerNotFoundException e) {
            // if workflow or handler not found
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
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e.getMessage());
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e.getMessage());
            status = Status.UNAUTHORIZED;
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("ingest")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();
        }

        return Response.status(Status.CREATED).entity(resp).build();
    }
}
