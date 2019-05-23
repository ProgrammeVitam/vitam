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
package fr.gouv.vitam.metadata.api;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.exception.*;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * MetaData interface for database operations
 */
public interface MetaData {

    // Unit

    /**
     * insert Unit
     *
     * @param insertRequest as String { $roots: roots, $query : query, $filter : multi, $data : data}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws IllegalArgumentException Throw if arguments of insert query is invalid
     * @throws MetaDataNotFoundException Throw if parent of this unit is not found
     * @throws MetaDataAlreadyExistException Throw if Unit id already exists
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    void insertUnit(JsonNode insertRequest)
        throws InvalidParseOperationException, IllegalArgumentException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataDocumentSizeException, VitamDBException;


    /**
     * Select an Accession Register linked to an Operation
     *
     * @param operationId the operation identifier
     * @param tenantId the tenant identifier
     * @return the list of documents
     */
    List<ObjectGroupPerOriginatingAgency> selectOwnAccessionRegisterOnObjectGroupByOperationId(Integer tenantId, String operationId);


    /**
     * Search UNITs by Select {@link Select}Query
     *
     * @param selectQuery the query of type JsonNode
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws MetaDataNotFoundException Throw if unit by id not found
     * @throws BadRequestException if a bad request is being used
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    RequestResponse<JsonNode> selectUnitsByQuery(JsonNode selectQuery)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException;

    /**
     * Search ObjectGroups by Select {@link Select}Query
     *
     * @param selectQuery the query of type JsonNode
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws MetaDataNotFoundException Throw if unit by id not found
     * @throws BadRequestException if a bad request is being used
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    RequestResponse<JsonNode> selectObjectGroupsByQuery(JsonNode selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException;



    /**
     * Search UNITs by Id {@link Select}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param selectQuery the select query of type JsonNode
     * @param unitId the unit id for query
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws MetaDataNotFoundException Throw if unit by id not found
     * @throws BadRequestException if a bad request is being used
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    RequestResponse<JsonNode> selectUnitsById(JsonNode selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException;

    /**
     * Search ObjectGroups by its Id and a Select Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param selectQuery the query to filter results and make projections
     * @param objectGroupId the objectgroup id
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of objectgroups(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws MetaDataNotFoundException Thrown if no objectGroup is found
     * @throws BadRequestException if a bad request is being used
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    RequestResponse<JsonNode> selectObjectGroupById(JsonNode selectQuery, String objectGroupId)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        MetaDataNotFoundException, BadRequestException, VitamDBException;


    /**
     * Update UNITs by Ids {@link UpdateMultiQuery}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id1"}, {id:"id2"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param updateQuery the update query as JsonNode containing unitIds in root parts
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     */
    RequestResponse<JsonNode> updateUnits(JsonNode updateQuery)
        throws InvalidParseOperationException;

    /**
     * Update UNITs Rules by Ids {@link UpdateMultiQuery}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :  {  <br>
     * "dslRequest": {"$roots":[{id:"id1"},{id:"id2"}],"$query":[]}, <br> 
     * "ruleActions": {"add":[{"AppraisalRule":{"Rules":[{"Rule":"APP-00001","StartDate":"1982-09-01"}],"FinalAction":"Keep"}} ]  }  <br>
     * }
     * @param updateQuery the update query as JsonNode containing unitIds in root parts and updates in ruleActions part
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     */
    RequestResponse<JsonNode> updateUnitsRules(JsonNode updateQuery, Map<String, DurationData> bindRuleToDuration)
            throws InvalidParseOperationException;

    /**
     * Update UNITs by Id {@link UpdateMultiQuery}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param updateQuery the update query as JsonNode
     * @param unitId the id of Unit for query
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * @throws MetaDataNotFoundException Throw if unit does not exist
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    RequestResponse<JsonNode> updateUnitById(JsonNode updateQuery, String unitId)
        throws MetaDataNotFoundException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, VitamDBException, SchemaValidationException, ArchiveUnitOntologyValidationException;


    /**
     * Insert an objectGroup
     *
     * @param objectRequest as JsonNode { $roots: roots, $query : query, $filter : multi, $data : data}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws MetaDataAlreadyExistException Throw if Unit id already exists
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     */
    void insertObjectGroup(JsonNode objectRequest)
        throws InvalidParseOperationException,
        MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataDocumentSizeException;

    void insertObjectGroups(List<JsonNode> objectGroupRequest)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException;

    /**
     * Creates the AccessionRegisterSymbolics from ElasticSearch aggregations and nested aggregation request.
     * Because the AccessionRegisterSymbolic is not available from this package, it is a list of Document
     * which is returned.
     *
     * @param tenant on related to the symbolic accession register to create
     * @return a list of AccessionRegisterSymbolic
     */
    List<Document> createAccessionRegisterSymbolic(Integer tenant);

    /**
     * find the number of archive unit per originating agency for a operationId
     *
     * @param operationId operation id
     * @return the list of documents
     */
    List<FacetBucket> selectOwnAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataExecutionException;


    /**
     * Update an object group
     *
     * @param updateRequest the request as a json
     * @param objectId the id of the object to be updated
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Thrown if error occurs when send Unit to database
     * @throws VitamDBException in case a desynchro is recorded between Mongo and ES
     */
    void updateObjectGroupId(JsonNode updateRequest, String objectId)
        throws InvalidParseOperationException, MetaDataExecutionException, VitamDBException;

    /**
     * Flush Unit Index
     *
     * @throws IllegalArgumentException if tenant is wrong
     * @throws VitamThreadAccessException if tenant is wrong
     */
    void refreshUnit() throws IllegalArgumentException, VitamThreadAccessException;

    /**
     * Flush ObjectGroup Index
     *
     * @throws IllegalArgumentException if tenant is wrong
     * @throws VitamThreadAccessException if tenant is wrong
     */
    void refreshObjectGroup() throws IllegalArgumentException, VitamThreadAccessException;

    /**
     * Reindex one or more collections
     *
     * @param indexParam the parameters specifying what to reindex
     * @return the reindexation result as a IndexationResult Object
     */
    IndexationResult reindex(IndexParameters indexParam);

    /**
     * Switch indexes for one or more collections
     *
     * @param alias the alias name
     * @param newIndexName the new index to be pointed on
     * @throws DatabaseException in case error with database occurs
     */
    void switchIndex(String alias, String newIndexName) throws DatabaseException;

    void insertUnits(List<JsonNode> jsonNodes)
        throws InvalidParseOperationException, IllegalArgumentException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataDocumentSizeException, VitamDBException;

    /**
     * delete units
     *
     * @param idsList idsList
     */
    void deleteUnits(List<String> idsList) throws IllegalArgumentException, MetaDataExecutionException;

    /**
     * delete objectsGroups
     *
     * @param idsList idsList
     */
    void deleteObjectGroups(List<String> idsList) throws IllegalArgumentException, MetaDataExecutionException;

}
