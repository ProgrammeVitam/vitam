/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientMock;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckHeaderActionHandlerTest {

    private static final String CONTRACT_NAME = "ArchivalAgreement0";
    private static final String SEDA_PARAMS_JSON = "Maps/sedaParams.json";

    private final SedaUtils sedaUtils = mock(SedaUtils.class);
    private GUID guid;
    private static final Integer TENANT_ID = 0;

    private static final String SIP_ADD_UNIT = "CheckHeaderActionHandler/manifest.xml";
    private static final String MANIFEST_WITHOUT_ARCHIVAL_PROFILE =
        "CheckHeaderActionHandler/manifest_no_archival_profile.xml";
    private static final String SEDA_PARAMS = "extractSedaActionHandler/SedaParams.json";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private AdminManagementClient adminManagementClient;
    private AdminManagementClientFactory adminManagementClientFactory;

    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private SedaUtilsFactory sedaUtilsFactory = mock(SedaUtilsFactory.class);

    private final HandlerIO handlerIO = mock(HandlerIO.class);
    private CheckHeaderActionHandler handler;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws ReferentialException, InvalidParseOperationException, ProcessingException {
        guid = GUIDFactory.newGUID();
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        adminManagementClient = mock(AdminManagementClient.class);
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);

        storageClient = mock(StorageClient.class);
        storageClientFactory = mock(StorageClientFactory.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        when(adminManagementClient.findIngestContractsByID(any())).thenReturn(
            new AdminManagementClientMock().findIngestContractsByID("contract")
        );
        when(adminManagementClient.findContextById(any())).thenReturn(
            new AdminManagementClientMock().findContextById("context")
        );
        when(adminManagementClient.getAgencies(any())).thenReturn(
            new AdminManagementClientMock().getAgencies(JsonHandler.createObjectNode())
        );

        when(adminManagementClient.findIngestContracts(any())).thenReturn(
            new AdminManagementClientMock().findIngestContracts(JsonHandler.createObjectNode())
        );

        when(sedaUtilsFactory.createSedaUtils(any())).thenReturn(sedaUtils);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");
    }

    @Test
    @RunWithCustomExecutor
    public void testHandlerWorking() throws ProcessingException {
        handler = new CheckHeaderActionHandler(adminManagementClientFactory, storageClientFactory, sedaUtilsFactory);
        HandlerIOImpl action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> sedaMap = new HashMap<>();
        sedaMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, "AG-0000001");
        sedaMap.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, CONTRACT_NAME);
        sedaMap.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, SedaConstants.TAG_MESSAGE_IDENTIFIER);
        when(sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(any())).thenReturn(sedaUtils);
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertNotNull(CheckHeaderActionHandler.getId());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("false");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
        assertNotNull(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER));
        assertEquals(SedaConstants.TAG_MESSAGE_IDENTIFIER, response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER));

        action.getInput().clear();
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        sedaMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, "");
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertEquals(handler.execute(params, action).getGlobalStatus(), StatusCode.KO);

        action.getInput().clear();
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        sedaMap.remove(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertEquals(handler.execute(params, action).getGlobalStatus(), StatusCode.KO);

        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testHandlerWorkingWithRealManifest() throws Exception {
        handler = new CheckHeaderActionHandler(
            adminManagementClientFactory,
            storageClientFactory,
            SedaUtilsFactory.getInstance()
        );

        HandlerIOImpl action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml"))).thenReturn(
            Response.status(Status.OK).entity(sedaLocal).build()
        );
        assertNotNull(CheckHeaderActionHandler.getId());
        when(handlerIO.getNewLocalFile(any())).thenReturn(temporaryFolder.newFile());
        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath("ANY_PATH"));

        final InputStream sedaParams = new FileInputStream(PropertiesUtils.findFile(SEDA_PARAMS));
        when(workspaceClient.isExistingObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(true);
        when(workspaceClient.getObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(
            Response.status(Status.OK).entity(sedaParams).build()
        );

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("false");

        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        final ItemStatus response = handler.execute(params, action);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER)).isNotNull();
        String evDetData = response.getEvDetailData();
        assertThat(evDetData.contains("ArchivalAgreement0")).isTrue();
        assertThat(evDetData.contains("English Comment")).isTrue();
        assertThat(evDetData.contains("ArchivalProfile0")).isTrue();
        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testHandlerWorkingWithRealManifestAndManagementContract() throws Exception {
        RequestResponseOK<IngestContractModel> ingestResponse = (RequestResponseOK<
                IngestContractModel
            >) new AdminManagementClientMock().findIngestContractsByID("contract");
        ingestResponse.getFirstResult().setManagementContractId("managementContractId");
        when(adminManagementClient.findIngestContractsByID(any())).thenReturn(ingestResponse);
        when(adminManagementClient.findManagementContractsByID(any())).thenReturn(
            ClientMockResultHelper.createResponse(
                (ManagementContractModel) new ManagementContractModel()
                    .setId("managementContractId")
                    .setStatus(ActivationStatus.ACTIVE)
            )
        );

        handler = new CheckHeaderActionHandler(
            adminManagementClientFactory,
            storageClientFactory,
            SedaUtilsFactory.getInstance()
        );

        HandlerIOImpl action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml"))).thenReturn(
            Response.status(Status.OK).entity(sedaLocal).build()
        );
        assertNotNull(CheckHeaderActionHandler.getId());
        when(handlerIO.getNewLocalFile(any())).thenReturn(temporaryFolder.newFile());
        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath("ANY_PATH"));

        final InputStream sedaParams = new FileInputStream(PropertiesUtils.findFile(SEDA_PARAMS));
        when(workspaceClient.isExistingObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(true);
        when(workspaceClient.getObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(
            Response.status(Status.OK).entity(sedaParams).build()
        );

        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("false");

        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        final ItemStatus response = handler.execute(params, action);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER)).isNotNull();
        String evDetData = response.getEvDetailData();
        assertThat(evDetData.contains("ArchivalAgreement0")).isTrue();
        assertThat(evDetData.contains("English Comment")).isTrue();
        assertThat(evDetData.contains("ArchivalProfile0")).isTrue();
        action.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testDefinedProfileInIngestContractButNotInManifest()
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, InvalidParseOperationException {
        handler = new CheckHeaderActionHandler(
            adminManagementClientFactory,
            storageClientFactory,
            SedaUtilsFactory.getInstance()
        );

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(MANIFEST_WITHOUT_ARCHIVAL_PROFILE));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml"))).thenReturn(
            Response.status(Status.OK).entity(sedaLocal).build()
        );

        final InputStream sedaParams = new FileInputStream(PropertiesUtils.findFile(SEDA_PARAMS));
        when(workspaceClient.isExistingObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(true);
        when(workspaceClient.getObject(any(), eq(SEDA_PARAMS_JSON))).thenReturn(
            Response.status(Status.OK).entity(sedaParams).build()
        );

        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(sedaLocal);
        when(handlerIO.getNewLocalFile(any())).thenReturn(temporaryFolder.newFile());
        when(handlerIO.getOutput(anyInt())).thenReturn(new ProcessingUri().setPath("ANY_PATH"));

        assertNotNull(CheckHeaderActionHandler.getId());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());

        HandlerIOImpl action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "contracts.json"));

        final ItemStatus response = handler.execute(params, action);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER)).isNotNull();
        String evDetData = response.getEvDetailData();
        assertThat(evDetData).contains("was not found in the ingest contract");
        assertThat(evDetData).contains("ArchivalAgreement0");
        assertThat(evDetData).contains("English Comment");
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo("DIFF");
        action.partialClose();
    }
}
