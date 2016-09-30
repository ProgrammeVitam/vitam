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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.access.client.AccessClient;
import fr.gouv.vitam.access.client.AccessClientFactory;
import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Manage all the transactions received form the User Interface : a gateway to VITAM intern
 *
 */
public class UserInterfaceTransactionManager {

    private static final AccessClient ACCESS_CLIENT = AccessClientFactory.getInstance().getAccessOperationClient();

    /**
     * Gets search units result
     * 
     * @param parameters search criteria as DSL query
     * @return
     * @throws AccessClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static JsonNode searchUnits(String parameters)
        throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        return ACCESS_CLIENT.selectUnits(parameters);
    }

    /**
     * 
     * Gets archive unit details
     * 
     * @param preparedDslQuery search criteria as DSL query
     * @param unitId archive unit id to find
     * @return
     * @throws AccessClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static JsonNode getArchiveUnitDetails(String preparedDslQuery, String unitId)
        throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        return ACCESS_CLIENT.selectUnitbyId(preparedDslQuery, unitId);
    }

    /**
     * Update units result
     * 
     * @param parameters search criteria as DSL query
     * @param unitId unitIdentifier
     * @return
     * @throws AccessClientServerException thrown when an errors occurs during the connection with the server
     * @throws AccessClientNotFoundException thrown when access client is not found
     * @throws InvalidParseOperationException thrown when the Json node format is not correct
     */
    public static JsonNode updateUnits(String parameters, String unitId)
        throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        return ACCESS_CLIENT.updateUnitbyId(parameters, unitId);
    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     * 
     * @param preparedDslQuery the query to be executed
     * @param objectId the Id of the ObjectGroup
     * @return JsonNode object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested object does not exist
     */
    public static JsonNode selectObjectbyId(String preparedDslQuery, String objectId)
        throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        return ACCESS_CLIENT.selectObjectbyId(preparedDslQuery, objectId);        
    }
    
    /**
     * Retrieve an Object data as an input stream
     * 
     * @param selectObjectQuery the query to be executed
     * @param objectGroupId the Id of the ObjectGroup
     * @param usage the requested usage
     * @param version the requested version of the usage
     * @return InputStream the object data
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested object does not exist
     */
    public static InputStream getObjectAsInputStream(String selectObjectQuery, String objectGroupId, String usage, int version)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException{
        return ACCESS_CLIENT.getObjectAsInputStream(selectObjectQuery, objectGroupId, usage, version);
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
        JsonNode allParentsRef = JsonTransformer.buildAllParentsRef(unitId, allParents);
        
        // All paths
        ArrayNode allPaths = JsonHandler.createArrayNode();

        // Start by the immediate parents
        ArrayNode immediateParents =
            (ArrayNode) allParentsRef.get(unitId).get(UiConstants.UNITUPS.getResultConstantValue());

        // Build all paths
        for (JsonNode currentParentNode : immediateParents) {
            String currentParentId = currentParentNode.asText();
            JsonNode currentParentDetails = allParentsRef.get(currentParentId);
           
            // Create path node
            ArrayNode currentPath = JsonHandler.createArrayNode();
            currentPath.add(currentParentDetails);
            
            buildOnePathForOneParent(currentPath, currentParentDetails, allPaths, allParentsRef);
        }

        return allPaths;
    }

    private static void buildOnePathForOneParent(ArrayNode path, JsonNode parent, ArrayNode allPaths,
        JsonNode allParentsRef) {
        ArrayNode immediateParents = (ArrayNode) parent.get(UiConstants.UNITUPS.getResultConstantValue());

        if (immediateParents.size() == 0) {
            // it is a root
            // update allPaths
            allPaths.add(path);
        } else if (immediateParents.size() == 1) {
            // One immediate parent
            JsonNode oneImmediateParent = allParentsRef.get(immediateParents.get(0).asText());
            path.add(oneImmediateParent);
            buildOnePathForOneParent(path, oneImmediateParent, allPaths, allParentsRef);
        } else {
            // More than one immediate parent
            // Duplicate path so many times as parents
            for (JsonNode currentParentNode : immediateParents) {
                String currentParentId = currentParentNode.asText();
                JsonNode currentParentDetails = allParentsRef.get(currentParentId);

                ArrayNode pathDuplicate = path.deepCopy();
                pathDuplicate.add(currentParentDetails);
                buildOnePathForOneParent(pathDuplicate, currentParentDetails, allPaths, allParentsRef);
            }
        }
    }
}
