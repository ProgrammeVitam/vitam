/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSymbolicModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

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
    private static final String UPDATE_ACCESS_CONTRACT_URI = "/accesscontracts/";
    private static final String UPDATE_INGEST_CONTRACT_URI = "/ingestcontracts/";
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

    private static final String FORCE_PAUSE_URI = "/forcepause";
    private static final String REMOVE_FORCE_PAUSE_URI = "/removeforcepause";

    AdminManagementClientRest(AdminManagementClientFactory factory) {
        super(factory);
    }

    // TODO P1 : Refactorisation à réfléchir pour ne pas avoir une seule classe gérant tous les endpoints (formats,
    // régles
    // de gestions, contrat , etc)
    @Override
    public Response checkFormat(InputStream stream) throws ReferentialException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        Status status = null;
        try {
            response = performRequest(POST, FORMAT_CHECK_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, APPLICATION_JSON_TYPE);
            status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                /* BAD_REQUEST status is more suitable when formats are not well formated */
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new ReferentialException(reason);
                default:
                    break;
            }
            return response;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        }
    }

    @Override
    public Status importFormat(InputStream stream, String filename)
        throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_FILENAME, filename);
        try {
            response = performRequest(POST, FORMAT_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new ReferentialException(reason);
                case CONFLICT:
                    LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                    throw new DatabaseConflictException("Collection input conflic");
                default:
                    break;
            }
            return status;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getFormatByID(String id) throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);
        Response response = null;
        try {
            response = performRequest(GET, FORMAT_URL + "/" + id, null,
                APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialNotFoundException("Formats Not found ");
                default:
                    break;
            }

            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse<FileFormatModel> getFormats(JsonNode query)
        throws ReferentialException, InvalidParseOperationException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        Response response = null;
        try {
            response = performRequest(POST, FORMAT_GET_DOCUMENT_URL, null,
                query, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE, false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialNotFoundException("File format not found");
                default:
                    throw new ReferentialException("Unknown error");
            }
            return getFromString(response.readEntity(String.class), RequestResponseOK.class, FileFormatModel.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Response checkRulesFile(InputStream stream) throws FileRulesException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        try {
            response = performRequest(POST, RULESMANAGER_CHECK_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return response;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        }
    }

    @Override
    public Response checkAgenciesFile(InputStream stream)
        throws FileRulesException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        try {
            response = performRequest(POST, AGENCIESMANAGER_CHECK_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return response;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        }
    }

    @Override
    public Status importRulesFile(InputStream stream, String filename)
        throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_FILENAME, filename);
        try {
            response = performRequest(POST, RULESMANAGER_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, APPLICATION_JSON_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case CREATED:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new FileRulesException(reason);
                case FORBIDDEN:
                    String forbiddenReason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Status.FORBIDDEN.getReasonPhrase();
                    LOGGER.error(forbiddenReason);
                    throw new FileRulesImportInProgressException(forbiddenReason);
                case CONFLICT:
                    LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                    throw new DatabaseConflictException("Collection input conflic");
                default:
                    break;
            }
            return status;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Status importAgenciesFile(InputStream stream, String filename)
        throws ReferentialException {
        ParametersChecker.checkParameter("filename is a mandatory parameter", filename);
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_FILENAME, filename);
        try {
            response = performRequest(POST, AGENCIESMANAGER_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, APPLICATION_JSON_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case CREATED:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new ReferentialException(reason);

                default:
                    break;
            }
            return status;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getAgencies(JsonNode query)
        throws ReferentialException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        Response response = null;
        try {
            response = performRequest(GET, AGENCIES_URL, null,
                query, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE, false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new FileRulesNotFoundException("Agency Not found ");
                default:
                    break;
            }
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AgenciesModel> getAgencyById(String id)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {


        ParametersChecker.checkParameter("The input documentId json is mandatory", id);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(AgenciesModel.TAG_IDENTIFIER, id);
            response = performRequest(GET, AGENCIES_URL, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE, false);

            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<AgenciesModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        AgenciesModel.class);


                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Agency not found with id: " + id);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, AgenciesModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getRuleByID(String id)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);
        Response response = null;
        try {
            response = performRequest(GET, RULESMANAGER_URL + "/" + id, null,
                APPLICATION_JSON_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new FileRulesNotFoundException("File Rules not found");
                default:
                    break;
            }
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public JsonNode getRules(JsonNode query)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        Response response = null;
        try {
            response = performRequest(POST, RULESMANAGER_GET_DOCUMENT_URL, null,
                query, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE, false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new FileRulesNotFoundException("Rule Not found ");
                default:
                    break;
            }
            return getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createorUpdateAccessionRegister(AccessionRegisterDetailModel register)
        throws AccessionRegisterException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("Accession register is a mandatory parameter", register);
        Response response = null;
        try {
            response = performRequest(POST, ACCESSION_REGISTER_CREATE_URI, null,
                register, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE, false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case CONFLICT:
                    // When Accession Register detail already exists
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;

                case BAD_REQUEST:
                    LOGGER.error(Response.Status.BAD_REQUEST.getReasonPhrase());
                    throw new AccessionRegisterException("File format error");
                default:
                    throw new AccessionRegisterException("Unknown error: " + status.getStatusCode());
            }
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> getAccessionRegister(JsonNode query)
        throws InvalidParseOperationException, ReferentialException, AccessUnauthorizedException {
        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        Response response = null;
        try {
            response = performRequest(POST, ACCESSION_REGISTER_GET_DOCUMENT_URL, null, query,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    LOGGER.error(Response.Status.BAD_REQUEST.getReasonPhrase());
                    String reason = (response.hasEntity()) ? response.readEntity(String.class)
                        : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new InvalidParseOperationException(reason);
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialNotFoundException("AccessionRegister Not found ");
                case UNAUTHORIZED:
                    LOGGER.error(Status.UNAUTHORIZED.getReasonPhrase());
                    throw new AccessUnauthorizedException("Contract not found ");
                default:
                    break;
            }
            String value = response.readEntity(String.class);
            @SuppressWarnings("unchecked")
            RequestResponseOK fromString =
                getFromString(value, RequestResponseOK.class,
                    AccessionRegisterSummaryModel.class);
            return fromString;
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterDetailModel> getAccessionRegisterDetail(String documentId, JsonNode query)
        throws InvalidParseOperationException, ReferentialException {

        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        ParametersChecker.checkParameter("documentId is a mandatory parameter", documentId);
        Response response = null;
        try {
            response = performRequest(POST, ACCESSION_REGISTER_GET_DETAIL_URL + "/" + documentId,
                null, query, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialNotFoundException("AccessionRegister Detail Not found ");
                default:
                    throw new AccessionRegisterException("Unknown error: " + status.getStatusCode());
            }
            return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                AccessionRegisterDetailModel.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Status importIngestContracts(List<IngestContractModel> ingestContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ingest contracts json is mandatory", ingestContractModelList);
        Response response = null;

        try {
            response = performRequest(POST, INGEST_CONTRACTS_URI, null,
                ingestContractModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            final Status status = Status.fromStatusCode(response.getStatus());

            return status;
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Status importAccessContracts(List<AccessContractModel> accessContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input access contracts json is mandatory", accessContractModelList);
        Response response = null;

        try {
            response = performRequest(POST, ACCESS_CONTRACTS_URI, null,
                accessContractModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            final Status status = Status.fromStatusCode(response.getStatus());
            return status;

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {

        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(GET, ACCESS_CONTRACTS_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    AccessContractModel.class);
            }

            return RequestResponse.parseFromResponse(response, AccessContractModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public RequestResponse<AccessContractModel> findAccessContractsByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(AccessContract.IDENTIFIER, documentId);


            response = performRequest(GET, ACCESS_CONTRACTS_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<AccessContractModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        AccessContractModel.class);


                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Access contract not found with id: " + documentId);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, AccessContractModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(GET, INGEST_CONTRACTS_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    IngestContractModel.class);
            }

            return RequestResponse.parseFromResponse(response, IngestContractModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractsByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(IngestContract.IDENTIFIER, documentId);

            response = performRequest(GET, INGEST_CONTRACTS_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<IngestContractModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        IngestContractModel.class);

                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Ingest contract not found with id: " + documentId);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, IngestContractModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createProfiles(List<ProfileModel> profileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input profile json is mandatory", profileModelList);
        Response response = null;

        try {
            response = performRequest(POST, PROFILE_URI, null,
                profileModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse importProfileFile(String profileMetadataId, InputStream stream)
        throws ReferentialException {

        ParametersChecker.checkParameter("The input profile stream is mandatory", stream);
        ParametersChecker.checkParameter(profileMetadataId, "The profile id is mandatory");
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, PROFILE_URI + "/" + profileMetadataId, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);

        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response downloadProfileFile(String profileMetadataId)
        throws AdminManagementClientServerException, ProfileNotFoundException {
        ParametersChecker.checkParameter("Profile id is required", profileMetadataId);

        Response response = null;
        Status status = null;
        try {
            response = performRequest(GET, PROFILE_URI + "/" + profileMetadataId, null, null,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
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
                    throw new ProfileNotFoundException(msgErr);
                }
            }
        } catch (final VitamClientInternalException e) {
            throw new AdminManagementClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
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
        Response response = null;
        try {
            response = performRequest(GET, PROFILE_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ProfileModel.class);
            }

            return RequestResponse.parseFromResponse(response, ProfileModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProfileModel> findProfilesByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(Profile.IDENTIFIER, documentId);


            response = performRequest(GET, PROFILE_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<ProfileModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        ProfileModel.class);


                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Profile not found with id: " + documentId);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, ProfileModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProfileModel> updateProfile(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_PROFIL_URI + id, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class);
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Profile not found with id: " + id);
            }

            return RequestResponse.parseFromResponse(response, AccessContractModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createArchiveUnitProfiles(List<ArchiveUnitProfileModel> profileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input archive unit profile json is mandatory", profileModelList);
        Response response = null;

        try {
            response = performRequest(POST, ARCHIVE_UNIT_PROFILE_URI, null,
                profileModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(GET, ARCHIVE_UNIT_PROFILE_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ArchiveUnitProfileModel.class);
            }

            return RequestResponse.parseFromResponse(response, ArchiveUnitProfileModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> findArchiveUnitProfilesByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(Profile.IDENTIFIER, documentId);


            response = performRequest(GET, ARCHIVE_UNIT_PROFILE_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<ArchiveUnitProfileModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        ArchiveUnitProfileModel.class);

                if (resp.getResults() == null || resp.getResults().size() == 0) {
                    throw new ReferentialNotFoundException("ArchiveUnitProfile not found with id: " + documentId);
                }

                return resp;
            }

            return RequestResponse.parseFromResponse(response, ArchiveUnitProfileModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> updateArchiveUnitProfile(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_ARCHIVE_UNIT_PROFILE_URI + id, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<ArchiveUnitProfileModel>().setHttpCode(Status.OK.getStatusCode());
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Profile not found with id: " + id);
            }

            return RequestResponse.parseFromResponse(response, ArchiveUnitProfileModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessContractModel> updateAccessContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_ACCESS_CONTRACT_URI + id, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class);
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Access contract not found with id: " + id);
            }
            return RequestResponse.parseFromResponse(response, AccessContractModel.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<IngestContractModel> updateIngestContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_INGEST_CONTRACT_URI + id, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class);
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Ingest contract not found with id: " + id);
            }
            return RequestResponse.parseFromResponse(response, IngestContractModel.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Status importContexts(List<ContextModel> ContextModelList)
        throws ReferentialException {
        ParametersChecker.checkParameter("The input ingest contracts json is mandatory", ContextModelList);
        Response response = null;

        try {
            response = performRequest(POST, CONTEXT_URI, null,
                ContextModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            final Status status = Status.fromStatusCode(response.getStatus());

            if (Response.Status.BAD_REQUEST.equals(status)) {
                String reason = (response.hasEntity()) ? response.readEntity(String.class)
                    : Response.Status.BAD_REQUEST.getReasonPhrase();
                LOGGER.error(reason);
                throw new ReferentialException("Referential Error: " + reason);
            }
            return status;
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl)
        throws AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_CONTEXT_URI + id, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());

            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class);
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Context not found with id: " + id);
            }
            return RequestResponse.parseFromResponse(response, ContextModel.class);
        } catch (VitamClientInternalException | InvalidParseOperationException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ContextModel> findContexts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(GET, CONTEXT_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    ContextModel.class);
            }

            return RequestResponse.parseFromResponse(response, ContextModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ContextModel> findContextById(String id)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", id);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(Context.IDENTIFIER, id);


            response = performRequest(GET, CONTEXT_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<ContextModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        ContextModel.class);


                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Context not found with id: " + id);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, ContextModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
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
            throw new AdminManagementClientServerException("Internal Server Error", e);
        }
    }

    @Override
    public RequestResponse<JsonNode> launchAuditWorkflow(JsonNode options) throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        return getJsonNodeRequestResponse(options, AUDIT_URI);
    }

    private RequestResponse<JsonNode> getJsonNodeRequestResponse(JsonNode options, String auditUri)
        throws AdminManagementClientServerException {
        Response response = null;
        try {
            response = performRequest(POST, auditUri, null, options,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> launchRuleAudit() throws AdminManagementClientServerException {
        Response response = null;
        try {
            response = performRequest(POST, AUDIT_RULE_URI, null, null,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Status importSecurityProfiles(List<SecurityProfileModel> securityProfileModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input security profiles json is mandatory", securityProfileModelList);
        Response response = null;

        try {
            response = performRequest(POST, SECURITY_PROFILES_URI, null,
                securityProfileModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return Status.fromStatusCode(response.getStatus());

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(GET, SECURITY_PROFILES_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    SecurityProfileModel.class);
            }

            return RequestResponse.parseFromResponse(response, SecurityProfileModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfileByIdentifier(String identifier)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input identifier is mandatory", identifier);
        Response response = null;
        try {

            response = performRequest(GET, SECURITY_PROFILES_URI + "/" + identifier, null, null,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<SecurityProfileModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        SecurityProfileModel.class);

                if (resp.getResults() == null || resp.getResults().size() == 0)
                    throw new ReferentialNotFoundException("Security profile not found with id: " + identifier);

                return resp;
            }

            return RequestResponse.parseFromResponse(response, SecurityProfileModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateSecurityProfile(String identifier, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, SECURITY_PROFILES_URI + "/" + identifier, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<SecurityProfileModel>().setHttpCode(Status.OK.getStatusCode());
            } else if (status == Status.NOT_FOUND) {
                throw new ReferentialNotFoundException("Security Profile not found with id: " + identifier);
            }
            return RequestResponse.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<IndexationResult> launchReindexation(JsonNode options)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        Response response = null;
        RequestResponse result = null;
        try {
            response = performRequest(POST, REINDEX_URI, null, options,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, IndexationResult.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<IndexationResult> switchIndexes(JsonNode options)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        Response response = null;
        RequestResponse result = null;
        try {
            response = performRequest(POST, ALIASES_URI, null, options,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, IndexationResult.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
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
        Response response = null;
        try {
            response = performRequest(POST, RECTIFICATION_AUDIT, null, operationId,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> exportProbativeValue(ProbativeValueRequest probativeValueRequest)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The query is mandatory", probativeValueRequest);
        Response response = null;
        try {
            response = performRequest(POST, PROBATIVE_VALUE_URI, null, probativeValueRequest,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error ", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse importOntologies(boolean forceUpdate, List<OntologyModel> ontologyModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The ontology json is mandatory", ontologyModelList);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.FORCE_UPDATE, forceUpdate);

        Response response = null;
        try {
            response = performRequest(POST, ONTOLOGY_URI, headers,
                ontologyModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<OntologyModel> findOntologies(JsonNode query)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", query);
        Response response = null;
        try {
            response = performRequest(GET, ONTOLOGY_URI, null, query,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    OntologyModel.class);
            }

            return RequestResponse.parseFromResponse(response, OntologyModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<OntologyModel> findOntologyByID(String documentId)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        ParametersChecker.checkParameter("The input documentId json is mandatory", documentId);
        Response response = null;
        try {

            JsonNode queryDsl = getIdentifierQuery(Ontology.IDENTIFIER, documentId);

            response = performRequest(GET, ONTOLOGY_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                @SuppressWarnings("unchecked")
                RequestResponseOK<OntologyModel> resp =
                    getFromString(response.readEntity(String.class), RequestResponseOK.class,
                        OntologyModel.class);

                if (resp.getResults() == null || resp.getResults().isEmpty()) {
                    throw new ReferentialNotFoundException("Ontology not found with id: " + documentId);
                }

                return resp;
            }

            return RequestResponse.parseFromResponse(response, OntologyModel.class);

        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
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
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, FORCE_PAUSE_URI, null, info,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, ProcessPause.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProcessPause> removeForcePause(ProcessPause info)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ProcessPause json is mandatory", info);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, REMOVE_FORCE_PAUSE_URI, null, info,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, ProcessPause.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterSymbolic> createAccessionRegisterSymbolic(Integer tenant)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("Tenant is mandatory.", tenant);
        Response response = null;
        try {
            response = performRequest(POST, "accession-register/symbolic", null, null, null, APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, AccessionRegisterSymbolic.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<List<AccessionRegisterSymbolicModel>> getAccessionRegisterSymbolic(Integer tenant,
        JsonNode queryDsl)
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("Tenant is mandatory.", tenant);
        ParametersChecker.checkParameter("QueryDsl is mandatory.", tenant);
        Response response = null;
        try {
            response = performRequest(GET, "accession-register/symbolic", null, queryDsl, APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, AccessionRegisterSymbolicModel.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse importGriffins(List<GriffinModel> griffinModelList)
        throws AdminManagementClientServerException {

        ParametersChecker.checkParameter("griffin file  is mandatory", griffinModelList);
        Response response = null;
        try {
            response = performRequest(POST, "/importGriffins", null,
                griffinModelList, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse importPreservationScenarios(List<PreservationScenarioModel> preservationScenarioModels)
        throws AdminManagementClientServerException {

        ParametersChecker.checkParameter("PreservationScenario file  is mandatory", preservationScenarioModels);
        Response response = null;
        try {
            response = performRequest(POST, "/importPreservationScenarios", null,
                preservationScenarioModels, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE,
                false);
            return RequestResponse.parseFromResponse(response);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<GriffinModel> findGriffin(JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException, ReferentialNotFoundException {
        Response response = null;
        try {

            response = performRequest(GET, "/griffin", null, queryDsl, APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {

                String entity = response.readEntity(String.class);
                @SuppressWarnings("unchecked")
                RequestResponseOK<GriffinModel> requestResponseOK =
                    getFromString(entity, RequestResponseOK.class, GriffinModel.class);


                return requestResponseOK;
            }
            return RequestResponse.parseFromResponse(response, GriffinModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
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
        Response response = null;

        try {
            response = performRequest(GET, "/preservationScenario", null, queryDsl, APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {

                String entity = response.readEntity(String.class);
                @SuppressWarnings("unchecked")
                RequestResponseOK<PreservationScenarioModel> requestResponseOK =
                    getFromString(entity, RequestResponseOK.class, PreservationScenarioModel.class);

                return requestResponseOK;
            }
            return RequestResponse.parseFromResponse(response, PreservationScenarioModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
