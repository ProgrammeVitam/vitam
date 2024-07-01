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
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;

import javax.ws.rs.core.Response;
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
     * Upload zip and consume
     *
     * Consume CommonMediaType.ZIP
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> uploadProjectZip(
        VitamContext vitamContext,
        String transactionId,
        InputStream inputStreamUploaded
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

    RequestResponse<JsonNode> updateUnits(VitamContext vitamContext, String transactionId, InputStream is)
        throws VitamClientException;

    RequestResponse<JsonNode> selectUnitsWithInheritedRules(
        VitamContext vitamContext,
        String transactionId,
        JsonNode selectQuery
    ) throws VitamClientException;
}
