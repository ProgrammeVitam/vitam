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

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.BasicClient;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Access External Client Interface
 */
public interface AccessExternalClient extends BasicClient {

    /**
     * selectUnits /units
     *
     * @param selectQuery the select query
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    RequestResponse selectUnits(JsonNode selectQuery, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * selectUnitbyId GET(POST overrided) /units/{id}
     *
     * @param selectQuery the select query
     * @param unitId the unit id to select
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    RequestResponse selectUnitbyId(JsonNode selectQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * updateUnitbyId UPDATE /units/{id}
     *
     * @param updateQuery the update query
     * @param unitId the unit id to update
     * @param tenantId
     * @param contractName the contract name
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    RequestResponse updateUnitbyId(JsonNode updateQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, NoWritingPermissionException, AccessUnauthorizedException;

    /**
     * getObjectAsInputStream<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param selectQuery the select query
     * @param objectId the object id to get
     * @param usage kind of usage
     * @param version the version
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return Response including InputStream
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     * @deprecated use getObjectByUnit
     */
    @Deprecated
    Response getObject(JsonNode selectQuery, String objectId, String usage, int version, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * selectObjectById
     *
     * @param selectQuery the select query
     * @param unitId the unit id for getting object
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return Json representation
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    RequestResponse selectObjectById(JsonNode selectQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * getObjectAsInputStream<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param selectQuery the select query
     * @param unitId the unit id for getting the object
     * @param usage kind of usage
     * @param version the version
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return Response including InputStream
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientServerException
     * @throws AccessExternalClientNotFoundException
     * @throws AccessUnauthorizedException
     */
    Response getUnitObject(JsonNode selectQuery, String unitId, String usage, int version, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException;

    /**
     * selectOperation
     *
     * @param select the select query
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return logbookOperation representation
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    RequestResponse<LogbookOperation> selectOperation(JsonNode select, Integer tenantId, String contractName)
        throws VitamClientException;

    /**
     * selectOperationbyId
     *
     * @param processId the process id
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return logbookOperation representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookOperation> selectOperationbyId(String processId, Integer tenantId, String contractName)
        throws VitamClientException;

    /**
     * selectUnitLifeCycleById
     *
     * @param idUnit the unit id
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(String idUnit, Integer tenantId, String contractName)
        throws VitamClientException;

    /**
     * selectObjectGroupLifeCycleById
     *
     * @param idObject the object id
     * @param tenantId the working tenant
     * @param contractName the contract name
     * @return logbooklifecycle representation
     * @throws VitamClientException
     */
    RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(String idObject, Integer tenantId,
        String contractName)
        throws VitamClientException;


    /**
     * DIP export of the unit (xml representation with SEDA schema)
     * 
     * @param queryDsl
     * @param idUnit
     * @param tenantId
     * @param contractName
     * @return unit with a xml representation
     * @throws AccessExternalClientServerException
     */
    Response getUnitByIdWithXMLFormat(JsonNode queryDsl, String idUnit, Integer tenantId, String contractName)
        throws AccessExternalClientServerException;

    /**
     * DIP export of the Object Group (xml representation with SEDA schema)
     * Be careful in the external you cannot access directly to the Object group
     *
     * @param queryDsl     the given query dsl
     * @param idUnit       the given unit
     * @param tenantId     the given tenant if
     * @param contractName the given contract Name
     * @return object group with a xml representation
     * @throws AccessExternalClientServerException
     */
    Response getObjectGroupByIdWithXMLFormat(JsonNode queryDsl, String idUnit, Integer tenantId, String contractName)
        throws AccessExternalClientServerException;
}


