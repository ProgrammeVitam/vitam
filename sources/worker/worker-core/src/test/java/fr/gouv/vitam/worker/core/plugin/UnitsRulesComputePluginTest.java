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

package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, AdminManagementClientFactory.class})
public class UnitsRulesComputePluginTest {

    UnitsRulesComputePlugin plugin = new UnitsRulesComputePlugin();

    private WorkspaceClient workspaceClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private AdminManagementClient adminManagementClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private static final String ARCHIVE_UNIT_RULE = "unitsRulesComputePlugin/AU_COMPUTE_ENDDATE_SAMPLE.json";
    private static final String ARCHIVE_UNIT_RULE_MGT_ONLY =
        "unitsRulesComputePlugin/AU_COMPUTE_ENDDATE_SAMPLE_MANAGEMENT_ONLY.json";
    private static final String ARBO_MD_RG_COMPLEXE_ROOT = "unitsRulesComputePlugin/ARBO_MD_RG_COMPLEXE_ROOT.json";
    private static final String AU_SIP_MGT_MD_OK1 = "unitsRulesComputePlugin/AU_SIP_MGT_MD_OK1.json";
    private final static String FAKE_URL = "http://localhost:1111";
    private InputStream archiveUnit;
    private HandlerIOImpl action;

    public UnitsRulesComputePluginTest() throws FileNotFoundException {

    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        adminManagementClient = mock(AdminManagementClient.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        action = new HandlerIOImpl(GUIDFactory.newGUID().toString(), GUIDFactory.newGUID().toString());
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_RULE);
    }


    @After
    public void tearDown() throws IOException {
        if (archiveUnit != null) {
            archiveUnit.close();
        }
        action.partialClose();
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK).entity(archiveUnit).build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKO() throws Exception {
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK).entity(archiveUnit).build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferentialPartial());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void givenWorkspaceExistAndEmptyRulesButManagementRulesWhenExecuteThenReturnResponseOK() throws Exception {
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK)
                .entity(PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_RULE_MGT_ONLY)).build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceArchiveUnitFileExistWhenExecuteThenReturnResponseOK() throws Exception {
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK).entity(archiveUnit).build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(Response.status(Status.OK).entity(archiveUnit).build());
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceArchiveUnitFileNullOrNotExistWhenExecuteThenReturnResponseKO() throws Exception {
        reset(adminManagementClient);
        reset(workspaceClient);
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");
        reset(workspaceClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void givenArboMdRgComplexeROOTWhenExecuteThenReturnResponseOK() throws Exception {
        reset(adminManagementClient);
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream(ARBO_MD_RG_COMPLEXE_ROOT))
                .build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferentialForArboMdRgComplexe());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenArchiveUnitMgtMdOk1WhenExecuteThenReturnResponseOK() throws Exception {
        reset(adminManagementClient);
        reset(workspaceClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName")))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream(AU_SIP_MGT_MD_OK1))
                .build());
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferentialForAuMgtMdOk());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }


    private JsonNode getRulesInReferential() {

        final ArrayNode root = JsonHandler.createArrayNode();
        root.add(createRule("ID420", "StorageRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID470", "ClassificationRule", "2", RuleMeasurementEnum.DAY.getType()));
        root.add(createRule("ID019", "AccessRule", "3", RuleMeasurementEnum.MONTH.getType()));

        final ObjectNode rule = JsonHandler.createObjectNode();
        rule.set("$results", root);
        return rule;
    }

    private JsonNode getRulesInReferentialForAuMgtMdOk() {

        final ArrayNode root = JsonHandler.createArrayNode();
        root.add(createRule("ID017", "StorageRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID018", "AppraisalRule", "2", RuleMeasurementEnum.DAY.getType()));
        root.add(createRule("ID019", "AccessRule", "1", RuleMeasurementEnum.YEAR.getType()));
        root.add(createRule("ID020", "DisseminationRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID022", "ReuseRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID024", "ClassificationRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID420", "StorageRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID450", "DisseminationRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID460", "ReuseRule", "3", RuleMeasurementEnum.MONTH.getType()));
        root.add(createRule("ID470", "ClassificationRule", "3", RuleMeasurementEnum.MONTH.getType()));

        final ObjectNode rule = JsonHandler.createObjectNode();
        rule.set("$results", root);
        return rule;
    }

    private JsonNode getRulesInReferentialForArboMdRgComplexe() {

        final ArrayNode root = JsonHandler.createArrayNode();
        root.add(createRule("R1", "StorageRule", "1", RuleMeasurementEnum.YEAR.getType()));
        root.add(createRule("R6", "StorageRule", "6", RuleMeasurementEnum.YEAR.getType()));
        root.add(createRule("ACC-00035", "AccessRule", "100", RuleMeasurementEnum.YEAR.getType()));

        final ObjectNode rule = JsonHandler.createObjectNode();
        rule.set("$results", root);
        return rule;
    }

    private JsonNode getRulesInReferentialPartial() {
        final ObjectNode accessRule = JsonHandler.createObjectNode();
        accessRule.put(FileRules.RULEID, "ID470");
        accessRule.put(FileRules.RULEDESCRIPTION, "rule content");
        accessRule.put(FileRules.RULEDURATION, "2");
        accessRule.put(FileRules.RULEMEASUREMENT, RuleMeasurementEnum.DAY.getType());


        final ObjectNode reuseRule2 = JsonHandler.createObjectNode();
        reuseRule2.put(FileRules.RULEID, "ID019");
        reuseRule2.put(FileRules.RULEDESCRIPTION, "rule description");
        reuseRule2.put(FileRules.RULEDURATION, "3");
        reuseRule2.put(FileRules.RULEMEASUREMENT, RuleMeasurementEnum.MONTH.getType());

        final ArrayNode root1 = JsonHandler.createArrayNode();
        root1.add(accessRule);
        root1.add(reuseRule2);

        final ObjectNode rule = JsonHandler.createObjectNode();
        rule.set("$results", root1);
        return rule;
    }

    private ObjectNode createRule(String ruleId, String ruleType, String ruleDuration, String ruleMeasurement) {
        final ObjectNode rule = JsonHandler.createObjectNode();
        rule.put(FileRules.RULEID, ruleId);
        rule.put(FileRules.RULEDESCRIPTION, "rule description");
        rule.put(FileRules.RULETYPE, ruleType);
        rule.put(FileRules.RULEDURATION, ruleDuration);
        rule.put(FileRules.RULEMEASUREMENT, ruleMeasurement);
        return rule;
    }
}
