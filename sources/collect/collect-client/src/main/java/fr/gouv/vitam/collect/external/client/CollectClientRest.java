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
import fr.gouv.vitam.collect.external.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

/**
 * Collect Client implementation for production environment
 */
public class CollectClientRest extends DefaultClient implements CollectClient {
    private static final String TRANSACTION_PATH = "/transactions";
    private static final String PROJECT_PATH = "/projects";
    private static final String UNITS_PATH = "/units";
    private static final String OBJECTS_PATH = "/objects";
    private static final String BINARY_PATH = "/binary";

    public CollectClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> initProject(VitamContext vitamContext,
        ProjectDto projectDto) throws VitamClientException {

        VitamRequestBuilder request = post()
            .withPath(PROJECT_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(projectDto)
            .withJsonContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateProject(VitamContext vitamContext,
        ProjectDto projectDto) throws VitamClientException {

        VitamRequestBuilder request = put()
            .withPath(PROJECT_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(projectDto)
            .withJsonContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> getProjectById(VitamContext vitamContext,
        String projectId) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(PROJECT_PATH + "/" + projectId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }


    @Override
    public RequestResponse<JsonNode> getTransactionById(VitamContext vitamContext,
        String transactionId) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(TRANSACTION_PATH + "/" + transactionId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> getTransactionByProjectId(VitamContext vitamContext,
        String projectId) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(PROJECT_PATH + "/" + projectId + TRANSACTION_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }


    @Override
    public RequestResponse<JsonNode> deleteProjectById(VitamContext vitamContext,
        String projectId) throws VitamClientException {

        VitamRequestBuilder request = delete()
            .withPath(PROJECT_PATH + "/" + projectId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> deleteTransactionById(VitamContext vitamContext,
        String transactionId) throws VitamClientException {

        VitamRequestBuilder request = delete()
            .withPath(TRANSACTION_PATH + "/" + transactionId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponse<JsonNode> getProjects(VitamContext vitamContext) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(PROJECT_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitById(VitamContext vitamContext, String unitId)
        throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(UNITS_PATH + "/" + unitId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> getUnitsByTransaction(VitamContext vitamContext,
        String transactionId) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath(TRANSACTION_PATH + "/" + transactionId + UNITS_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> getObjectById(VitamContext vitamContext,
        String gotId) throws VitamClientException {

        VitamRequestBuilder request = get()
            .withPath("/objects/" + gotId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public RequestResponse<JsonNode> initTransaction(VitamContext vitamContext,
        TransactionDto transactionDto, String projectId)
        throws VitamClientException {

        VitamRequestBuilder request = post()
            .withPath(PROJECT_PATH + "/" + projectId + TRANSACTION_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(transactionDto)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        }
    }

    @Override
    public RequestResponseOK<JsonNode> uploadArchiveUnit(VitamContext vitamContext,
        JsonNode unitJsonNode, String transactionId)
        throws VitamClientException {
        try (Response response = make(
            post().withPath(TRANSACTION_PATH + "/" + transactionId + UNITS_PATH).withHeaders(vitamContext.getHeaders())
                .withBody(unitJsonNode)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> addObjectGroup(VitamContext vitamContext,
        String unitId, Integer version, JsonNode objectJsonNode, String usage) throws VitamClientException {
        try (Response response = make(
            post().withPath(UNITS_PATH + "/" + unitId + "/objects/" + usage + "/" + version)
                .withHeaders(vitamContext.getHeaders())
                .withBody(objectJsonNode)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public Response addBinary(VitamContext vitamContext, String unitId, Integer version,
        InputStream inputStreamUploaded, String usage)
        throws VitamClientException {
        try (Response response = make(post()
            .withPath(UNITS_PATH + "/" + unitId + "/objects/" + usage + "/" + version + "/binary")
            .withHeaders(vitamContext.getHeaders())
            .withBody(inputStreamUploaded)
            .withOctetContentType())) {
            check(response);
            return response;
        }
    }

    @Override
    public Response closeTransaction(VitamContext vitamContext, String transactionId) throws VitamClientException {
        try (Response response = make(post()
            .withPath(TRANSACTION_PATH + "/" + transactionId + "/close")
            .withHeaders(vitamContext.getHeaders())
            .withJsonAccept())) {
            check(response);
            return response;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> ingest(VitamContext vitamContext,
        String transactionId)
        throws VitamClientException {
        try (Response response = make(post()
            .withPath(TRANSACTION_PATH + "/" + transactionId + "/send")
            .withHeaders(vitamContext.getHeaders())
            .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    @Override
    public Response uploadProjectZip(VitamContext vitamContext, String projectId, InputStream inputStreamUploaded)
        throws VitamClientException {
        try (Response response = make(post()
            .withPath(PROJECT_PATH + "/" + projectId + "/binary")
            .withHeaders(vitamContext.getHeaders())
            .withBody(inputStreamUploaded)
            .withContentType(CommonMediaType.ZIP_TYPE))) {
            check(response);
            return response;
        }
    }

    @Override
    public RequestResponseOK<JsonNode> selectUnits(VitamContext vitamContext, JsonNode jsonQuery)
        throws VitamClientException {
        try (Response response = make(
            get().withPath(UNITS_PATH)
                .withHeaders(vitamContext.getHeaders())
                .withBody(jsonQuery)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }
    
     @Override
     public RequestResponseOK<JsonNode> getUnitsByProjectId(VitamContext vitamContext,
         String projectId, JsonNode dslQuery) throws VitamClientException{
         try (Response response = make(
             get().withPath(PROJECT_PATH + "/" + projectId + UNITS_PATH)
                 .withHeaders(vitamContext.getHeaders())
                 .withHeader(EXPECT, EXPECT_CONTINUE)
                 .withBody(dslQuery)
                 .withJson())) {
             check(response);
             RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
             return (RequestResponseOK<JsonNode>) result;
         }
     }

    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage, int version)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(UNITS_PATH + "/" + unitId + OBJECTS_PATH + "/" + usage + "/" + version + BINARY_PATH)
            .withHeaders(vitamContext.getHeaders())
            .withJsonContentType()
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } finally {
            if (response != null && SUCCESSFUL != response.getStatusInfo().getFamily()) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponseOK<JsonNode> searchProject(VitamContext vitamContext, CriteriaProjectDto criteria)
        throws VitamClientException {
        try (Response response = make(
            get().withPath(PROJECT_PATH)
                .withHeaders(vitamContext.getHeaders())
                .withBody(criteria)
                .withJson())) {
            check(response);
            RequestResponse<JsonNode> result = RequestResponse.parseFromResponse(response, JsonNode.class);
            return (RequestResponseOK<JsonNode>) result;
        }
    }

    private void check(Response response) throws VitamClientException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientException(String
            .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }


}
