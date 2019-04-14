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
package fr.gouv.vitam.access.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

import javax.ws.rs.core.Response;

/**
 * Access client interface
 */
public interface AccessInternalClient extends MockOrRestClient {

    /**
     * Select Units
     *
     * @param selectQuery the query used to select units
     * @return a response containing a json node object including DSL queries and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     * @throws BadRequestException if empty query is found
     */
    RequestResponse<JsonNode> selectUnits(JsonNode selectQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException;

    /**
     * select Unit By Id
     *
     * @param sqlQuery the query to be executed
     * @param id the id of the unit
     * @return a response containing a json node object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectUnitbyId(JsonNode sqlQuery, String id)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * update Unit By Id
     *
     * @param updateQuery the query to be executed as an update
     * @param unitId the id of the unit
     * @return a response containing a json node object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     * @throws AccessInternalRuleExecutionException
     */
    RequestResponse<JsonNode> updateUnitbyId(JsonNode updateQuery, String unitId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, NoWritingPermissionException, AccessUnauthorizedException;

    /**
     * Mass update of archive units with dsl query.
     *
     * @param updateQuery the query to be executed to update archive units
     * @return a response containing a json node object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     * @throws AccessInternalRuleExecutionException
     */
    RequestResponse<JsonNode> updateUnits(JsonNode updateQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException, NoWritingPermissionException,
        AccessUnauthorizedException;

    /**
     * Mass update of archive units rules.
     * @param massUpdateUnitRuleRequest the request to be used to update archive units rules
     * @return a response containing a json node object including queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     * @throws AccessInternalRuleExecutionException
     */
    RequestResponse<JsonNode> updateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest)
            throws InvalidParseOperationException, AccessInternalClientServerException, NoWritingPermissionException,
            AccessUnauthorizedException;

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param selectObjectQuery the query to be executed
     * @param objectId the Id of the ObjectGroup
     * @return a response containing a json node object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested object does not exist
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectObjectbyId(JsonNode selectObjectQuery, String objectId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * Retrieve an Object data as an input stream
     *
     * @param objectGroupId the Id of the ObjectGroup
     * @param usage         the requested usage
     * @param version       the requested version of the usage
     * @param unitId        the id used by the user to have access to the object
     * @return Response containing InputStream for the object data
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested object does not exist
     * @throws AccessUnauthorizedException
     */
    Response getObject(String objectGroupId, String usage, int version, String unitId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException;


    /**
     * selectOperation
     *
     * @param select
     * @return a response containing a json node
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectOperation(JsonNode select)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * selectOperationbyId
     *
     * @param processId ID of the operation
     * @param queryDsl query to be executed
     * @return a response containing a json node
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectOperationById(String processId, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * selectUnitLifeCycleById
     *
     * @param idUnit
     * @param queryDsl query to be executed
     * @return a response containing a json node
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectUnitLifeCycleById(String idUnit, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * selectObjectGroupLifeCycleById
     *
     * @param idObject
     * @param queryDsl query to be executed
     * @return a response containing a json node
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> selectObjectGroupLifeCycleById(String idObject, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * Checks operation traceability
     *
     * @param query to be executed
     * @return a response containing a json node
     * @throws LogbookClientServerException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<JsonNode> checkTraceabilityOperation(JsonNode query)
        throws LogbookClientServerException, InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * @param operationId
     * @return a response containing the traceability file
     * @throws AccessUnauthorizedException
     */
    Response downloadTraceabilityFile(String operationId)
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * launch a DIP operation by a DSL query
     *
     * @param dslRequest query for the DIP creation
     * @return
     * @throws AccessInternalClientServerException
     */
    RequestResponse<JsonNode> exportDIP(JsonNode dslRequest) throws AccessInternalClientServerException;

    /**
     * launch a DIP operation by a DSL query
     *
     * @param dipExportRequest query for the DIP creation
     * @return
     * @throws AccessInternalClientServerException
     */
    RequestResponse<JsonNode> exportDIPByUsageFilter(DipExportRequest dipExportRequest) throws AccessInternalClientServerException;

    /**
     * get a zip file containing a DIP by an operation id
     *
     * @param id operationId
     * @return stream containing zip file
     * @throws AccessInternalClientServerException
     */
    Response findDIPByID(String id) throws AccessInternalClientServerException;

    /**
     * Performs a reclassification workflow.
     *
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     * @return Response
     */
    RequestResponse<JsonNode> reclassification(JsonNode reclassificationRequest)
        throws AccessInternalClientServerException;

    /**
     * Select Objects group based on DSL query
     *
     * @param selectQuery the query used to select objects
     * @return a response containing a json node object including DSL queries and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested object does not exist
     * @throws AccessUnauthorizedException
     * @throws BadRequestException if empty query is found
     */
    RequestResponse<JsonNode> selectObjects(JsonNode selectQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException;

    /**
     * Select units with inherited rules by select query (DSL)
     *
     * @param selectQuery : select query
     * @return a response containing a json node object including DSL queries and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested unit does not exist
     * @throws AccessUnauthorizedException
     * @throws BadRequestException if empty query is found
     */
    RequestResponse<JsonNode> selectUnitsWithInheritedRules(JsonNode selectQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException;

    /**
     * Get Zipped AccessLog files as Stream
     *
     * @return a response containing zipped accessLog files as Stream
     * @throws AccessInternalClientServerException if the server encountered an exception
     * @throws AccessInternalClientNotFoundException if the requested object does not exist
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessUnauthorizedException
     */
    Response downloadAccessLogFile(JsonNode params)
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException;

    /**
     * Performs an elimination analysis workflow.
     *
     * @param eliminationRequestBody Dsl request for elimination.
     * @return Response given response
     */
    RequestResponse<JsonNode> startEliminationAnalysis(EliminationRequestBody eliminationRequestBody)
        throws AccessInternalClientServerException;

    /**
     * Performs an elimination action workflow.
     *
     * @param eliminationRequestBody Dsl request for elimination.
     * @return Response given response
     * @throws AccessInternalClientServerException AccessInternalClientServerException
     */
    RequestResponse<JsonNode> startEliminationAction(EliminationRequestBody eliminationRequestBody)
        throws AccessInternalClientServerException;

    /**
     * Perform a preservation workflow
     *
     * @param request preservation request
     * @return the given RequestResponse
     * @throws AccessInternalClientServerException AccessInternalClientServerException
     */
    RequestResponse<JsonNode> startPreservation(PreservationRequest request) throws AccessInternalClientServerException;
}
