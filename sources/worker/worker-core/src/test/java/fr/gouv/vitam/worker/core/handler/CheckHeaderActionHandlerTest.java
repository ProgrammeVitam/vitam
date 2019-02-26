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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
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
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({SedaUtilsFactory.class, WorkspaceClientFactory.class})
public class CheckHeaderActionHandlerTest {
    private static final String CONTRACT_NAME = "ArchivalAgreement0";
    CheckHeaderActionHandler handler = new CheckHeaderActionHandler();
    private SedaUtils sedaUtils;
    private GUID guid;
    private static final Integer TENANT_ID = 0;

    private static final String SIP_ADD_UNIT = "CheckHeaderActionHandler/manifest.xml";
    private static final String MANIFEST_WITHOUT_ARCHIVAL_PROFILE =
        "CheckHeaderActionHandler/manifest_no_archival_profile.xml";
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private final HandlerIO handlerIO = mock(HandlerIO.class);
    private final SedaUtils utils = SedaUtilsFactory.create(handlerIO);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
        guid = GUIDFactory.newGUID();
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

    }

    @Test
    @RunWithCustomExecutor
    public void testHandlerWorking() throws ProcessingException {
        HandlerIOImpl action = new HandlerIOImpl(guid.getId(), "workerId", com.google.common.collect.Lists.newArrayList());
        PowerMockito.when(SedaUtilsFactory.create(action)).thenReturn(sedaUtils);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> sedaMap = new HashMap<>();
        sedaMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, "AG-0000001");
        sedaMap.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, CONTRACT_NAME);
        sedaMap.put(SedaConstants.TAG_MESSAGE_IDENTIFIER, SedaConstants.TAG_MESSAGE_IDENTIFIER);
        AdminManagementClientFactory.changeMode(null);
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertNotNull(CheckHeaderActionHandler.getId());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("false");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "ingestcontract.json"));

        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
        assertNotNull(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER));
        assertEquals(SedaConstants.TAG_MESSAGE_IDENTIFIER,
            response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER));

        action.getInput().clear();
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "ingestcontract.json"));

        sedaMap.put(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER, "");
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertEquals(handler.execute(params, action).getGlobalStatus(), StatusCode.KO);


        action.getInput().clear();
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "ingestcontract.json"));

        sedaMap.remove(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER);
        doReturn(sedaMap).when(sedaUtils).getMandatoryValues(any());
        assertEquals(handler.execute(params, action).getGlobalStatus(), StatusCode.KO);

        action.partialClose();

    }


    @Test
    @RunWithCustomExecutor
    public void testHandlerWorkingWithRealManifest() throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {

        HandlerIOImpl action = new HandlerIOImpl(guid.getId(), "workerId", com.google.common.collect.Lists.newArrayList());
        PowerMockito.when(SedaUtilsFactory.create(action)).thenReturn(utils);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(SIP_ADD_UNIT));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(sedaLocal);

        AdminManagementClientFactory.changeMode(null);
        //Mockito.doCallRealMethod().when(utils).getMandatoryValues(any());
        assertNotNull(CheckHeaderActionHandler.getId());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("false");

        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "ingestcontract.json"));
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
        assertNotNull(response.getData(SedaConstants.TAG_MESSAGE_IDENTIFIER));
        String evDetData = response.getEvDetailData();
        assertTrue(evDetData.contains("ArchivalAgreement0"));
        assertTrue(evDetData.contains("English Comment"));
        assertTrue(evDetData.contains("ArchivalProfile0"));
        action.partialClose();

    }


    @Test
    @RunWithCustomExecutor
    public void testDefinedProfileInIngestContractButNotInManifest() throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {

        HandlerIOImpl action = new HandlerIOImpl(guid.getId(), "workerId", com.google.common.collect.Lists.newArrayList());
        PowerMockito.when(SedaUtilsFactory.create(action)).thenReturn(utils);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile(MANIFEST_WITHOUT_ARCHIVAL_PROFILE));
        when(workspaceClient.getObject(any(), eq("SIP/manifest.xml")))
            .thenReturn(Response.status(Status.OK).entity(sedaLocal).build());
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(sedaLocal);

        AdminManagementClientFactory.changeMode(null);
        assertNotNull(CheckHeaderActionHandler.getId());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        action.getInput().add("true");
        action.getInput().add("true");
        action.getInput().add("true");
        action.getOutput().add(new ProcessingUri(UriPrefix.WORKSPACE, "ingestcontract.json"));

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
