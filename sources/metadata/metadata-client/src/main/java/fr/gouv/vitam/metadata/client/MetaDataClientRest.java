/**
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
 **/

package fr.gouv.vitam.metadata.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;

/**
 * Rest client for metadata
 */
public class MetaDataClientRest extends DefaultClient implements MetaDataClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientRest.class);

    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public MetaDataClientRest(VitamClientFactoryInterface factory) {
        super(factory);
    }

    @Override
    public String insertUnit(String insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.INSERT_UNITS_QUERY_NULL.getMessage(), insertQuery);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/units", null, insertQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
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
            }
            return response.readEntity(String.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        }finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnits(String selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_UNITS_QUERY_NULL.getMessage(), selectQuery);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, "/units", headers, selectQuery, MediaType
                .APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override public JsonNode selectUnitbyId(String selectQuery, String unitId) throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter("One parameter is empty", selectQuery, unitId);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, "/units/" + unitId, headers, selectQuery, MediaType
                .APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGrouptbyId(String selectQuery, String objectGroupId) throws
        MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetadataInvalidSelectException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.SELECT_OBJECT_GROUP_QUERY_NULL.getMessage(), selectQuery);
            ParametersChecker.checkParameter(ErrorMessage.BLANK_PARAM.getMessage(), objectGroupId);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, "/objectgroups/" + objectGroupId, headers, selectQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new MetadataInvalidSelectException(ErrorMessage.MISSING_SELECT_QUERY.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode updateUnitbyId(String updateQuery, String unitId) throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException {
        try {
            ParametersChecker.checkParameter(ErrorMessage.UPDATE_UNITS_QUERY_NULL.getMessage(), updateQuery, unitId);
        } catch (IllegalArgumentException e) {
            throw new InvalidParseOperationException(e);
        }
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, "/units/" + unitId, headers, updateQuery, MediaType
                .APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new MetaDataExecutionException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                throw new MetaDataDocumentSizeException(ErrorMessage.SIZE_TOO_LARGE.getMessage());
            } else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(ErrorMessage.INVALID_PARSE_OPERATION.getMessage());
            }
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public String insertObjectGroup(String insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        ParametersChecker.checkParameter("Insert Request is a mandatory parameter", insertQuery);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "/objectgroups", null, insertQuery, MediaType
                .APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
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
            }
            return response.readEntity(String.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new MetaDataClientServerException(INTERNAL_SERVER_ERROR, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
