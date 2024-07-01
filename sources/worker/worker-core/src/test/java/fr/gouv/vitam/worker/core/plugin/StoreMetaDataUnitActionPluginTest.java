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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreMetaDataUnitActionPluginTest {

    private static final String CONTAINER_NAME = "aebaaaaaaaag3r7cabf4aak2izdlnwiaaaaq";

    private static final String UNIT_GUID = "aeaqaaaaaaag3r7cabf4aak2izdloiiaaaaq";
    private static final String UNIT_GUID_2 = "aeaqaaaaamhbbettabfs4ali3tyuitiaaabq";

    private static final String LFC_UNIT = "storeMetadataUnitPlugin/aeaqaaaaaaag3r7cabf4aak2izdloiiaaaaq_lfc.json";
    private static final String LFC_UNIT_2 = "storeMetadataUnitPlugin/aeaqaaaaamhbbettabfs4ali3tyuitiaaabq_lfc.json";

    private static final String UNIT_MD = "storeMetadataUnitPlugin/aeaqaaaaaaag3r7cabf4aak2izdloiiaaaaq_md.json";
    private static final String UNIT_MD_2 = "storeMetadataUnitPlugin/aeaqaaaaamhbbettabfs4ali3tyuitiaaabq_md.json";

    private static final String UNIT_LFC_WITH_MD_1 =
        "storeMetadataUnitPlugin/aeaqaaaaaaag3r7cabf4aak2izdloiiaaaaq_md_with_lfc.json";
    private static final String UNIT_LFC_WITH_MD_2 =
        "storeMetadataUnitPlugin/aeaqaaaaamhbbettabfs4ali3tyuitiaaabq_md_with_lfc.json";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClient metaDataClient;
    private MetaDataClientFactory metaDataClientFactory;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;
    private HandlerIOImpl action;

    private final JsonNode lfcUnit1;
    private final JsonNode lfcUnit2;
    private JsonNode rawUnit1;
    private JsonNode rawUnit2;

    private File unitWithLfc1;
    private File unitWithLfc2;

    private StoreMetaDataUnitActionPlugin plugin;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public StoreMetaDataUnitActionPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        rawUnit1 = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UNIT_MD));
        rawUnit2 = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UNIT_MD_2));

        lfcUnit1 = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LFC_UNIT));
        lfcUnit2 = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LFC_UNIT_2));

        unitWithLfc1 = PropertiesUtils.getResourceFile(UNIT_LFC_WITH_MD_1);
        unitWithLfc2 = PropertiesUtils.getResourceFile(UNIT_LFC_WITH_MD_2);
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

        doAnswer(args -> {
            String container = args.getArgument(0);
            String filename = args.getArgument(1);
            InputStream is = args.getArgument(2);
            FileUtils.copyToFile(is, new File(temporaryFolder.getRoot(), container + "/" + filename));
            return null;
        })
            .when(workspaceClient)
            .putObject(any(), any(), any());

        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            CONTAINER_NAME,
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenMetadataClientWhenSearchUnitThenReturnNotFound() throws Exception {
        // Given
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Arrays.asList(UNIT_GUID + ".json", UNIT_GUID_2 + ".json"));

        when(metaDataClient.getUnitsByIdsRaw(eq(Arrays.asList(UNIT_GUID, UNIT_GUID_2)))).thenReturn(
            VitamCodeHelper.toVitamError(VitamCode.METADATA_NOT_FOUND, "not found")
        );
        plugin = new StoreMetaDataUnitActionPlugin(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        // When
        final List<ItemStatus> response = plugin.executeList(params, action);

        // Then
        checkItemStatus(response, StatusCode.FATAL);
    }

    @Test
    public void givenMetadataClientAndLogbookLifeCycleClientAndWorkspaceResponsesWhenSearchUnitThenReturnOK()
        throws Exception {
        // Given
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Arrays.asList(UNIT_GUID + ".json", UNIT_GUID_2 + ".json"));

        when(metaDataClient.getUnitsByIdsRaw(eq(Arrays.asList(UNIT_GUID, UNIT_GUID_2)))).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(rawUnit1).addResult(rawUnit2)
        );

        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(Arrays.asList(UNIT_GUID, UNIT_GUID_2))).thenReturn(
            Arrays.asList(lfcUnit1, lfcUnit2)
        );

        when(storageClient.bulkStoreFilesFromWorkspace(eq("default-unit-fake"), any())).thenReturn(
            new BulkObjectStoreResponse(
                Arrays.asList("offer1", "offer2"),
                DigestType.SHA512.getName(),
                ImmutableMap.of(UNIT_GUID + ".json", "digest1", UNIT_GUID_2, "digest2")
            )
        );

        when(storageClient.bulkStoreFilesFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), any())).thenThrow(
            new StorageServerClientException("wrong strategy")
        );

        plugin = new StoreMetaDataUnitActionPlugin(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        // When
        List<ItemStatus> response = plugin.executeList(params, action);

        // Then
        checkItemStatus(response, StatusCode.OK);

        JsonNode unitWithLfc1CreatedFile = JsonHandler.getFromFile(
            getSavedFile(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + UNIT_GUID + ".json")
        );
        JsonNode unitWithLfc1MockFile = JsonHandler.getFromFile(unitWithLfc1);
        JsonNode unitWithLfc2CreatedFile = JsonHandler.getFromFile(
            getSavedFile(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + UNIT_GUID_2 + ".json")
        );
        JsonNode unitWithLfc2MockFile = JsonHandler.getFromFile(unitWithLfc2);
        assertThat(unitWithLfc1CreatedFile).isEqualTo(unitWithLfc1MockFile);
        assertThat(unitWithLfc2CreatedFile).isEqualTo(unitWithLfc2MockFile);
    }

    private File getSavedFile(String filename) {
        return new File(temporaryFolder.getRoot(), CONTAINER_NAME + "/" + filename);
    }

    @Test
    public void givenMetadataClientWhensearchUnitThenThrowsException() throws Exception {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Arrays.asList(UNIT_GUID + ".json", UNIT_GUID_2 + ".json"));

        doThrow(new VitamClientException("Error Metadata"))
            .when(metaDataClient)
            .getUnitsByIdsRaw(eq(Arrays.asList(UNIT_GUID, UNIT_GUID_2)));

        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(Arrays.asList(UNIT_GUID, UNIT_GUID_2))).thenReturn(
            Arrays.asList(lfcUnit1, lfcUnit2)
        );

        plugin = new StoreMetaDataUnitActionPlugin(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        // When
        final List<ItemStatus> response = plugin.executeList(params, action);

        // Then
        checkItemStatus(response, StatusCode.FATAL);
    }

    @Test
    public void givenLogbookLifeCycleClientWhenSearchLfcThenThrowsException() throws Exception {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Arrays.asList(UNIT_GUID + ".json", UNIT_GUID_2 + ".json"));

        when(metaDataClient.getUnitsByIdsRaw(eq(Arrays.asList(UNIT_GUID, UNIT_GUID_2)))).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(rawUnit1).addResult(rawUnit2)
        );

        doThrow(new LogbookClientException("Error Logbook"))
            .when(logbookLifeCyclesClient)
            .getRawUnitLifeCycleByIds(Arrays.asList(UNIT_GUID, UNIT_GUID_2));

        plugin = new StoreMetaDataUnitActionPlugin(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        // When
        final List<ItemStatus> response = plugin.executeList(params, action);

        // Then
        checkItemStatus(response, StatusCode.FATAL);
    }

    @Test
    public void givenStorageClientWhenStoreFromWorkspaceThenThrowStorageNotFoundClientExceptionThenFATAL()
        throws Exception {
        // Given
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(CONTAINER_NAME)
            .setUrlMetadata("http://localhost:8083")
            .setUrlWorkspace("http://localhost:8083")
            .setObjectNameList(Arrays.asList(UNIT_GUID + ".json", UNIT_GUID_2 + ".json"));

        when(metaDataClient.getUnitsByIdsRaw(eq(Arrays.asList(UNIT_GUID, UNIT_GUID_2)))).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(rawUnit1).addResult(rawUnit2)
        );

        when(logbookLifeCyclesClient.getRawUnitLifeCycleByIds(Arrays.asList(UNIT_GUID, UNIT_GUID_2))).thenReturn(
            Arrays.asList(lfcUnit1, lfcUnit2)
        );

        doThrow(new StorageAlreadyExistsClientException("Error Metadata"))
            .when(storageClient)
            .bulkStoreFilesFromWorkspace(any(), any());

        plugin = new StoreMetaDataUnitActionPlugin(
            metaDataClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        // When
        final List<ItemStatus> response = plugin.executeList(params, action);

        // Then
        checkItemStatus(response, StatusCode.FATAL);
    }

    private void checkItemStatus(List<ItemStatus> response, StatusCode ok) {
        assertThat(response).hasSize(2);
        for (ItemStatus itemStatus : response) {
            assertThat(itemStatus.getGlobalStatus()).isSameAs(ok);
        }
    }
}
