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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StoreObjectGroupActionPluginTest {

    private static final String CONTAINER_NAME = "aeaaaaaaaaaaaaabaa4quakwgip7nuaaaaaq";
    private static final String OBJECT_GROUP_GUID = "aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq";
    private static final String OBJECT_GROUP_GUID_2 = "aebaaaaaaaakwtamaaxakak32oqku2qaaaaq";
    StoreObjectGroupActionPlugin plugin;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClient metaDataClient;
    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;

    private HandlerIOImpl action;
    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_2 = "storeObjectGroupHandler/aebaaaaaaaakwtamaaxakak32oqku2qaaaaq.json";
    private final InputStream objectGroup;
    private final InputStream objectGroup2;
    private List<IOParameter> out;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public StoreObjectGroupActionPluginTest() throws FileNotFoundException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup2 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_2);
    }

    @Before
    public void setUp() throws Exception {
        // clients
        workspaceClient = mock(WorkspaceClient.class);
        metaDataClient = mock(MetaDataClient.class);
        storageClient = mock(StorageClient.class);
        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);

        // workspace client
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        storageClientFactory = mock(StorageClientFactory.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            CONTAINER_NAME,
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        action.addOutIOParameters(out);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenWorkspaceErrorWhenExecuteThenReturnResponseFATAL() throws Exception {
        final WorkerParameters paramsObjectGroups = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID + ".json"))
            .setObjectName(OBJECT_GROUP_GUID + ".json")
            .setCurrentStep("Store ObjectGroup");

        when(
            workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json")
        ).thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        doThrow(new StorageServerClientException("Error storage"))
            .when(storageClient)
            .bulkStoreFilesFromWorkspace(any(), any());

        plugin = new StoreObjectGroupActionPlugin(storageClientFactory);

        final List<ItemStatus> response = plugin.executeList(paramsObjectGroups, action);
        assertEquals(StatusCode.FATAL, response.get(0).getGlobalStatus());
        verify(workspaceClient, never()).putObject(any(), any(), any());
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        final WorkerParameters paramsObjectGroups = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID + ".json"))
            .setObjectName(OBJECT_GROUP_GUID + ".json")
            .setCurrentStep("Store ObjectGroup");

        when(
            workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json")
        ).thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        doReturn(
            getStorageResult(
                "aeaaaaaaaaaam7myaaaamakxfgivurqaaaaq",
                "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804"
            )
        )
            .when(storageClient)
            .bulkStoreFilesFromWorkspace(any(), any());

        plugin = new StoreObjectGroupActionPlugin(storageClientFactory);

        final List<ItemStatus> response = plugin.executeList(paramsObjectGroups, action);
        assertEquals(StatusCode.OK, response.get(0).getGlobalStatus());
        verify(workspaceClient).putObject(
            eq(CONTAINER_NAME),
            eq("ObjectGroup/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json"),
            any()
        );
    }

    @Test
    public void givenWorkspaceExistAndPdoWhenExecuteThenReturnResponseOK() throws Exception {
        final WorkerParameters paramsObjectGroups = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID_2 + ".json"))
            .setObjectName(OBJECT_GROUP_GUID_2 + ".json")
            .setCurrentStep("Store ObjectGroup");

        when(workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/" + OBJECT_GROUP_GUID_2 + ".json")).thenReturn(
            Response.status(Status.OK).entity(objectGroup2).build()
        );

        doReturn(
            getStorageResult(
                "aeaaaaaaaaakwtamaaxakak32oqku2iaaaaq",
                "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7"
            )
        )
            .when(storageClient)
            .bulkStoreFilesFromWorkspace(any(), any());

        plugin = new StoreObjectGroupActionPlugin(storageClientFactory);
        saveWorkspacePutObject();

        final List<ItemStatus> response = plugin.executeList(paramsObjectGroups, action);
        assertEquals(StatusCode.OK, response.get(0).getGlobalStatus());

        ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(workspaceClient).putObject(
            anyString(),
            ArgumentMatchers.startsWith("ObjectGroup/"),
            any(InputStream.class)
        );
        verify(workspaceClient, atLeastOnce()).putObject(
            anyString(),
            fileNameArgumentCaptor.capture(),
            any(InputStream.class)
        );
        String gotFilename = fileNameArgumentCaptor
            .getAllValues()
            .stream()
            .filter(str -> str.startsWith("ObjectGroup/"))
            .findFirst()
            .get();
        JsonNode gotJson = getSavedWorkspaceObject(gotFilename);
        assertNotNull(gotJson);
        final JsonPointer storagePointer = JsonPointer.compile("/_qualifiers/1/versions/0/_storage");
        final JsonPointer storageStrategyPointer = JsonPointer.compile("/_qualifiers/1/versions/0/_storage/strategyId");
        final JsonPointer storageOfferIdPointer = JsonPointer.compile("/_qualifiers/1/versions/0/_storage/offerIds");
        assertTrue(gotJson.at(storagePointer).isObject());
        assertEquals("default", gotJson.at(storageStrategyPointer).asText());
        assertTrue(gotJson.at(storageOfferIdPointer).isMissingNode());
    }

    private BulkObjectStoreResponse getStorageResult(String objectId, String objectDigest) {
        final BulkObjectStoreResponse storedInfoResult = new BulkObjectStoreResponse();
        storedInfoResult.setOfferIds(Arrays.asList("idoffer1"));
        Map<String, String> objectDigests = Maps.newHashMap();
        objectDigests.put(objectId, objectDigest);
        storedInfoResult.setObjectDigests(objectDigests);
        return storedInfoResult;
    }

    private void saveWorkspacePutObject() throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            String filename = invocation.getArgument(1);
            InputStream inputStream = invocation.getArgument(2);
            Path filePath = Paths.get(folder.getRoot().getAbsolutePath(), filename);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath);
            return null;
        })
            .when(workspaceClient)
            .putObject(anyString(), anyString(), any(InputStream.class));
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        Path filePath = Paths.get(folder.getRoot().getAbsolutePath(), filename);
        return JsonHandler.getFromFile(filePath.toFile());
    }
}
