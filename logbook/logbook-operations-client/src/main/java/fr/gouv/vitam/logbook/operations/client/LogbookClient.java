/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.operations.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.client.StatusMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;

/**
 * Logbook client interface
 */
public interface LogbookClient {

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
    void create(LogbookParameters parameters)
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
    void update(LogbookParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    /**
     * Get the status from the service
     *
     * @return the Message status
     * @throws LogbookClientServerException if the Server got an internal error
     */
    StatusMessage status() throws LogbookClientServerException;

    /**
     * Not implemented yet (think about pool logbook client)
     */
    void close();
   

    /**
     * @param select
     * @return logbook operation as String
     * @throws LogbookClientException
     * @throws InvalidParseOperationException 
     */
    JsonNode selectOperation(String select) throws LogbookClientException, InvalidParseOperationException;
    
    /**
     * @param select
     * @return logbook operation as String
     * @throws LogbookClientException
     * @throws InvalidParseOperationException 
     */
    JsonNode selectOperationbyId(String id) throws LogbookClientException, InvalidParseOperationException;
        
}
