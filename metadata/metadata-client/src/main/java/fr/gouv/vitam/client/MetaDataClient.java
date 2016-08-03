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
package fr.gouv.vitam.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.api.exception.*;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * MetaData client,contains same methods for select, insert,update and delete the units or/and objects group
 */
public class MetaDataClient {

    private final Client client;
    private final String url;
    private static final String RESOURCE_PATH = "/metadata/v1";

    private static final String SELECT_UNITS_QUERY_NULL = "Select units query is null";
    private static final String UPDATE_UNITS_QUERY_NULL = "Update units query is null";
    private static final String INSERT_UNITS_QUERY_NULL = "Insert units query is null";
    private static final String BLANK_PARAM = "Unit id parameter is blank";
    private static final String X_HTTP_METHOD = "X-Http-Method-Override";

    private static final String ELAPSED_TIME_MESSAGE =
        "MetaDataClient / Total elapsed time in execution of method";
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetaDataClient.class);


    /**
     * @param url of metadata server
     */
    public MetaDataClient(String url) {
        super();
        client = ClientBuilder.newClient();
        this.url = url + RESOURCE_PATH;
    }

    /**
     * @param insertQuery as String <br>
     *        null is not allowed
     * @return : response as String
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     */
    public String insertUnit(String insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException {
        if (StringUtils.isEmpty(insertQuery)) {
            throw new IllegalArgumentException(INSERT_UNITS_QUERY_NULL);
        }

        final Response response = client.target(url).path("units").request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(insertQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            throw new MetaDataNotFoundException("Not Found Exception");
        } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
            throw new MetaDataAlreadyExistException("Data Already Exists");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }

        return response.readEntity(String.class);
    }

    /**
     * @return : status of metadata server 200 : server is alive
     */
    // TODO REVIEW See Logbook REST
    public Response status() {
        return client.target(url).path("status").request().get();
    }

    /**
     * Search units by select query (DSL)
     * 
     * @param selectQuery : select query {@link Select} as String <br>
     *        Null is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     */
    public JsonNode selectUnits(String selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException {

        if (StringUtils.isEmpty(selectQuery)) {
            throw new InvalidParseOperationException(SELECT_UNITS_QUERY_NULL);
        }
        long time = System.currentTimeMillis();
        final Response response =
            client.target(url).path("units").request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).header(X_HTTP_METHOD, "GET")
                .post(Entity.entity(selectQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }
        LOGGER.info(ELAPSED_TIME_MESSAGE + "selectUnits :" + ((System.currentTimeMillis() - time) / 1000) + "s");
        return response.readEntity(JsonNode.class);
    }


    /**
     * Search units by query (DSL) and path unit id
     * 
     * @param selectQuery : select query {@link Select} as String <br>
     *        Null is not allowed
     * @param unitId : unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws IllegalArgumentException thrown when unit id is null or blank
     */
    public JsonNode selectUnitbyId(String selectQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        IllegalArgumentException {
        long time = System.currentTimeMillis();
        // check parameters before call web service
        // check select query
        if (StringUtils.isBlank(selectQuery)) {
            throw new InvalidParseOperationException(SELECT_UNITS_QUERY_NULL);
        }
        // check unit id
        if (StringUtils.isBlank(unitId)) {
            throw new IllegalArgumentException(BLANK_PARAM);
        }

        final Response response =
            client.target(url).path("units/" + unitId).request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).header(X_HTTP_METHOD, "GET")
                .post(Entity.entity(selectQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }

        LOGGER.info(ELAPSED_TIME_MESSAGE + "selectUnits :" + ((System.currentTimeMillis() - time) / 1000) + "s");
        return response.readEntity(JsonNode.class);
    }

    /**
     * Update units by query (DSL) and path unit id
     * 
     * @param query : update query {@link Select} as String <br>
     *        Null is not allowed
     * @param unitId : unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws IllegalArgumentException thrown when unit id is null or blank
     */
    public JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        IllegalArgumentException {
        long time = System.currentTimeMillis();
        // check parameters before call web service
        // check update query
        if (StringUtils.isBlank(updateQuery)) {
            throw new InvalidParseOperationException(UPDATE_UNITS_QUERY_NULL);
        }

        final Response response =
            client.target(url).path("units/" + unitId).request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).header(X_HTTP_METHOD, "GET")
                .put(Entity.entity(updateQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }

        LOGGER.info(ELAPSED_TIME_MESSAGE + "update Units by Id :" + ((System.currentTimeMillis() - time) / 1000) + "s");
        return response.readEntity(JsonNode.class);
    }

    /**
     * @param insertQuery as String
     * @return response as String contains the request result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     */
    public String insertObjectGroup(String insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException {
        ParametersChecker.checkParameter("Insert Request is a mandatory parameter", insertQuery);

        final Response response = client.target(url).path("objectgroups").request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(insertQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            throw new MetaDataNotFoundException("Not Found Exception");
        } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
            throw new MetaDataAlreadyExistException("Data Already Exists");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }

        return response.readEntity(String.class);
    }
}
