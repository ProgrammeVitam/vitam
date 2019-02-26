package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({MetaDataClientFactory.class, WorkspaceClientFactory.class, AdminManagementClientFactory.class})
public class PrepareAuditActionHandlerTest {

    PrepareAuditActionHandler handler = new PrepareAuditActionHandler();
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataClientFactory;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private AdminManagementClient adminClient;
    private AdminManagementClientFactory adminClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;

    private static final String SEARCH_RESULTS = "PrepareAuditHandler/searchResults.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE)
            .putParameterValue(WorkerParameterName.auditType, "tenant")
            .putParameterValue(WorkerParameterName.objectId, "0");

    @Before
    public void setUp() throws Exception {

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();
        PowerMockito.mockStatic(MetaDataClientFactory.class);

        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);


        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = mock(AdminManagementClient.class);
        adminClientFactory = mock(AdminManagementClientFactory.class);

        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(metadataClientFactory);
        PowerMockito.when(MetaDataClientFactory.getInstance().getClient())
            .thenReturn(metadataClient);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient())
            .thenReturn(workspaceClient);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient())
            .thenReturn(adminClient);

        RequestResponseOK<AccessionRegisterSummaryModel> registerSummary = new RequestResponseOK<>();
        AccessionRegisterSummaryModel register = new AccessionRegisterSummaryModel();
        register.setOriginatingAgency("originatingAgency");
        register.setTotalObjects(new RegisterValueDetailModel().setIngested(1).setRemained(1));
        registerSummary.addResult(register);
        when(adminClient.getAccessionRegister(any())).thenReturn(registerSummary);

        action = new HandlerIOImpl(guid.getId(), "workerId", Lists.newArrayList());
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.AU_TO_BE_UPDATED_JSON)));
    }

    @Test
    public void executeOK() throws Exception {
        action.addOutIOParameters(out);
        final JsonNode searchResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SEARCH_RESULTS));
        reset(workspaceClient);
        reset(metadataClient);
        Mockito.doNothing().when(workspaceClient).createContainer(any());
        Mockito.doNothing().when(workspaceClient).putObject(any(), any(), any());
        when(metadataClient.selectObjectGroups(any())).thenReturn(searchResults);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void executeWARNING() throws Exception {
        action.addOutIOParameters(out);RequestResponseOK<AccessionRegisterSummaryModel> registerSummary = new RequestResponseOK<>();
        AccessionRegisterSummaryModel register = new AccessionRegisterSummaryModel();
        register.setOriginatingAgency("originatingAgency");
        register.setTotalObjects(new RegisterValueDetailModel().setRemained(1));
        registerSummary.addResult(register);
        when(adminClient.getAccessionRegister(any())).thenReturn(registerSummary);
        final JsonNode searchResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SEARCH_RESULTS));
        reset(workspaceClient);
        reset(metadataClient);
        Mockito.doNothing().when(workspaceClient).createContainer(any());
        Mockito.doNothing().when(workspaceClient).putObject(any(), any(), any());
        when(metadataClient.selectObjectGroups(any())).thenReturn(searchResults);
        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void executeFATAL() throws Exception {

        action.addOutIOParameters(out);
        final JsonNode searchResults =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SEARCH_RESULTS));
        reset(workspaceClient);
        reset(metadataClient);
        Mockito.doThrow(new ContentAddressableStorageAlreadyExistException(""))
            .when(workspaceClient).createContainer(any());
        Mockito.doNothing()
            .when(workspaceClient).putObject(any(), any(), any());
        when(metadataClient.selectObjectGroups(any())).thenReturn(searchResults);

        final ItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

}
