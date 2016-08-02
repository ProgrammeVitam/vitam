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
     * @param stream
     * @throws  ReferentialException
     */
    void checkFormat(InputStream stream) throws  ReferentialException;
    
    
    /**
     * @param stream
     * @throws  ReferentialException
     * @throws DatabaseConflictException 
     */
    void importFormat(InputStream stream) throws  ReferentialException, DatabaseConflictException;
    
    
    /**
     * @param puid
     * @throws  ReferentialException
     */
    void deleteFormat() throws  ReferentialException;
    
    /**
     * Get the status from the service
     *
     * @return the Message status
     */
    
    Status status();


    /**
     * @param id
     * @return JsonNode 
     * @throws ReferentialException 
     * @throws InvalidParseOperationException 
     */
    JsonNode getFormatByID(String id) throws  ReferentialException, InvalidParseOperationException;


    /**
     * @param query
     * @return JsonNode
     * @throws  ReferentialException
     * @throws InvalidParseOperationException 
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     */
    JsonNode getFormats(JsonNode query) throws  ReferentialException, InvalidParseOperationException, JsonGenerationException, JsonMappingException, IOException;
}
