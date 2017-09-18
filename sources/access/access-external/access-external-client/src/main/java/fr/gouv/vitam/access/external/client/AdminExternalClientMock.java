package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.client.VitamContext;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {
    private static final String COLLECTION_NOT_VALID = "Collection not valid";


    @Override
    public Response checkDocuments(VitamContext vitamContext, AdminCollections documentType,
        InputStream stream)
        throws VitamClientException {
        StreamUtils.closeSilently(stream);

        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream("Vitam Test"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
        } else {
            try {
                return new AbstractMockClient.FakeInboundResponse(Status.INTERNAL_SERVER_ERROR,
                    JsonHandler.writeToInpustream(VitamCodeHelper
                        .toVitamError(VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_ERROR, "Collection not found")),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            } catch (InvalidParseOperationException e) {
                return new AbstractMockClient.FakeInboundResponse(Status.INTERNAL_SERVER_ERROR,
                    new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes()),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            }
        }
    }

    @Override
    public Status createDocuments(VitamContext vitamContext, AdminCollections documentType,
        InputStream stream, String filename)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(VitamContext vitamContext, String id,
        JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterDetail();
    }

    @Override
    public RequestResponse importContracts(VitamContext vitamContext, InputStream contracts,
        AdminCollections collection)
        throws InvalidParseOperationException {
        if (AdminCollections.ACCESS_CONTRACTS.equals(collection))
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
        else
            return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());

    }

    @Override
    public RequestResponse updateAccessContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
    }

    @Override
    public RequestResponse updateIngestContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());
    }

    @Override
    public RequestResponse createProfiles(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse importProfileFile(VitamContext vitamContext,
        String profileMetadataId, InputStream profile)
        throws InvalidParseOperationException, AccessExternalClientException {
        return new RequestResponseOK().setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public Response downloadProfileFile(VitamContext vitamContext, String profileMetadataId)
        throws AccessExternalClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream("Vitam Test"),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse importContexts(VitamContext vitamContext, InputStream contexts)
        throws InvalidParseOperationException {
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse updateContext(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws AccessExternalClientException, InvalidParseOperationException {
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse checkTraceabilityOperation(VitamContext vitamContext,
        JsonNode query)
        throws AccessExternalClientServerException, InvalidParseOperationException {
        return ClientMockResultHelper.checkOperationTraceability();
    }

    @Override
    public Response downloadTraceabilityOperationFile(VitamContext vitamContext,
        String operationId)
        throws AccessExternalClientServerException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public Status launchAudit(VitamContext vitamContext, JsonNode auditOption) {
        return Status.OK;
    }

    @Override
    public RequestResponse<FileFormatModel> findFormats(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRules(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getRuleList();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContexts(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public RequestResponse<FileFormatModel> findFormatById(VitamContext vitamContext,
        String formatId)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRuleById(VitamContext vitamContext,
        String ruleId)
        throws VitamClientException {
        return ClientMockResultHelper.getRule();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractById(
        VitamContext vitamContext, String contractId) throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractById(
        VitamContext vitamContext, String contractId) throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContextById(VitamContext vitamContext,
        String contextId)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfileById(VitamContext vitamContext,
        String profileId)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(
        VitamContext vitamContext, String accessionRegisterId) throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

}
