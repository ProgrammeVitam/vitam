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
package fr.gouv.vitam.access.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.BasicClient;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

import javax.ws.rs.core.Response;

/**
 * Access External Client Interface
 */
public interface AccessExternalClient extends BasicClient {

    /**
     * selectUnits /units
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * selectUnitbyId GET(POST overrided) /units/{id}
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @param unitId the unit id to select
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException;

    /**
     * updateUnitbyId UPDATE /units/{id}
     * @param vitamContext the vitam context
     * @param updateQuery the update query
     * @param unitId the unit id to update
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery, String unitId)
        throws VitamClientException;

    /**
     * selectObjectById
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @param unitId the unit id for getting object
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext, JsonNode selectQuery,
        String unitId)
        throws VitamClientException;

    /**
     * getObjectAsInputStream<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     * @param vitamContext the vitam context
     * @param unitId the unit id for getting the object
     * @param usage kind of usage
     * @param version the version
     * @return Response including InputStream
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    Response getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage,
        int version)
        throws VitamClientException;

    /**
     * selectOperation
     * @param vitamContext the vitam context
     * @param select the select query
     * @return logbookOperation representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<LogbookOperation> selectOperations(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException;

    /**
     * selectOperationbyId
     * @param vitamContext the vitam context
     * @param operationId the operation id
     * @param select the select query
     * @return logbookOperation representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String operationId, JsonNode select)
        throws VitamClientException;

    /**
     * selectUnitLifeCycleById
     *
     * @param vitamContext the vitam context
     * @param unitLifeCycleId the unit LFC id
     * @param select the select query
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(VitamContext vitamContext,
        String unitLifeCycleId, JsonNode select)
        throws VitamClientException;

    /**
     * selectObjectGroupLifeCycleById
     *
     * @param vitamContext the vitam context
     * @param objectGroupLifeCycleId the objectGroup LFC id
     * @param select the select query
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String objectGroupLifeCycleId, JsonNode select)
        throws VitamClientException;

    /**
     * @param vitamContext the vitam context
     * @return the result of the information obtained in the DIP
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> exportDIP(VitamContext vitamContext,
                                        JsonNode dslRequest) throws VitamClientException;

    /**
     * getDIPById<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param dipId the previously generated DIP id to download the DIP
     * @return Response including InputStream
     * @throws VitamClientException
     */
    Response getDIPById(VitamContext vitamContext, String dipId)
        throws VitamClientException;

    /**
     * Performs a reclassification workflow.
     * @param vitamContext the vitam context
     * @param reclassificationRequest List of attachment and detachment operations in unit graph.
     * @return Response
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> reclassification(VitamContext vitamContext, JsonNode reclassificationRequest)
        throws VitamClientException;

    /**
     * Mass update of archive units.
     * @param vitamContext the vitam context
     * @param updateQuery the update query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> massUpdateUnits(VitamContext vitamContext, JsonNode updateQuery)
        throws VitamClientException;

    /**
     * Mass update of archive units rules.
     * @param vitamContext the vitam context
     * @param updateRulesQuery the update request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> massUpdateUnitsRules(VitamContext vitamContext, JsonNode updateRulesQuery)
        throws VitamClientException;

    /**
     * selectObjects /objects
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectObjects(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * Select units with inherited rules by select query (DSL)
     *
     * @param vitamContext the vitam context
     * @param selectQuery the select query
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException;

    /**
     * Performs an elimination analysis workflow .
     *
     * @param eliminationRequestBody Object Body DSL request for elimination and Date
     * @return Json representation
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> startEliminationAnalysis(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody)
        throws VitamClientException;

    /**
     * Performs an elimination action workflow .
     *
     * @param eliminationRequestBody Object Body DSL request for elimination and Date
     * @return Json representation
     * @throws VitamClientException VitamClientException
     */
    RequestResponse<JsonNode> startEliminationAction(VitamContext vitamContext,
        EliminationRequestBody eliminationRequestBody)
        throws VitamClientException;

    /**
     * Get AccessLog file matching the given params
     * @param vitamContext the vitam context
     * @param params Could contains StartDate and EndDate information for file filter
     * @return accesslog file
     * @throws VitamClientException
     */
    Response getAccessLog(VitamContext vitamContext, JsonNode params)
        throws VitamClientException;

    /**
     * compute inherited rules.
     * @param vitamContext the vitam context
     * @param ComputedInheritedRulesQuery the query request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> computedInheritedRules(VitamContext vitamContext, JsonNode ComputedInheritedRulesQuery)
        throws VitamClientException;

    /**
     * compute inherited rules.
     * @param vitamContext the vitam context
     * @param deleteComputedInheritedRulesQuery the query request (query and actions)
     * @return Json representation
     * @throws VitamClientException
     */
    RequestResponse<JsonNode> deleteComputedInheritedRules(VitamContext vitamContext, JsonNode deleteComputedInheritedRulesQuery)
        throws VitamClientException;

    RequestResponse<JsonNode> launchPreservation(VitamContext vitamContext, PreservationRequest preservationRequest)
        throws VitamClientException;
}
