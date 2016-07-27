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
package fr.gouv.vitam.functional.administration.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;

/**
 * Mock client implementation for AdminManagement 
 */
public class AdminManagementClientMock implements AdminManagementClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientMock.class);

    private FileFormat createFileFormat(String _id){        
        List<String> testList = new ArrayList<>();
        testList.add("test1"); 
        return (FileFormat) new FileFormat()
            .setCreatedDate("now")
            .setExtension(testList)
            .setMimeType(testList)
            .setName("name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID("puid")
            .setVersion("version")
            .append("_id", _id);                          
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
    
    @Override
    public Status status() {
        return Status.OK;
    }

    @Override
    public void checkFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        LOGGER.info("Check file format request:");
        
    }

    @Override
    public void importFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        LOGGER.info("import file format request:");        
    }

    @Override
    public void deleteFormat() throws FileFormatException {
        LOGGER.info("Delete file format request:");
    }
    
    @Override
    public JsonNode getFormatByID(String id) throws FileFormatException, InvalidParseOperationException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", id);
        LOGGER.info("get format by id request:");        
        FileFormat file = createFileFormat(id);        
        return JsonHandler.toJsonNode(file);        
    }
    
    @Override
    public JsonNode getDocument(JsonNode query) throws FileFormatException, JsonGenerationException, JsonMappingException, InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", query);
        LOGGER.info("get document format request:");                
        FileFormat file1 = createFileFormat(GUIDFactory.newGUID().toString());
        FileFormat file2 = createFileFormat(GUIDFactory.newGUID().toString());        
        List<FileFormat> fileFormatList = new ArrayList<FileFormat>();
        fileFormatList.add(file1);
        fileFormatList.add(file2);
        return JsonHandler.getFromString(fileFormatListToJsonString(fileFormatList));
    }

}
