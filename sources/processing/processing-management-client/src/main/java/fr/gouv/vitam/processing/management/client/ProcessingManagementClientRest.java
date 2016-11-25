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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnauthorizeException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;

/**
 * Processing Management Client
 */
class ProcessingManagementClientRest extends DefaultClient implements ProcessingManagementClient {
    private static final String PROCESSING_INTERNAL_SERVER_ERROR = "Processing Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR2 = "Internal Server Error";
    private static final String ILLEGAL_ARGUMENT = "Illegal Argument";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingManagementClientRest.class);

    /**
     * Constructor
     *
     * @param factory
     */
    ProcessingManagementClientRest(ProcessingManagementClientFactory factory) {
        super(factory);
    }

    @Override
    public ItemStatus executeVitamProcess(String container, String workflow)
        throws ProcessingUnauthorizeException, ProcessingBadRequestException, WorkflowNotFoundException,
        ProcessingException {
        ParametersChecker.checkParameter("container is a mandatory parameter", container);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, "/operations", null,
                    JsonHandler.toJsonNode(new ProcessingEntry(container, workflow)), MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException("Workflow Not Found");
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new ProcessingUnauthorizeException("Unauthorized Operation");
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new ProcessingInternalServerException(INTERNAL_SERVER_ERROR2);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return response.readEntity(ItemStatus.class);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new ProcessingInternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingInternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
        throws ProcessingBadRequestException, WorkerAlreadyExistsException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        ParametersChecker.checkParameter("workerDescription is a mandatory parameter", workerDescription);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, "/worker_family/" + familyId + "/" + "workers" + "/" + workerId, null,
                    JsonHandler.toJsonNode(workerDescription), MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new ProcessingBadRequestException("Bad Request");
            } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
                throw new WorkerAlreadyExistsException("Worker already exist");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void unregisterWorker(String familyId, String workerId)
        throws ProcessingBadRequestException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, "/worker_family/" + familyId + "/" + "workers" +
                    "/" + workerId, null, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new ProcessingBadRequestException("Worker Family, or worker does not exist");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }
}
