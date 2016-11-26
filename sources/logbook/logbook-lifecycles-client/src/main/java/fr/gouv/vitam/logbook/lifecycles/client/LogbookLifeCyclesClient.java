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
package fr.gouv.vitam.logbook.lifecycles.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;

/**
 * Logbook client interface
 */
public interface LogbookLifeCyclesClient extends BasicClient {

    /**
     * Create logbook entry <br>
     * <br>
     * To be used ONLY once at top level of process startup (where eventIdentifierProcess is set for the first time).
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     * @throws LogbookClientException if client received an error from server
     */
    void create(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException;

    /**
     * Update logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;


    /**
     * Commit logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Rollback logbook entry <br>
     * <br>
     * To be used everywhere except very first time (when eventIdentifierProcess already used once)
     *
     * @param parameters the entry parameters
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * returns the unit life cycle
     *
     * @param id unit life cycle id
     * @return the unit life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectUnitLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns the object group life cycle
     *
     * @param id the object group life cycle id
     * @return the object group life cycle
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    JsonNode selectObjectGroupLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns VitamRequestIterator on ObjectGroupLifecycles for this operation.</br>
     * </br>
     * Example of code using it:</br>
     *
     * <pre>
     * <code>
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try (VitamRequestIterator iterator = client.objectGroupLifeCyclesByOperationIterator(operationId)) {
                while (iterator.hasNext()) {
                    JsonNode objectGroupLifeCycle = iterator.next();
                    // use it
                }
            }
    
        }
     * </code>
     * </pre>
     *
     * @param operationId the operation id from which this ObjectGroup Lifecycles will be retrieved
     * @return the VitamRequestIterator on ObjectGroupLifecycles as JsonNode
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    VitamRequestIterator objectGroupLifeCyclesByOperationIterator(String operationId)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * returns VitamRequestIterator on UnitLifeCycles for this operation.</br>
     * </br>
     * Example of code using it:</br>
     *
     * <pre>
     * <code>
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try (VitamRequestIterator iterator = client.unitLifeCyclesByOperationIterator(operationId)) {
                while (iterator.hasNext()) {
                    JsonNode unitLifeCycle = iterator.next();
                    // use it
                }
            }
    
        }
     * </code>
     * </pre>
     *
     * @param operationId the operation id from which this UnitLife Lifecycles will be retrieved
     * @return the VitamRequestIterator on UnitLifeCycles as JsonNode
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    VitamRequestIterator unitLifeCyclesByOperationIterator(String operationId)
        throws LogbookClientException, InvalidParseOperationException;

    /**
     * Bulk Create for Unit<br>
     * <br>
     * To be used ONLY once at top level of process startup (where objectIdentifier is set for the first time).
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to create
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkCreateUnit(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException;

    /**
     * Bulk Update for Unit<br>
     * <br>
     * To be used everywhere except very first time (when objectIdentifier already used once)
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to update
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkUpdateUnit(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

    /**
     * Bulk Create for ObjectGroup<br>
     * <br>
     * To be used ONLY once at top level of process startup (where objectIdentifier is set for the first time).
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to create
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientAlreadyExistsException if the element already exists
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkCreateObjectGroup(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException;

    /**
     * Bulk Update for ObjectGroup<br>
     * <br>
     * To be used everywhere except very first time (when objectIdentifier already used once)
     *
     * @param objectIdentifier object Identifier
     * @param queue queue of LogbookLifeCycleParameters to update
     * @throws LogbookClientBadRequestException if the argument is incorrect
     * @throws LogbookClientNotFoundException if the element was not created before
     * @throws LogbookClientServerException if the Server got an internal error
     * @throws IllegalArgumentException if some mandatories parameters are empty or null
     */
    void bulkUpdateObjectGroup(String objectIdentifier, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException;

}
