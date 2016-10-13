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
package fr.gouv.vitam.processing.management.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnauthorizeException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;

/**
 *
 */
public class ProcessingManagementClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingManagementClient.class);

    private final Client client;
    private final String url;
    private static final String RESOURCE_PATH = "/processing/v1";

    // FIXME REVIEW user should not specified the url, the factory should handle this directly (see Logbook client)
    /**
     * @param url of metadata server
     */
    public ProcessingManagementClient(String url) {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JacksonJsonProvider.class);
        clientConfig.register(JacksonFeature.class);

        client = ClientBuilder.newClient(clientConfig);
        this.url = url + RESOURCE_PATH;
    }

    /**
     * @return : status of metadata server 200 : server is alive
     */
    public Response status() {
        return client.target(url).path("status").request().get();
    }


    /**
     * executeVitamProcess : processing operation of a workflow
     *
     * @param container : name of the container
     * @param workflow : id of the workflow
     * @return Engine response containing message and status
     * @throws IllegalArgumentException thrown in case of illegal argument in request server error
     * @throws WorkflowNotFoundException thrown if the defined workfow is not found by server
     * @throws ProcessingUnauthorizeException thrown in case of unauthorized request server error
     * @throws ProcessingBadRequestException thrown in case of bad request server error
     * @throws ProcessingInternalServerException thrown in case of internal server error or technical error between
     *         client and server
     */
    public String executeVitamProcess(String container, String workflow)
        throws ProcessingUnauthorizeException, ProcessingBadRequestException, WorkflowNotFoundException,
        ProcessingException {
        ParametersChecker.checkParameter("container is a mandatory parameter", container);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);

        try {

            final Response response = client.target(url).path("operations").request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new ProcessingEntry(container, workflow), MediaType.APPLICATION_JSON),
                    Response.class);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException("Workflow Not Found");
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException("Illegal Argument");
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new ProcessingUnauthorizeException("Unauthorized Operation");
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new ProcessingBadRequestException("Bad Request");
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new ProcessingInternalServerException("Internal Server Error");
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return response.readEntity(String.class);
        } catch (javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new ProcessingInternalServerException("Internal Server Error", e);
        }
    }

    /**
     * Register a new worker knowing its family and with a WorkerBean. If a problem is encountered, an exception is
     * thrown.
     * 
     * @param familyId the id of the family to which the worker has to be registered
     * @param workerId the id of the worker to be registered
     * @param workerDescription the description of the worker as a workerBean
     * @throws ProcessingBadRequestException if a bad request has been sent
     * @throws WorkerAlreadyExistsException if the worker family does not exist
     */
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
        throws ProcessingBadRequestException, WorkerAlreadyExistsException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        ParametersChecker.checkParameter("workerDescription is a mandatory parameter", workerDescription);
        final Response response = client.target(url).path("worker_family/" + familyId + "/" + "workers" +
            "/" + workerId).request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(workerDescription, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new ProcessingBadRequestException("Bad Request");
        } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
            throw new WorkerAlreadyExistsException("Worker already exist");
        }
    }

    /**
     * Unregister a worker knowing its family and its workerId. If the familyId or the workerId is unknown, an exception
     * is thrown.
     * 
     * @param familyId the id of the family to which the worker has to be registered
     * @param workerId the id of the worker to be registered
     * @throws ProcessingBadRequestException if the worker or the family does not exist
     */
    public void unregisterWorker(String familyId, String workerId)
        throws ProcessingBadRequestException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        final Response response = client.target(url).path("worker_family/" + familyId + "/" + "workers" +
            "/" + workerId).request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .delete();

        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            throw new ProcessingBadRequestException("Worker Family, or worker does not exist");
        }
    }
}
