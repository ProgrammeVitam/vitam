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
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.massupdate.MassUpdateUnitRuleRequest;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Access Resource REST API
 */
public interface AccessInternalResource {

    /**
     * gets archive units with Json query
     *
     * @param dslQuery null not allowed
     * @return a archive unit result list
     */
    Response getUnits(JsonNode dslQuery)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException;

    Response selectUnitsWithInheritedRules(JsonNode queryDsl);

    /**
     * @param dslRequest
     * @return
     */
    Response exportDIP(JsonNode dslRequest);

    /**
     * @param dipExportRequest
     * @return
     */
    Response exportDIPByUsageFilter(DipExportRequest dipExportRequest);

    /**
     * @param id of operation (X-Request-Id)
     * @return
     */
    Response findDIPByID(String id);

    /**
     * Starts a reclassification workflow.
     *
     * @param reclassificationRequest Reclassification request.
     * @return response
     */
    Response startReclassificationWorkflow(JsonNode reclassificationRequest);

    /**
     * Starts elimination analysis workflow.
     *
     * @param eliminationRequestBody elimination DSL request
     * @return response
     */
    Response startEliminationAnalysisWorkflow(EliminationRequestBody eliminationRequestBody);

    /**
     * Starts elimination action workflow.
     *
     * @param eliminationRequestBody elimination DSL request
     * @return response
     */
    Response startEliminationActionWorkflow(EliminationRequestBody eliminationRequestBody);

    /**
     * gets archive units by Id with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @param unitId units identifier
     * @return a archive unit result list on json format
     */
    Response getUnitById(JsonNode dslQuery, String unitId);

    /**
     * gets archive units by Id with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @param unitId units identifier
     * @return a archive unit result list on xml format
     */
    Response getUnitByIdWithXMLFormat(JsonNode dslQuery, String unitId);

    /**
     * gets object group by Id with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @param objectId units identifier
     * @return an object group result list on xml format
     */
    Response getObjectByIdWithXMLFormat(JsonNode dslQuery, String objectId);

    /**
     * gets object group by Id with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @param unitId units identifier
     * @return an object group result list on xml format
     */
    Response getObjectByUnitIdWithXMLFormat(JsonNode dslQuery, String unitId);

    /**
     * update archive units by Id with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @param unitId units identifier
     * @param requestId request identifier
     * @return a archive unit result list
     */
    Response updateUnitById(JsonNode dslQuery, String unitId, String requestId);

    /**
     * Retrieve an ObjectGroup by its id
     *
     * @param idObjectGroup the ObjectGroup id
     * @param query the json query
     * @return an http response containing the objectGroup as json or a json serialized error
     */
    Response getObjectGroup(String idObjectGroup, JsonNode query);

    /**
     * Retrieve an Object associated to the given ObjectGroup id based on given (via headers) Qualifier and Version
     * (Async version)
     *
     * @param headers
     * @param idObjectGroup
     * @return response
     */
    Response getObjectStreamAsync(HttpHeaders headers, String idObjectGroup, String idUnit);

    /**
     * gets accesslog file by id as an InputStream
     *
     * @param headers request headers
     * @param params given params in order to filter accessLog files
     * @return
     */
    Response getAccessLogStreamAsync(HttpHeaders headers, JsonNode params);

    /**
     * Mass update of archive units with Json query
     *
     * @param dslQuery DSL, null not allowed
     * @return the response
     */
    Response massUpdateUnits(JsonNode dslQuery);

    /**
     * Mass update of archive units rules
     *
     * @param massUpdateUnitRuleRequest wrapper for {DSL, RuleActions}, null not allowed
     * @return the response
     */
    Response massUpdateUnitsRules(MassUpdateUnitRuleRequest massUpdateUnitRuleRequest);

    /**
     * gets objects group with Json query
     *
     * @param dslQuery null not allowed
     * @return a objects group result list
     */
    Response getObjects(JsonNode dslQuery)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException;

    Response transferReply(InputStream transferReply);
}
