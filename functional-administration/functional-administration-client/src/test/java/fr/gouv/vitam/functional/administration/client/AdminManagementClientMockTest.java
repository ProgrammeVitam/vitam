package fr.gouv.vitam.functional.administration.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory.AdminManagementClientType;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;


public class AdminManagementClientMockTest {

    AdminManagementClientMock client = new AdminManagementClientMock();
    InputStream stream;

    @Test
    public void givenClientMockWhenStatusThenReturnOK() {
        client.status();
    }
          
    @Test
    public void givenClientMockWhenAndInputXMLOKThenReturnOK() throws FileFormatException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.checkFormat(stream);    
    }
    
    @Test
    public void givenClientMockWhenWhenImportThenReturnOK() throws FileFormatException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);    
    }    
    
    @Test
    public void givenClientMockWhenDeleteThenReturnOK() throws FileFormatException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("FF-vitam.xml");
        client.deleteFormat();    
    }        
    
    @Test
    public void getFormatByIDTest() throws  InvalidParseOperationException, ReferentialException {
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        assertNotNull(client.getFormatByID("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaz"));
    }

    @Test
    public void getDocumentTest() throws  InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException, IOException {
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        Select select = new Select();        
        assertNotNull(client.getFormats(select.getFinalSelect()));
    }
    
}
