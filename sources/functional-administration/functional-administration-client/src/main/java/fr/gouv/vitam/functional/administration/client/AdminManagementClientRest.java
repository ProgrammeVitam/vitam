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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;

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
    private static final String UPDATE_CONTEXT_URI = "/context/";
    private static final String UPDATE_PROFIL_URI = "/profiles/";
    private static final String SECURITY_PROFILES_URI = "/securityprofiles";

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
            response = performRequest(HttpMethod.POST, FORMAT_CHECK_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
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
            response = performRequest(HttpMethod.POST, FORMAT_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
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
            response = performRequest(HttpMethod.GET, FORMAT_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE);
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

            return JsonHandler.getFromString(response.readEntity(String.class));
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
            response = performRequest(HttpMethod.POST, FORMAT_GET_DOCUMENT_URL, null,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
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
            return JsonHandler
                .getFromString(response.readEntity(String.class), RequestResponseOK.class, FileFormatModel.class);
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
            response = performRequest(HttpMethod.POST, RULESMANAGER_CHECK_URL, null,
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
            response = performRequest(HttpMethod.POST, AGENCIESMANAGER_CHECK_URL, null,
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
            response = performRequest(HttpMethod.POST, RULESMANAGER_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);

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
            response = performRequest(HttpMethod.POST, AGENCIESMANAGER_IMPORT_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);

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
            response = performRequest(HttpMethod.GET, AGENCIES_URL, null,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
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
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getAgencyById(String id)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("id is a mandatory parameter", id);
        Response response = null;

        try {

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(AgenciesModel.TAG_IDENTIFIER, id));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();
            response = performRequest(HttpMethod.GET, AGENCIES_URL, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialNotFoundException("File Agency not found");
                default:
                    break;
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("unable to create query", e);
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
            response = performRequest(HttpMethod.GET, RULESMANAGER_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE);

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
            return JsonHandler.getFromString(response.readEntity(String.class));
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
            response = performRequest(HttpMethod.POST, RULESMANAGER_GET_DOCUMENT_URL, null,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
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
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse createorUpdateAccessionRegister(AccessionRegisterDetailModel register)
        throws DatabaseConflictException, AccessionRegisterException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("Accession register is a mandatory parameter", register);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, ACCESSION_REGISTER_CREATE_URI, null,
                mappingDetailModelToDetail(register), MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
                false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case PRECONDITION_FAILED:
                    LOGGER.error(Response.Status.PRECONDITION_FAILED.getReasonPhrase());
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
            response = performRequest(HttpMethod.POST, ACCESSION_REGISTER_GET_DOCUMENT_URL, null, query,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
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
            RequestResponseOK fromString =
                JsonHandler.getFromString(value, RequestResponseOK.class,
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
            response = performRequest(HttpMethod.POST, ACCESSION_REGISTER_GET_DETAIL_URL + "/" + documentId,
                null, query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
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
            return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
                AccessionRegisterDetailModel.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    private AccessionRegisterDetail mappingDetailModelToDetail(AccessionRegisterDetailModel model) {
        AccessionRegisterDetail accessionRegisterDetail = new AccessionRegisterDetail();
        RegisterValueDetailModel totalObjectsGroups = new RegisterValueDetailModel();
        RegisterValueDetailModel totalUnits = new RegisterValueDetailModel();
        RegisterValueDetailModel totalObjects = new RegisterValueDetailModel();
        RegisterValueDetailModel objectSize = new RegisterValueDetailModel();
        accessionRegisterDetail.setId(model.getId()).setOriginatingAgency(model.getOriginatingAgency())
            .setSubmissionAgency(model.getSubmissionAgency())
            .setArchivalAgreement(model.getArchivalAgreement()).setEndDate(model.getEndDate())
            .setStartDate(model.getStartDate())
            .setSymbolic(model.isSymbolic());
        if (model.getStatus() != null) {
            accessionRegisterDetail.setStatus(model.getStatus());

        }
        accessionRegisterDetail.setLastUpdate(model.getLastUpdate());

        if (model.getTotalObjectsGroups() != null) {
            totalObjectsGroups.setIngested(model.getTotalObjectsGroups().getIngested())
                .setRemained(model.getTotalObjectsGroups().getRemained())
                .setDeleted(model.getTotalObjectsGroups().getDeleted())
                .setSymbolicRemained(model.getTotalObjectsGroups().getSymbolicRemained())
                .setAttached(model.getTotalObjectsGroups().getAttached())
                .setDetached(model.getTotalObjectsGroups().getDetached());

            accessionRegisterDetail.setTotalObjectGroups(totalObjectsGroups);
        }
        if (model.getTotalUnits() != null) {
            totalUnits.setIngested(model.getTotalUnits().getIngested())
                .setRemained(model.getTotalUnits().getRemained())
                .setDeleted(model.getTotalUnits().getDeleted())
                .setAttached(model.getTotalUnits().getAttached())
                .setSymbolicRemained(model.getTotalUnits().getSymbolicRemained())
                .setDetached(model.getTotalUnits().getDetached());

            accessionRegisterDetail.setTotalUnits(totalUnits);
        }
        if (model.getTotalObjects() != null) {
            totalObjects.setIngested(model.getTotalObjects().getIngested())
                .setRemained(model.getTotalObjects().getRemained())
                .setDeleted(model.getTotalObjects().getDeleted())
                .setSymbolicRemained(model.getTotalObjects().getSymbolicRemained())
                .setAttached(model.getTotalObjects().getAttached())
                .setDetached(model.getTotalObjects().getDetached());

            accessionRegisterDetail.setTotalObjects(totalObjects);
        }
        if (model.getObjectSize() != null) {
            objectSize.setIngested(model.getObjectSize().getIngested())
                .setRemained(model.getObjectSize().getRemained())
                .setDeleted(model.getObjectSize().getDeleted())
                .setAttached(model.getObjectSize().getAttached())
                .setSymbolicRemained(model.getObjectSize().getSymbolicRemained())
                .setDetached(model.getObjectSize().getDetached());
            accessionRegisterDetail.setObjectSize(objectSize);
        }

        if (model.getOperationsIds() != null) {
            accessionRegisterDetail.setOperationIds(model.getOperationsIds());
        }

        return accessionRegisterDetail;
    }

    @Override
    public Status importIngestContracts(List<IngestContractModel> ingestContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input ingest contracts json is mandatory", ingestContractModelList);
        Response response = null;

        try {
            response = performRequest(HttpMethod.POST, INGEST_CONTRACTS_URI, null,
                ingestContractModelList, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
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
            response = performRequest(HttpMethod.POST, ACCESS_CONTRACTS_URI, null,
                accessContractModelList, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
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
            response = performRequest(HttpMethod.GET, ACCESS_CONTRACTS_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(AccessContract.IDENTIFIER, documentId));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();


            response = performRequest(HttpMethod.GET, ACCESS_CONTRACTS_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<AccessContractModel> resp =
                    JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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
            response = performRequest(HttpMethod.GET, INGEST_CONTRACTS_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(IngestContract.IDENTIFIER, documentId));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();

            response = performRequest(HttpMethod.GET, INGEST_CONTRACTS_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<IngestContractModel> resp =
                    JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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
            response = performRequest(HttpMethod.POST, PROFILE_URI, null,
                profileModelList, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
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
                MediaType.APPLICATION_JSON_TYPE);
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
            response = performRequest(HttpMethod.GET, PROFILE_URI + "/" + profileMetadataId, null, null,
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
            response = performRequest(HttpMethod.GET, PROFILE_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(Profile.IDENTIFIER, documentId));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();


            response = performRequest(HttpMethod.GET, PROFILE_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<ProfileModel> resp =
                    JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_PROFIL_URI + id, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<ProfileModel>().setHttpCode(Status.OK.getStatusCode());
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
    public RequestResponse<AccessContractModel> updateAccessContract(String id, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_ACCESS_CONTRACT_URI + id, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<AccessContractModel>().setHttpCode(Status.OK.getStatusCode());
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
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_INGEST_CONTRACT_URI + id, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<IngestContractModel>().setHttpCode(Status.OK.getStatusCode());
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
            response = performRequest(HttpMethod.POST, CONTEXT_URI, null,
                ContextModelList, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
                false);
            final Status status = Status.fromStatusCode(response.getStatus());

            if (Response.Status.BAD_REQUEST.equals(status)) {
                String reason = (response.hasEntity()) ? response.readEntity(String.class) :
                    Response.Status.BAD_REQUEST.getReasonPhrase();
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
        throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UPDATE_CONTEXT_URI + id, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<ContextModel>().setHttpCode(Status.OK.getStatusCode());
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
    public RequestResponse<ContextModel> findContexts(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTEXT_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(Context.IDENTIFIER, id));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();


            response = performRequest(HttpMethod.GET, CONTEXT_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<ContextModel> resp =
                    JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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
    public RequestResponse<JsonNode> launchAuditWorkflow(JsonNode options) throws AdminManagementClientServerException {
        ParametersChecker.checkParameter("The options are mandatory", options);
        Response response = null;
        RequestResponse result = null;
        try {
            response = performRequest(HttpMethod.POST, AUDIT_URI, null, options,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

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
            response = performRequest(HttpMethod.POST, SECURITY_PROFILES_URI, null,
                securityProfileModelList, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE,
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
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, SECURITY_PROFILES_URI, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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

            response = performRequest(HttpMethod.GET, SECURITY_PROFILES_URI + "/" + identifier, null, null,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                RequestResponseOK<SecurityProfileModel> resp =
                    JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
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
    public RequestResponse<SecurityProfileModel> updateSecurityProfile(String identifier, JsonNode queryDsl)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, SECURITY_PROFILES_URI + "/" + identifier, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return new RequestResponseOK<SecurityProfileModel>().setHttpCode(Status.OK.getStatusCode());
            }
            return RequestResponse.parseFromResponse(response, SecurityProfileModel.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
