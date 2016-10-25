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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class ExtractSedaActionHandlerTest {
    private static final String TMP_TESTS = "/tmp/tests";
    ExtractSedaActionHandler handler = new ExtractSedaActionHandler();
    private static final String HANDLER_ID = "CHECK_CONSISTENCY";
    private static final String SIP_ARBORESCENCE = "SIP_Arborescence.xml";
    private WorkspaceClient workspaceClient;
    private final InputStream seda_arborescence;
    private HandlerIO action;

    public ExtractSedaActionHandlerTest() throws FileNotFoundException {
        seda_arborescence =
            PropertiesUtils.getResourceAsStream(SIP_ARBORESCENCE);
    }

    @Before
    public void setUp() throws URISyntaxException {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        action = new HandlerIO("");
        for (int i = 0; i < ExtractSedaActionHandler.HANDLER_IO_PARAMETER_NUMBER; i++) {
            action.addOutput(TMP_TESTS);
        }
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        assertNotNull(ExtractSedaActionHandler.getId());
        assertEquals(ExtractSedaActionHandler.getId(), HANDLER_ID);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Ignore
    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        assertNotNull(ExtractSedaActionHandler.getId());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(seda_arborescence);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);

        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenSipWithBdoWithoutGoWhenReadSipThenDetectBdoWithoutGo()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok1.xml"));

        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(sedaLocal);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok2.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(sedaLocal);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenSipFctTestWithBdoWithGoWithArchiveUnitReferenceBDOWhenReadSipThenThrowException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok3-listBDO.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(sedaLocal);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenSipWithBdoWithGoWithArchiveUnitNotReferenceGoWhenReadSipThenReadSuccess()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-ok4.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(sedaLocal);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenSipWithBdoWithGoWithArchiveUnitReferenceGoWhenReadSipThenThrowException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        FileNotFoundException {

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

        final InputStream sedaLocal = new FileInputStream(PropertiesUtils.findFile("sip-bdo-orphan-err2.xml"));
        when(workspaceClient.getObject(anyObject(), eq("SIP/manifest.xml"))).thenReturn(sedaLocal);
        PowerMockito.when(WorkspaceClientFactory.create(Matchers.anyObject())).thenReturn(workspaceClient);
        final CompositeItemStatus response = handler.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

}
