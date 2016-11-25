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
package fr.gouv.vitam.ihmdemo.core;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.server2.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Manage all the transactions received form the User Interface : a gateway to VITAM intern
 *
 */
public class UserInterfaceTransactionManager {

    /**
     * Gets search units result
     *
     * @param parameters search criteria as DSL query
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static RequestResponse searchUnits(JsonNode parameters)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnits(parameters);
        }
    }

    /**
     *
     * Gets archive unit details
     *
     * @param preparedDslQuery search criteria as DSL query
     * @param unitId archive unit id to find
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static RequestResponse getArchiveUnitDetails(JsonNode preparedDslQuery, String unitId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnitbyId(preparedDslQuery, unitId);
        }
    }

    /**
     * Update units result
     *
     * @param parameters search criteria as DSL query
     * @param unitId unitIdentifier
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static RequestResponse updateUnits(JsonNode parameters, String unitId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.updateUnitbyId(parameters, unitId);
        }
    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param preparedDslQuery the query to be executed
     * @param objectId the Id of the ObjectGroup
     * @return JsonNode object including DSL queries, context and results
     * @throws AccessExternalClientServerException if the server encountered an exception
     * @throws AccessExternalClientNotFoundException if the requested object does not exist
     * @throws InvalidParseOperationException if the query is not well formatted
     */
    public static RequestResponse selectObjectbyId(JsonNode preparedDslQuery, String objectId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectObjectById(preparedDslQuery, objectId);
        }
    }

    /**
     * Retrieve an Object data as an input stream
     *
     * @param selectObjectQuery the query to be executed
     * @param objectGroupId the Id of the ObjectGroup
     * @param usage the requested usage
     * @param version the requested version of the usage
     * @return boolean for test purpose (solve mock issue)
     * @throws InvalidParseOperationException if the query is not well formatted <<<<<<<
     *         77cb9f1d4aa9f9546a4df939c2baaace172bc65a
     * @throws AccessExternalClientServerException if the server encountered an exception
     * @throws AccessExternalClientNotFoundException if the requested object does not exist
     */
    // TODO: review this return (should theoretically be a void) because we got mock issue with this class on
    // web application resource
    public static boolean getObjectAsInputStream(AsyncResponse asyncResponse, JsonNode selectObjectQuery,
        String objectGroupId, String usage, int version, String filename)
        throws AccessExternalClientNotFoundException, AccessExternalClientServerException,
        InvalidParseOperationException, UnsupportedEncodingException {
        Response response = null;
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            response = client.getObject(selectObjectQuery, objectGroupId, usage, version);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            final Response.ResponseBuilder responseBuilder = Response.status(Response.Status.OK)
                .header(GlobalDataRest.X_QUALIFIER, response.getHeaderString(GlobalDataRest.X_QUALIFIER))
                .header(GlobalDataRest.X_VERSION, response.getHeaderString(GlobalDataRest.X_VERSION))
                .header("Content-Disposition", "filename=\"" + URLDecoder.decode(filename, "UTF-8") + "\"")
                .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } finally {
            // close response on error case
            if (response != null && response.getStatus() != Response.Status.OK.getStatusCode()) {
                try {
                    if (response.hasEntity()) {
                        final Object object = response.getEntity();
                        if (object instanceof InputStream) {
                            StreamUtils.closeSilently((InputStream) object);
                        }
                    }
                } catch (final IllegalStateException | ProcessingException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                } finally {
                    response.close();
                }
            }
        }
        return true;
    }

    /**
     * Build all paths relative to a unit based on its all parents list (_us)
     *
     * @param unitId the unit Id for which all paths will be constructed
     * @param allParents unit's all parents (_us field value + the unit id)
     * @return all paths relative to the specified unit
     * @throws VitamException
     */
    public static JsonNode buildUnitTree(String unitId, JsonNode allParents) throws VitamException {
        // Construct all parents referential
        final JsonNode allParentsRef = JsonTransformer.buildAllParentsRef(unitId, allParents);
        // All paths
        final ArrayNode allPaths = JsonHandler.createArrayNode();

        // Start by the immediate parents
        final ArrayNode immediateParents =
            (ArrayNode) allParentsRef.get(unitId).get(UiConstants.UNITUPS.getResultCriteria());

        // Build all paths
        for (final JsonNode currentParentNode : immediateParents) {
            final String currentParentId = currentParentNode.asText();
            final JsonNode currentParentDetails = allParentsRef.get(currentParentId);

            // Create path node
            final ArrayNode currentPath = JsonHandler.createArrayNode();
            currentPath.add(currentParentDetails);

            buildOnePathForOneParent(currentPath, currentParentDetails, allPaths, allParentsRef);
        }

        return allPaths;
    }

    private static void buildOnePathForOneParent(ArrayNode path, JsonNode parent, ArrayNode allPaths,
        JsonNode allParentsRef) {
        final ArrayNode immediateParents = (ArrayNode) parent.get(UiConstants.UNITUPS.getResultCriteria());

        if (immediateParents.size() == 0) {
            // it is a root
            // update allPaths
            allPaths.add(path);
        } else if (immediateParents.size() == 1) {
            // One immediate parent
            final JsonNode oneImmediateParent = allParentsRef.get(immediateParents.get(0).asText());
            path.add(oneImmediateParent);
            buildOnePathForOneParent(path, oneImmediateParent, allPaths, allParentsRef);
        } else {
            // More than one immediate parent
            // Duplicate path so many times as parents
            for (final JsonNode currentParentNode : immediateParents) {
                final String currentParentId = currentParentNode.asText();
                final JsonNode currentParentDetails = allParentsRef.get(currentParentId);

                final ArrayNode pathDuplicate = path.deepCopy();
                pathDuplicate.add(currentParentDetails);
                buildOnePathForOneParent(pathDuplicate, currentParentDetails, allPaths, allParentsRef);
            }
        }
    }

    /**
     * @param unitLifeCycleId
     * @return JsonNode result
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */

    public static RequestResponse selectUnitLifeCycleById(String unitLifeCycleId)
        throws LogbookClientException, InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnitLifeCycleById(unitLifeCycleId);

        }
    }

    /**
     * @param query
     * @return JsonNode result
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */
    public static RequestResponse selectOperation(JsonNode query)
        throws LogbookClientException, InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectOperation(query);
        }
    }

    /**
     * @param operationId
     * @return JsonNode result
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */

    public static RequestResponse selectOperationbyId(String operationId)
        throws LogbookClientException, InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectOperationbyId(operationId);
        }
    }

    /**
     * @param objectGroupLifeCycleId
     * @return JsonNode result
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */

    public static RequestResponse selectObjectGroupLifeCycleById(String objectGroupLifeCycleId)
        throws LogbookClientException, InvalidParseOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectObjectGroupLifeCycleById(objectGroupLifeCycleId);
        }
    }

    /**
     * @param options
     * @return JsonNode result
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws InvalidCreateOperationException
     */

    public static RequestResponse findAccessionRegisterSummary(String options)
        throws LogbookClientException, InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidCreateOperationException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            return client.getAccessionRegisterSummary(query);
        }
    }

    /**
     * @param id
     * @param options
     * @return JsonNode result
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws InvalidCreateOperationException
     */

    public static RequestResponse findAccessionRegisterDetail(String id, String options)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidCreateOperationException {

        try (AccessExternalClient accessClient = AccessExternalClientFactory.getInstance().getClient()) {
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            return accessClient.getAccessionRegisterDetail(id, query);
        }
    }

}
