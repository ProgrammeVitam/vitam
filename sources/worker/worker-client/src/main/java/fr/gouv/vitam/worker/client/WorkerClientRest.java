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
package fr.gouv.vitam.worker.client;

import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.common.WorkerAccessRequest;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

/**
 * WorkerClient implementation for production environment using REST API.
 */
class WorkerClientRest extends DefaultClient implements WorkerClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerClientRest.class);
    private static final String DATA_MUST_HAVE_A_VALID_VALUE = "data must have a valid value";
    private static final GenericType<List<WorkerAccessRequest>> LIST_AR_TYPE = new GenericType<>() {};

    WorkerClientRest(WorkerClientFactory factory) {
        super(factory);
    }

    @Override
    public ItemStatus submitStep(DescriptionStep step)
        throws WorkerNotFoundClientException, WorkerServerClientException, ProcessingRetryAsyncException {
        VitamThreadUtils.getVitamSession().checkValidRequestId();

        VitamRequestBuilder request = post().withPath("/tasks").withBody(step, DATA_MUST_HAVE_A_VALID_VALUE).withJson();

        try (Response response = make(request)) {
            return handleCommonResponseStatus(step, response);
        } catch (VitamClientInternalException e) {
            throw new WorkerServerClientException(e);
        }
    }

    private ItemStatus handleCommonResponseStatus(DescriptionStep step, Response response)
        throws WorkerNotFoundClientException, WorkerServerClientException, ProcessingRetryAsyncException {
        if (CustomVitamHttpStatusCode.UNAVAILABLE_ASYNC_DATA_RETRY_LATER.getStatusCode() == response.getStatus()) {
            throw extractException(response);
        }
        final Response.Status status = fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                return response.readEntity(ItemStatus.class);
            case NOT_FOUND:
                throw new WorkerNotFoundClientException(status.getReasonPhrase());
            default:
                try {
                    LOGGER.error(
                        INTERNAL_SERVER_ERROR.getReasonPhrase() +
                        " during execution of " +
                        VitamThreadUtils.getVitamSession().getRequestId() +
                        " Request, stepname:  " +
                        step.getStep().getStepName() +
                        " : " +
                        status.getReasonPhrase()
                    );
                } catch (final VitamThreadAccessException e) {
                    LOGGER.error(
                        INTERNAL_SERVER_ERROR.getReasonPhrase() +
                        " during execution of <unknown request id> Request, stepname:  " +
                        step.getStep().getStepName() +
                        " : " +
                        status.getReasonPhrase()
                    );
                }
                throw new WorkerServerClientException(INTERNAL_SERVER_ERROR.getReasonPhrase());
        }
    }

    private ProcessingRetryAsyncException extractException(Response response) {
        Map<AccessRequestContext, List<String>> accessRequestIdByContext = new HashMap<>();
        List<WorkerAccessRequest> workerAccessRequests = response.readEntity(LIST_AR_TYPE);
        workerAccessRequests.forEach(item -> {
            if (StringUtils.isEmpty(item.getAccessRequestId())) {
                throw new ProcessingException("Invalid accessRequestId was returned by worker");
            }
            accessRequestIdByContext
                .computeIfAbsent(
                    new AccessRequestContext(item.getStrategyId(), item.getOfferId()),
                    (x -> new ArrayList<>())
                )
                .add(item.getAccessRequestId());
        });
        return new ProcessingRetryAsyncException(accessRequestIdByContext);
    }
}
