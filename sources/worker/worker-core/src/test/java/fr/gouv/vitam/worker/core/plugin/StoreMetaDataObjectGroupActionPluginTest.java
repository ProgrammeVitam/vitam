package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class, StorageClientFactory.class})
public class StoreMetaDataObjectGroupActionPluginTest {

    private static final String METDATA_OG_RESPONSE_JSON =
        "storeMetadataObjectGroupPlugin/MetadataObjectGroupResponse.json";
    private static final String CONTAINER_NAME = "aebaaaaaaaag3r3cabgjaak2izdlnwiaaaaq";
    private static final String OG_GUID = "aebaaaaaaaag3r7caarvuak2ij3chpyaaaaq";
    private static final String OG_GUID_2 = "aebaaaaaaaakwtamaaxakak32oqku2qaaaaq";
    private static final String OBJECT_GROUP =
        "storeMetadataObjectGroupPlugin/aebaaaaaaaag3r7caarvuak2ij3chpyaaaaq.json";
    private static final String OBJECT_GROUP_2 =
        "storeMetadataObjectGroupPlugin/aebaaaaaaaakwtamaaxakak32oqku2qaaaaq.json";
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClient metadataClient;
    private StorageClient storageClient;
    private HandlerIOImpl action;
    private final InputStream objectGroup;
    private final InputStream objectGroup2;
    private final JsonNode oGResponse;

    private StorageClientFactory storageClientFactory;


    private StoreMetaDataObjectGroupActionPlugin plugin;

    public StoreMetaDataObjectGroupActionPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup2 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_2);
        File file = PropertiesUtils.getResourceFile(METDATA_OG_RESPONSE_JSON);
        oGResponse = JsonHandler.getFromFile(file);
    }


    @Before
    public void setUp() throws Exception {
        LogbookOperationsClientFactory.changeMode(null);
        LogbookLifeCyclesClientFactory.changeMode(null);
        workspaceClient = mock(WorkspaceClient.class);
        metadataClient = mock(MetaDataClient.class);
        storageClient = mock(StorageClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        PowerMockito.mockStatic(StorageClientFactory.class);
        // workspace client
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        // metadata client
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        // storage client
        storageClientFactory = PowerMockito.mock(StorageClientFactory.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);

        action = new HandlerIOImpl(CONTAINER_NAME, "workerId");
    }

    @After
    public void clean() {
        action.partialClose();
    }



    @Test
    public void givenMetadataClientWhensearchOGrThenReturnNull() throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OG_GUID + ".json"))
                .setObjectName(OG_GUID + ".json").setCurrentStep("Store objectGroup");

        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void givenMetadataClientAndWorkspaceResponsesWhenSearchOGThenReturnOK() throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OG_GUID + ".json"))
                .setObjectName(OG_GUID + ".json").setCurrentStep("Store objectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);

        SelectMultiQuery query = new SelectMultiQuery();
        ObjectNode constructQuery = query.getFinalSelect();

        when(metadataClient.selectObjectGrouptbyId(constructQuery, OG_GUID))
            .thenReturn(oGResponse);

        when(workspaceClient.getObject(CONTAINER_NAME,
            DataCategory.OBJECT_GROUP.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);
        when(storageClient.storeFileFromDatabase(anyObject(), anyObject(), anyObject()))
            .thenReturn(getStoredInfoResult());
        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenMetadataClientAndWorkspaceResponsesAdnPdosWhenSearchOGThenReturnOK() throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083").setObjectNameList(Lists.newArrayList(OG_GUID_2 + ".json"))
                .setObjectName(OG_GUID_2 + ".json")
                .setCurrentStep("Store objectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);

        SelectMultiQuery query = new SelectMultiQuery();
        ObjectNode constructQuery = query.getFinalSelect();

        when(metadataClient.selectObjectGrouptbyId(constructQuery, OG_GUID_2)).thenReturn(oGResponse);

        when(workspaceClient.getObject(CONTAINER_NAME, DataCategory.OBJECT_GROUP.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Status.OK).entity(objectGroup2).build());

        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);

        when(storageClient.storeFileFromDatabase(anyObject(), anyObject(), anyObject()))
            .thenReturn(getStoredInfoResult());

        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    private StoredInfoResult getStoredInfoResult() {
        StoredInfoResult result = new StoredInfoResult();
        result.setNbCopy(1).setCreationTime(LocalDateUtil.now().toString()).setId("id")
            .setLastAccessTime(LocalDateUtil.now().toString()).setLastModifiedTime(LocalDateUtil.now().toString())
            .setObjectGroupId("id").setOfferIds(Arrays.asList("id1")).setStrategy("default");
        return result;
    }

    @Test
    public void givenMetadataClientWhensearchOGThenThrowsException() throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083").setObjectNameList(Lists.newArrayList(OG_GUID + ".json"))
                .setObjectName(OG_GUID + ".json").setCurrentStep("Store ObjectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);

        Mockito.doThrow(new MetaDataExecutionException("Error Metadata")).when(metadataClient)
            .selectObjectGrouptbyId(anyObject(), anyObject());
        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }



    @Test
    public void givenStorageClientWhenStoreFromWorkspaceThenThrowStorageNotFoundClientExceptionThenFATAL()
        throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OG_GUID + ".json"))
                .setObjectName(OG_GUID + ".json").setCurrentStep("Store unit");

        SelectMultiQuery query = new SelectMultiQuery();
        ObjectNode constructQuery = query.getFinalSelect();

        when(metadataClient.selectObjectGrouptbyId(constructQuery, OG_GUID))
            .thenReturn(oGResponse);

        when(workspaceClient.getObject(CONTAINER_NAME,
            DataCategory.OBJECT_GROUP.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        Mockito.doThrow(new StorageNotFoundClientException("Error Metadata")).when(storageClient)
            .storeFileFromDatabase(anyObject(), anyObject(), anyObject());

        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }


    @Test
    public void givenStorageClientWhenStoreFromWorkspaceThenThrowStorageAlreadyExistsClientExceptionThenKO()
        throws Exception {
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OG_GUID + ".json"))
                .setObjectName(OG_GUID + ".json").setCurrentStep("Store unit");

        SelectMultiQuery query = new SelectMultiQuery();
        ObjectNode constructQuery = query.getFinalSelect();

        when(metadataClient.selectObjectGrouptbyId(constructQuery, OG_GUID))
            .thenReturn(oGResponse);

        when(workspaceClient.getObject(CONTAINER_NAME,
            DataCategory.OBJECT_GROUP.name() + "/" + params.getObjectName()))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        Mockito.doThrow(new StorageAlreadyExistsClientException("Error Metadata ")).when(storageClient)
            .storeFileFromDatabase(anyObject(), anyObject(), anyObject());

        plugin = new StoreMetaDataObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }


}
