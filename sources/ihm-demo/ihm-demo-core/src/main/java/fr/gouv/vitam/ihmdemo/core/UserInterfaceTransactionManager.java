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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Manage all the transactions received form the User Interface : a gateway to VITAM intern
 *
 */
public class UserInterfaceTransactionManager {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UserInterfaceTransactionManager.class);

    /**
     * Gets search units result
     *
     * @param parameters search criteria as DSL query
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     * @throws AccessUnauthorizedException
     */
    public static RequestResponse<JsonNode> searchUnits(JsonNode parameters, Integer tenantId, String contractId,
        String appSessionId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnits(new VitamContext(tenantId).setAccessContract(contractId)
                .setApplicationSessionId(appSessionId), parameters);
        }
    }

    /**
     *
     * Gets archive unit details
     * 
     * @param preparedDslQuery search criteria as DSL query
     * @param unitId archive unit id to find
     * @param tenantId the working tenant
     * @param contractId the contract Id
     * @param appSessionId the application session id
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     * @throws AccessUnauthorizedException
     */
    public static RequestResponse<JsonNode> getArchiveUnitDetails(JsonNode preparedDslQuery, String unitId,
        Integer tenantId, String contractId, String appSessionId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnitbyId(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), preparedDslQuery,
                unitId);
        }
    }

    /**
     * Update units result
     *
     * @param parameters search criteria as DSL query
     * @param unitId unitIdentifier
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return result
     * @throws AccessExternalClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessExternalClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     * @throws NoWritingPermissionException
     * @throws AccessUnauthorizedException
     */
    public static RequestResponse<JsonNode> updateUnits(JsonNode parameters, String unitId, Integer tenantId,
        String contractId, String appSessionId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException, NoWritingPermissionException, AccessUnauthorizedException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.updateUnitbyId(new VitamContext(tenantId).setAccessContract(contractId)
                .setApplicationSessionId(appSessionId), parameters, unitId);
        }
    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param preparedDslQuery the query to be executed
     * @param objectId the Id of the ObjectGroup
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return JsonNode object including DSL queries, context and results
     * @throws AccessExternalClientServerException if the server encountered an exception
     * @throws AccessExternalClientNotFoundException if the requested object does not exist
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessUnauthorizedException
     */
    public static RequestResponse<JsonNode> selectObjectbyId(JsonNode preparedDslQuery, String objectId,
        Integer tenantId, String contractId, String appSessionId)
        throws AccessExternalClientServerException, AccessExternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectObjectById(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), preparedDslQuery,
                objectId);
        }
    }

    /**
     * Retrieve an Object data as an input stream
     *
     * @param asyncResponse the asynchronous response to be used
     * @param selectObjectQuery the query to be executed
     * @param unitId the Id of the ObjectGroup
     * @param usage the requested usage
     * @param version the requested version of the usage
     * @param filename the name od the file
     * @param tenantId the working tenant
     * @param appSessionId the application session id
     * @return boolean for test purpose (solve mock issue)
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessExternalClientServerException if the server encountered an exception
     * @throws AccessExternalClientNotFoundException if the requested object does not exist
     * @throws UnsupportedEncodingException if unsupported encoding error for input file content
     * @throws AccessUnauthorizedException
     */
    // TODO: review this return (should theoretically be a void) because we got mock issue with this class on
    // web application resource
    public static boolean getObjectAsInputStream(AsyncResponse asyncResponse, JsonNode selectObjectQuery,
        String unitId, String usage, int version, String filename, Integer tenantId, String contractId,
        String appSessionId)
        throws AccessExternalClientNotFoundException, AccessExternalClientServerException,
        InvalidParseOperationException, UnsupportedEncodingException, AccessUnauthorizedException {
        Response response = null;
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            response = client.getUnitObject(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), selectObjectQuery,
                unitId, usage, version);
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
                    LOGGER.debug(e);
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
     * @throws VitamException if error when build the tree
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

            if (currentParentDetails != null) {
                currentPath.add(currentParentDetails);
                buildOnePathForOneParent(currentPath, currentParentDetails, allPaths, allParentsRef);
            }
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
     * @param unitLifeCycleId the unit lifecycle id to select
     * @param tenantId the working tenant
     * @param contractId the access contract id
     * @param appSessionId the application session id
     * @return JsonNode result
     * @throws InvalidParseOperationException if json data not well-formed
     * @throws LogbookClientException if the request with illegal parameter
     * @throws AccessUnauthorizedException
     */

    public static RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(String unitLifeCycleId, Integer tenantId,
        String contractId, String appSessionId)
        throws VitamClientException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectUnitLifeCycleById(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId),
                unitLifeCycleId, new Select().getFinalSelect());

        }
    }

    /**
     * @param query the select query
     * @param tenantId the working tenant
     * @param appSessionId the application session id
     * @return logbook operation result
     * @throws VitamClientException access client exception
     */
    public static RequestResponse<LogbookOperation> selectOperation(JsonNode query, Integer tenantId, String contractId,
        String appSessionId)
        throws VitamClientException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectOperation(new VitamContext(tenantId).setAccessContract(contractId)
                .setApplicationSessionId(appSessionId), query);
        }
    }

    /**
     * @param operationId the operation id
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return logbook operation result
     * @throws VitamClientException
     */
    public static RequestResponse<LogbookOperation> selectOperationbyId(String operationId, Integer tenantId,
        String contractId, String appSessionId)
        throws VitamClientException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectOperationbyId(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), operationId,
                new Select().getFinalSelect());
        }
    }

    /**
     * @param objectGroupLifeCycleId the object lifecycle id to select
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return logbook lifecycle result
     * @throws VitamClientException if the request with illegal parameter
     */

    public static RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(String objectGroupLifeCycleId,
        Integer tenantId, String contractId, String appSessionId)
        throws VitamClientException {
        try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            return client.selectObjectGroupLifeCycleById(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId),
                objectGroupLifeCycleId, new Select().getFinalSelect());
        }
    }

    /**
     * @param options for creating query
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return AccessionRegisterSummaryModel result
     * @throws VitamClientException if the request with illegal parameter
     * @throws InvalidParseOperationException if json data not well-formed
     * @throws InvalidCreateOperationException if error when create query
     */
    public static RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterSummary(String options,
        Integer tenantId,
        String contractId, String appSessionId)
        throws VitamClientException, InvalidParseOperationException, InvalidCreateOperationException {
        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            return adminExternalClient.findAccessionRegister(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId),
                query);
        }
    }

    /**
     * @param id the id of accession register
     * @param options for creating query
     * @param tenantId the working tenant
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return JsonNode result
     * @throws InvalidParseOperationException if json data not well-formed
     * @throws AccessExternalClientServerException if access internal server error
     * @throws AccessExternalClientNotFoundException if access external resource not found
     * @throws InvalidCreateOperationException if error when create query
     * @throws AccessUnauthorizedException
     */
    public static RequestResponse<JsonNode> findAccessionRegisterDetail(String id, String options, Integer tenantId,
        String contractId, String appSessionId)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, InvalidCreateOperationException, AccessUnauthorizedException {

        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(options);
            final JsonNode query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            return adminExternalClient
                .getAccessionRegisterDetail(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), id, query);
        }
    }


    /**
     * Starts a Verification process based on a given DSLQuery
     * 
     * @param query DSLQuery to execute
     * @param tenantId Tenant Id
     * @param contractId the access contract Id
     * @param appSessionId the application session id
     * @return A RequestResponse contains the created logbookOperation for verification process
     * @throws AccessExternalClientServerException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    @SuppressWarnings("unchecked")
    public static RequestResponse<JsonNode> checkTraceabilityOperation(JsonNode query, Integer tenantId,
        String contractId, String appSessionId)
        throws AccessExternalClientServerException, InvalidParseOperationException, AccessUnauthorizedException {
        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            return adminExternalClient
                .checkTraceabilityOperation(new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(appSessionId), query);
        }
    }


    /**
     * Extract information from timestamp
     * 
     * @param timestamp the timestamp to be used for extraction
     * @return json node containing genTime and issuer certificate information
     * @throws BadRequestException if the timestamp cant be extracted
     */
    public static JsonNode extractInformationFromTimestamp(String timestamp) throws BadRequestException {
        final ObjectNode result = JsonHandler.createObjectNode();
        try {
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(
                org.bouncycastle.util.encoders.Base64.decode(timestamp.getBytes())));
            ASN1Primitive obj = bIn.readObject();
            TimeStampResponse tsResp = new TimeStampResponse(obj.toASN1Primitive().getEncoded());
            SignerId signerId = tsResp.getTimeStampToken().getSID();
            X500Name signerCertIssuer = signerId.getIssuer();
            result.put("genTime", LocalDateUtil.getString(
                LocalDateUtil.fromDate(tsResp.getTimeStampToken().getTimeStampInfo().getGenTime())));
            result.put("signerCertIssuer", signerCertIssuer.toString());
        } catch (TSPException | IOException e) {
            LOGGER.error("Error while transforming timestamp", e);
            throw new BadRequestException("Error while transforming timestamp", e);
        }

        return result;
    }



}
