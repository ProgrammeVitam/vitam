/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.ForbiddenClientException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AbstractContractModel;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSymbolicModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientBadRequestException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.List;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

/**
 * AdminManagement client
 */
class AdminManagementClientRest extends DefaultClient implements AdminManagementClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientRest.class);
    private static final String FORMAT_CHECK_URL = "/format/check";
    private static final String FORMAT_IMPORT_URL = "/format/import";
    private static final String FORMAT_GET_DOCUMENT_URL = "/format/document";
    private static final String FORMAT_URL = "/format";

    private static final String RULESMANAGER_CHECK_URL = "/rules/check";
    private static final String AGENCIESMANAGER_CHECK_URL = "/agencies/check";
    private static final String RULESMANAGER_IMPORT_URL = "/rules/import";
    private static final String AGENCIESMANAGER_IMPORT_URL = "/agencies/import";
    private static final String AGENCIES_URL = "/agencies";

    private static final String RULESMANAGER_GET_DOCUMENT_URL = "/rules/document";

    private static final String RULESMANAGER_URL = "/rules";

    private static final String ACCESSION_REGISTER_CREATE_URI = "/accession-register";
    private static final String ACCESSION_REGISTER_GET_DOCUMENT_URL = "/accession-register/document";
    private static final String ACCESSION_REGISTER_GET_DETAIL_URL = "/accession-register/detail";
    private static final String INGEST_CONTRACTS_URI = "/ingestcontracts";
    private static final String ACCESS_CONTRACTS_URI = "/accesscontracts";
    private static final String MANAGEMENT_CONTRACTS_URI = "/managementcontracts";
    private static final String UPDATE_ACCESS_CONTRACT_URI = "/accesscontracts/";
    private static final String UPDATE_INGEST_CONTRACT_URI = "/ingestcontracts/";
    private static final String UPDATE_MANAGEMENT_CONTRACT_URI = "/managementcontracts/";
    private static final String PROFILE_URI = "/profiles";
    private static final String CONTEXT_URI = "/contexts";
    private static final String AUDIT_URI = "/audit";
    private static final String AUDIT_RULE_URI = "/auditRule";
    private static final String UPDATE_CONTEXT_URI = "/context/";
    private static final String UPDATE_PROFIL_URI = "/profiles/";
    private static final String SECURITY_PROFILES_URI = "/securityprofiles";
    private static final String EVIDENCE_AUDIT_URI = "/evidenceaudit";
    private static final String ARCHIVE_UNIT_PROFILE_URI = "/archiveunitprofiles";
    private static final String UPDATE_ARCHIVE_UNIT_PROFILE_URI = "/archiveunitprofiles/";
    private static final String ONTOLOGY_URI = "/ontologies";
    private static final String PROBATIVE_VALUE_URI = "/probativevalueexport";
    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";
    private static final String RECTIFICATION_AUDIT = "/rectificationaudit";
    private static final String CREATE_EXTERNAL_OPERATION_URI = "/logbookoperations";

    private static final String FORCE_PAUSE_URI = "/forcepause";
    private static final String REMOVE_FORCE_PAUSE_URI = "/removeforcepause";
    private static final String INTERNAL_SERVER_ERROR_MSG = "Internal Server Error";

    AdminManagementClientRest(AdminManagementClientFactory factory) {
        super(factory);
    }

    @Override
    public Response checkFormat(InputStream stream) throws ReferentialException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);

        VitamRequestBuilder request = post()
            .withPath(FORMAT_CHECK_URL)
            .withBody(stream)
            .withOctetContentType()
            .withJsonAccept();

        Response response;
        try {
            response = make(request);
            checkWithSpecificException(response);
            return response;
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException | ForbiddenClientException | DatabaseConflictException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public Status importFormat(InputStream stream, String filename)
        throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);

        VitamRequestBuilder request = post()
            .withPath(FORMAT_IMPORT_URL)
            .withHeader(GlobalDataRest.X_FILENAME, filename)
            .withBody(stream)
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return fromStatusCode(response.getStatus());
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException | ForbiddenClientException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public JsonNode getFormatByID(String id) throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);

        VitamRequestBuilder request = get()
            .withPath(FORMAT_URL + "/" + id)
            .withJsonAccept();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }

    }

    @Override
    public RequestResponse<FileFormatModel> getFormats(JsonNode query)
        throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);

        VitamRequestBuilder request = post()
            .withPath(FORMAT_GET_DOCUMENT_URL)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class, FileFormatModel.class);
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }


    @Override
    public Response checkRulesFile(InputStream stream) throws FileRulesException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        VitamRequestBuilder request = post()
            .withPath(RULESMANAGER_CHECK_URL)
            .withBody(stream)
            .withOctetContentType()
            .withOctetAccept();

        Response response = null;
        try {
            response = make(request);
            checkWithSpecificException(response);
            return response;
        } catch (DatabaseConflictException | ReferentialNotFoundException | AccessUnauthorizedException | ForbiddenClientException | VitamClientInternalException e) {
            String reason = (response != null) ? response.readEntity(String.class) : INTERNAL_SERVER_ERROR_MSG;
            throw new AdminManagementClientServerException(reason, e);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    @Override
    public Response checkAgenciesFile(InputStream stream)
        throws FileRulesException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        VitamRequestBuilder request = post()
            .withPath(AGENCIESMANAGER_CHECK_URL)
            .withBody(stream)
            .withOctetContentType()
            .withOctetAccept();
        Response response;
        try {
            response = make(request);
            checkWithSpecificException(response);
            return response;
        } catch (ReferentialNotFoundException | DatabaseConflictException | AccessUnauthorizedException |
            ForbiddenClientException | VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        }
    }

    @Override
    public Status importRulesFile(InputStream stream, String filename)
        throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);

        VitamRequestBuilder request = post()
            .withPath(RULESMANAGER_IMPORT_URL)
            .withHeader(GlobalDataRest.X_FILENAME, filename)
            .withBody(stream)
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return fromStatusCode(response.getStatus());
        } catch (final VitamClientInternalException | AccessUnauthorizedException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (BadRequestException e) {
            throw new FileRulesException(e);
        } catch (ForbiddenClientException e) {
            throw new FileRulesImportInProgressException(e);
        } catch (DatabaseConflictException e) {
            throw new DatabaseConflictException("Collection input conflic");
        }
    }

    @Override
    public Status importAgenciesFile(InputStream stream, String filename)
        throws ReferentialException {
        ParametersChecker.checkParameter("filename is a mandatory parameter", filename);
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);

        VitamRequestBuilder request = post()
            .withPath(AGENCIESMANAGER_IMPORT_URL)
            .withHeader(GlobalDataRest.X_FILENAME, filename)
            .withBody(stream)
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return fromStatusCode(response.getStatus());
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException | ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public JsonNode getAgencies(JsonNode query)
        throws ReferentialException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);

        VitamRequestBuilder request = get()
            .withPath(AGENCIES_URL)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException | AccessUnauthorizedException | ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (BadRequestException e) {
            throw new FileRulesNotFoundException("Agency Not found ");
        }
    }

    @Override
    public RequestResponse<AgenciesModel> getAgencyById(String id)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {

        ParametersChecker.checkParameter("The input documentId json is mandatory", id);
        JsonNode queryDsl;
        try {
            queryDsl = getIdentifierQuery(AgenciesModel.TAG_IDENTIFIER, id);
        } catch (InvalidCreateOperationException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }

        VitamRequestBuilder request = get()
            .withPath(AGENCIES_URL)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            RequestResponseOK<AgenciesModel> resp =
                getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    AgenciesModel.class);

            if (resp.getResults() == null || resp.getResults().isEmpty())
                throw new ReferentialNotFoundException("Agency not found with id: " + id);

            return resp;
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public JsonNode getRuleByID(String id)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);

        VitamRequestBuilder request = get()
            .withPath(RULESMANAGER_URL + "/" + id)
            .withJsonAccept();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (ReferentialNotFoundException e) {
            throw new FileRulesNotFoundException("Rule Not found ");
        }
    }

    @Override
    public JsonNode getRules(JsonNode query)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);

        VitamRequestBuilder request = post()
            .withPath(RULESMANAGER_GET_DOCUMENT_URL)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (ReferentialNotFoundException e) {
            throw new FileRulesNotFoundException("Rule Not found ");
        }
    }

    @Override
    public RequestResponse createOrUpdateAccessionRegister(AccessionRegisterDetailModel register)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("Accession register is a mandatory parameter", register);

        VitamRequestBuilder request = post()
            .withPath(ACCESSION_REGISTER_CREATE_URI)
            .withBody(register)
            .withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response);
        } catch (Exception e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> getAccessionRegister(JsonNode query)
        throws InvalidParseOperationException, ReferentialException, AccessUnauthorizedException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);

        VitamRequestBuilder request = post()
            .withPath(ACCESSION_REGISTER_GET_DOCUMENT_URL)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            String value = response.readEntity(String.class);
            return getFromString(value, RequestResponseOK.class, AccessionRegisterSummaryModel.class);
        } catch (final VitamClientInternalException | BadRequestException | ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterDetailModel> getAccessionRegisterDetail(String originatingAgency,
        JsonNode query)
        throws InvalidParseOperationException, ReferentialException {

        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        ParametersChecker.checkParameter("documentId is a mandatory parameter", originatingAgency);

        VitamRequestBuilder request = post()
            .withPath(ACCESSION_REGISTER_GET_DETAIL_URL + "/" + originatingAgency)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                AccessionRegisterDetailModel.class);
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterDetailModel> getAccessionRegisterDetail(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {

        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        VitamRequestBuilder request = post()
            .withPath(ACCESSION_REGISTER_GET_DETAIL_URL)
            .withBody(query)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                AccessionRegisterDetailModel.class);
        } catch (final VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public Status importIngestContracts(List<IngestContractModel> ingestContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ingest contracts json is mandatory", ingestContractModelList);
        return importContracts(ingestContractModelList, INGEST_CONTRACTS_URI);
    }

    @Override
    public Status importAccessContracts(List<AccessContractModel> accessContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input access contracts json is mandatory", accessContractModelList);
        return importContracts(accessContractModelList, ACCESS_CONTRACTS_URI);
    }

    @Override
    public Status importManagementContracts(List<ManagementContractModel> managementContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker
            .checkParameter("The input management contracts json is mandatory", managementContractModelList);
        return importContracts(managementContractModelList, MANAGEMENT_CONTRACTS_URI);
    }

    private <T extends AbstractContractModel> Status importContracts(List<T> contractModelList, String uri)
        throws AdminManagementClientServerException {

        VitamRequestBuilder request = post()
            .withPath(uri)
            .withBody(contractModelList)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return fromStatusCode(response.getStatus());
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return findContracts(queryDsl, ACCESS_CONTRACTS_URI, AccessContractModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return findContracts(queryDsl, INGEST_CONTRACTS_URI, IngestContractModel.class);
    }

    @Override
    public RequestResponse<ManagementContractModel> findManagementContracts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return findContracts(queryDsl, MANAGEMENT_CONTRACTS_URI, ManagementContractModel.class);
    }

    private <T extends AbstractContractModel> RequestResponse<T> findContracts(JsonNode queryDsl, String uri,
        Class<T> clasz)
        throws InvalidParseOperationException, AdminManagementClientServerException {

        VitamRequestBuilder request = get()
            .withPath(uri)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class, clasz);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractsByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        return findContractByID(documentId, ACCESS_CONTRACTS_URI, AccessContractModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractsByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        return findContractByID(documentId, INGEST_CONTRACTS_URI, IngestContractModel.class);
    }


    @Override
    public RequestResponse<ManagementContractModel> findManagementContractsByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        return findContractByID(documentId, MANAGEMENT_CONTRACTS_URI, ManagementContractModel.class);
    }

    private <T extends AbstractContractModel> RequestResponse<T> findContractByID(String documentId, String uri,
        Class<T> clasz)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {

        JsonNode queryDsl;
        try {
            queryDsl = getIdentifierQuery(AbstractContractModel.TAG_IDENTIFIER, documentId);
        } catch (InvalidCreateOperationException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
        VitamRequestBuilder request = get()
            .withPath(uri)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            RequestResponseOK<T> resp = getFromString(response.readEntity(String.class),
                RequestResponseOK.class, clasz);
            if (resp.getResults() == null || resp.getResults().isEmpty())
                throw new ReferentialNotFoundException("Contract not found with id: " + documentId);
            return resp;
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }


    @Override
    public RequestResponse<AccessContractModel> updateAccessContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return updateContract(id, queryDsl, UPDATE_ACCESS_CONTRACT_URI, AccessContractModel.class);
    }

    @Override
    public RequestResponse<IngestContractModel> updateIngestContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return updateContract(id, queryDsl, UPDATE_INGEST_CONTRACT_URI, IngestContractModel.class);
    }

    @Override
    public RequestResponse<ManagementContractModel> updateManagementContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        return updateContract(id, queryDsl, UPDATE_MANAGEMENT_CONTRACT_URI, ManagementContractModel.class);
    }

    private <T extends AbstractContractModel> RequestResponse<T> updateContract(String id, JsonNode queryDsl,
        String updateUri, Class<T> clasz)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {

        VitamRequestBuilder request = put()
            .withPath(updateUri + id)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        } catch (VitamClientInternalException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse createProfiles(List<ProfileModel> profileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input profile json is mandatory", profileModelList);

        VitamRequestBuilder request = post()
            .withPath(PROFILE_URI)
            .withBody(profileModelList)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse importProfileFile(String profileMetadataId, InputStream stream)
        throws ReferentialException {

        ParametersChecker.checkParameter("The input profile stream is mandatory", stream);
        ParametersChecker.checkParameter(profileMetadataId, "The profile id is mandatory");

        VitamRequestBuilder request = put()
            .withPath(PROFILE_URI + "/" + profileMetadataId)
            .withBody(stream)
            .withOctetContentType()
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public Response downloadProfileFile(String profileMetadataId)
        throws AdminManagementClientServerException, ProfileNotFoundException {
        ParametersChecker.checkParameter("Profile id is required", profileMetadataId);

        VitamRequestBuilder request = get()
            .withPath(PROFILE_URI + "/" + profileMetadataId)
            .withOctetAccept();

        Response response = null;
        Status status = null;
        try {
            response = make(request);
            status = fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response;
                default: {
                    String msgErr = "Error while download profile file : " + profileMetadataId;
                    final RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response);
                    if (!requestResponse.isOk()) {
                        VitamError error = (VitamError) requestResponse;
                        msgErr = error.getDescription();
                    }
                    throw new ProfileNotFoundException(msgErr);
                }
            }
        } catch (final VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR.getReasonPhrase(), e); // access-common
        } finally {
            if (status != Status.OK) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = get()
            .withPath(PROFILE_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                ProfileModel.class);
        } catch (VitamClientInternalException | BadRequestException | ReferentialNotFoundException |
            AccessUnauthorizedException | ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ProfileModel> findProfilesByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        JsonNode queryDsl;
        try {
            queryDsl = getIdentifierQuery(Profile.IDENTIFIER, documentId);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }

        VitamRequestBuilder request = get()
            .withPath(PROFILE_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            RequestResponseOK<ProfileModel> resp =
                getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ProfileModel.class);
            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                throw new ReferentialNotFoundException("Profile not found with id: " + documentId);
            }
            return resp;
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ProfileModel> updateProfile(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        VitamRequestBuilder request = put()
            .withPath(UPDATE_PROFIL_URI + id)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        } catch (VitamClientInternalException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse createArchiveUnitProfiles(List<ArchiveUnitProfileModel> profileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input archive unit profile json is mandatory", profileModelList);

        VitamRequestBuilder request = post()
            .withPath(ARCHIVE_UNIT_PROFILE_URI)
            .withBody(profileModelList)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = get()
            .withPath(ARCHIVE_UNIT_PROFILE_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                ArchiveUnitProfileModel.class);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfilesByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);

        JsonNode queryDsl;
        try {
            queryDsl = getIdentifierQuery(Profile.IDENTIFIER, documentId);
        } catch (InvalidCreateOperationException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }

        VitamRequestBuilder request = get()
            .withPath(ARCHIVE_UNIT_PROFILE_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            RequestResponseOK<ArchiveUnitProfileModel> resp =
                getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ArchiveUnitProfileModel.class);
            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                throw new ReferentialNotFoundException("ArchiveUnitProfile not found with id: " + documentId);
            }
            return resp;

        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> updateArchiveUnitProfile(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = put()
            .withPath(UPDATE_ARCHIVE_UNIT_PROFILE_URI + id)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return new RequestResponseOK<ArchiveUnitProfileModel>().setHttpCode(Status.OK.getStatusCode());
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public Status importContexts(List<ContextModel> contextModelList)
        throws ReferentialException {
        ParametersChecker.checkParameter("The input ingest contracts json is mandatory", contextModelList);

        VitamRequestBuilder request = post()
            .withPath(CONTEXT_URI)
            .withBody(contextModelList)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return fromStatusCode(response.getStatus());
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl)
        throws AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = put()
            .withPath(UPDATE_CONTEXT_URI + id)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class);
        } catch (VitamClientInternalException | InvalidParseOperationException |
            BadRequestException | AccessUnauthorizedException | ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ContextModel> findContexts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = get()
            .withPath(CONTEXT_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return getFromString(response.readEntity(String.class), RequestResponseOK.class, ContextModel.class);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ContextModel> findContextById(String id)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", id);

        JsonNode queryDsl;
        try {
            queryDsl = getIdentifierQuery(Context.IDENTIFIER, id);
        } catch (InvalidCreateOperationException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }

        VitamRequestBuilder request = get()
            .withPath(CONTEXT_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            RequestResponseOK<ContextModel> resp =
                getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ContextModel.class);

            if (resp.getResults() == null || resp.getResults().isEmpty())
                throw new ReferentialNotFoundException("Context not found with id: " + id);

            return resp;
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<Boolean> securityProfileIsUsedInContexts(String securityProfileId)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input security profile Id json is mandatory", securityProfileId);
        try {

            JsonNode queryDsl = getIdentifierQuery(Context.SECURITY_PROFILE, securityProfileId);

            RequestResponse<ContextModel> requestResponse = findContexts(queryDsl);

            List<ContextModel> results = ((RequestResponseOK<ContextModel>) requestResponse).getResults();
            RequestResponseOK<Boolean> result = new RequestResponseOK<>();
            if (results.isEmpty()) {
                result.addResult(false);
                return result;
            }

            result.addResult(true);
            return result;
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<JsonNode> launchAuditWorkflow(AuditOptions options)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        VitamRequestBuilder request = post()
            .withPath(AUDIT_URI)
            .withBody(options)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    private RequestResponse<JsonNode> getJsonNodeRequestResponse(JsonNode options, String auditUri)
        throws AdminManagementClientServerException {

        VitamRequestBuilder request = post()
            .withPath(auditUri)
            .withBody(options)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<JsonNode> launchRuleAudit() throws AdminManagementClientServerException {

        VitamRequestBuilder request = post()
            .withPath(AUDIT_RULE_URI)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }


    @Override
    public Status importSecurityProfiles(List<SecurityProfileModel> securityProfileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input security profiles json is mandatory", securityProfileModelList);

        VitamRequestBuilder request = post()
            .withPath(SECURITY_PROFILES_URI)
            .withBody(securityProfileModelList)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return fromStatusCode(response.getStatus());
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);

        VitamRequestBuilder request = get()
            .withPath(SECURITY_PROFILES_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);

            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                SecurityProfileModel.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfileByIdentifier(String identifier)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input identifier is mandatory", identifier);

        VitamRequestBuilder request = get()
            .withPath(SECURITY_PROFILES_URI + "/" + identifier)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                SecurityProfileModel.class);
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse updateSecurityProfile(String identifier, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        ParametersChecker.checkParameter("Tidentifier is mandatory", identifier);

        VitamRequestBuilder request = put()
            .withPath(SECURITY_PROFILES_URI + "/" + identifier)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return new RequestResponseOK<SecurityProfileModel>().setHttpCode(Status.OK.getStatusCode());
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        } catch (VitamClientInternalException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<IndexationResult> launchReindexation(JsonNode options)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);

        VitamRequestBuilder request = post()
            .withPath(REINDEX_URI)
            .withBody(options)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, IndexationResult.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<IndexationResult> switchIndexes(JsonNode options)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        VitamRequestBuilder request = post()
            .withPath(ALIASES_URI)
            .withBody(options)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, IndexationResult.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<JsonNode> evidenceAudit(JsonNode query) throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The query is mandatory", query);

        return getJsonNodeRequestResponse(query, EVIDENCE_AUDIT_URI);
    }

    @Override
    public RequestResponse<JsonNode> rectificationAudit(String operationId)
        throws AdminManagementClientServerException {
        VitamRequestBuilder request = post()
            .withPath(RECTIFICATION_AUDIT)
            .withBody(operationId)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportProbativeValue(ProbativeValueRequest probativeValueRequest)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The query is mandatory", probativeValueRequest);

        VitamRequestBuilder request = post()
            .withPath(PROBATIVE_VALUE_URI)
            .withBody(probativeValueRequest)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error ", e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse importOntologies(boolean forceUpdate, List<OntologyModel> ontologyModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The ontology json is mandatory", ontologyModelList);

        VitamRequestBuilder request = post()
            .withPath(ONTOLOGY_URI)
            .withHeader(GlobalDataRest.FORCE_UPDATE, forceUpdate)
            .withBody(ontologyModelList)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<OntologyModel> findOntologies(JsonNode query)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", query);

        VitamRequestBuilder request = get()
            .withPath(ONTOLOGY_URI)
            .withBody(query)
            .withJson();
        try (Response response = make(request)) {
            check(response);

            LOGGER.debug(Response.Status.OK.getReasonPhrase());
            return getFromString(response.readEntity(String.class), RequestResponseOK.class, OntologyModel.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new VitamRuntimeException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<OntologyModel> findOntologyByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        JsonNode queryDsl;

        try {
            queryDsl = getIdentifierQuery(Ontology.IDENTIFIER, documentId);
        } catch (InvalidCreateOperationException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }


        VitamRequestBuilder request = get()
            .withPath(ONTOLOGY_URI)
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);

            @SuppressWarnings("unchecked")
            RequestResponseOK<OntologyModel> resp =
                getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    OntologyModel.class);

            return resp;
        } catch (VitamClientInternalException | BadRequestException | AccessUnauthorizedException |
            ForbiddenClientException | DatabaseConflictException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    private JsonNode getIdentifierQuery(String identifierFiled, String documentId)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserSingle parser = new SelectParserSingle();
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq(identifierFiled, documentId));
        return parser.getRequest().getFinalSelect();
    }

    @Override
    public RequestResponse<ProcessPause> forcePause(ProcessPause info) throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ProcessPause json is mandatory", info);
        VitamRequestBuilder request = post()
            .withPath(FORCE_PAUSE_URI)
            .withBody(info)
            .withJson();
        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ProcessPause.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<ProcessPause> removeForcePause(ProcessPause info)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ProcessPause json is mandatory", info);

        VitamRequestBuilder request = post()
            .withPath(REMOVE_FORCE_PAUSE_URI)
            .withBody(info)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, ProcessPause.class);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterSymbolic> createAccessionRegisterSymbolic(Integer tenant)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("Tenant is mandatory.", tenant);

        VitamRequestBuilder request = post()
            .withPath("accession-register/symbolic")
            .withJsonAccept();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, AccessionRegisterSymbolic.class);
        } catch (final VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<List<AccessionRegisterSymbolicModel>> getAccessionRegisterSymbolic(Integer tenant,
        JsonNode queryDsl)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("Tenant is mandatory.", tenant);
        ParametersChecker.checkParameter("QueryDsl is mandatory.", tenant);

        VitamRequestBuilder request = get()
            .withPath("accession-register/symbolic")
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);
            return RequestResponse.parseFromResponse(response, AccessionRegisterSymbolicModel.class);
        } catch (final VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse importGriffins(List<GriffinModel> griffinModelList)
        throws AdminManagementClientServerException {

        ParametersChecker.checkParameter("griffin file  is mandatory", griffinModelList);

        VitamRequestBuilder request = post()
            .withPath("/importGriffins")
            .withBody(griffinModelList)
            .withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException | AccessUnauthorizedException | ReferentialNotFoundException |
            DatabaseConflictException | ForbiddenClientException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        }
    }

    @Override
    public RequestResponse importPreservationScenarios(List<PreservationScenarioModel> preservationScenarioModels)
        throws AdminManagementClientServerException {

        ParametersChecker.checkParameter("PreservationScenario file  is mandatory", preservationScenarioModels);

        VitamRequestBuilder request = post()
            .withPath("/importPreservationScenarios")
            .withBody(preservationScenarioModels)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException | DatabaseConflictException | ReferentialNotFoundException |
            AccessUnauthorizedException | ForbiddenClientException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (BadRequestException e) {
            throw new AdminManagementClientBadRequestException(e);
        }
    }

    @Override
    public RequestResponse<GriffinModel> findGriffin(JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException, ReferentialNotFoundException {

        VitamRequestBuilder request = get()
            .withPath("/griffin")
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);

            String entity = response.readEntity(String.class);
            @SuppressWarnings("unchecked")
            RequestResponseOK<GriffinModel> requestResponseOK =
                getFromString(entity, RequestResponseOK.class, GriffinModel.class);

            return requestResponseOK;
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public RequestResponse<GriffinModel> findGriffinByID(String id)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", id);
        try {
            JsonNode queryDsl = getIdentifierQuery(GriffinModel.TAG_IDENTIFIER, id);
            RequestResponse<GriffinModel> requestResponse = findGriffin(queryDsl);

            if (((RequestResponseOK) requestResponse).getResults() == null ||
                ((RequestResponseOK) requestResponse).getResults().isEmpty()) {
                throw new ReferentialNotFoundException("Griffin not found ");
            }
            return requestResponse;
        } catch (InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public RequestResponse<PreservationScenarioModel> findPreservationByID(String id)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", id);
        try {
            JsonNode queryDsl = getIdentifierQuery(PreservationScenarioModel.TAG_IDENTIFIER, id);

            RequestResponse<PreservationScenarioModel> requestResponseOK = findPreservation(queryDsl);

            if (((RequestResponseOK) requestResponseOK).getResults() == null ||
                ((RequestResponseOK) requestResponseOK).isEmpty()) {
                throw new ReferentialNotFoundException(String.format("Preservation Scenario not found %s", id));
            }
            return requestResponseOK;
        } catch (InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public RequestResponse<PreservationScenarioModel> findPreservation(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {

        VitamRequestBuilder request = get()
            .withPath("/preservationScenario")
            .withBody(queryDsl)
            .withJson();

        try (Response response = make(request)) {
            check(response);

            String entity = response.readEntity(String.class);

            return getFromString(entity, RequestResponseOK.class, PreservationScenarioModel.class);
        } catch (VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        }
    }

    @Override
    public Status createExternalOperation(LogbookOperationParameters logbookOperationparams)
        throws AdminManagementClientServerException, BadRequestException, LogbookClientAlreadyExistsException {

        VitamRequestBuilder request = post()
            .withPath(CREATE_EXTERNAL_OPERATION_URI)
            .withBody(logbookOperationparams)
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return fromStatusCode(response.getStatus());
        } catch (VitamClientInternalException | ReferentialNotFoundException | AccessUnauthorizedException |
            ForbiddenClientException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR_MSG, e);
        } catch (DatabaseConflictException e) {
            throw new LogbookClientAlreadyExistsException(e);
        }
    }

    private void check(Response response) throws VitamClientInternalException {
        final Status status = fromStatusCode(response.getStatus());
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientInternalException(
            String.format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }

    private void checkWithSpecificException(Response response)
        throws BadRequestException, VitamClientInternalException, ReferentialNotFoundException,
        AccessUnauthorizedException,
        ForbiddenClientException, DatabaseConflictException {
        final Status status = fromStatusCode(response.getStatus());

        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case OK:
            case CREATED:
                return;
            case NOT_FOUND:
                throw new ReferentialNotFoundException(status.getReasonPhrase());
            case BAD_REQUEST:
                String reason = (response.hasEntity()) ? response.readEntity(String.class)
                    : Response.Status.BAD_REQUEST.getReasonPhrase();
                throw new BadRequestException(reason);
            case UNAUTHORIZED:
                throw new AccessUnauthorizedException("Contract not found ");
            case FORBIDDEN:
                reason = (response.hasEntity()) ? response.readEntity(String.class)
                    : Response.Status.BAD_REQUEST.getReasonPhrase();
                throw new ForbiddenClientException(reason);
            case CONFLICT:
                throw new DatabaseConflictException(Response.Status.CONFLICT.getReasonPhrase());
            default:
                throw new VitamClientInternalException(
                    String.format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                        fromStatusCode(response.getStatus()).getReasonPhrase()));
        }
    }
}
