package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * AdminManagementClient interface
 */
public interface AdminManagementClient {
    
    /**
     * @param stream as InputStream;
     * @throws  ReferentialException when check exception occurs
     */
    void checkFormat(InputStream stream) throws  ReferentialException;
    
    
    /**
     * @param stream as InputStream
     * @throws  ReferentialException when import exception occurs
     * @throws DatabaseConflictException conflict exception occurs
     */
    void importFormat(InputStream stream) throws  ReferentialException, DatabaseConflictException;
    
    
    /**
     * @throws  ReferentialException when delete exception occurs
     */
    void deleteFormat() throws  ReferentialException;
    
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
    JsonNode getFormatByID(String id) throws  ReferentialException, InvalidParseOperationException;


    /**
     * @param query as JsonNode
     * @return JsonNode 
     * @throws  ReferentialException when referential format exception occurs
     * @throws InvalidParseOperationException when json exception occurs
     * @throws IOException when io data exception occurs
     * @throws JsonMappingException when json exception occurs
     * @throws JsonGenerationException when json exception occurs
     */
    JsonNode getFormats(JsonNode query) throws  ReferentialException, InvalidParseOperationException, JsonGenerationException, JsonMappingException, IOException;
}
