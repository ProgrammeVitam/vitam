package fr.gouv.vitam.access.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {
    private static final String COLLECTION_NOT_VALID = "Collection not valid";


    @Override
    public Response checkDocuments(AdminCollections documentType, InputStream stream)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return Response.ok().build();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public Response createDocuments(AdminCollections documentType, InputStream stream)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        return Response.ok().build();
    }

    @Override
    public RequestResponse findDocuments(AdminCollections documentType, JsonNode select)
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
    public RequestResponse findDocumentById(AdminCollections documentType, String documentId)
        throws AccessExternalClientException, InvalidParseOperationException {
        if (AdminCollections.RULES.equals(documentType)) {
            return ClientMockResultHelper.getRule();
        }
        if (AdminCollections.FORMATS.equals(documentType)) {
            return ClientMockResultHelper.getFormat();
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

}
