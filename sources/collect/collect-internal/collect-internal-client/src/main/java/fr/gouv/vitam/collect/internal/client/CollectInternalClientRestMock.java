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
package fr.gouv.vitam.collect.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Collect Client implementation for production environment
 */
public class CollectInternalClientRestMock extends AbstractMockClient implements CollectInternalClient {
    public static String TRANSACTION_ID = "transaction-id";

    public CollectInternalClientRestMock() {
    }


    @Override
    public RequestResponse<JsonNode> initProject(ProjectDto projectDto) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> updateProject(ProjectDto projectDto) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getProjectById(String projectId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getTransactionByProjectId(String projectId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getTransactionById(String transactionId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getProjects() {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> deleteTransactionById(String transactionId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> deleteProjectById(String projectId) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitById(String unitId) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitsByTransaction(String transactionId, JsonNode query) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getObjectById(String gotId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> initTransaction(TransactionDto transactionDto, String projectId)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> uploadArchiveUnit(JsonNode unitJsonNode, String transactionId)
        throws VitamClientException {
        if (TRANSACTION_ID.equals(transactionId)) {

            return null;
        }
        throw new VitamClientException("Mock exception in client");
    }

    @Override
    public RequestResponseOK<JsonNode> addObjectGroup(String unitId, Integer version, JsonNode objectJsonNode,
        String usage) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> addBinary(String unitId, Integer version, InputStream inputStreamUploaded,
        String usage) {
        return null;
    }

    @Override
    public Response closeTransaction(String transactionId) {
        return null;
    }

    @Override
    public InputStream generateSip(String transactionId) {
        return null;
    }

    @Override
    public Response abortTransaction(String transactionId) {
        return null;
    }

    @Override
    public Response reopenTransaction(String transactionId) {
        return null;
    }

    @Override
    public void uploadZipToTransaction(String transactionId, InputStream inputStreamUploaded) {

    }

    @Override
    public RequestResponseOK<JsonNode> getUnitsByProjectId(String projectId, JsonNode dslQuery) {
        return null;
    }

    @Override
    public Response getObjectStreamByUnitId(String unitId, String usage, int version) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> searchProject(CriteriaProjectDto criteria) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> updateTransaction(TransactionDto transactionDto) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> updateUnits(String transactionId, InputStream is) {
        return null;
    }

    @Override
    public Response changeTransactionStatus(String transactionId, TransactionStatus transactionStatus) {
        return null;
    }

    @Override
    public Response attachVitamOperationId(String transactionId, String operationId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> selectUnitsWithInheritedRules(String transactionId, JsonNode selectQuery) {
        return null;
    }
}