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
package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.PoolingStatusClient;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

/**
 * Ingest external interface
 */
public interface IngestExternalClient extends MockOrRestClient, PoolingStatusClient {
    /**
     * ingest upload file in local
     *
     * @param stream
     * @param tenantId
     * @param contextId
     * @param action
     * @return response
     * @throws IngestExternalException
     */
    // TODO P0 : add file name

    RequestResponse<JsonNode> upload(InputStream stream, Integer tenantId, String contextId, String action)
        throws IngestExternalException;

    /**
     * Download object stored by ingest operation
     *
     * @param objectId
     * @param type
     * @param tenantId
     * @return object as stream
     * @throws IngestExternalException
     * @throws VitamClientException
     * 
     */
    Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId)
        throws IngestExternalException;

    RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId, Integer tenantId)
        throws VitamClientException;

    Response updateOperationActionProcess(String actionId, String operationId, Integer tenantId)
        throws VitamClientException;

    ItemStatus getOperationProcessStatus(String id, Integer tenantId) throws VitamClientException;

    ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query, Integer tenantId)
        throws VitamClientException;

    RequestResponse<JsonNode> cancelOperationProcessExecution(String id, Integer tenantId)
        throws VitamClientException, BadRequestException;

    /**
     * Use updateOperationActionProcess
     * 
     * @param contextId
     * @param actionId
     * @param container
     * @param workflow
     * @param tenantId
     * @return
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     */
    @Deprecated // Not used
    ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow,
        Integer tenantId)
        throws InternalServerException, BadRequestException, VitamClientException;

    void initVitamProcess(String contextId, String container, String workFlow, Integer tenantId)
        throws InternalServerException, VitamClientException;

    /**
     * Use initVitamProcess
     * 
     * @param contextId
     * @param tenantId
     * @throws VitamException
     */
    @Deprecated
    void initWorkFlow(String contextId, Integer tenantId) throws VitamException;

    /**
     * Get the list of operations details
     * 
     * @param tenantId tenant id
     * @param query filter query
     * @return list of operations details
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> listOperationsDetails(Integer tenantId, ProcessQuery query) throws VitamClientException;

    // FIXME P1 : is tenant really necessary ?
    RequestResponse<JsonNode> getWorkflowDefinitions(Integer tenantId) throws VitamClientException;
}
