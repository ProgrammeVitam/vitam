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
package fr.gouv.vitam.access.client;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Access client interface
 */
public interface AccessClient extends MockOrRestClient {

    /**
     * Select Units
     *
     * @param selectQuery the query used to select units
     * @return JsonNode object including DSL queries and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested unit does not exist
     */
    JsonNode selectUnits(String selectQuery)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException;

    /**
     * select Unit By Id
     *
     * @param sqlQuery the query to be executed
     * @param id the id of the unit
     * @return JsonNode object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested unit does not exist
     */
    JsonNode selectUnitbyId(String sqlQuery, String id)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException;

    /**
     * update Unit By Id
     *
     * @param updateQuery the query to be executed as an update
     * @param unitId the id of the unit
     * @return JsonNode object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested unit does not exist
     */
    JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException;

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param selectObjectQuery the query to be executed
     * @param objectId the Id of the ObjectGroup
     * @return JsonNode object including DSL queries, context and results
     * @throws InvalidParseOperationException if the query is not well formatted
     * @throws AccessClientServerException if the server encountered an exception
     * @throws AccessClientNotFoundException if the requested object does not exist
     */
    JsonNode selectObjectbyId(String selectObjectQuery, String objectId)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException;

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
    InputStream getObjectAsInputStream(String selectObjectQuery, String objectGroupId, String usage, int version)
        throws InvalidParseOperationException, AccessClientServerException, AccessClientNotFoundException;


}
