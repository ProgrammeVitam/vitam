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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientRest;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.api.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class})
public class IndexObjectGroupActionHandlerTest {

    IndexObjectGroupActionHandler handler;
    private WorkspaceClient workspaceClient;
    private MetaDataClient metadataClient;
    private static final String HANDLER_ID = "OG_METADATA_INDEXATION";
    private static final String OBJECT_GROUP = "objectGroup.json";
    private final InputStream objectGroup;
    private WorkspaceClientFactory workspaceClientFactory;        
    final HandlerIOImpl handlerIO = new HandlerIOImpl("IndexObjectGroupActionHandlerTest", "workerId");

    public IndexObjectGroupActionHandlerTest() throws FileNotFoundException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
    }
    @Before
    public void setUp() throws URISyntaxException {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        metadataClient = mock(MetaDataClientRest.class);
    }

    @After
    public void clean() {
        handlerIO.close();
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseWARNING()
        throws Exception {
        handler = new IndexObjectGroupActionHandler();
        WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);
        MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        assertEquals(IndexObjectGroupActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083").setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("IndexObjectGroupActionHandlerTest");
        final CompositeItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenReturn("");
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(objectGroup);
        WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);
        MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        handler = new IndexObjectGroupActionHandler();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083").setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("IndexObjectGroupActionHandlerTest");
        final CompositeItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void testMetadataException()
        throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenThrow(new MetaDataExecutionException(""));

        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(objectGroup);
        WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);
        MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        handler = new IndexObjectGroupActionHandler();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083").setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("IndexObjectGroupActionHandlerTest");
        final CompositeItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void testMetadataParseException()
        throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenThrow(new InvalidParseOperationException(""));

        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(objectGroup);
        WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);
        MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        handler = new IndexObjectGroupActionHandler();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083").setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("IndexObjectGroupActionHandlerTest");
        final CompositeItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }

    @Test
    public void testWorkspaceException()
        throws Exception {
        when(metadataClient.insertObjectGroup(anyObject())).thenReturn("");

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);
        MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        handler = new IndexObjectGroupActionHandler();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083").setUrlMetadata("http://localhost:8083")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("IndexObjectGroupActionHandlerTest");
        final CompositeItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
    }
}
