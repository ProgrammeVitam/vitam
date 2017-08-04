package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.stream.StreamUtils;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {
    private static final String COLLECTION_NOT_VALID = "Collection not valid";


    @Override
    public Status checkDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return Status.OK;
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public Status createDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
    }

    @Override
    public RequestResponse findDocuments(AdminCollections documentType, JsonNode select, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRuleList();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormatList();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public RequestResponse findDocuments(AdminCollections documentType, JsonNode select, Integer tenantId, String contractName)
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRuleList();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormatList();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public RequestResponse findDocumentById(AdminCollections documentType, String documentId, Integer tenantId)
        throws AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRule();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormat();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(String id, JsonNode query, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterDetail();
    }

    @Override
    public RequestResponse importContracts(InputStream contracts, Integer tenantId, AdminCollections collection)
        throws InvalidParseOperationException {
        if (AdminCollections.ACCESS_CONTRACTS.equals(collection))
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
        else
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());

    }

    @Override
    public RequestResponse updateAccessContract(String id, JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
    }

    @Override
    public RequestResponse updateIngestContract(String id, JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());
    }

    @Override
    public RequestResponse createProfiles(InputStream profiles, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse importProfileFile(String profileMetadataId, InputStream profile, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return new RequestResponseOK().setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public Response downloadProfileFile(String profileMetadataId, Integer tenantId) throws AccessExternalClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, IOUtils.toInputStream("Vitam Test"),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse importContexts(InputStream contexts, Integer tenantId) 
        throws InvalidParseOperationException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse updateContext(String id, JsonNode queryDsl, Integer tenantId)
        throws AccessExternalClientException, InvalidParseOperationException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse checkTraceabilityOperation(JsonNode query, Integer tenantId, String contractName)
        throws AccessExternalClientServerException, InvalidParseOperationException {
        return ClientMockResultHelper.checkOperationTraceability();
    }

    @Override
    public Response downloadTraceabilityOperationFile(String operationId, Integer tenantId, String contractName)
        throws AccessExternalClientServerException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }
}
