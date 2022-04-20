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
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.common.GlobalDataRest.X_ACCESS_CONTRAT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

/**
 * Collect Client implementation for production environment
 */
public class CollectClientRest extends DefaultClient implements CollectClient {
    private static final String TENANT_ID = "0";
    private static final String X_ACCESS_CONTRACT_ID = "ContratTNR";
    private static final String TRANSACTION_PATH = "/transactions";
    private static final String UNITS_PATH = "/units";

    public CollectClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> initTransaction(TransactionDto transactionDto)
        throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);

        VitamRequestBuilder request = post()
            .withPath(TRANSACTION_PATH)
            .withHeaders(headers)
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(transactionDto)
            .withJsonContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponseOK<JsonNode> uploadArchiveUnit(String transactionId, JsonNode unitJsonNode)
        throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);
        try (Response response = make(
            post().withPath(TRANSACTION_PATH + "/" + transactionId + UNITS_PATH).withHeaders(headers)
                .withBody(unitJsonNode)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> addObjectGroup(String unitId, String usage, Integer version,
        JsonNode objectJsonNode) throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);
        try (Response response = make(
            post().withPath(UNITS_PATH + "/" + unitId + "/objects/" + usage + "/" + version).withHeaders(headers)
                .withBody(objectJsonNode)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public Response addBinary(String unitId, String usage, Integer version, InputStream inputStreamUploaded)
        throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);
        try (Response response = make(post()
            .withPath(UNITS_PATH + "/" + unitId + "/objects/" + usage + "/" + version + "/binary")
            .withHeaders(headers)
            .withBody(inputStreamUploaded)
            .withOctetContentType())) {
            check(response);
            return response;
        }
    }

    @Override
    public Response closeTransaction(String transactionId) throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);
        try (Response response = make(post()
            .withPath(TRANSACTION_PATH + "/" + transactionId + "/close")
            .withHeaders(headers)
            .withJsonAccept())) {
            check(response);
            return response;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> ingest(String transactionId)
        throws VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_TENANT_ID, TENANT_ID);
        headers.add(X_ACCESS_CONTRAT_ID, X_ACCESS_CONTRACT_ID);
        try (Response response = make(post()
            .withPath(TRANSACTION_PATH + "/" + transactionId + "/send")
            .withHeaders(headers)
            .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    private void check(Response response) throws VitamClientException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientException(String
            .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }


}
