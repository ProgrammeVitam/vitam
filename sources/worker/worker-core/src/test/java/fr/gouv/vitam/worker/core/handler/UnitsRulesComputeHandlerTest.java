package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, AdminManagementClientFactory.class})
public class UnitsRulesComputeHandlerTest {


    UnitsRulesComputeHandler handler = new UnitsRulesComputeHandler();

    private WorkspaceClient workspaceClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private AdminManagementClient adminManagementClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private static final String ARCHIVE_UNIT_RULE = "AU_COMPUTE_ENDDATE_SAMPLE.xml";
    private final static String FAKE_URL = "localhost:1111";
    private InputStream archiveUnit;
    private HandlerIO action;

    public UnitsRulesComputeHandlerTest() throws FileNotFoundException {

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

        when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_RULE);
    }


    @After
    public void tearDown() throws IOException {
        if (archiveUnit != null) {
            archiveUnit.close();
        }
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(anyObject(), eq("Units/objectName"))).thenReturn(archiveUnit);
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());

        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName").setCurrentStep("currentStep").setContainerName("containerName");

        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceArchiveUnitFileExistWhenExecuteThenReturnResponseOK() throws Exception {
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName"))).thenReturn(archiveUnit);
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(archiveUnit);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceArchiveUnitFileNullOrNotExistWhenExecuteThenReturnResponseKO() throws Exception {
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        when(workspaceClient.getObject(anyObject(), eq("Units/objectName"))).thenReturn(archiveUnit);
        when(adminManagementClient.getRules(anyObject())).thenReturn(getRulesInReferential());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(null);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }


    private JsonNode getRulesInReferential() {
        final ObjectNode reuseRule = JsonHandler.createObjectNode();

        reuseRule.put(FileRules.RULEID, "ID420");
        reuseRule.put(FileRules.RULEDESCRIPTION, "rule description");
        reuseRule.put(FileRules.RULEDURATION, "3");
        reuseRule.put(FileRules.RULEMEASUREMENT, RuleMeasurementEnum.MOIS.getType());

        final ObjectNode accessRule = JsonHandler.createObjectNode();
        accessRule.put(FileRules.RULEID, "ID470");
        accessRule.put(FileRules.RULEDESCRIPTION, "rule content");
        accessRule.put(FileRules.RULEDURATION, "2");
        accessRule.put(FileRules.RULEMEASUREMENT, RuleMeasurementEnum.JOURS.getType());


        final ObjectNode reuseRule2 = JsonHandler.createObjectNode();
        reuseRule2.put(FileRules.RULEID, "ID019");
        reuseRule2.put(FileRules.RULEDESCRIPTION, "rule description");
        reuseRule2.put(FileRules.RULEDURATION, "3");
        reuseRule2.put(FileRules.RULEMEASUREMENT, RuleMeasurementEnum.MOIS.getType());

        final ArrayNode root1 = JsonHandler.createArrayNode();
        root1.add(accessRule);
        root1.add(reuseRule);
        root1.add(reuseRule2);
        return root1;
    }


}
