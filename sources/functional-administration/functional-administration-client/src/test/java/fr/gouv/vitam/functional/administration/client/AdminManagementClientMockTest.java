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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;


public class AdminManagementClientMockTest {

    AdminManagementClientMock client = new AdminManagementClientMock();
    InputStream stream;

    @Test
    public void givenClientMockWhenAndInputXMLOKThenReturnOK() throws FileFormatException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.checkFormat(stream);
    }

    @Test
    public void givenClientMockWhenWhenImportThenReturnOK() throws FileFormatException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
    }

    @Test
    public void givenClientMockWhenDeleteThenReturnOK() throws FileFormatException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.deleteFormat();
    }

    @Test
    public void getFormatByIDTest() throws InvalidParseOperationException, ReferentialException {
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        assertNotNull(client.getFormatByID("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaz"));
    }

    @Test
    public void getDocumentTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        final Select select = new Select();
        assertNotNull(client.getFormats(select.getFinalSelect()));
    }

    /****************
     * Rules Manager
     * 
     * @throws FileNotFoundException
     *****/
    @Test
    public void givenClientMockWhenAndInputCSVOKThenReturnOK()
        throws FileFormatException, FileRulesException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.checkRulesFile(stream);
    }

    @Test
    public void givenClientMockWhenWhenImportRuleThenReturnOK()
        throws FileFormatException, FileRulesException, DatabaseConflictException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.importRulesFile(stream);
    }

    @Test
    public void givenClientMockWhenDeleteRuleThenReturnOK()
        throws FileFormatException, FileRulesException, FileNotFoundException {
        stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.deleteRulesFile();
    }

    @Test
    public void getRuleByIDTest() throws InvalidParseOperationException, ReferentialException {
        // ObjectNode objectNode=
        // {"RuleId":"APP-00001","RuleType":"testList","RuleDescription":"testList","RuleDuration":"10","RuleMeasurement":"Annee"};
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        final ObjectNode objectNode = (ObjectNode) client.getRuleByID("APP-00001");
        assertEquals("AppraiseRule", objectNode.get("RuleType").asText().toString());
        assertEquals("10", objectNode.get("RuleDuration").asText().toString());
        assertEquals("Annee", objectNode.get("RuleMeasurement").asText().toString());
    }

    @Test
    public void getRuleTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        final Select select = new Select();
        assertNotNull(client.getRule(select.getFinalSelect()));
    }

    @Test
    public void givenClientMockWhenCreateAccessionRegister() throws Exception {
        client.createorUpdateAccessionRegister(new AccessionRegisterDetail());
    }

    @Test
    public void getFundRegisterTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        final Select select = new Select();
        assertNotNull(client.getAccessionRegister(select.getFinalSelect()));
    }
    
    @Test
    public void getAccessionRegisterDetailTest()
        throws InvalidParseOperationException, ReferentialException, JsonGenerationException, JsonMappingException,
        IOException {
        AdminManagementClientFactory.changeMode(null);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        final Select select = new Select();
        JsonNode detailResponse = client.getAccessionRegisterDetail(select.getFinalSelect());
        JsonNode detail = detailResponse.get("results");
        assertNotNull(detail);
        assertTrue(detail.isArray());
        ArrayNode detailAsArray = (ArrayNode) detail;
        assertEquals(2, detailAsArray.size());
        JsonNode item = detailAsArray.get(0);
        assertEquals("AG2", item.get("SubmissionAgency").asText());
    }
    
}
