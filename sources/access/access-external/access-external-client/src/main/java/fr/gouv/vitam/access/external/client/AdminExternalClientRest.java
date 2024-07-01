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
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.exception.AdminExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalNotFoundException;
import fr.gouv.vitam.access.external.common.exception.LogbookExternalClientException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSymbolicModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.access.external.api.AccessExtAPI.ACCESSION_REGISTERS_API;
import static fr.gouv.vitam.access.external.api.AccessExtAPI.ACCESSION_REGISTERS_DETAIL;
import static fr.gouv.vitam.access.external.api.AccessExtAPI.OPERATIONS_API;
import static fr.gouv.vitam.access.external.api.AccessExtAPI.TRACEABILITY_API;
import static fr.gouv.vitam.access.external.api.AdminCollections.ACCESSION_REGISTERS_SYMBOLIC;
import static fr.gouv.vitam.access.external.api.AdminCollections.TRACEABILITY;
import static fr.gouv.vitam.common.GlobalDataRest.X_ACTION;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTEXT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_FILENAME;
import static fr.gouv.vitam.common.GlobalDataRest.X_GLOBAL_EXECUTION_STATE;
import static fr.gouv.vitam.common.GlobalDataRest.X_GLOBAL_EXECUTION_STATUS;
import static fr.gouv.vitam.common.GlobalDataRest.X_HTTP_METHOD_OVERRIDE;
import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.common.error.VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR;
import static fr.gouv.vitam.common.error.VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

public class AdminExternalClientRest extends DefaultClient implements AdminExternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminExternalClientRest.class);

    private static final String ACCESS_EXTERNAL_MODULE = "AccessExternalModule";
    private static final String UPDATE_ACCESS_CONTRACT = AccessExtAPI.ACCESS_CONTRACT_API_UPDATE + "/";
    private static final String UPDATE_INGEST_CONTRACT = AccessExtAPI.INGEST_CONTRACT_API_UPDATE + "/";
    private static final String UPDATE_MANAGEMENT_CONTRACT = AccessExtAPI.MANAGEMENT_CONTRACT_API_UPDATE + "/";
    private static final String UPDATE_CONTEXT = AccessExtAPI.CONTEXTS_API_UPDATE + "/";
    private static final String UPDATE_PROFILE = AccessExtAPI.PROFILES_API_UPDATE + "/";
    private static final String UPDATE_AU_PROFILE = AccessExtAPI.ARCHIVE_UNIT_PROFILE + "/";
    private static final String UPDATE_SECURITY_PROFILE = AccessExtAPI.SECURITY_PROFILES + "/";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final String BLANK_TENANT_ID = "Tenant identifier should be filled";
    private static final String BLANK_ACTION_ID = "Action should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";

    AdminExternalClientRest(AdminExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<FileFormatModel> findFormats(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.FORMATS, select, FileFormatModel.class);
    }

    @Override
    public RequestResponse<FileRulesModel> findRules(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.RULES, select, FileRulesModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.INGEST_CONTRACTS,
            select,
            IngestContractModel.class
        );
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.ACCESS_CONTRACTS,
            select,
            AccessContractModel.class
        );
    }

    @Override
    public RequestResponse<ManagementContractModel> findManagementContracts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.MANAGEMENT_CONTRACTS,
            select,
            ManagementContractModel.class
        );
    }

    @Override
    public RequestResponse<ContextModel> findContexts(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.CONTEXTS, select, ContextModel.class);
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.PROFILE, select, ProfileModel.class);
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.ACCESSION_REGISTERS,
            select,
            AccessionRegisterSummaryModel.class
        );
    }

    @Override
    public RequestResponse<AccessionRegisterDetailModel> findAccessionRegisterDetails(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.ACCESSION_REGISTER_DETAILS,
            select,
            AccessionRegisterDetailModel.class
        );
    }

    @Override
    public RequestResponse<AccessionRegisterSymbolicModel> findAccessionRegisterSymbolic(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            ACCESSION_REGISTERS_SYMBOLIC,
            select,
            AccessionRegisterSymbolicModel.class
        );
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.SECURITY_PROFILES,
            select,
            SecurityProfileModel.class
        );
    }

    private <T> RequestResponse<T> internalFindDocuments(
        VitamContext vitamContext,
        AdminCollections documentType,
        JsonNode select,
        Class<T> clazz
    ) throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(documentType.getName())
            .withHeaders(vitamContext.getHeaders())
            .withBody(select)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, clazz);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(VitamContext vitamContext, String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        VitamRequestBuilder request = post()
            .withPath(ACCESSION_REGISTERS_API + "/" + id + "/" + ACCESSION_REGISTERS_DETAIL)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(X_HTTP_METHOD_OVERRIDE, HttpMethod.GET)
            .withBody(query)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e
                .getVitamError()
                .setMessage(ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage())
                .setDescription(ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage());
        }
    }

    @Override
    public RequestResponse updateAccessContract(VitamContext vitamContext, String id, JsonNode queryDsl)
        throws AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_ACCESS_CONTRACT + id)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse updateIngestContract(VitamContext vitamContext, String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_INGEST_CONTRACT + id)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse updateManagementContract(VitamContext vitamContext, String id, JsonNode queryDsl)
        throws AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_MANAGEMENT_CONTRACT + id)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse createProfiles(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter("The input profile json is mandatory", profiles, AdminCollections.PROFILE);
        VitamRequestBuilder request = post()
            .withPath(AdminCollections.PROFILE.getName())
            .withHeaders(vitamContext.getHeaders())
            .withBody(profiles)
            .withOctetContentType()
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse createProfileFile(VitamContext vitamContext, String profileMetadataId, InputStream profile)
        throws InvalidParseOperationException, AccessExternalClientException {
        ParametersChecker.checkParameter(profileMetadataId, "The profile id is mandatory");
        VitamRequestBuilder request = put()
            .withPath(AdminCollections.PROFILE.getName() + "/" + profileMetadataId)
            .withHeaders(vitamContext.getHeaders())
            .withBody(profile, "The input profile stream is mandatory")
            .withOctetContentType()
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public Response downloadProfileFile(VitamContext vitamContext, String profileMetadataId)
        throws AccessExternalClientException, AccessExternalNotFoundException {
        ParametersChecker.checkParameter("Profile is is required", profileMetadataId);
        VitamRequestBuilder request = get()
            .withPath(AdminCollections.PROFILE.getName() + "/" + profileMetadataId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            throw new AccessExternalNotFoundException("Error while download profile file : " + profileMetadataId, e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse createContexts(VitamContext vitamContext, InputStream contexts)
        throws InvalidParseOperationException, AccessExternalClientServerException {
        VitamRequestBuilder request = post()
            .withPath(AdminCollections.CONTEXTS.getName())
            .withHeaders(vitamContext.getHeaders())
            .withBody(contexts)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK()
                .setHttpCode(OK.getStatusCode())
                .addHeader(X_REQUEST_ID, response.getHeaderString(X_REQUEST_ID));
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse updateContext(VitamContext vitamContext, String id, JsonNode queryDsl)
        throws AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_CONTEXT + id)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse updateProfile(VitamContext vitamContext, String profileMetadataId, JsonNode queryDsl)
        throws AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_PROFILE + profileMetadataId)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    @Deprecated
    public RequestResponse checkTraceabilityOperation(VitamContext vitamContext, JsonNode query)
        throws AccessExternalClientServerException {
        VitamRequestBuilder request = post()
            .withPath(TRACEABILITY.getCheckURI())
            .withHeaders(vitamContext.getHeaders())
            .withBody(query)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError().setMessage(ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage());
        }
    }

    @Override
    public RequestResponse<JsonNode> checkTraceabilityOperations(VitamContext vitamContext, JsonNode query)
        throws AccessExternalClientServerException {
        VitamRequestBuilder request = post()
            .withPath(TRACEABILITY_API + "/linkedchecks")
            .withHeaders(vitamContext.getHeaders())
            .withBody(query)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e
                .<JsonNode>getVitamError()
                .setMessage(ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage());
        }
    }

    @Override
    public Response downloadTraceabilityOperationFile(VitamContext vitamContext, String operationId)
        throws AccessExternalClientServerException, AccessUnauthorizedException {
        Response response = null;
        try {
            VitamRequestBuilder request = get()
                .withPath(AccessExtAPI.TRACEABILITY_API + "/" + operationId + "/datafiles")
                .withHeaders(vitamContext.getHeaders())
                .withOctetAccept();
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        } catch (AdminExternalClientException e) {
            if (e.getStatus().equals(UNAUTHORIZED)) {
                throw new AccessUnauthorizedException(e);
            }
            throw new AccessExternalClientServerException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse launchAudit(VitamContext vitamContext, JsonNode auditOption)
        throws AccessExternalClientServerException {
        VitamRequestBuilder post = post()
            .withPath(AccessExtAPI.AUDITS_API)
            .withHeaders(vitamContext.getHeaders())
            .withBody(auditOption)
            .withJson();
        try (Response response = make(post)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientServerException(e);
        }
    }

    private <T> RequestResponse<T> internalFindDocumentById(
        VitamContext vitamContext,
        AdminCollections documentType,
        String documentId,
        Class<T> clazz
    ) throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(documentType.getName() + "/" + documentId)
            .withHeaders(vitamContext.getHeaders())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, clazz);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<FileFormatModel> findFormatById(VitamContext vitamContext, String formatId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.FORMATS, formatId, FileFormatModel.class);
    }

    @Override
    public RequestResponse<FileRulesModel> findRuleById(VitamContext vitamContext, String ruleId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.RULES, ruleId, FileRulesModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractById(VitamContext vitamContext, String contractId)
        throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.INGEST_CONTRACTS,
            contractId,
            IngestContractModel.class
        );
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractById(VitamContext vitamContext, String contractId)
        throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.ACCESS_CONTRACTS,
            contractId,
            AccessContractModel.class
        );
    }

    @Override
    public RequestResponse<ManagementContractModel> findManagementContractById(
        VitamContext vitamContext,
        String contractId
    ) throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.MANAGEMENT_CONTRACTS,
            contractId,
            ManagementContractModel.class
        );
    }

    @Override
    public RequestResponse<ContextModel> findContextById(VitamContext vitamContext, String contextId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.CONTEXTS, contextId, ContextModel.class);
    }

    @Override
    public RequestResponse<ProfileModel> findProfileById(VitamContext vitamContext, String profileId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.PROFILE, profileId, ProfileModel.class);
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(
        VitamContext vitamContext,
        String accessionRegisterId
    ) throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.ACCESSION_REGISTERS,
            accessionRegisterId,
            AccessionRegisterSummaryModel.class
        );
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencies(VitamContext vitamContext, JsonNode query)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.AGENCIES, query, AgenciesModel.class);
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencyByID(VitamContext vitamContext, String agencyId)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.AGENCIES, agencyId, AgenciesModel.class);
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfileById(VitamContext vitamContext, String identifier)
        throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.SECURITY_PROFILES,
            identifier,
            SecurityProfileModel.class
        );
    }

    @Override
    public RequestResponse updateSecurityProfile(VitamContext vitamContext, String identifier, JsonNode queryDsl)
        throws VitamClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_SECURITY_PROFILE + identifier)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDsl)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(VitamContext vitamContext, ProcessQuery query)
        throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(OPERATIONS_API)
            .withHeaders(vitamContext.getHeaders())
            .withBody(query)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(
        VitamContext vitamContext,
        String actionId,
        String operationId
    ) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());
        ParametersChecker.checkParameter(BLANK_ACTION_ID, actionId);
        VitamRequestBuilder request = put()
            .withPath(OPERATIONS_API + "/" + operationId)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(X_ACTION, actionId)
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext, String id)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        VitamRequestBuilder request = VitamRequestBuilder.head()
            .withPath(OPERATIONS_API + "/" + id)
            .withHeaders(vitamContext.getHeaders())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            ItemStatus itemStatus = new ItemStatus()
                .setGlobalState(ProcessState.valueOf(response.getHeaderString(X_GLOBAL_EXECUTION_STATE)))
                .setLogbookTypeProcess(response.getHeaderString(X_CONTEXT_ID))
                .increment(StatusCode.valueOf(response.getHeaderString(X_GLOBAL_EXECUTION_STATUS)));
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(response.getStatus());
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(VitamContext vitamContext, String id)
        throws VitamClientException, IllegalArgumentException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());
        VitamRequestBuilder request = delete()
            .withPath(OPERATIONS_API + "/" + id)
            .withHeaders(vitamContext.getHeaders())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(VitamContext vitamContext, String id)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, vitamContext.getTenantId());
        VitamRequestBuilder request = get()
            .withPath(OPERATIONS_API + "/" + id)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(X_ACTION, id)
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions(VitamContext vitamContext) throws VitamClientException {
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.WORKFLOWS_API)
            .withHeaders(vitamContext.getHeaders())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    private RequestResponse internalCreateDocument(
        VitamContext vitamContext,
        AdminCollections documentType,
        InputStream stream,
        String filename,
        MediaType type
    ) throws AccessExternalClientException {
        ParametersChecker.checkParameter("The document type is mandatory", documentType);
        VitamRequestBuilder request = post()
            .withPath(documentType.getName())
            .withHeaders(vitamContext.getHeaders())
            .withHeader(X_FILENAME, filename)
            .withBody(stream)
            .withContentType(type)
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK()
                .setHttpCode(CREATED.getStatusCode())
                .addHeader(X_REQUEST_ID, response.getHeaderString(X_REQUEST_ID));
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        }
    }

    @Override
    public RequestResponse createAgencies(VitamContext vitamContext, InputStream agencies, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.AGENCIES,
            agencies,
            filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE
        );
    }

    @Override
    public RequestResponse createFormats(VitamContext vitamContext, InputStream formats, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.FORMATS,
            formats,
            filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE
        );
    }

    @Override
    public RequestResponse createRules(VitamContext vitamContext, InputStream rules, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.RULES,
            rules,
            filename,
            MediaType.APPLICATION_OCTET_STREAM_TYPE
        );
    }

    @Override
    public RequestResponse createSecurityProfiles(
        VitamContext vitamContext,
        InputStream securityProfiles,
        String filename
    ) throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.SECURITY_PROFILES,
            securityProfiles,
            filename,
            MediaType.APPLICATION_JSON_TYPE
        );
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

    private Response internalCheckDocuments(
        VitamContext vitamContext,
        AdminCollections documentType,
        InputStream stream
    ) throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(documentType.getCheckURI())
            .withHeaders(vitamContext.getHeaders())
            .withBody(stream)
            .withOctet();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    private RequestResponse internalCreateContracts(
        VitamContext vitamContext,
        InputStream contracts,
        AdminCollections collection
    ) throws AccessExternalClientException {
        ParametersChecker.checkParameter("The collection parameter is mandatory", collection);
        VitamRequestBuilder request = post()
            .withPath(collection.getName())
            .withHeaders(vitamContext.getHeaders())
            .withBody(contracts, "The input contracts json is mandatory")
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK()
                .setHttpCode(OK.getStatusCode())
                .addHeader(X_REQUEST_ID, response.getHeaderString(X_REQUEST_ID));
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
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
    public RequestResponse createManagementContracts(VitamContext vitamContext, InputStream managementContracts)
        throws InvalidParseOperationException, AccessExternalClientException {
        return internalCreateContracts(vitamContext, managementContracts, AdminCollections.MANAGEMENT_CONTRACTS);
    }

    @Override
    public Response downloadDistributionReport(VitamContext vitamContext, String opId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.DISTRIBUTION_REPORT_API + "/" + opId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public Response downloadBatchReport(VitamContext vitamContext, String opId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.BATCH_REPORT_API + opId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public Response downloadRulesReport(VitamContext vitamContext, String opId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.RULES_REPORT_API + "/" + opId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
        // DO NOT CLOSE response in finally block as it would break external API
    }

    @Override
    public Response downloadAgenciesCsvAsStream(VitamContext vitamContext, String opId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.AGENCIES_REFERENTIAL_CSV_DOWNLOAD + "/" + opId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public Response downloadRulesCsvAsStream(VitamContext vitamContext, String opId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, opId);
        VitamRequestBuilder request = get()
            .withPath(AccessExtAPI.RULES_REFERENTIAL_CSV_DOWNLOAD + "/" + opId)
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        Response response = null;
        try {
            response = make(request);
            check(response);
            return response;
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse evidenceAudit(VitamContext vitamContext, JsonNode dslQuery) throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(AccessExtAPI.UNIT_EVIDENCE_AUDIT_API)
            .withHeaders(vitamContext.getHeaders())
            .withBody(dslQuery)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse rectificationAudit(VitamContext vitamContext, String operationId)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(AccessExtAPI.RECTIFICATION_AUDIT)
            .withHeaders(vitamContext.getHeaders())
            .withBody(operationId)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse exportProbativeValue(VitamContext vitamContext, ProbativeValueRequest probativeValueRequest)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(AccessExtAPI.EXPORT_PROBATIVE_VALUE)
            .withHeaders(vitamContext.getHeaders())
            .withBody(probativeValueRequest)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse createArchiveUnitProfile(VitamContext vitamContext, InputStream archiveUnitProfiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamRequestBuilder request = post()
            .withPath(AdminCollections.ARCHIVE_UNIT_PROFILE.getName())
            .withHeaders(vitamContext.getHeaders())
            .withBody(archiveUnitProfiles, "The input profile json is mandatory")
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfileById(VitamContext vitamContext, String id)
        throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.ARCHIVE_UNIT_PROFILE,
            id,
            ArchiveUnitProfileModel.class
        );
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfiles(VitamContext vitamContext, JsonNode query)
        throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.ARCHIVE_UNIT_PROFILE,
            query,
            ArchiveUnitProfileModel.class
        );
    }

    @Override
    public RequestResponse updateArchiveUnitProfile(
        VitamContext vitamContext,
        String archiveUnitprofileId,
        JsonNode queryDSL
    ) throws InvalidParseOperationException, AccessExternalClientException {
        VitamRequestBuilder request = put()
            .withPath(UPDATE_AU_PROFILE + archiveUnitprofileId)
            .withHeaders(vitamContext.getHeaders())
            .withBody(queryDSL)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    @Override
    public RequestResponse importOntologies(boolean forceUpdate, VitamContext vitamContext, InputStream ontologies)
        throws InvalidParseOperationException, AccessExternalClientException {
        VitamRequestBuilder request = post()
            .withPath(AdminCollections.ONTOLOGY.getName())
            .withHeaders(vitamContext.getHeaders())
            .withHeader(GlobalDataRest.FORCE_UPDATE, forceUpdate)
            .withBody(ontologies, "The input ontologies json is mandatory")
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK()
                .setHttpCode(OK.getStatusCode())
                .addHeader(X_REQUEST_ID, response.getHeaderString(X_REQUEST_ID));
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        } catch (VitamClientInternalException e) {
            throw new AccessExternalClientException(e);
        }
    }

    @Override
    public RequestResponse<OntologyModel> findOntologyById(VitamContext vitamContext, String id)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.ONTOLOGY, id, OntologyModel.class);
    }

    @Override
    public RequestResponse<OntologyModel> findOntologies(VitamContext vitamContext, JsonNode query)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.ONTOLOGY, query, OntologyModel.class);
    }

    @Override
    public RequestResponse importGriffin(VitamContext vitamContext, InputStream griffinStream, String filename)
        throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.GRIFFIN,
            griffinStream,
            filename,
            MediaType.APPLICATION_JSON_TYPE
        );
    }

    @Override
    public RequestResponse importPreservationScenario(
        VitamContext vitamContext,
        InputStream scenarios,
        String fileName
    ) throws AccessExternalClientException {
        return internalCreateDocument(
            vitamContext,
            AdminCollections.PRESERVATION_SCENARIO,
            scenarios,
            fileName,
            MediaType.APPLICATION_JSON_TYPE
        );
    }

    @Override
    public RequestResponse<GriffinModel> findGriffinById(VitamContext vitamContext, String id)
        throws VitamClientException {
        return internalFindDocumentById(vitamContext, AdminCollections.GRIFFIN, id, GriffinModel.class);
    }

    @Override
    public RequestResponse<PreservationScenarioModel> findPreservationScenarioById(
        VitamContext vitamContext,
        String id
    ) throws VitamClientException {
        return internalFindDocumentById(
            vitamContext,
            AdminCollections.PRESERVATION_SCENARIO,
            id,
            PreservationScenarioModel.class
        );
    }

    @Override
    public RequestResponse<PreservationScenarioModel> findPreservationScenario(
        VitamContext vitamContext,
        JsonNode select
    ) throws VitamClientException {
        return internalFindDocuments(
            vitamContext,
            AdminCollections.PRESERVATION_SCENARIO,
            select,
            PreservationScenarioModel.class
        );
    }

    @Override
    public RequestResponse<GriffinModel> findGriffin(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return internalFindDocuments(vitamContext, AdminCollections.GRIFFIN, select, GriffinModel.class);
    }

    @Override
    public RequestResponse createExternalOperation(
        VitamContext vitamContext,
        LogbookOperationParameters logbookOperationparams
    ) throws LogbookExternalClientException {
        VitamRequestBuilder request = post()
            .withPath(AccessExtAPI.LOGBOOK_OPERATIONS)
            .withHeaders(vitamContext.getHeaders())
            .withBody(logbookOperationparams)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new LogbookExternalClientException(e);
        } catch (AdminExternalClientException e) {
            LOGGER.error(e);
            return e.getVitamError();
        }
    }

    private void check(Response response) throws AdminExternalClientException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        String message = String.format(
            "Error with the response, get status: '%d' and reason '%s'.",
            response.getStatus(),
            fromStatusCode(response.getStatus()).getReasonPhrase()
        );
        VitamError<JsonNode> vitamError = new VitamError<JsonNode>(message)
            .setDescription(message)
            .setHttpCode(status.getStatusCode())
            .setMessage(message)
            .setState(KO.name())
            .setContext(ACCESS_EXTERNAL_MODULE);

        vitamError.parseHeadersFromResponse(response);

        throw new AdminExternalClientException(message, status, vitamError);
    }
}
