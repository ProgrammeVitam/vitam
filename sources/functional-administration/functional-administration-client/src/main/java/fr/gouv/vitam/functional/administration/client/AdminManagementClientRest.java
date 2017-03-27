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

import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterDetailModel;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterSummaryModel;
import fr.gouv.vitam.functional.administration.client.model.FileFormatModel;
import fr.gouv.vitam.functional.administration.client.model.RegisterValueDetailModel;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
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
    private static final String RULESMANAGER_IMPORT_URL = "/rules/import";
    private static final String RULESMANAGER_GET_DOCUMENT_URL = "/rules/document";
    private static final String RULESMANAGER_URL = "/rules";

    private static final String ACCESSION_REGISTER_CREATE_URI = "/accession-register";
    private static final String ACCESSION_REGISTER_GET_DOCUMENT_URL = "/accession-register/document";
    private static final String ACCESSION_REGISTER_GET_DETAIL_URL = "accession-register/detail";

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
        try {
            response = performRequest(HttpMethod.POST, FORMAT_CHECK_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                /* BAD_REQUEST status is more suitable when formats are not well formated */    
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ?  response.readEntity(String.class) : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new ReferentialException(reason);
                default:
                    break;
            }
            return Response.fromResponse(response).build();
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response importFormat(InputStream stream) throws ReferentialException, DatabaseConflictException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, FORMAT_IMPORT_URL, null,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ?  response.readEntity(String.class) : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new ReferentialException(reason);
                case CONFLICT:
                    LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                    throw new DatabaseConflictException("Collection input conflic");
                default:
                    break;
            }
            return Response.fromResponse(response).build();
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
            response = performRequest(HttpMethod.POST, FORMAT_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new ReferentialException("Rules Not found ");
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
                    throw new ReferentialException("File format error");
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
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_JSON_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                /* BAD_REQUEST status is more suitable when rules are not well formated */
                case BAD_REQUEST:
                    String reason = (response.hasEntity()) ?  response.readEntity(String.class) : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new FileRulesException(reason);
                default:
                    break;
            }
            return Response.fromResponse(response).build();
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response importRulesFile(InputStream stream)
        throws FileRulesException, DatabaseConflictException, AdminManagementClientServerException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, RULESMANAGER_IMPORT_URL, null,
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
                    String reason = (response.hasEntity()) ?  response.readEntity(String.class) : Response.Status.BAD_REQUEST.getReasonPhrase();
                    LOGGER.error(reason);
                    throw new FileRulesException(reason);
                case CONFLICT:
                    LOGGER.debug(Response.Status.CONFLICT.getReasonPhrase());
                    throw new DatabaseConflictException("Collection input conflic");
                default:
                    break;
            }
            return Response.fromResponse(response).build();
        } catch (final VitamClientInternalException e) {
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
            response = performRequest(HttpMethod.POST, RULESMANAGER_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE, false);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(Response.Status.NOT_FOUND.getReasonPhrase());
                    throw new FileRulesException("File Rules not found");
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
                    throw new FileRulesException("Rule Not found ");
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
    public void createorUpdateAccessionRegister(AccessionRegisterDetailModel register)
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
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> getAccessionRegister(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {
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
                default:
                    break;
            }
            return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
                AccessionRegisterSummaryModel.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new AdminManagementClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<AccessionRegisterDetailModel> getAccessionRegisterDetail(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {

        ParametersChecker.checkParameter("query is a mandatory parameter", query);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, ACCESSION_REGISTER_GET_DETAIL_URL, null, query,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
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
        accessionRegisterDetail.setId(model.getId()).
            setOriginatingAgency(model.getOriginatingAgency()).
            setSubmissionAgency(model.getSubmissionAgency()).
            setEndDate(model.getEndDate()).
            setStartDate(model.getStartDate());
        if (model.getStatus() != null) {
            accessionRegisterDetail.setStatus(model.getStatus());

        }
        accessionRegisterDetail.setLastUpdate(model.getLastUpdate());

        if (model.getTotalObjectsGroups() != null) {
            totalObjectsGroups.setTotal(model.getTotalObjectsGroups().getTotal()).
                setRemained(model.getTotalObjectsGroups().getRemained()).
                setDeleted(model.getTotalObjectsGroups().getDeleted());

            accessionRegisterDetail.setTotalObjectGroups(totalObjectsGroups);
        }
        if (model.getTotalUnits() != null) {
            totalUnits.setTotal(model.getTotalUnits().getTotal()).
                setRemained(model.getTotalUnits().getRemained()).
                setDeleted(model.getTotalUnits().getDeleted());

            accessionRegisterDetail.setTotalUnits(totalUnits);
        }
        if (model.getTotalObjects() != null) {
            totalObjects.setTotal(model.getTotalObjects().getTotal()).
                setRemained(model.getTotalObjects().getRemained()).
                setDeleted(model.getTotalObjects().getDeleted());

            accessionRegisterDetail.setTotalObjects(totalObjects);
        }
        if (model.getObjectSize() != null) {
            objectSize.setTotal(model.getObjectSize().getTotal()).
                setRemained(model.getObjectSize().getRemained()).
                setDeleted(model.getObjectSize().getDeleted());
            accessionRegisterDetail.setObjectSize(objectSize);
        }

        if (model.getOperationsIds() != null) {
            accessionRegisterDetail.setOperationIds(model.getOperationsIds().toString());
        }

        return accessionRegisterDetail;
    }

}
