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
package fr.gouv.vitam.access.internal.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.UpdatePermissionException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

import javax.ws.rs.core.Response;
import java.text.ParseException;

/**
 * AccessModule interface for database operations in select
 */
public interface AccessInternalModule {

    /**
     * select Unit
     *
     * @param queryJson as String { $query : query}
     * @return the result of the select on Unit
     * @throws IllegalArgumentException if json query is null
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */
    JsonNode selectUnit(JsonNode queryJson)
        throws InvalidParseOperationException, AccessInternalExecutionException, VitamDBException;

    /**
     * select Unit by id
     *
     * @param queryJson as String { $query : query}
     * @param idUnit as String
     * @return the corresponding JsonNode
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException Throw if error occurs when checking argument
     */
    JsonNode selectUnitbyId(JsonNode queryJson, String idUnit)
        throws InvalidParseOperationException, AccessInternalExecutionException, MetaDataNotFoundException;

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param idUnit as String
     * @param requestId the request id
     * @return the result of the update on Unit
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     * @throws AccessInternalRuleExecutionException Throw When error occures on rules update check
     * @throws IllegalArgumentException Throw if error occurs when checking argument
     * @throws MetaDataNotFoundException Throw if unit is not found
     */
    JsonNode updateUnitbyId(JsonNode queryJson, String idUnit, String requestId)
        throws MetaDataNotFoundException, InvalidParseOperationException, AccessInternalExecutionException,
        IllegalArgumentException, UpdatePermissionException, AccessInternalRuleExecutionException;

    /**
     * Retrieve an ObjectGroup by its id with results fields filtered based on given query
     *
     * @param queryJson the query DSL as a Json node
     * @param idObjectGroup the id of the ObjectGroup as
     * @return the ObjectGroup metadata as a JsonNode
     * @throws IllegalArgumentException in case of null/incorrect parameters
     * @throws InvalidParseOperationException thrown if json query is not syntactically correct
     * @throws AccessInternalExecutionException in case of access failure
     * @throws MetaDataNotFoundException
     */
    JsonNode selectObjectGroupById(JsonNode queryJson, String idObjectGroup)
        throws InvalidParseOperationException, AccessInternalExecutionException, MetaDataNotFoundException;

    /**
     * Retrieve an object as InputStream based on the associated ObjectGroupId and qualifier + version requested
     *
     * @param idObjectGroup The Object Group Id
     * @param qualifier the qualifier to be retrieve (ie: Dissemination etc.)
     * @param version the version number to get
     * @param idUnit identifier of the parent archiveunit used to have access to the object
     * @return response
     * @throws StorageNotFoundException If the object is not found in storage
     * @throws InvalidParseOperationException when a query is badly structured
     * @throws AccessInternalExecutionException For other technical errors
     * @throws MetaDataNotFoundException
     */
    Response getOneObjectFromObjectGroup(String idObjectGroup,
        String qualifier, int version, String idUnit)
        throws StorageNotFoundException, InvalidParseOperationException, MetaDataNotFoundException,
        AccessInternalExecutionException;

    /**
     * Retrieve all accessLog by the concatenation of all accesslog files as InputStream
     *
     * @param params a json containing query on StartDate or EndDate
     * @return the Storage response for the asked file
     * @throws StorageNotFoundException
     * @throws AccessInternalExecutionException
     */
    Response getAccessLog(JsonNode params)
        throws AccessInternalExecutionException, StorageNotFoundException, ParseException;

    /**
     * retrieve a DIP file according to an operationId
     *
     * @param id operation id
     * @return zip file containing a DIP
     * @throws AccessInternalExecutionException
     */
    Response findDIPByOperationId(String id) throws AccessInternalExecutionException;

    /**
     * select Objects Group based on DSL query
     *
     * @param queryJson as String { $query : query}
     * @return the result of the select on object
     * @throws IllegalArgumentException if json query is null
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Object to database
     */
    JsonNode selectObjects(JsonNode queryJson)
        throws InvalidParseOperationException, AccessInternalExecutionException, VitamDBException;

    /**
     * Select units by DSL and computes inherited rules for matching units
     *
     * @param jsonQuery the query DSL
     * @return the selected units with there inherited rules
     * @throws IllegalArgumentException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     * @throws InvalidParseOperationException
     */
    JsonNode selectUnitsWithInheritedRules(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException;

    /**
     * check ClassificationLevel in update Unit
     *
     * @param query query
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    void checkClassificationLevel(JsonNode query) throws IllegalArgumentException, InvalidParseOperationException;
}
