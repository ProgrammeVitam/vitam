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

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.builder.request.construct.Select;
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
    private static final String INSERT_UNITS_QUERY_NULL = "Insert units query is null";

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
     * @param query as String <br>
     *        null is not allowed
     * @return : response as String
     * @throws InvalidParseOperationException
     */
    public String insertUnit(String insertQuery) throws InvalidParseOperationException {
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
     * @param query : select query {@link Select} as String <br>
     *        Null is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetadataInvalidSelectException thrown when invalid select query (reference {@link Select})
     */
    public JsonNode selectUnits(String selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetadataInvalidSelectException {

        if (StringUtils.isEmpty(selectQuery)) {
            throw new MetadataInvalidSelectException(SELECT_UNITS_QUERY_NULL);
        }
        long time = System.currentTimeMillis();
        final Response response =
            client.target(url).path("units").request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).header("X-HTTP-Method-Override", "GET")
                .post(Entity.entity(selectQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new MetaDataExecutionException("Internal Server Error");
        } else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
            throw new MetaDataDocumentSizeException("Document Size is Too Large");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        } else if (response.getStatus() == Status.NOT_ACCEPTABLE.getStatusCode()) {
            throw new MetadataInvalidSelectException("Invalid select query");
        }
        LOGGER.info(ELAPSED_TIME_MESSAGE + "selectUnits :" + ((System.currentTimeMillis() - time) / 1000) + "s");
        return response.readEntity(JsonNode.class);
    }
}
