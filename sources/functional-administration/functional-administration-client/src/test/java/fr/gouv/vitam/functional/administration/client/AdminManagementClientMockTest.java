package fr.gouv.vitam.functional.administration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory.AdminManagementClientType;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
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
    public void getFormatByIDTest() throws InvalidParseOperationException, ReferentialException {
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        assertNotNull(client.getFormatByID("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaz"));
    }

    @Test
    public void getDocumentTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        Select select = new Select();
        assertNotNull(client.getFormats(select.getFinalSelect()));
    }

    /**************** Rules Manager *****/
    @Test
    public void givenClientMockWhenAndInputCSVOKThenReturnOK() throws FileFormatException, FileRulesException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.checkRulesFile(stream);
    }

    @Test
    public void givenClientMockWhenWhenImportRuleThenReturnOK()
        throws FileFormatException, FileRulesException, DatabaseConflictException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.importRulesFile(stream);
    }

    @Test
    public void givenClientMockWhenDeleteRuleThenReturnOK() throws FileFormatException, FileRulesException {
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.deleteRulesFile();
    }

    @Test
    public void getRuleByIDTest() throws InvalidParseOperationException, ReferentialException {
        // ObjectNode objectNode=
        // {"RuleId":"APP-00001","RuleType":"testList","RuleDescription":"testList","RuleDuration":"10","RuleMeasurement":"Annee"};
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        ObjectNode objectNode = (ObjectNode) (client.getRuleByID("APP-00001"));
        assertEquals("AppraiseRule", objectNode.get("RuleType").asText().toString());
        assertEquals("10", objectNode.get("RuleDuration").asText().toString());
        assertEquals("Annee", objectNode.get("RuleMeasurement").asText().toString());
    }

    @Test
    public void getRuleTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.setConfiguration(AdminManagementClientType.MOCK_CLIENT, null, 0);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        Select select = new Select();
        assertNotNull(client.getRule(select.getFinalSelect()));
    }
}
