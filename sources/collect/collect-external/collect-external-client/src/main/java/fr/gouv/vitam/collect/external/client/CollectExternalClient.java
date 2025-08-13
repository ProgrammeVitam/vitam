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
package fr.gouv.vitam.collect.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.Beta;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.OperationIdDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.elimination.DeletionRequestBody;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Collect Client Interface
 */
public interface CollectExternalClient extends MockOrRestClient {
    /**
     * Initialize a collect project
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> initProject(VitamContext vitamContext, ProjectDto projectDto) throws VitamClientException;

    /**
     * Update a collect project
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> updateProject(VitamContext vitamContext, ProjectDto projectDto)
        throws VitamClientException;

    /**
     * get a collect project
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getProjectById(VitamContext vitamContext, String projectId) throws VitamClientException;

    /**
     * get a collect transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getTransactionByProjectId(VitamContext vitamContext, String projectId)
        throws VitamClientException;

    /**
     * get a collect transaction by ID
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getTransactionById(VitamContext vitamContext, String transactionId)
        throws VitamClientException;

    /**
     * get all collect project by tenant
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @param vitamContext
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getProjects(VitamContext vitamContext) throws VitamClientException;

    /**
     * delete a collect transaction
     *
     * produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> deleteTransactionById(VitamContext vitamContext, String transactionId)
        throws VitamClientException;

    /**
     * delete a collect project
     *
     * produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> deleteProjectById(VitamContext vitamContext, String projectId)
        throws VitamClientException;

    /**
     * get an archive unit by Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getUnitById(VitamContext vitamContext, String unitId) throws VitamClientException;

    /**
     * get an archive unit by transaction Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getUnitsByTransaction(VitamContext vitamContext, String transactionId, JsonNode query)
        throws VitamClientException;

    /**
     * get an object group by Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getObjectById(VitamContext vitamContext, String gotId) throws VitamClientException;

    /**
     * Initialize a collect transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> guid created for the transaction
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> initTransaction(
        VitamContext vitamContext,
        TransactionDto transactionDto,
        String projectId
    ) throws VitamClientException;

    /**
     * ADD Archive Unit
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> Archive Unit saved
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> uploadArchiveUnit(VitamContext vitamContext, JsonNode unitJsonNode, String transactionId)
        throws VitamClientException;

    /**
     * ADD Object Group
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> objectgroup saved
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> addObjectGroup(
        VitamContext vitamContext,
        String unitId,
        Integer version,
        JsonNode objectJsonNode,
        String usage
    ) throws VitamClientException;

    /**
     * ADD Binary
     *
     * Consume and produce MediaType.OCTET_STREAM
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    Response addBinary(
        VitamContext vitamContext,
        String unitId,
        Integer version,
        InputStream inputStreamUploaded,
        String usage
    ) throws VitamClientException;

    /**
     * Close Transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse closeTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException;

    /**
     * Generate SIP + Send to Vitam
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse ingest(VitamContext vitamContext, String transactionId) throws VitamClientException;

    /**
     * Abort Transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse abortTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException;

    /**
     * Reopen Transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse reopenTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException;

    /**
     * Upload zip to a transaction.
     * Consumes a ZIP (application/zip).
     *
     * @return RequestResponse<OperationIdDto> containing the operation ID
     * @throws VitamClientException exception occurs when parse operation failed
     * @deprecated Use uploadZipToTransaction() instead.
     */
    @Deprecated(forRemoval = true, since = "Vitam 7.1")
    default RequestResponse<JsonNode> uploadProjectZip(
        VitamContext vitamContext,
        String transactionId,
        InputStream inputStreamUploaded
    ) throws VitamClientException {
        return uploadZipToTransaction(vitamContext, transactionId, inputStreamUploaded, null, null);
    }

    /**
     * Upload zip to a project with automatic transaction management.
     * Consumes a ZIP (application/zip).
     * Warning: This Method is marked as "beta". API signature & behavior might evolve in next versions.
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    @Beta
    RequestResponse<String> uploadZipToProject(
        VitamContext vitamContext,
        String projectId,
        InputStream inputStreamUploaded
    ) throws VitamClientException;

    /**
     * Upload zip to a project with automatic transaction management.
     * Consumes a ZIP (application/zip) and it's encoding (optional)
     * Warning: This Method is marked as "beta". API signature & behavior might evolve in next versions.
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    @Beta
    RequestResponse<String> uploadZipToProject(
        VitamContext vitamContext,
        String projectId,
        InputStream inputStreamUploaded,
        @Nullable String encoding
    ) throws VitamClientException;

    /**
     * Upload zip to a transaction.
     * Consumes a ZIP (application/zip) and it's encoding (optional)
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> uploadZipToTransaction(
        VitamContext vitamContext,
        String transactionId,
        InputStream inputStreamUploaded,
        @Nullable String encoding,
        @Nullable String attachementId
    ) throws VitamClientException;

    /**
     * Get all AU attached to transactions related to project Id param
     *
     * @param vitamContext security context
     * @param unitId unit id
     * @param usage usage
     * @param version version
     * @return Response
     * @throws VitamClientException Thrown exception
     */
    Response getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage, int version)
        throws VitamClientException;

    /**
     * get all projects by criteria
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @param vitamContext
     * @param criteria
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse searchProject(VitamContext vitamContext, CriteriaProjectDto criteria) throws VitamClientException;

    /**
     * Update a collect transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> updateTransaction(VitamContext vitamContext, TransactionDto transactionDto)
        throws VitamClientException;

    /**
     * Update transaction units using CSV metadata file.
     * Defaults to updateUnitsWithCsvMetadata() method.
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     * @deprecated Use updateUnitsWithCsvMetadata() or updateUnitsWithJsonlMetadata() instead.
     */
    @Deprecated(forRemoval = true, since = "Vitam 7.1")
    default RequestResponse<JsonNode> updateUnits(VitamContext vitamContext, String transactionId, InputStream is)
        throws VitamClientException {
        return this.updateUnitsWithCsvMetadata(vitamContext, transactionId, is);
    }

    /**
     * Update transaction units using CSV metadata file
     * Consumes a csv file (text/csv).
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> updateUnitsWithCsvMetadata(
        VitamContext vitamContext,
        String transactionId,
        InputStream metadataCsvInputStream
    ) throws VitamClientException;

    /**
     * Update transaction units using JSON-Lines (jsonl) metadata file
     * Consumes an JSON-Lines file (application/octet-stream).
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> updateUnitsWithJsonlMetadata(
        VitamContext vitamContext,
        String transactionId,
        InputStream metadataJsonlInputStream
    ) throws VitamClientException;

    RequestResponse<JsonNode> selectUnitsWithInheritedRules(
        VitamContext vitamContext,
        String transactionId,
        JsonNode selectQuery
    ) throws VitamClientException;

    /**
     * Bulk atomic update of archive units with json queries of the provided collect transaction.
     * <br />
     * Units are update in blocking mode (might take a few moments to proceed before returning).
     * Please ensure proper request size / timeout is configured.
     *
     * @param transactionId the transaction Id. Must be a valid OPEN transaction.
     * @param updateQueriesJson the bulk update queries (null not allowed)
     */
    RequestResponseOK<BulkAtomicUpdateResult> bulkAtomicUpdateUnits(
        VitamContext vitamContext,
        String transactionId,
        JsonNode updateQueriesJson
    ) throws VitamClientException;

    /**
     * Performs a deletion action workflow  on transaction.
     *
     * @param deletionRequestBody Object Body DSL request for deletion and Date
     * @param transactionId transaction ID
     * @return Json representation
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> performDeletionActionOnTransaction(
        VitamContext vitamContext,
        String transactionId,
        DeletionRequestBody deletionRequestBody
    ) throws VitamClientException;

    /**
     * Performs a reclassification workflow on transaction.
     *
     * @param vitamContext the vitam context
     * @param transactionId transaction ID
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     * @return Response
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> performReclassificationOnTransaction(
        VitamContext vitamContext,
        String transactionId,
        JsonNode reclassificationRequest
    ) throws VitamClientException;

    /**
     * ingest SIP to transaction
     *
     * @param vitamContext the vitam context
     * @param transactionId
     * @param stream
     * @return response containing the operation ID
     * @throws VitamClientException
     */
    RequestResponse<OperationIdDto> uploadSipToTransaction(
        VitamContext vitamContext,
        String transactionId,
        InputStream stream
    ) throws VitamClientException;
}
