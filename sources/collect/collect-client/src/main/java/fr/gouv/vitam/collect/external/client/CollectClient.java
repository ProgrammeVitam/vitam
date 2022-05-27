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
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Collect Client Interface
 */
public interface CollectClient extends MockOrRestClient {

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
    RequestResponse<JsonNode> updateProject(VitamContext vitamContext,
        ProjectDto projectDto) throws VitamClientException;

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
     * get all collect project by tenant
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     * @param vitamContext
     */
    RequestResponse<JsonNode> getProjects(VitamContext vitamContext) throws VitamClientException;



    /**
     * get an archive unit by Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponseOK<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> getUnitById(VitamContext vitamContext, String unitId) throws VitamClientException;



    /**
     * get an archive unit by transaction Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> getUnitsByTransaction(VitamContext vitamContext,
        String transactionId) throws VitamClientException;



    /**
     * get an object group by Id
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponseOK<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> getObjectById(VitamContext vitamContext, String gotId) throws VitamClientException;



    /**
     * Initialize a collect transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> guid created for the transaction
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponse<JsonNode> initTransaction(VitamContext vitamContext,
        TransactionDto transactionDto)
        throws VitamClientException;

    /**
     * ADD Archive Unit
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> Archive Unit saved
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> uploadArchiveUnit(VitamContext vitamContext,
        JsonNode unitJsonNode, String transactionId)
        throws VitamClientException;

    /**
     * ADD Object Group
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponse<JsonNode> objectgroup saved
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> addObjectGroup(VitamContext vitamContext, String unitId,
        Integer version, JsonNode objectJsonNode, String usage)
        throws VitamClientException;

    /**
     * ADD Binary
     *
     * Consume and produce MediaType.OCTET_STREAM
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    Response addBinary(VitamContext vitamContext, String unitId, Integer version, InputStream inputStreamUploaded,
        String usage)
        throws VitamClientException;

    /**
     * Close Transaction
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    Response closeTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException;


    /**
     * Generate SIP + Send to Vitam
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return RequestResponseOK<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> ingest(VitamContext vitamContext, String transactionId) throws
        VitamClientException;

    /**
     * Upload zip and consume
     *
     * Consume and produce CommonMediaType.ZIP
     *
     * @return RequestResponse<JsonNode>
     * @throws VitamClientException exception occurs when parse operation failed
     */
    Response uploadProjectZip(VitamContext vitamContext, String projectId, InputStream inputStreamUploaded) throws VitamClientException;


    /**
     * get archive unit by Dsl query
     *
     * Consume and produce MediaType.APPLICATION_JSON
     *
     * @return Response
     * @throws VitamClientException exception occurs when parse operation failed
     */
    RequestResponseOK<JsonNode> selectUnits(VitamContext vitamContext, JsonNode jsonQuery) throws VitamClientException;


}
