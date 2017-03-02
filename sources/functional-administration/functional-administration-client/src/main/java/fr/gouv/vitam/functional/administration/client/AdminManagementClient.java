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
package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterDetailModel;
import fr.gouv.vitam.functional.administration.client.model.FileFormatModel;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * AdminManagementClient interface
 */
public interface AdminManagementClient extends MockOrRestClient {

    /**
     * @param stream as InputStream;
     * @return Response
     * @throws ReferentialException when check exception occurs
     */
    Response checkFormat(InputStream stream) throws ReferentialException;

    /**
     * @param stream as InputStream
     * @return the response to the request
     * @throws ReferentialException      when import exception occurs
     * @throws DatabaseConflictException conflict exception occurs
     */
    Response importFormat(InputStream stream) throws ReferentialException, DatabaseConflictException;


    /**
     * @param id as String
     * @return JsonNode
     * @throws ReferentialException check exception occurs
     * @throws InvalidParseOperationException when json exception occurs
     */
    JsonNode getFormatByID(String id) throws ReferentialException, InvalidParseOperationException;


    /**
     * @param query as JsonNode
     * @return JsonNode
     * @throws ReferentialException when referential format exception occurs
     * @throws InvalidParseOperationException when json exception occurs
     * @throws IOException when io data exception occurs
     */
    RequestResponse<FileFormatModel> getFormats(JsonNode query)
        throws ReferentialException, InvalidParseOperationException,
        IOException;

    /**
     * Check if rule file is well formated
     * 
     * @param stream
     * @return
     * @throws FileRulesException
     * @throws AdminManagementClientServerException
     */
    Response checkRulesFile(InputStream stream) throws FileRulesException, AdminManagementClientServerException;

    /**
     * Import a the set of rules for a given tenant
     * 
     * @param stream
     * @return the response to the request
     * @throws FileRulesException                   when file rules exception occurs
     * @throws DatabaseConflictException            when Database conflict exception occurs
     * @throws AdminManagementClientServerException
     */
    Response importRulesFile(InputStream stream)
        throws FileRulesException, DatabaseConflictException, AdminManagementClientServerException;

    /**
     *
     * @param id The rule identifier
     * @return Rule in JsonNode format
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     * @throws AdminManagementClientServerException
     */
    JsonNode getRuleByID(String id)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException;

    /**
     * List the rules that match the query
     * 
     * @param query
     * @return Rules in JsonNode format
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     * @throws IOException when IO Exception occurs
     * @throws AdminManagementClientServerException
     */
    JsonNode getRules(JsonNode query)
        throws FileRulesException, InvalidParseOperationException,
        IOException, AdminManagementClientServerException;

    /**
     * @param register AccessionRegisterDetail
     * @throws AccessionRegisterException when AccessionRegisterDetailexception occurs
     * @throws DatabaseConflictException when Database conflict exception occurs
     * @throws AdminManagementClientServerException
     */
    void createorUpdateAccessionRegister(AccessionRegisterDetailModel register)
        throws AccessionRegisterException, DatabaseConflictException, AdminManagementClientServerException;

    /**
     * Get the accession register summary matching the given query
     *
     * @param query The DSL Query as Json Node
     * @return The AccessionregisterSummary list as a response JsonNode
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    RequestResponse getAccessionRegister(JsonNode query) throws InvalidParseOperationException, ReferentialException;

    /**
     * Get the accession register details matching the given query
     *
     * @param query The DSL Query as a JSON Node
     * @return The AccessionregisterDetails list as a response jsonNode
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    RequestResponse getAccessionRegisterDetail(JsonNode query)
        throws InvalidParseOperationException, ReferentialException;


    /**
     * Import a set of contracts after passing the validation steps If all the contracts are valid, they are stored in
     * the collection and indexed The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts elready exist in the database</li>
     * </ul>
     * 
     * @param contractsToImport
     * @return The server response
     */
    Response importContracts(ArrayNode contractsToImport);

}
