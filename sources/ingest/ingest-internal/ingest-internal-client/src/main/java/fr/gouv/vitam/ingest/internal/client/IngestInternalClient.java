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
package fr.gouv.vitam.ingest.internal.client;

import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

/**
 * Ingest Internal client interface
 */
public interface IngestInternalClient extends MockOrRestClient {
    /**
     * Upload compressed SIP as stream
     *
     * @param archiveType is a format (mime type) of SIP (should be zip ,tar, tar.gz or tar.bz2)
     * @param inputStream SIP
     * @param workflow workflow information
     * @param action Identifier
     * @throws VitamException if stream is null
     */
    void upload(InputStream inputStream, MediaType archiveType, WorkFlow workflow, String action) throws VitamException;

    /**
     * Create only Logbook
     *
     * @param logbookParametersList
     * @throws VitamException
     */
    void uploadInitialLogbook(Iterable<LogbookOperationParameters> logbookParametersList) throws VitamException;

    /**
     * Download object stored by ingest operation
     *
     * @param objectId
     * @param type
     * @return object as stream
     * @throws InvalidParseOperationException
     * @throws IngestInternalClientServerException
     * @throws IngestInternalClientNotFoundException
     */
    Response downloadObjectAsync(String objectId, IngestCollection type)
        throws InvalidParseOperationException, IngestInternalClientServerException, IngestInternalClientNotFoundException;

    /**
     * getOperationProcessStatus:
     *
     * get operation process status**
     *
     * @param id : operation identifier*
     * @return ItemStatus response containing message and status*
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */

    ItemStatus getOperationProcessStatus(String id)
        throws VitamClientException, InternalServerException, BadRequestException;

    /**
     * getOperationProcessExecutionDetails : get operation processing execution details
     *
     * @param id : operation identifier
     * @return Engine response containing message and status
     * @throws VitamClientException
     */

    RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id) throws VitamClientException;

    /**
     * cancelOperationProcessExecution : cancel processing operation
     *
     * @param id : operation identifier
     * @return ItemStatus response containing message and status
     * @throws VitamClientException
     */
    RequestResponse<ItemStatus> cancelOperationProcessExecution(String id) throws VitamClientException;

    /**
     * updateOperationActionProcess : update operation processing status
     *
     * @param actionId : identify the action to be executed by the workflow(next , pause,resume)
     * @param operationId : operation identifier
     * @return Response containing message and status
     * @throws VitamClientException
     */
    RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
        throws VitamClientException;

    /**
     * initWorkflow : init workFlow Process
     *
     * @param workFlow information
     * @throws VitamClientException
     * @throws VitamException
     */
    void initWorkflow(WorkFlow workFlow) throws VitamException;

    /**
     * Retrieve all the workflow operations
     *
     * @param query Query model
     * @return All details of the operations
     * @throws VitamClientException
     */
    RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) throws VitamClientException;

    /**
     * Retrieve all the workflow definitions.
     *
     * @return workflow definitions
     * @throws VitamClientException
     */
    RequestResponse<WorkFlow> getWorkflowDefinitions() throws VitamClientException;

    /**
     * @param WorkflowIdentifier
     * @return
     * @throws VitamClientException
     */
    Optional<WorkFlow> getWorkflowDetails(String WorkflowIdentifier) throws VitamClientException;

    void saveObjectToWorkspace(String id, String objectName, InputStream inputStream) throws VitamClientException;
}
