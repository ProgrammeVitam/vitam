/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;

/**
 * FormatManagementResourceImpl implements AccessResource
 */
@Path("/adminmanagement/v1")
public class AdminManagementResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResource.class);
    private ReferentialFormatFileImpl formatManagement;

    /**
     * Constructor
     *
     * @param configuration
     */
    public AdminManagementResource(AdminManagementConfiguration configuration) {
        formatManagement = new ReferentialFormatFileImpl(configuration);
        LOGGER.info("init Admin Management Resource server");
    }

    /**
     * Check the state of the admin management service API
     * 
     * @return an http response with OK status (200)
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * @param xmlPronom
     * @return Response
     * @throws FileFormatException
     */
    @Path("format/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFormat(InputStream xmlPronom){
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        
        try {
            formatManagement.checkFile(xmlPronom);
        }  catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        }
        return Response.status(Status.OK).build();
    }

    
    /**
     * @param xmlPronom
     * @return Response
     * @throws FileFormatException
     */
    @Path("format/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importFormat(InputStream xmlPronom){
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);        
        try {
            formatManagement.importFile(xmlPronom);
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(status)
                .build();
        } 
        return Response.status(Status.OK).entity(Status.OK.name()).build();
    }

    /**
     * @param puid
     * @return Response
     * @throws FileFormatException
     */
    @Path("format/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFormat() throws FileFormatException{
        formatManagement.deleteCollection();         
        return Response.status(Status.OK).build();
    }
    
    /**
     * @param formatId 
     * @param id 
     * @return Response
     * @throws InvalidParseOperationException 
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     * @throws ReferentialException
     */
    @POST    
    @Path("format/{id_format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId) throws InvalidParseOperationException, JsonGenerationException, JsonMappingException, IOException{        
        ParametersChecker.checkParameter("formatId is a mandatory parameter", formatId);        
        FileFormat fileFormat = null;        
        try {            
            fileFormat = formatManagement.findDocumentById(formatId);
        }  catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        }         
        return Response.status(Status.OK).entity(JsonHandler.toJsonNode(fileFormat)).build();
    }
    
    /**
     * @param select 
     * @return Response
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     * @throws InvalidParseOperationException 
     * @throws ReferentialException
     */
    @Path("format/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocument(JsonNode select) throws JsonGenerationException, JsonMappingException, InvalidParseOperationException, IOException{
        ParametersChecker.checkParameter("select is a mandatory parameter", select);        
        List<FileFormat> fileFormatList = new ArrayList<FileFormat>();                
        try {
            fileFormatList = formatManagement.findDocuments(select);
        }  catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        } 
        
        return Response.status(Status.OK).entity(JsonHandler.getFromString(fileFormatListToJsonString(fileFormatList))).build();
    }

    private String fileFormatListToJsonString(List<FileFormat> formatList)
        throws JsonGenerationException, JsonMappingException, IOException {
        final OutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, formatList);
        final byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        final String fileFormatAsString = new String(data);
        return fileFormatAsString;
    }    
    
}
