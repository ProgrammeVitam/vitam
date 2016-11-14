package fr.gouv.vitam.access.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.client2.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {
    private static final String COLLECTION_NOT_VALID = "Collection not valid";


    @Override
    public Status checkDocuments(AdminCollections documentType, InputStream stream) throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return Status.OK;
        }
        throw new AccessExternalClientNotFoundException(COLLECTION_NOT_VALID);
    }

    @Override
    public Status createDocuments(AdminCollections documentType, InputStream stream) throws AccessExternalClientNotFoundException, AccessExternalClientException {        
        StreamUtils.closeSilently(stream);
        return Status.OK;
    }

    @Override
    public JsonNode findDocuments(AdminCollections documentType, JsonNode select)
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
    public JsonNode findDocumentById(AdminCollections documentType, String documentId)
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
    public Status deleteDocuments(AdminCollections documentType) throws AccessExternalClientException {
        return Status.OK;
    }
}
