package fr.gouv.vitam.access.external.client;

import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalNotFoundException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;

/**
 * Rest client implementation for Access External
 */
public class AdminExternalClientRest extends DefaultClient implements AdminExternalClient {

    private static final String ACCESS_EXTERNAL_MODULE = "AccessExternalModule";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminExternalClientRest.class);

    private static final String URI_NOT_FOUND = "URI not found";
    private static final String UPDATE_ACCESS_CONTRACT = AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/";
    private static final String UPDATE_INGEST_CONTRACT = AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/";
    private static final String UPDATE_CONTEXT = AccessExtAPI.CONTEXTS_API_UPDATE + "/";
    private static final String UPDATE_PROFILE = AccessExtAPI.PROFILES_API_UPDATE + "/";
    private static final String UPDATE_AU_PROFILE = AccessExtAPI.ARCHIVE_UNIT_PROFILE + "/";
    private static final String UPDATE_SECURITY_PROFILE = AccessExtAPI.SECURITY_PROFILES + "/";
    private static final String UPDATE_ONTOLOGY = AccessExtAPI.ONTOLOGY + "/";

    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final String BLANK_TENANT_ID = "Tenant identifier should be filled";
    private static final String BLANK_ACTION_ID = "Action should be filled";

    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String UNAUTHORIZED = "Unauthorized";

    private static final String BLANK_OBJECT_ID = "object identifier should be filled";


    AdminExternalClientRest(AdminExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<FileFormatModel> findFormats(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.FORMATS, select, FileFormatModel.class);
    }

    @Override
    public RequestResponse<FileRulesModel> findRules(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.RULES, select, FileRulesModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.INGEST_CONTRACTS, select,
            IngestContractModel.class);
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.ACCESS_CONTRACTS, select,
            AccessContractModel.class);
    }

    @Override
    public RequestResponse<ContextModel> findContexts(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.CONTEXTS, select, ContextModel.class);
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.PROFILE, select, ProfileModel.class);
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.ACCESSION_REGISTERS, select,
            AccessionRegisterSummaryModel.class);
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.SECURITY_PROFILES, select,
            SecurityProfileModel.class);
    }


    private RequestResponse internalFindDocuments(VitamContext vitamContext, AdminCollections documentType,
        JsonNode select,
        Class clazz)
        throws VitamClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        try {
            response = performRequest(HttpMethod.GET, documentType.getName(), headers,
                select, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, clazz);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new VitamClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(VitamContext vitamContext, String id,
        JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.putAll(vitamContext.getHeaders());

        try {
            response = performRequest(HttpMethod.POST,
                AccessExtAPI.ACCESSION_REGISTERS_API + "/" + id + "/" +
                    AccessExtAPI.ACCESSION_REGISTERS_DETAIL,
                headers,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError =
                    new VitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getItem())
                        .setMessage(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage())
                        .setState(StatusCode.KO.name())
                        .setContext(ACCESS_EXTERNAL_MODULE)
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(
                            VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                                Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(
                            VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                                Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    return vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(
                            VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                                Status.PRECONDITION_FAILED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateAccessContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws AccessExternalClientException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_ACCESS_CONTRACT + id, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateIngestContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_INGEST_CONTRACT + id, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createProfiles(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter("The input profile json is mandatory", profiles, AdminCollections.PROFILE);
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, AdminCollections.PROFILE.getName(), headers,
                profiles, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createProfileFile(VitamContext vitamContext,
        String profileMetadataId, InputStream profile)
        throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter("The input profile stream is mandatory", profile, AdminCollections.PROFILE);
        ParametersChecker.checkParameter(profileMetadataId, "The profile id is mandatory");
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response =
                performRequest(HttpMethod.PUT, AdminCollections.PROFILE.getName() + "/" + profileMetadataId, headers,
                    profile, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response downloadProfileFile(VitamContext vitamContext, String profileMetadataId)
        throws AccessExternalClientException,
        AccessExternalNotFoundException {
        ParametersChecker.checkParameter("Profile is is required", profileMetadataId);


        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;

        Status status = Status.BAD_REQUEST;
        try {
            response =
                performRequest(HttpMethod.GET, AdminCollections.PROFILE.getName() + "/" + profileMetadataId, headers,
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);
            status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response;
                default: {
                    String msgErr = "Error while download profile file : " + profileMetadataId;
                    final RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
                    if (!requestResponse.isOk()) {
                        VitamError error = (VitamError) requestResponse;
                        msgErr = error.getDescription();
                    }
                    throw new AccessExternalNotFoundException(msgErr);
                }
            }

        } catch (final VitamClientInternalException e) {
            throw new AccessExternalClientException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            if (status != Status.OK) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public RequestResponse createContexts(VitamContext vitamContext, InputStream contexts)
        throws InvalidParseOperationException, AccessExternalClientServerException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, AdminCollections.CONTEXTS.getName(), headers,
                contexts, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                return new RequestResponseOK().setHttpCode(Status.OK.getStatusCode());
            } else {
                return RequestResponse.parseFromResponse(response);
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateContext(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws AccessExternalClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_CONTEXT + id, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateProfile(VitamContext vitamContext, String profileMetadataId, JsonNode queryDsl)
        throws AccessExternalClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_PROFILE + profileMetadataId, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse checkTraceabilityOperation(VitamContext vitamContext,
        JsonNode query)
        throws AccessExternalClientServerException, AccessUnauthorizedException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());

            response = performRequest(HttpMethod.POST, AdminCollections.TRACEABILITY.getCheckURI(), headers, query,
                MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError =
                    new VitamError(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getItem())
                        .setMessage(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage())
                        .setContext(ACCESS_EXTERNAL_MODULE)
                        .setDescription(
                            VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage() + " Cause : " +
                                ((VitamError) requestResponse).getDescription());

                switch (status) {
                    case OK:
                        return requestResponse;
                    case UNAUTHORIZED:
                        return vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                            .setDescription(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage() +
                                " Cause : " +
                                Status.UNAUTHORIZED.getReasonPhrase());
                    default:
                        LOGGER
                            .error(
                            "checks operation tracebility is " + status.name() + ":" + vitamError.getDescription());
                        return vitamError.setHttpCode(status.getStatusCode());
                }
            }

        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response downloadTraceabilityOperationFile(VitamContext vitamContext,
        String operationId)
        throws AccessExternalClientServerException, AccessUnauthorizedException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());

            response = performRequest(HttpMethod.GET, AccessExtAPI.TRACEABILITY_API + "/" + operationId +
                "/datafiles", headers,
                null,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response;
                case UNAUTHORIZED:
                    throw new AccessUnauthorizedException(status.getReasonPhrase());
                default:
                    LOGGER.error("checks operation tracebility is " + status.name() + ":" + status.getReasonPhrase());
                    throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public RequestResponse launchAudit(VitamContext vitamContext, JsonNode auditOption)
        throws AccessExternalClientServerException {
        Response response = null;

        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());

            response = performRequest(HttpMethod.POST, AccessExtAPI.AUDITS_API, headers, auditOption,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    private RequestResponse internalFindDocumentById(VitamContext vitamContext, AdminCollections documentType,
        String documentId,
        Class clazz)
        throws VitamClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        try {
            response = performRequest(HttpMethod.GET, documentType.getName() + "/" + documentId, headers,
                null, null, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, clazz);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new VitamClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<FileFormatModel> findFormatById(VitamContext vitamContext,
        String formatId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.FORMATS, formatId, FileFormatModel.class);
    }

    @Override
    public RequestResponse<FileRulesModel> findRuleById(VitamContext vitamContext,
        String ruleId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.RULES, ruleId, FileRulesModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractById(
        VitamContext vitamContext, String contractId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.INGEST_CONTRACTS, contractId,
            IngestContractModel.class);
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractById(
        VitamContext vitamContext, String contractId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.ACCESS_CONTRACTS, contractId,
            AccessContractModel.class);
    }

    @Override
    public RequestResponse<ContextModel> findContextById(VitamContext vitamContext,
        String contextId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.CONTEXTS, contextId, ContextModel.class);
    }

    @Override
    public RequestResponse<ProfileModel> findProfileById(VitamContext vitamContext,
        String profileId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.PROFILE, profileId, ProfileModel.class);
    }


    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(
        VitamContext vitamContext, String accessionRegisterId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.ACCESSION_REGISTERS, accessionRegisterId,
            AccessionRegisterSummaryModel.class);
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencies(VitamContext vitamContext, JsonNode query)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.AGENCIES, query,
            AgenciesModel.class);
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencyByID(
        VitamContext vitamContext, String agencyId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.AGENCIES, agencyId,
            AgenciesModel.class);
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfileById(
        VitamContext vitamContext, String identifier)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.SECURITY_PROFILES, identifier,
            SecurityProfileModel.class);
    }

    @Override
    public RequestResponse updateSecurityProfile(VitamContext vitamContext, String identifier, JsonNode queryDsl)
        throws VitamClientException {

        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_SECURITY_PROFILE + identifier, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new VitamClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(VitamContext vitamContext,
        ProcessQuery query)
        throws VitamClientException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());

            response = performRequest(HttpMethod.GET, AccessExtAPI.OPERATIONS_API, headers, query,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(
        VitamContext vitamContext, String actionId,
        String operationId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());
        ParametersChecker.checkParameter(BLANK_ACTION_ID, actionId);
        Response response = null;
        try {

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());
            headers.add(GlobalDataRest.X_ACTION, actionId);
            response =
                performRequest(HttpMethod.PUT, AccessExtAPI.OPERATIONS_API + "/" + operationId, headers,
                    MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext,
        String id)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD, AccessExtAPI.OPERATIONS_API + "/" + id, headers,
                    MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_PRECONDITION_FAILED,
                    REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, UNAUTHORIZED);
            } else if (response.getStatus() != Status.OK.getStatusCode() &&
                response.getStatus() != Status.ACCEPTED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
            }

            ItemStatus itemStatus =
                new ItemStatus()
                    .setGlobalState(
                        ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE)))
                    .setLogbookTypeProcess(response.getHeaderString(GlobalDataRest.X_CONTEXT_ID))
                    .increment(StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS)));
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(response.getStatus());


        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(
        VitamContext vitamContext, String id)
        throws VitamClientException, IllegalArgumentException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());
            response =
                performRequest(HttpMethod.DELETE, AccessExtAPI.OPERATIONS_API + "/" + id, headers,
                    MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(
        VitamContext vitamContext, String id)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());
            headers.add(GlobalDataRest.X_ACTION, id);
            response =
                performRequest(HttpMethod.GET, AccessExtAPI.OPERATIONS_API + "/" + id, headers,
                    MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions(VitamContext vitamContext) throws VitamClientException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());

            response =
                performRequest(HttpMethod.GET, AccessExtAPI.WORKFLOWS_API, headers, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    private RequestResponse internalCreateDocument(VitamContext vitamContext, AdminCollections documentType,
        InputStream stream, String filename, MediaType type)
        throws AccessExternalClientException {
        ParametersChecker.checkParameter("The input is mandatory", stream, documentType);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        headers.add(GlobalDataRest.X_FILENAME, filename);
        try {
            response = performRequest(HttpMethod.POST, documentType.getName(), headers,
                stream, type, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                return new RequestResponseOK().setHttpCode(Status.CREATED.getStatusCode());
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(URI_NOT_FOUND);
            } else if (response.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
                throw new AccessExternalClientException(JsonHandler.unprettyPrint(response.readEntity(String.class)));
            } else {
                return RequestResponse.parseFromResponse(response);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createAgencies(VitamContext vitamContext, InputStream agencies, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(vitamContext, AdminCollections.AGENCIES, agencies, filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Override
    public RequestResponse createFormats(VitamContext vitamContext, InputStream formats, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(vitamContext, AdminCollections.FORMATS, formats, filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Override
    public RequestResponse createRules(VitamContext vitamContext, InputStream rules, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(vitamContext, AdminCollections.RULES, rules, filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE);
    }

    @Override
    public RequestResponse createSecurityProfiles(VitamContext vitamContext, InputStream securityProfiles,
        String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(vitamContext, AdminCollections.SECURITY_PROFILES, securityProfiles, filename,
            MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Response checkRules(VitamContext vitamContext, InputStream rules) throws VitamClientException {
        return internalCheckDocuments(vitamContext, AdminCollections.RULES, rules);
    }

    @Override
    public Response checkFormats(VitamContext vitamContext, InputStream formats) throws VitamClientException {
        return internalCheckDocuments(vitamContext, AdminCollections.FORMATS, formats);
    }

    @Override
    public Response checkAgencies(VitamContext vitamContext, InputStream agencies) throws VitamClientException {
        return internalCheckDocuments(vitamContext, AdminCollections.AGENCIES, agencies);
    }

    private Response internalCheckDocuments(VitamContext vitamContext, AdminCollections documentType,
        InputStream stream)
        throws VitamClientException {

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            return performRequest(HttpMethod.POST, documentType.getCheckURI(), headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
    }

    private RequestResponse internalCreateContracts(VitamContext vitamContext, InputStream contracts,
        AdminCollections collection)
        throws AccessExternalClientException {
        ParametersChecker.checkParameter("The input contracts json is mandatory", contracts, collection);
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, collection.getName(), headers,
                contracts, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            // FIXME quick fix for response OK, adapt response for all response types
            if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                return new RequestResponseOK().setHttpCode(Status.OK.getStatusCode());
            } else {
                return RequestResponse.parseFromResponse(response);
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createIngestContracts(VitamContext vitamContext, InputStream ingestContracts)
        throws InvalidParseOperationException, AccessExternalClientException {
        return internalCreateContracts(vitamContext, ingestContracts, AdminCollections.INGEST_CONTRACTS);
    }

    @Override
    public RequestResponse createAccessContracts(VitamContext vitamContext, InputStream accessContracts)
        throws InvalidParseOperationException, AccessExternalClientException {
        return internalCreateContracts(vitamContext, accessContracts, AdminCollections.ACCESS_CONTRACTS);
    }

    @Override
    public Response downloadRulesReport(VitamContext vitamContext, String opId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);

        Response response;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.GET, AccessExtAPI.RULES_REPORT_API + "/" + opId,
                headers, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
        return response;
    }

    @Override
    public RequestResponse evidenceAudit(VitamContext vitamContext, JsonNode dslQuery)
        throws VitamClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        try {
            response = performRequest(HttpMethod.POST,
                AccessExtAPI.UNIT_EVIDENCE_AUDIT_API,
                headers,
                dslQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new VitamClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }



    @Override
    public RequestResponse createArchiveUnitProfile(VitamContext vitamContext, InputStream archiveUnitProfiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter("The input profile json is mandatory", archiveUnitProfiles,
            AdminCollections.ARCHIVE_UNIT_PROFILE);
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, AdminCollections.ARCHIVE_UNIT_PROFILE.getName(), headers,
                archiveUnitProfiles, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfileById(VitamContext vitamContext, String id)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.ARCHIVE_UNIT_PROFILE, id,
            ArchiveUnitProfileModel.class);
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfiles(VitamContext vitamContext, JsonNode query)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.ARCHIVE_UNIT_PROFILE, query,
            ArchiveUnitProfileModel.class);
    }

    @Override
    public RequestResponse updateArchiveUnitProfile(VitamContext vitamContext, String archiveUnitprofileId,
        JsonNode queryDSL) throws InvalidParseOperationException, AccessExternalClientException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_AU_PROFILE + archiveUnitprofileId, headers,
                queryDSL, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override public RequestResponse createOntologies(VitamContext vitamContext, InputStream ontologies) throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter("The input ontologies json is mandatory", ontologies,
            AdminCollections.ONTOLOGY);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.POST, AdminCollections.ONTOLOGY.getName(), headers,
                ontologies, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.OK.getStatusCode() ||
                response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                return new RequestResponseOK().setHttpCode(Status.OK.getStatusCode());
            } else {
                return RequestResponse.parseFromResponse(response);
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override public RequestResponse updateOntology(VitamContext vitamContext, String ontologyId, JsonNode queryDSL)
        throws InvalidParseOperationException, AccessExternalClientException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_ONTOLOGY + ontologyId, headers,
                queryDSL, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override public RequestResponse<OntologyModel> findOntologyById(VitamContext vitamContext, String id) throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.ONTOLOGY, id,
            OntologyModel.class);
    }


    @Override public RequestResponse<OntologyModel> findOntologies(VitamContext vitamContext, JsonNode query) throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.ONTOLOGY, query,
            OntologyModel.class);
    }
}


