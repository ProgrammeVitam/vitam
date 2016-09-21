package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * AdminManagementClient interface
 */
public interface AdminManagementClient {

    /**
     * @param stream as InputStream;
     * @return
     * @throws ReferentialException when check exception occurs
     */
    Status checkFormat(InputStream stream) throws ReferentialException;


    /**
     * @param stream as InputStream
     * @throws ReferentialException when import exception occurs
     * @throws DatabaseConflictException conflict exception occurs
     */
    void importFormat(InputStream stream) throws ReferentialException, DatabaseConflictException;


    /**
     * @throws ReferentialException when delete exception occurs
     */
    // FIXME delete the collection without any check on legal to do so (does any object using this referential ?) ?
    // Il me semble que cette fonction devrait être interne et appelée par la méthode importFormat en interne de Vitam
    // et surtout pas en externe !!!
    // Fonctionnalité demandé par les POs pour la démo
    void deleteFormat() throws ReferentialException;

    /**
     * Get the status from the service
     *
     * @return the Message status
     */

    Status status();


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
    JsonNode getFormats(JsonNode query)
        throws ReferentialException, InvalidParseOperationException,
        IOException;

    /**
     * 
     * @param stream
     * @return
     * @throws FileRulesException
     */

    Status checkRulesFile(InputStream stream) throws FileRulesException;

    /**
     * 
     * @param stream
     * @throws FileRulesException when file rules exception occurs
     * @throws DatabaseConflictException when Database conflict exception occurs
     */
    void importRulesFile(InputStream stream) throws FileRulesException, DatabaseConflictException;

    /**
     * 
     * @throws FileRulesException
     */

    void deleteRulesFile() throws FileRulesException;

    /**
     * 
     * @param id ide de rule
     * @return
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     */
    JsonNode getRuleByID(String id) throws FileRulesException, InvalidParseOperationException;

    /**
     * 
     * @param query
     * @return
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     * @throws IOException when IO Exception occurs
     */
    JsonNode getRule(JsonNode query)
        throws FileRulesException, InvalidParseOperationException,
        IOException;
}
