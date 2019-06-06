/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 **/

package fr.gouv.vitam.metadata.client;

import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.*;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.*;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.ReclassificationChildNodeExportRequest;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

/**
 * Rest client for metadata
 */
public class MetaDataClientRest extends DefaultClient implements MetaDataClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientRest.class);

    public final String COMPUTE_GRAPH_URI = "/computegraph";


    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";
    public static final String DESCRIPTION = "description";

    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public MetaDataClientRest(VitamClientFactoryInterface factory) {
        super(factory);
    }

    @Override
    public JsonNode insertUnit(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), insertQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(POST, "/units", null, insertQuery, APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_METADATA_VALUE.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode insertUnitBulk(List<ObjectNode> insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), insertQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response =
                performRequest(POST, "/units/bulk", null, insertQuery, APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_METADATA_VALUE.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_UNITS_QUERY_NULL.getMessage(), selectQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/units", null, selectQuery, APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGroups(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL.getMessage(), selectQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, "/objectgroups", null, selectQuery, APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter("One parameter is empty", selectQuery, unitId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId, null, selectQuery,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL.getMessage(), selectQuery);
            ParametersChecker.checkParameter(ErrorMessage.BLANK_PARAM.getMessage(), objectGroupId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/objectgroups/" + objectGroupId, null, selectQuery,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode updateUnitById(JsonNode updateQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException,
        MetaDataNotFoundException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage(), updateQuery, unitId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(unitId)) {
            throw new InvalidParseOperationException("unitId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/units/" + unitId, null, updateQuery,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            if(response.getStatus() == Status.OK.getStatusCode()) {
                return response.readEntity(JsonNode.class);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                JsonNode resp = response.readEntity(JsonNode.class);
                if (null != resp) {
                    JsonNode errNode = resp.get(DESCRIPTION);
                    if (null != errNode) {
                        throw new InvalidParseOperationException(JsonHandler.unprettyPrint(errNode));
                    }
                }
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            }
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        ParametersChecker.checkParameter("Insert Request is a mandatory parameter", insertQuery);
        Response response = null;
        try {
            response =
                performRequest(POST, "/objectgroups", null, insertQuery, APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode insertObjectGroups(List<JsonNode> insertQueries)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        ParametersChecker.checkParameter("Insert Request is a mandatory parameter", insertQueries);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, "/objectgroups/bulk", null, insertQueries, APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new MetaDataAlreadyExistException(ErrorMessage.DATA_ALREADY_EXISTS.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public void updateObjectGroupById(JsonNode queryUpdate, String objectGroupId)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException {
        try {
            ParametersChecker
                .checkParameter(ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage(), queryUpdate, objectGroupId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        if (Strings.isNullOrEmpty(objectGroupId)) {
            throw new InvalidParseOperationException("objectGroupId may not be empty");
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/objectgroups/" + objectGroupId, null, queryUpdate,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.EXPECTATION_FAILED.getStatusCode()) {
                throw new MetaDataExecutionException(Status.EXPECTATION_FAILED.getReasonPhrase());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() != Status.CREATED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public List<UnitPerOriginatingAgency> selectAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataClientServerException {
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.GET, "/accession-registers/units/" + operationId, null, null,
                    APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);

            RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response);
            if (requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;

                List<UnitPerOriginatingAgency> unitPerOriginatingAgencies = new ArrayList<>();
                for (JsonNode jsonNode : requestResponseOK.getResults()) {
                    unitPerOriginatingAgencies
                        .add(JsonHandler.getFromJsonNode(jsonNode, UnitPerOriginatingAgency.class));
                }

                return unitPerOriginatingAgencies;
            } else {
                VitamError vitamError = (VitamError) requestResponse;
                LOGGER
                    .error("find accession register for unit failed, http code is {}, error is {}",
                        vitamError.getCode(),
                        vitamError.getErrors());
                throw new MetaDataClientServerException(vitamError.getDescription());
            }

        } catch (VitamClientInternalException | InvalidParseOperationException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<ObjectGroupPerOriginatingAgency> selectAccessionRegisterOnObjectByOperationId(String operationId)
        throws MetaDataClientServerException {
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.GET, "/accession-registers/objects/" + operationId, null, null,
                    APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);

            RequestResponse<ObjectGroupPerOriginatingAgency> requestResponse =
                RequestResponse.parseFromResponse(response, ObjectGroupPerOriginatingAgency.class);
            if (requestResponse.isOk()) {
                RequestResponseOK<ObjectGroupPerOriginatingAgency> requestResponseOK =
                    (RequestResponseOK<ObjectGroupPerOriginatingAgency>) requestResponse;

                return requestResponseOK.getResults();
            } else {
                VitamError vitamError = (VitamError) requestResponse;
                LOGGER
                    .error("find accession register for object group failed, http code is {}, error is {}",
                        vitamError.getCode(),
                        vitamError.getErrors());
                throw new MetaDataClientServerException(vitamError.getDescription());
            }

        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean refreshUnits() throws MetaDataClientServerException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/units", null, APPLICATION_JSON_TYPE);
            return response.getStatus() == OK.getStatusCode();
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean refreshObjectGroups() throws MetaDataClientServerException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "/objectgroups", null, APPLICATION_JSON_TYPE);
            return response.getStatus() == OK.getStatusCode();
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        ParametersChecker.checkParameter("The options are mandatory", indexParam);
        Response response = null;
        try {
            response = performRequest(POST, REINDEX_URI, null, indexParam,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException {
        ParametersChecker.checkParameter("The options are mandatory", switchIndexParam);
        Response response = null;
        try {
            response = performRequest(POST, ALIASES_URI, null, switchIndexParam,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            throw new MetaDataClientServerException("Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getUnitByIdRaw(String unitId) throws VitamClientException {
        ParametersChecker.checkParameter("The unit id is mandatory", unitId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/units/" + unitId, null, null,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getUnitsByIdsRaw(Collection<String> unitIds) throws VitamClientException {
        ParametersChecker.checkParameter("The unit ids are mandatory", unitIds);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/units", null, unitIds,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupByIdRaw(String objectGroupId) throws VitamClientException {
        ParametersChecker.checkParameter("The unit id is mandatory", objectGroupId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/objectgroups/" + objectGroupId, null, null,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getObjectGroupsByIdsRaw(Collection<String> objectGroupId) throws VitamClientException {
        ParametersChecker.checkParameter("The object group ids are mandatory", objectGroupId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/raw/objectgroups", null, objectGroupId,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public GraphComputeResponse computeGraph(JsonNode queryDsl) throws VitamClientException {
        ParametersChecker.checkParameter("The queryDsl is mandatory", queryDsl);
        Response response = null;
        try {
            response = performRequest(POST, COMPUTE_GRAPH_URI, null, queryDsl,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return response.readEntity(GraphComputeResponse.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public GraphComputeResponse computeGraph(GraphComputeResponse.GraphComputeAction action, Set<String> ids)
        throws VitamClientException {
        ParametersChecker.checkParameter("All params are mandatory", action, ids);
        Response response = null;
        try {
            response = performRequest(POST, COMPUTE_GRAPH_URI + "/" + action.name(), null, ids,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            return response.readEntity(GraphComputeResponse.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void exportReclassificationChildNodes(Set<String> ids, String unitsToUpdateChainedFileName,
                                                 String objectGroupsToUpdateChainedFileName)
        throws VitamClientException, MetaDataExecutionException {
        ParametersChecker.checkParameter("All params are mandatory", ids);
        Response response = null;
        try {

            ReclassificationChildNodeExportRequest reclassificationChildNodeExportRequest =
                new ReclassificationChildNodeExportRequest(ids, unitsToUpdateChainedFileName,
                    objectGroupsToUpdateChainedFileName);

            response = performRequest(POST, "exportReclassificationChildNodes", null,
                reclassificationChildNodeExportRequest,
                APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);

            if (response.getStatus() == OK.getStatusCode()) {
                // Every thing is OK
                return;
            }
            throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR + ". Status= " + response.getStatus());

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new VitamClientException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void deleteUnitsBulk(Collection<String> listIds)
        throws MetaDataExecutionException, MetaDataClientServerException {

        ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), listIds);
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.DELETE, "/units/bulkDelete", null, listIds, APPLICATION_JSON_TYPE,
                    APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                // Every thing is OK
                return;
            }
            throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR + ". Status= " + response.getStatus());

        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void deleteObjectGroupBulk(Collection<String> listIds)
        throws MetaDataExecutionException, MetaDataClientServerException {

        ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), listIds);
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.DELETE, "/objectGroups/bulkDelete", null, listIds, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                // Every thing is OK
                return;
            }
            throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR + ". Status= " + response.getStatus());

        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Update units Bulk.
     *
     * @param updateQuery
     *
     * @return
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     */
    public RequestResponse<JsonNode> updateUnitBulk(JsonNode updateQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), updateQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(POST, "/units/updatebulk", null, updateQuery, APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_METADATA_VALUE.getMessage());
            }
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateUnitsRulesBulk(List<String> unitsIds, RuleActions actions, Map<String, DurationData> rulesToDurationData)
            throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
            MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), unitsIds);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            BatchRulesUpdateInfo requestContext = new BatchRulesUpdateInfo(unitsIds, actions, rulesToDurationData);
            response = performRequest(HttpMethod.POST, "/units/updaterulesbulk", null,
                JsonHandler.toJsonNode(requestContext), APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new MetaDataNotFoundException(ErrorMessage.NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_METADATA_VALUE.getMessage());
            }
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public JsonNode selectUnitsWithInheritedRules(JsonNode selectQuery)
        throws MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException, MetaDataExecutionException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_UNITS_QUERY_NULL.getMessage(), selectQuery);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "/unitsWithInheritedRules", null, selectQuery,
                APPLICATION_JSON_TYPE,
                APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode createAccessionRegisterSymbolic()
        throws MetaDataClientServerException, MetaDataExecutionException {
        Response response = null;
        try {
            response = performRequest(POST, "accession-registers/symbolic", null, APPLICATION_JSON_TYPE);

            if (response.getStatus() != OK.getStatusCode()) {
                throw new MetaDataExecutionException(String.format("Error status code %d on request POST 'accession-registers/symbolic'", response.getStatus()));
            }

            return response.readEntity(JsonNode.class);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }
}
