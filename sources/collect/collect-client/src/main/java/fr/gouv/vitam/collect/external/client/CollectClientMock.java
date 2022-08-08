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
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.UUID;

/**
 * Collect Client implementation for integration tests
 */
public class CollectClientMock extends AbstractMockClient implements CollectClient {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public RequestResponse<JsonNode> initProject(VitamContext vitamContext,
        ProjectDto projectDto) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> updateProject(VitamContext vitamContext,
        ProjectDto projectDto) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getProjectById(VitamContext vitamContext,
        String projectId) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> getProjects(VitamContext vitamContext) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> deleteTransactionById(VitamContext vitamContext, String transactionId) {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> deleteProjectById(VitamContext vitamContext, String projectId) {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitById(VitamContext vitamContext, String unitId)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitsByTransaction(VitamContext vitamContext,
        String transactionId) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getObjectById(VitamContext vitamContext,
        String gotId) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponse<JsonNode> initTransaction(VitamContext vitamContext,
        TransactionDto transactionDto, String projectId) throws VitamClientException {
        transactionDto.setId(UUID.randomUUID().toString());
        return new RequestResponseOK<JsonNode>()
            .setHttpCode(Response.Status.OK.getStatusCode())
            .addResult(mapper.valueToTree(transactionDto));
    }

    @Override
    public RequestResponseOK<JsonNode> uploadArchiveUnit(VitamContext vitamContext,
        JsonNode unitJsonNode, String transactionId)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> addObjectGroup(VitamContext vitamContext,
        String unitId, Integer version, JsonNode objectJsonNode, String usage) throws VitamClientException {
        return null;
    }

    @Override
    public Response addBinary(VitamContext vitamContext, String unitId, Integer version,
        InputStream inputStreamUploaded, String usage)
        throws VitamClientException {
        return null;
    }

    @Override
    public Response closeTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> ingest(VitamContext vitamContext,
        String transactionId) throws VitamClientException {
        return null;
    }

    @Override
    public Response uploadProjectZip(VitamContext vitamContext, String projectId, InputStream inputStreamUploaded)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> selectUnits(VitamContext vitamContext, JsonNode jsonQuery)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitsByProjectId(VitamContext vitamContext, String projectId,
        JsonNode dslQuery)
        throws VitamClientException {
        return null;
    }

    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage, int version)
        throws VitamClientException {
        return null;
    }

}
