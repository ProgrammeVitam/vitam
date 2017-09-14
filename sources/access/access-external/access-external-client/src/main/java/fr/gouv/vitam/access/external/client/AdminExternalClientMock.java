package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
    public Response checkDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws VitamClientException {
        StreamUtils.closeSilently(stream);

        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType)) {
            return new AbstractMockClient.FakeInboundResponse(Status.OK, IOUtils.toInputStream("Vitam Test"),
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
    public Status createDocuments(AdminCollections documentType, InputStream stream, String filename, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException {
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
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
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse importProfileFile(String profileMetadataId, InputStream profile, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException {
        return new RequestResponseOK().setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public Response downloadProfileFile(String profileMetadataId, Integer tenantId)
        throws AccessExternalClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, IOUtils.toInputStream("Vitam Test"),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse importContexts(InputStream contexts, Integer tenantId)
        throws InvalidParseOperationException {
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse updateContext(String id, JsonNode queryDsl, Integer tenantId)
        throws AccessExternalClientException, InvalidParseOperationException {
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
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

    @Override
    public Status launchAudit(JsonNode auditOption, Integer tenantId, String contractName) {
        return Status.OK;
    }

    @Override
    public RequestResponse<FileFormatModel> findFormats(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRules(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getRuleList();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(JsonNode select, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(JsonNode select, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContexts(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(JsonNode select, Integer tenantId,
        String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public RequestResponse<FileFormatModel> findFormatById(String formatId, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRuleById(String ruleId, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getRule();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractById(String contractId, Integer tenantId,
        String contractName) throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractById(String contractId, Integer tenantId,
        String contractName) throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContextById(String contextId, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfileById(String profileId, Integer tenantId, String contractName)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(String accessionRegisterId,
        Integer tenantId, String contractName) throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

}
