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
 */

package fr.gouv.vitam.access.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;

/**
 * Access client
 */
public class AccessClientRest implements AccessClient {
    private static final String RESOURCE_PATH = "/access/v1";
    private static final String BLANK_DSL = "select DSL is blank";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";

    private final String serviceUrl;
    private final Client client;

    /**
     * @param server - localhost
     * @param port - define 8082
     */
    public AccessClientRest(String server, int port) {
        serviceUrl = "http://" + server + ":" + port + RESOURCE_PATH;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
    }

    /**
     * @return : status of access server 200 : server is alive
     */
    public Response status() {
        return client.target(serviceUrl).path("status").request().get();
    }

    /**
     *
     * AccessClient to send a “GET” request and the returned json data.
     *
     * @param selectQuery
     * @return Object JsonNode
     * @throws InvalidParseOperationException
     * @throws AccessClientServerException
     * @throws AccessClientNotFoundException
     */
    public JsonNode selectUnits(String selectQuery)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        if (StringUtils.isBlank(selectQuery)) {
            throw new IllegalArgumentException("select DSL is blank");
        }

        final GUID guid = GUIDFactory.newGUID();

        final Response response = client.target(serviceUrl).path("units").request(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
            .header(GlobalDataRest.X_REQUEST_ID, guid.toString()).accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(selectQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new AccessClientServerException("Internal Server Error"); // access-common
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
            throw new AccessClientNotFoundException("Not Found Exception");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");// common
        }

        return response.readEntity(JsonNode.class);
    }

    /**
     *
     * AccessClient to send a “GET” request based on select by Id and the returned json data.
     *
     * @param selectQuery,unit_id
     * @return Object JsonNode
     * @throws InvalidParseOperationException
     * @throws AccessClientServerException
     * @throws AccessClientNotFoundException
     */
    public JsonNode selectUnitbyId(String selectQuery, String id_unit)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException {
        if (StringUtils.isBlank(selectQuery)) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        if (StringUtils.isEmpty(id_unit)) {
            throw new IllegalArgumentException(BLANK_UNIT_ID);
        }

        final GUID guid = GUIDFactory.newGUID();

        final Response response =
            client.target(serviceUrl).path("units/" + id_unit).request(MediaType.APPLICATION_JSON)
                .header(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "GET")
                .header("X-REQUEST-ID", guid.toString())
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(selectQuery, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new AccessClientServerException("Internal Server Error"); // access-common
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
            throw new AccessClientNotFoundException("Not Found Exception");
        } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");// common
        }

        return response.readEntity(JsonNode.class);
    }

}
