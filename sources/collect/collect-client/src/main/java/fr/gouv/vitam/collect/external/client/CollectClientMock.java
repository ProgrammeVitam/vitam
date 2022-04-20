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
package fr.gouv.vitam.collect.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
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
    public RequestResponse<JsonNode> initTransaction(TransactionDto transactionDto) throws VitamClientException {
        transactionDto.setId(UUID.randomUUID().toString());
        return new RequestResponseOK<JsonNode>()
            .setHttpCode(Response.Status.OK.getStatusCode())
            .addResult(mapper.valueToTree(transactionDto));
    }

    @Override
    public RequestResponseOK<JsonNode> uploadArchiveUnit(String transactionId, JsonNode unitJsonNode)
        throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> addObjectGroup(String unitId, String usage, Integer version,
        JsonNode objectJsonNode) throws VitamClientException {
        return null;
    }

    @Override
    public Response addBinary(String unitId, String usage, Integer version, InputStream inputStreamUploaded)
        throws VitamClientException {
        return null;
    }

    @Override
    public Response closeTransaction(String transactionId) throws VitamClientException {
        return null;
    }

    @Override
    public RequestResponseOK<JsonNode> ingest(String transactionId) throws VitamClientException {
        return null;
    }
}
