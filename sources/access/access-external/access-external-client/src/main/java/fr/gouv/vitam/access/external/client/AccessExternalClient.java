/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientAccessUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientIllegalAccessRequestOperationOnSyncOfferException;
import fr.gouv.vitam.common.external.client.BasicClient;
import fr.gouv.vitam.common.model.JsonLineIterator;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.storage.AccessRequestReference;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.StatusByAccessRequest;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;

/**
 * Access External Client Interface
 */
public interface AccessExternalClient extends BasicClient {

    /**
     * selectUnits /units
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * streamUnits /units
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    JsonLineIterator<JsonNode> streamUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * selectUnitbyId GET(POST overrided) /units/{id}
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @param unitId the unit id to select
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException;

    /**
     * updateUnitbyId UPDATE /units/{id}
     *
     * @param vitamContext the vitam context
     * @param updateQuery the update query
     * @param unitId the unit id to update
     * @return Json representation
     * @throws VitamClientException
     * @deprecated This Method is no longer maintained and is no longer acceptable for updating units.
     * <p> Use {@link AccessExternalClient#massUpdateUnits(VitamContext, JsonNode)} or
     * {@link AccessExternalClient#massUpdateUnitsRules(VitamContext, JsonNode)} instead.
     */
    @Deprecated
    RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery, String unitId)
        throws VitamClientException;

    /**
     * selectObjectById
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @param unitId the unit id for getting object
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws VitamClientException;

    /**
     * Download the object by parent unit id, qualifier usage & version
     *
     * The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * When accessing an object stored on tape storage offer that is not available for immediate access, a {@link VitamClientAccessUnavailableDataFromAsyncOfferException} is thrown. Access to object require creating an Access Request using the {@link #createObjectAccessRequestByUnitId} method.
     *
     * @param vitamContext the vitam context
     * @param unitId the unit id for getting the object
     * @param usage kind of usage
     * @param version the version
     * @return Response including InputStream
     * @throws VitamClientAccessUnavailableDataFromAsyncOfferException thrown when access to object in not currently available from async storage offer. Access request required.
     * @throws VitamClientException when an error occurs
     */
    Response getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage,
        int version)
        throws VitamClientException;

    /**
     * Create an access request for accessing the object of an archive unit stored on an async storage offer (tape storage offer)
     * Access requests for objects stored on non-asynchronous storage offers does NOT require Access Request creation.
     *
     * The status of returned Access Requests should be monitoring periodically (typically every few minutes) to check the availability of data via the {@link #checkAccessRequestStatuses} method.
     * Once an Access Request status is {@link AccessRequestStatus#READY}, the data can be downloaded, using the {@link #getObjectStreamByUnitId} method, within the Access Request expiration delay defined in the tape storage offer configuration by the administrator.
     *
     * CAUTION : After successful download of object, caller SHOULD remove the Access Request using the {@link #removeAccessRequest} method to free up disk resources on tape storage offer.
     *
     * @param vitamContext the vitam context
     * @param unitId the unit id for getting the object
     * @param usage kind of usage
     * @param version the version
     * @return RequestResponse with an optional {@link AccessRequestReference} entry. An Access request is created and returned only if required (async storage offer). Otherwise, an empty RequestResponse is returned.
     * @throws AccessExternalClientNotFoundException when unit has no object group with such object qualifier usage and version
     * @throws VitamClientException when an error occurs
     */
    RequestResponse<AccessRequestReference> createObjectAccessRequestByUnitId(VitamContext vitamContext, String unitId,
        String usage, int version)
        throws VitamClientException, AccessExternalClientNotFoundException;

    /**
     * Bulk check of the status of a set of Access Requests.
     * Once Access Request status is {@link AccessRequestStatus#READY}, the object is guaranteed to be available for download using the {@link #getObjectStreamByUnitId} method within the Access Request expiration delay defined in the tape storage offer configuration by the administrator.
     *
     * Access Requests are only visible within their tenant. Attempts to check status of Access Requests of another tenant will return a {@link AccessRequestStatus#NOT_FOUND}.
     * Attempts to check an Access Request status for a non-asynchronous storage offer (tape storage offer) throws an {@link VitamClientIllegalAccessRequestOperationOnSyncOfferException}.
     *
     * @param vitamContext the vitam context
     * @param accessRequestReferences the set of Access Requests to check
     * @return RequestResponse with of access request statuses.
     * @throws VitamClientException when an error occurs
     */
    RequestResponse<StatusByAccessRequest> checkAccessRequestStatuses(VitamContext vitamContext,
        Collection<AccessRequestReference> accessRequestReferences)
        throws VitamClientException;

    /**
     * Removes an Access Request from an async storage offer (tape storage offer).
     * After removing an Access Request, object in not more guaranteed to be accessible for download.
     *
     * Attempts to remove an Access Request from a non-asynchronous storage offer throws an {@link VitamClientIllegalAccessRequestOperationOnSyncOfferException}.
     * Attempts to remove an already removed Access Request does NOT throw any error (idempotency).
     * Access Requests are only visible within their tenant. Attempts to remove an Access Request of another tenant does NOT delete access request.
     *
     * @param vitamContext the vitam context
     * @param accessRequestReference the Access Request to remove
     * @return RequestResponse.
     * @throws VitamClientException when an error occurs
     */
    RequestResponse<Void> removeAccessRequest(VitamContext vitamContext, AccessRequestReference accessRequestReference)
        throws VitamClientException;

    /**
     * selectOperation
     *
     * @param vitamContext the vitam context
     * @param select the select query
     * @return logbookOperation representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookOperation> selectOperations(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException;

    /**
     * selectOperationbyId
     *
     * @param vitamContext the vitam context
     * @param operationId the operation id
     * @param select the select query
     * @return logbookOperation representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String operationId, JsonNode select)
        throws VitamClientException;

    /**
     * selectUnitLifeCycleById
     *
     * @param vitamContext the vitam context
     * @param unitLifeCycleId the unit LFC id
     * @param select the select query
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(VitamContext vitamContext,
        String unitLifeCycleId, JsonNode select)
        throws VitamClientException;

    /**
     * selectObjectGroupLifeCycleById
     *
     * @param vitamContext the vitam context
     * @param objectGroupLifeCycleId the objectGroup LFC id
     * @param select the select query
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String objectGroupLifeCycleId, JsonNode select)
        throws VitamClientException;

    /**
     * @param vitamContext the vitam context
     * @return the result of the information obtained in the DIP
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> exportDIP(VitamContext vitamContext,
        JsonNode dslRequest) throws VitamClientException;

    RequestResponse<JsonNode> transfer(VitamContext vitamContext,
        TransferRequest transferRequest) throws VitamClientException;

    /**
     * getDIPById<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param dipId the previously generated DIP id to download the DIP
     * @return Response including InputStream
     * @throws VitamClientException
     */
    Response getDIPById(VitamContext vitamContext, String dipId)
        throws VitamClientException;

    /**
     * Get Transfer SIP
     *
     * @param vitamContext
     * @param transferId
     * @return
     */
    Response getTransferById(VitamContext vitamContext, String transferId) throws VitamClientException;

    /**
     * Performs a reclassification workflow.
     *
     * @param vitamContext the vitam context
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     * @return Response
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> reclassification(VitamContext vitamContext, JsonNode reclassificationRequest)
        throws VitamClientException;

    /**
     * Mass update of archive units.
     *
     * @param vitamContext the vitam context
     * @param updateQuery the update query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> massUpdateUnits(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException;

    /**
     * Mass update of archive units rules.
     *
     * @param vitamContext the vitam context
     * @param updateRulesQuery the update request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> massUpdateUnitsRules(VitamContext vitamContext, JsonNode updateRulesQuery)
        throws VitamClientException;

    RequestResponse<JsonNode> revertUpdateUnits(VitamContext vitamContext, JsonNode revertUpdateQuery)
        throws VitamClientException;

    /**
     * Bulk atomic update of archive units by atomic query.
     *
     * @param vitamContext the vitam context
     * @param updateQuery the bulk atomic update request
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> bulkAtomicUpdateUnits(VitamContext vitamContext, JsonNode updateRequest)
        throws VitamClientException;

    /**
     * selectObjects /objects
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectObjects(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * Select units with inherited rules by select query (DSL)
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * Performs an elimination analysis workflow .
     *
     * @param eliminationRequestBody Object Body DSL request for elimination and Date
     * @return Json representation
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> startEliminationAnalysis(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody)
        throws VitamClientException;

    /**
     * Performs an elimination action workflow .
     *
     * @param eliminationRequestBody Object Body DSL request for elimination and Date
     * @return Json representation
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> startEliminationAction(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody)
        throws VitamClientException;

    /**
     * Get AccessLog file matching the given params
     *
     * @param vitamContext the vitam context
     * @param params Could contains StartDate and EndDate information for file filter
     * @return accesslog file
     * @throws VitamClientException
     */
    Response getAccessLog(VitamContext vitamContext, JsonNode params)
        throws VitamClientException;

    /**
     * compute inherited rules.
     *
     * @param vitamContext the vitam context
     * @param ComputedInheritedRulesQuery the query request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> computedInheritedRules(VitamContext vitamContext, JsonNode ComputedInheritedRulesQuery)
        throws VitamClientException;

    /**
     * compute inherited rules.
     *
     * @param vitamContext the vitam context
     * @param deleteComputedInheritedRulesQuery the query request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> deleteComputedInheritedRules(VitamContext vitamContext,
        JsonNode deleteComputedInheritedRulesQuery)
        throws VitamClientException;

    RequestResponse<JsonNode> launchPreservation(VitamContext vitamContext, PreservationRequest preservationRequest)
        throws VitamClientException;

    RequestResponse<JsonNode> transferReply(VitamContext vitamContext, InputStream transferReply)
        throws VitamClientException;
}
