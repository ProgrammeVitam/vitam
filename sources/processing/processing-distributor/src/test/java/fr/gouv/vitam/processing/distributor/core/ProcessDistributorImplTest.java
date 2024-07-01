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
package fr.gouv.vitam.processing.distributor.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Distribution;
import fr.gouv.vitam.common.model.processing.DistributionKind;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.common.model.processing.PauseOrCancelAction;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.AsyncResourceCallback;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static fr.gouv.vitam.common.GlobalDataRest.X_CHUNK_LENGTH;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTENT_LENGTH;
import static fr.gouv.vitam.processing.distributor.api.ProcessDistributor.JSON_EXTENSION;
import static fr.gouv.vitam.processing.distributor.api.ProcessDistributor.OBJECTS_LIST_EMPTY;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProcessDistributorImplTest {

    private static final String FAKE_REQUEST_ID = "FakeRequestId";
    private static final String FAKE_CONTEXT_ID = "FakeContextId";

    private static final String FILE_FULL_GUIDS = "file_full_guids.jsonl";
    private static final String FILE_WITH_GUIDS = "file_with_guids.jsonl";
    private static final String FILE_GUIDS_INVALID = "file_guids_invalid.jsonl";
    private static final String FILE_EMPTY_GUIDS = "file_empty_guids.jsonl";
    private static final String operationId = "FakeOperationId";
    private static final Integer TENANT = 0;
    private static final String FAKE_UUID = "c938e5c2-66fe-443f-8e93-96ee86d13b6a";
    private final WorkerClientFactory workerClientFactory = mock(WorkerClientFactory.class);
    private final WorkerClient workerClient = mock(WorkerClient.class);

    @Rule
    public TempFolderRule testFolder = new TempFolderRule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private ProcessDataManagement processDataManagement;
    private AsyncResourcesMonitor asyncResourcesMonitor;
    private AsyncResourceCleaner asyncResourceCleaner;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClientFactory metaDataClientFactory;
    private IWorkerManager workerManager;
    private WorkerParameters workerParameters;
    private WorkspaceClient workspaceClient;
    private MetaDataClient metaDataClient;
    private ProcessDistributorImpl processDistributor;
    private ServerConfiguration serverConfiguration;

    @BeforeClass
    public static void tearUpBeforeClass() {
        VitamConfiguration.setWorkerBulkSize(1);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        VitamConfiguration.setWorkerBulkSize(16);
        VitamConfiguration.setDistributeurBatchSize(800);
    }

    @Before
    public void setUp() throws Exception {
        final WorkerBean workerBean = new WorkerBean(
            "DefaultWorker",
            "DefaultWorker",
            2,
            "status",
            new WorkerRemoteConfiguration("localhost", 8999)
        );
        workerBean.setWorkerId("FakeWorkerId");

        workerManager = new WorkerManager(workerClientFactory);
        workerManager.registerWorker(workerBean);

        when(workerClientFactory.getClient()).thenReturn(workerClient);
        workerParameters = WorkerParametersFactory.newWorkerParameters();
        workerParameters.setWorkerGUID(GUIDFactory.newGUID().getId());
        workerParameters.setContainerName(operationId);
        workerParameters.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        processDataManagement = mock(ProcessDataManagement.class);
        asyncResourcesMonitor = mock(AsyncResourcesMonitor.class);
        asyncResourceCleaner = mock(AsyncResourceCleaner.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);

        workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(workerClient.submitStep(any())).thenAnswer(invocation -> getMockedItemStatus(StatusCode.OK));
        metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);

        VitamConfiguration.setDistributeurBatchSize(20);
        serverConfiguration = new ServerConfiguration()
            .setMaxDistributionInMemoryBufferSize(100_000)
            .setMaxDistributionOnDiskBufferSize(100_000_000);

        processDistributor = new ProcessDistributorImpl(
            workerManager,
            asyncResourcesMonitor,
            asyncResourceCleaner,
            serverConfiguration,
            processDataManagement,
            workspaceClientFactory,
            metaDataClientFactory,
            workerClientFactory
        );

        if (Thread.currentThread() instanceof VitamThreadFactory.VitamThread) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT);
            VitamThreadUtils.getVitamSession().setRequestId(FAKE_REQUEST_ID);
            VitamThreadUtils.getVitamSession().setContextId(FAKE_CONTEXT_ID);
        }
    }

    ItemStatus getMockedItemStatus(StatusCode statusCode) {
        return getMockedItemStatus(statusCode, 1);
    }

    ItemStatus getMockedItemStatus(StatusCode statusCode, int times) {
        return new ItemStatus("StepId").setItemsStatus(
            "ItemId",
            new ItemStatus("ItemId").setMessage("message").increment(statusCode, times)
        );
    }

    /**
     * Test the constructor
     */
    @Test
    public void testConstructor() {
        ServerConfiguration configuration = new ServerConfiguration()
            .setMaxDistributionInMemoryBufferSize(100_000)
            .setMaxDistributionOnDiskBufferSize(100_000_000);

        new ProcessDistributorImpl(workerManager, asyncResourcesMonitor, asyncResourceCleaner, configuration);

        try {
            new ProcessDistributorImpl(null, asyncResourcesMonitor, asyncResourceCleaner, configuration);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(workerManager, null, asyncResourceCleaner, configuration);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(workerManager, asyncResourcesMonitor, null, configuration);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                null,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration,
                processDataManagement,
                workspaceClientFactory,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                null,
                asyncResourceCleaner,
                configuration,
                processDataManagement,
                workspaceClientFactory,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                null,
                configuration,
                processDataManagement,
                workspaceClientFactory,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                null,
                processDataManagement,
                workspaceClientFactory,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration,
                null,
                workspaceClientFactory,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration,
                processDataManagement,
                null,
                metaDataClientFactory,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration,
                processDataManagement,
                workspaceClientFactory,
                null,
                workerClientFactory
            );
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration,
                processDataManagement,
                workspaceClientFactory,
                metaDataClientFactory,
                null
            );
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            fail("Should not throw an exception");
        }
    }

    private ProcessStep getStep(DistributionKind distributionKind, String distributorElement) {
        return getStep(distributionKind, distributorElement, null);
    }

    private ProcessStep getStep(DistributionKind distributionKind, String distributorElement, Integer bulkSize) {
        final Distribution distribution = new Distribution();
        distribution.setKind(distributionKind);
        distribution.setElement(distributorElement);
        distribution.setType(DistributionType.Units);
        distribution.setBulkSize(bulkSize);

        final Step step = new Step();
        step.setStepName("FakeStepName");
        step.setDistribution(distribution);
        step.setBehavior(ProcessBehavior.NOBLOCKING);
        return new ProcessStep(step, new AtomicLong(0), new AtomicLong(0), "FakeStepId");
    }

    /**
     * Test parameter required for the method distribute
     */
    @Test
    @RunWithCustomExecutor
    public void whenDistributeRequiredParametersThenOK() {
        final ProcessDistributor processDistributor = new ProcessDistributorImpl(
            workerManager,
            asyncResourcesMonitor,
            asyncResourceCleaner,
            serverConfiguration,
            processDataManagement,
            workspaceClientFactory,
            metaDataClientFactory,
            workerClientFactory
        );

        try {
            processDistributor.distribute(null, getStep(DistributionKind.REF, "manifest.xml"), operationId);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            processDistributor.distribute(workerParameters, null, operationId);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            processDistributor.distribute(workerParameters, getStep(DistributionKind.REF, "manifest.xml"), null);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeManifestThenOK() {
        final ProcessDistributor processDistributor = new ProcessDistributorImpl(
            workerManager,
            asyncResourcesMonitor,
            asyncResourceCleaner,
            serverConfiguration,
            processDataManagement,
            workspaceClientFactory,
            metaDataClientFactory,
            workerClientFactory
        );

        ProcessStep step = getStep(DistributionKind.REF, "manifest.xml");
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertNotNull(itemStatus);
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
        assertThat(step.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_COMPLETE);
    }

    @Test
    @RunWithCustomExecutor
    public void givenMetaDataDownWhenDistributeManifestThenFATAL() throws MetaDataClientServerException {
        when(metaDataClient.refreshUnits()).thenThrow(new MetaDataClientServerException(""));
        ProcessStep step = getStep(DistributionKind.REF, "manifest.xml");
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void givenWorkspaceDownWhenDistributeManifestThenFATAL()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        when(workspaceClient.getObject(anyString(), anyString(), anyLong(), anyLong())).thenThrow(
            new ContentAddressableStorageNotFoundException("")
        );
        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, "manifest.xml");
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void giveWorkerItemStatusResponseFatalWhenDistributeManifestThenFATAL()
        throws WorkerNotFoundClientException, WorkerServerClientException, ProcessingRetryAsyncException {
        when(workerClient.submitStep(any())).thenAnswer(invocation -> getMockedItemStatus(StatusCode.FATAL));
        ProcessStep step = getStep(DistributionKind.REF, "manifest.xml");
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(step.getPauseOrCancelAction()).isEqualTo(PauseOrCancelAction.ACTION_RUN);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelOK() throws Exception {
        int numberOfObjectInIngestLevelStack = 170;
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);

        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());

        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are OK
        assertEquals(
            (int) imap.get("ItemId").getStatusMeter().get(StatusCode.OK.getStatusLevel()),
            numberOfObjectInIngestLevelStack
        );
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionEmptyListWithLevelOK() throws Exception {
        int numberOfObjectInIngestLevelStack = 170;
        final File fileContracts = PropertiesUtils.getResourceFile("emptyIngestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);

        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelKO() throws Exception {
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);
            if (descriptionStep.getWorkParams().getObjectNameList().iterator().next().equals("aaa1.json")) {
                //throw new RuntimeException("Exception While Executing aaa1");
                return getMockedItemStatus(StatusCode.KO);
            }
            return getMockedItemStatus(StatusCode.OK);
        });

        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertEquals(StatusCode.KO, itemStatus.getGlobalStatus());
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are KO
        assertEquals(1, (int) imap.get("ItemId").getStatusMeter().get(StatusCode.KO.getStatusLevel()));
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelWARNING() throws Exception {
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);
            if (descriptionStep.getWorkParams().getObjectNameList().iterator().next().equals("aaa1.json")) {
                //throw new RuntimeException("Exception While Executing aaa1");
                return getMockedItemStatus(StatusCode.WARNING);
            }
            return getMockedItemStatus(StatusCode.OK);
        });

        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertEquals(StatusCode.WARNING, itemStatus.getGlobalStatus());
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are WARNING
        assertEquals(1, (int) imap.get("ItemId").getStatusMeter().get(StatusCode.WARNING.getStatusLevel()));
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelFATAL() throws Exception {
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        when(workerClient.submitStep(any())).thenThrow(new RuntimeException("WorkerException"));

        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);
        assertNotNull(itemStatus);
        assertEquals(StatusCode.FATAL, itemStatus.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindLargeFileOK() throws Exception {
        File file = PropertiesUtils.getResourceFile(FILE_WITH_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_WITH_GUIDS);

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_WITH_GUIDS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertNotNull(itemStatus);
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindFullLargeFileOK() throws Exception {
        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", FILE_FULL_GUIDS);

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertThat(itemStatus).isNotNull();

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnStreamWithUnavailableAsyncResourcesThenAwaitResourceAvailableAndContinue()
        throws Exception {
        // Given
        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", FILE_FULL_GUIDS);

        // bulk0, bulk2 & bulk5 will require access requests
        List<List<String>> bulks = List.of(
            // distribGroup: 1
            List.of("aeaqaaaaaafwjo6paalh2aldxmeoyhyaaaaq", "aeaqaaaaaafwjo6paalh2aldxmeoypqaaaba"),
            // distribGroup: 2
            List.of("aeaqaaaaaafwjo6paalh2aldxmeoyuaaaaba", "aeaqaaaaaafwjo6paalh2aldxmeoylyaaaaq"),
            List.of("aeaqaaaaaafwjo6paalh2aldxmd56piaaabq"),
            // distribGroup: 3
            List.of("aeaqaaaaaafwjo6paalh2aldxmd57eiaaaaq"),
            // distribGroup: 4
            List.of("aeaqaaaaaafwjo6paalh2aldxmd57ciaaaaq", "aeaqaaaaaafwjo6paalh2aldxmd57eyaaaaq"),
            List.of("aeaqaaaaaafwjo6paalh2aldxmeilcqaaabq", "aeaqaaaaaafwjo6paalh2aldxmeildiaaaaq")
        );

        AtomicBoolean bulk0Ready = new AtomicBoolean(false);
        AtomicBoolean bulk2Ready = new AtomicBoolean(false);
        AtomicBoolean bulk5Ready = new AtomicBoolean(false);

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);

            int bulkId = bulks.indexOf(descriptionStep.getWorkParams().getObjectNameList());

            if (bulkId == 0 && !bulk0Ready.get()) {
                throw new ProcessingRetryAsyncException(
                    Map.of(
                        new AccessRequestContext("strategy1"),
                        List.of("accessRequest1", "accessRequest2"),
                        new AccessRequestContext("strategy2"),
                        List.of("accessRequest3")
                    )
                );
            }

            if (bulkId == 2 && !bulk2Ready.get()) {
                throw new ProcessingRetryAsyncException(
                    Map.of(new AccessRequestContext("strategy1"), List.of("accessRequest4"))
                );
            }

            if (bulkId == 5 && !bulk5Ready.get()) {
                throw new ProcessingRetryAsyncException(
                    Map.of(new AccessRequestContext("strategy3"), List.of("accessRequest5", "accessRequest6"))
                );
            }

            return getMockedItemStatus(StatusCode.OK, descriptionStep.getWorkParams().getObjectNameList().size());
        });

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS, 2);

        // When
        CompletableFuture<ItemStatus> itemStatusCompletableFuture = CompletableFuture.supplyAsync(
            () -> processDistributor.distribute(workerParameters, step, operationId),
            VitamThreadPoolExecutor.getDefaultExecutor()
        );

        Thread.sleep(3000);

        // Then : Wait for bulk0
        if (itemStatusCompletableFuture.isDone()) {
            fail("Not expected do be completed yet, got " + itemStatusCompletableFuture.get());
        }
        verify(workerClient, times(1)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback0ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(
                Map.of(
                    "accessRequest1",
                    new AccessRequestContext("strategy1"),
                    "accessRequest2",
                    new AccessRequestContext("strategy1"),
                    "accessRequest3",
                    new AccessRequestContext("strategy2")
                )
            ),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback0ArgumentCaptor.capture()
        );

        // When : bulk0 ready
        bulk0Ready.set(true);
        callback0ArgumentCaptor.getValue().notifyWorkflow();
        Thread.sleep(3000);

        // Then : Wait for bulk2
        assertThat(itemStatusCompletableFuture).isNotCompleted();
        // Expected re-execution of bulk0 + execution of bulk1 & bulk2 (unavailable async resources)
        verify(workerClient, times(1 + 3)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback2ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(Map.of("accessRequest4", new AccessRequestContext("strategy1"))),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback2ArgumentCaptor.capture()
        );

        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of(
                "accessRequest1",
                new AccessRequestContext("strategy1"),
                "accessRequest2",
                new AccessRequestContext("strategy1"),
                "accessRequest3",
                new AccessRequestContext("strategy2")
            )
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        // When : Bulk2 ready
        bulk2Ready.set(true);
        callback2ArgumentCaptor.getValue().notifyWorkflow();
        Thread.sleep(3000);

        // Then : Wait for bulk5
        assertThat(itemStatusCompletableFuture).isNotCompleted();
        // Expected re-execution of bulk2 + execution of bulk3, bulk4 & bulk5 (unavailable async resources)
        verify(workerClient, times(1 + 3 + 4)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback5ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(
                Map.of(
                    "accessRequest5",
                    new AccessRequestContext("strategy3"),
                    "accessRequest6",
                    new AccessRequestContext("strategy3")
                )
            ),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback5ArgumentCaptor.capture()
        );

        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of("accessRequest4", new AccessRequestContext("strategy1"))
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        // When : Bulk6 ready
        bulk5Ready.set(true);
        callback5ArgumentCaptor.getValue().notifyWorkflow();
        Thread.sleep(3000);

        // Then : completed
        assertThat(itemStatusCompletableFuture).isCompleted();
        // Expected re-execution of bulk5
        verify(workerClient, times(1 + 3 + 4 + 1)).submitStep(any());
        verify(workerClient, times(1 + 3 + 4 + 1)).close();
        verifyNoMoreInteractions(workerClient);
        verifyNoMoreInteractions(asyncResourcesMonitor);

        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of(
                "accessRequest5",
                new AccessRequestContext("strategy3"),
                "accessRequest6",
                new AccessRequestContext("strategy3")
            )
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        ItemStatus itemStatus = itemStatusCompletableFuture.get();
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getStatusMeter().get(StatusCode.OK.ordinal())).isEqualTo(10);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnStreamWithUnavailableAsyncResourcesAndWorkflowPausedThenAwaitResourceAvailableInterrupted()
        throws Exception {
        // Given
        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", FILE_FULL_GUIDS);

        // bulk2 will require access requests
        List<List<String>> bulks = List.of(
            // distribGroup: 1
            List.of("aeaqaaaaaafwjo6paalh2aldxmeoyhyaaaaq", "aeaqaaaaaafwjo6paalh2aldxmeoypqaaaba"),
            // distribGroup: 2
            List.of("aeaqaaaaaafwjo6paalh2aldxmeoyuaaaaba", "aeaqaaaaaafwjo6paalh2aldxmeoylyaaaaq"),
            List.of("aeaqaaaaaafwjo6paalh2aldxmd56piaaabq"),
            // distribGroup: 3
            List.of("aeaqaaaaaafwjo6paalh2aldxmd57eiaaaaq"),
            // distribGroup: 4
            List.of("aeaqaaaaaafwjo6paalh2aldxmd57ciaaaaq", "aeaqaaaaaafwjo6paalh2aldxmd57eyaaaaq"),
            List.of("aeaqaaaaaafwjo6paalh2aldxmeilcqaaabq", "aeaqaaaaaafwjo6paalh2aldxmeildiaaaaq")
        );

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);

            int bulkId = bulks.indexOf(descriptionStep.getWorkParams().getObjectNameList());

            if (bulkId == 1) {
                throw new ProcessingRetryAsyncException(
                    Map.of(
                        new AccessRequestContext("strategy1"),
                        List.of("accessRequest1", "accessRequest2"),
                        new AccessRequestContext("strategy2"),
                        List.of("accessRequest3")
                    )
                );
            }

            return getMockedItemStatus(StatusCode.OK, descriptionStep.getWorkParams().getObjectNameList().size());
        });

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS, 2);

        // When
        CompletableFuture<ItemStatus> itemStatusCompletableFuture = CompletableFuture.supplyAsync(
            () -> processDistributor.distribute(workerParameters, step, operationId),
            VitamThreadPoolExecutor.getDefaultExecutor()
        );

        Thread.sleep(3000);

        // Then : Wait for bulk1
        if (itemStatusCompletableFuture.isDone()) {
            fail("Not expected do be completed yet, got " + itemStatusCompletableFuture.get());
        }
        verify(workerClient, times(3)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback0ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(
                Map.of(
                    "accessRequest1",
                    new AccessRequestContext("strategy1"),
                    "accessRequest2",
                    new AccessRequestContext("strategy1"),
                    "accessRequest3",
                    new AccessRequestContext("strategy2")
                )
            ),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback0ArgumentCaptor.capture()
        );

        // When : Step paused
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
        callback0ArgumentCaptor.getValue().notifyWorkflow();

        Thread.sleep(3000);

        // Then : distribution finished / interrupted
        assertThat(itemStatusCompletableFuture).isCompleted();
        // Expected no re-execution of bulk1
        verify(workerClient, times(3)).submitStep(any());
        verify(workerClient, times(3)).close();

        // Ensure access requests cleanup
        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of(
                "accessRequest1",
                new AccessRequestContext("strategy1"),
                "accessRequest2",
                new AccessRequestContext("strategy1"),
                "accessRequest3",
                new AccessRequestContext("strategy2")
            )
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        verifyNoMoreInteractions(workerClient);
        verifyNoMoreInteractions(asyncResourcesMonitor);

        ItemStatus itemStatus = itemStatusCompletableFuture.get();
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        // Only 3 items processed (bulk1 + bulk3 items)
        assertThat(itemStatus.getStatusMeter().get(StatusCode.OK.ordinal())).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldDistributeOnStream() throws Exception {
        AtomicInteger actualLevel = new AtomicInteger(0);

        File file = createRandomDataSetInfo();

        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", file.getAbsolutePath());

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            Map<WorkerParameterName, String> mapParameters =
                ((DescriptionStep) invocation.getArguments()[0]).getWorkParams().getMapParameters();

            JsonNode objectMetadataList = JsonHandler.getFromString(
                mapParameters.get(WorkerParameterName.objectMetadataList)
            );

            String level = objectMetadataList.get(0).get("distributionNumber").textValue();

            synchronized (this) {
                if (!String.valueOf(actualLevel.get()).equals(level)) {
                    int newLevel = actualLevel.incrementAndGet();

                    assertThat(level).isEqualTo(String.valueOf(newLevel));
                }
            }

            return getMockedItemStatus(StatusCode.OK);
        });

        ItemStatus itemStatus = processDistributor.distribute(
            workerParameters,
            getStep(DistributionKind.LIST_IN_JSONL_FILE, file.getAbsolutePath()),
            operationId
        );

        verify(workerClient, times(750)).submitStep(any());

        assertThat(itemStatus).isNotNull();

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnStreamPauseThenResumeWithIndexOffsetOK() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<DistributorIndex> distributorIndex = new AtomicReference<>();

        File file = createRandomDataSetInfo();

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, file.getAbsolutePath());

        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", file.getAbsolutePath());

        when(workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, FAKE_UUID)))).thenAnswer(
            invocation -> {
                step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
                countDownLatch.countDown();
                return getMockedItemStatus(StatusCode.OK);
            }
        );

        doAnswer(invocation -> {
            DistributorIndex myDistributorIndex = invocation.getArgument(1);
            distributorIndex.set(myDistributorIndex);
            return myDistributorIndex;
        })
            .when(processDataManagement)
            .persistDistributorIndex(eq(operationId), any(DistributorIndex.class));

        ItemStatus is = processDistributor.distribute(workerParameters, step, operationId);

        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter()).isNotNull();

        int treatedElements = is.getStatusMeter().stream().mapToInt(o -> o).sum();
        assertThat(treatedElements).isEqualTo(step.getElementProcessed().get());
        assertThat(treatedElements).isBetween(370, 375);

        countDownLatch.await();

        doReturn(Optional.of(distributorIndex.get())).when(processDataManagement).getDistributorIndex(eq(operationId));

        when(workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, FAKE_UUID)))).thenAnswer(
            invocation -> getMockedItemStatus(StatusCode.OK)
        );

        // simulate resume action
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);
        is = processDistributor.distribute(workerParameters, step, operationId);
        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter()).isNotNull();

        treatedElements = is.getStatusMeter().stream().mapToInt(o -> o).sum();
        AtomicLong processedElements = step.getElementProcessed();
        assertThat(treatedElements).isEqualTo(processedElements.get());
        assertThat(processedElements.get()).isEqualTo(750L);
        verify(workerClient, times(750)).submitStep(any());
    }

    private File createRandomDataSetInfo() throws IOException {
        File file = testFolder.newFile();

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
            int line = 0;
            for (int distributionLevel = 1; distributionLevel <= 100; distributionLevel++) {
                int nbEntriesPerLevel = distributionLevel % 2 == 0 ? 5 : 10;

                for (int entry = 0; entry < nbEntriesPerLevel; entry++) {
                    writer
                        .append("{ \"id\": \"")
                        .append((++line == 375) ? FAKE_UUID : String.valueOf(UUID.randomUUID()))
                        .append("\", \"distribGroup\": ")
                        .append(String.valueOf(distributionLevel))
                        .append(",\"params\":{\"name\":\"someData\",\"distributionNumber\":\"")
                        .append(String.valueOf(distributionLevel))
                        .append("\"}}");
                    writer.append("\n");
                }
            }
            writer.flush();
        }

        return file;
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindFullLargeFileResumptionAfterPauseOK() throws Exception {
        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_FULL_GUIDS);

        Step step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS);
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);

        String NOLEVEL = "_no_level";
        DistributorIndex distributorIndex = new DistributorIndex(
            NOLEVEL,
            7,
            new ItemStatus(),
            FAKE_REQUEST_ID,
            step.getId(),
            new ArrayList<>()
        );

        when(processDataManagement.getDistributorIndex(operationId)).thenReturn(Optional.of(distributorIndex));

        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertNotNull(itemStatus);
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
    }

    private void givenWorkspaceClientReturnsFileContent(File file, String containerName, String objectId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        when(workspaceClient.getObject(containerName, objectId)).thenAnswer(
            args -> Response.ok(Files.newInputStream(file.toPath())).status(Response.Status.OK).build()
        );
        when(workspaceClient.getObject(eq(containerName), eq(objectId), anyLong(), anyLong())).thenAnswer(args -> {
            long startOffset = args.getArgument(2);
            Long maxSize = args.getArgument(3);
            long actualMaxSize = maxSize == null ? Long.MAX_VALUE : maxSize;

            BoundedInputStream inputStream = new BoundedInputStream(
                Channels.newInputStream(Files.newByteChannel(file.toPath()).position(startOffset)),
                actualMaxSize
            );

            long actualSize = Math.min(file.length() - startOffset, actualMaxSize);

            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(X_CHUNK_LENGTH, actualSize);
            headers.add(X_CONTENT_LENGTH, file.length());
            return new AbstractMockClient.FakeInboundResponse(
                Response.Status.OK,
                new BufferedInputStream(inputStream),
                null,
                headers
            );
        });
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindLargeFileFATAL() throws Exception {
        File invalidJsonLFile = PropertiesUtils.getResourceFile(FILE_GUIDS_INVALID);
        givenWorkspaceClientReturnsFileContent(invalidJsonLFile, operationId, FILE_GUIDS_INVALID);

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_GUIDS_INVALID);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertNotNull(itemStatus);
        assertEquals(StatusCode.FATAL, itemStatus.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindEmptyLargeFileThenWarning() throws Exception {
        File file = PropertiesUtils.getResourceFile(FILE_EMPTY_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_EMPTY_GUIDS);

        ProcessStep step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_EMPTY_GUIDS);
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertThat(imap).containsOnlyKeys(OBJECTS_LIST_EMPTY);
        assertThat(imap.get(OBJECTS_LIST_EMPTY).getGlobalStatus()).isEqualTo(StatusCode.WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void should_call_worker_with_bulk_size_from_step() throws Exception {
        // Given
        String list_elements = "list_guids_with_7_elements.json";

        File chainedFile = PropertiesUtils.getResourceFile(list_elements);
        givenWorkspaceClientReturnsFileContent(chainedFile, operationId, list_elements);

        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, list_elements, 5);

        // When
        ItemStatus itemStatus = processDistributor.distribute(workerParameters, step, operationId);

        // Then
        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnListInFileWithUnavailableAsyncResourcesThenAwaitResourceAvailableAndContinue()
        throws Exception {
        // Given
        String list_elements = "list_guids_with_7_elements.json";

        File chainedFile = PropertiesUtils.getResourceFile(list_elements);
        givenWorkspaceClientReturnsFileContent(chainedFile, operationId, list_elements);

        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, list_elements, 2);

        // bulk0 & bulk2 will require access requests
        List<List<String>> bulks = List.of(
            List.of("94fb3884-bf49-4f93-bfb0-6c1859430ca6", "097f50f8-5333-464d-95ff-80b5aaf5da2c"),
            List.of("941ffa5c-f487-45d9-9072-e82a25940bb2", "5669cf4a-cd84-4f92-bc2e-0cd7393643d6"),
            List.of("f5cd0edd-ccd5-4275-b86a-e4ea77687ded", "5669cf4a-cd84-4f92-bc2e-0cd7393643d4"),
            List.of("f5cd0edd-ccd5-4275-b86a-e4ea77687ae1")
        );

        AtomicBoolean bulk0Ready = new AtomicBoolean(false);
        AtomicBoolean bulk2Ready = new AtomicBoolean(false);

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);

            int bulkId = bulks.indexOf(descriptionStep.getWorkParams().getObjectNameList());

            if (bulkId == 0 && !bulk0Ready.get()) {
                throw new ProcessingRetryAsyncException(
                    Map.of(
                        new AccessRequestContext("strategy1"),
                        List.of("accessRequest1", "accessRequest2"),
                        new AccessRequestContext("strategy1", "offer1"),
                        List.of("accessRequest3"),
                        new AccessRequestContext("strategy2"),
                        List.of("accessRequest4")
                    )
                );
            }

            if (bulkId == 2 && !bulk2Ready.get()) {
                throw new ProcessingRetryAsyncException(
                    Map.of(new AccessRequestContext("strategy1"), List.of("accessRequest5"))
                );
            }

            return getMockedItemStatus(StatusCode.OK, descriptionStep.getWorkParams().getObjectNameList().size());
        });

        // When
        CompletableFuture<ItemStatus> itemStatusCompletableFuture = CompletableFuture.supplyAsync(
            () -> processDistributor.distribute(workerParameters, step, operationId),
            VitamThreadPoolExecutor.getDefaultExecutor()
        );

        Thread.sleep(3000);

        // Then : bulk0 & bulk2 incomplete
        if (itemStatusCompletableFuture.isDone()) {
            fail("Not expected do be completed yet, got " + itemStatusCompletableFuture.get());
        }

        verify(workerClient, times(4)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback0ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(
                Map.of(
                    "accessRequest1",
                    new AccessRequestContext("strategy1"),
                    "accessRequest2",
                    new AccessRequestContext("strategy1"),
                    "accessRequest3",
                    new AccessRequestContext("strategy1", "offer1"),
                    "accessRequest4",
                    new AccessRequestContext("strategy2")
                )
            ),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback0ArgumentCaptor.capture()
        );

        ArgumentCaptor<AsyncResourceCallback> callback2ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(Map.of("accessRequest5", new AccessRequestContext("strategy1"))),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback2ArgumentCaptor.capture()
        );

        // When : bulk0 ready
        bulk0Ready.set(true);
        callback0ArgumentCaptor.getValue().notifyWorkflow();
        Thread.sleep(3000);

        // Then : bulk2 still incomplete
        if (itemStatusCompletableFuture.isDone()) {
            fail("Not expected do be completed yet, got " + itemStatusCompletableFuture.get());
        }
        // Expected re-execution of bulk0
        verify(workerClient, times(4 + 1)).submitStep(any());
        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of(
                "accessRequest1",
                new AccessRequestContext("strategy1"),
                "accessRequest2",
                new AccessRequestContext("strategy1"),
                "accessRequest3",
                new AccessRequestContext("strategy1", "offer1"),
                "accessRequest4",
                new AccessRequestContext("strategy2")
            )
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        // When : Bulk2 ready
        bulk2Ready.set(true);
        callback2ArgumentCaptor.getValue().notifyWorkflow();
        Thread.sleep(3000);

        // Then : completed
        assertThat(itemStatusCompletableFuture).isCompleted();
        // Expected re-execution of bulk2 + execution of bulk3
        verify(workerClient, times(4 + 1 + 1)).submitStep(any());
        verify(workerClient, times(4 + 1 + 1)).close();
        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of("accessRequest5", new AccessRequestContext("strategy1"))
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        verifyNoMoreInteractions(workerClient);
        verifyNoMoreInteractions(asyncResourcesMonitor);

        ItemStatus itemStatus = itemStatusCompletableFuture.get();
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatus.getStatusMeter().get(StatusCode.OK.ordinal())).isEqualTo(7);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnListInFileWithUnavailableAsyncResourcesAndWorkflowPausedThenAwaitResourceAvailableInterrupted()
        throws Exception {
        // Given
        String list_elements = "list_guids_with_7_elements.json";

        File chainedFile = PropertiesUtils.getResourceFile(list_elements);
        givenWorkspaceClientReturnsFileContent(chainedFile, operationId, list_elements);

        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, list_elements, 2);

        // bulk0 & bulk2 will require access requests
        List<List<String>> bulks = List.of(
            List.of("94fb3884-bf49-4f93-bfb0-6c1859430ca6", "097f50f8-5333-464d-95ff-80b5aaf5da2c"),
            List.of("941ffa5c-f487-45d9-9072-e82a25940bb2", "5669cf4a-cd84-4f92-bc2e-0cd7393643d6"),
            List.of("f5cd0edd-ccd5-4275-b86a-e4ea77687ded", "5669cf4a-cd84-4f92-bc2e-0cd7393643d4"),
            List.of("f5cd0edd-ccd5-4275-b86a-e4ea77687ae1")
        );

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);

            int bulkId = bulks.indexOf(descriptionStep.getWorkParams().getObjectNameList());

            if (bulkId == 0) {
                throw new ProcessingRetryAsyncException(
                    Map.of(
                        new AccessRequestContext("strategy1"),
                        List.of("accessRequest1", "accessRequest2"),
                        new AccessRequestContext("strategy1", "offer1"),
                        List.of("accessRequest3")
                    )
                );
            }

            if (bulkId == 2) {
                throw new ProcessingRetryAsyncException(
                    Map.of(new AccessRequestContext("strategy1"), List.of("accessRequest4"))
                );
            }

            return getMockedItemStatus(StatusCode.OK, descriptionStep.getWorkParams().getObjectNameList().size());
        });

        // When
        CompletableFuture<ItemStatus> itemStatusCompletableFuture = CompletableFuture.supplyAsync(
            () -> processDistributor.distribute(workerParameters, step, operationId),
            VitamThreadPoolExecutor.getDefaultExecutor()
        );

        Thread.sleep(3000);

        // Then : bulk0 & bulk2 incomplete
        if (itemStatusCompletableFuture.isDone()) {
            fail("Not expected do be completed yet, got " + itemStatusCompletableFuture.get());
        }

        verify(workerClient, times(4)).submitStep(any());

        ArgumentCaptor<AsyncResourceCallback> callback0ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(
                Map.of(
                    "accessRequest1",
                    new AccessRequestContext("strategy1"),
                    "accessRequest2",
                    new AccessRequestContext("strategy1"),
                    "accessRequest3",
                    new AccessRequestContext("strategy1", "offer1")
                )
            ),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback0ArgumentCaptor.capture()
        );

        ArgumentCaptor<AsyncResourceCallback> callback2ArgumentCaptor = ArgumentCaptor.forClass(
            AsyncResourceCallback.class
        );
        verify(asyncResourcesMonitor, times(1)).watchAsyncResourcesForBulk(
            eq(Map.of("accessRequest4", new AccessRequestContext("strategy1"))),
            eq(VitamThreadUtils.getVitamSession().getRequestId()),
            anyString(),
            any(),
            callback2ArgumentCaptor.capture()
        );

        // When : Step paused
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
        callback0ArgumentCaptor.getValue().notifyWorkflow();
        callback2ArgumentCaptor.getValue().notifyWorkflow();

        Thread.sleep(3000);

        // Then : distribution finished / interrupted
        assertThat(itemStatusCompletableFuture).isCompleted();
        // Expected no re-execution of bulk0 & bulk2
        verify(workerClient, times(4)).submitStep(any());
        verify(workerClient, times(4)).close();

        // Ensure access requests cleanup
        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of(
                "accessRequest1",
                new AccessRequestContext("strategy1"),
                "accessRequest2",
                new AccessRequestContext("strategy1"),
                "accessRequest3",
                new AccessRequestContext("strategy1", "offer1")
            )
        );
        verify(asyncResourceCleaner).markAsyncResourcesForRemoval(
            Map.of("accessRequest4", new AccessRequestContext("strategy1"))
        );
        verifyNoMoreInteractions(asyncResourceCleaner);

        verifyNoMoreInteractions(workerClient);
        verifyNoMoreInteractions(asyncResourcesMonitor);

        ItemStatus itemStatus = itemStatusCompletableFuture.get();
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        // Only 3 items processed (bulk1 + bulk3 items)
        assertThat(itemStatus.getStatusMeter().get(StatusCode.OK.ordinal())).isEqualTo(3);
    }

    @Test
    public void should_take_step_bulk_size_in_priority() {
        // Given
        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, "", 5);

        // When
        Integer bulkSize = processDistributor.findBulkSize(step.getDistribution());

        // Then
        assertThat(bulkSize).isEqualTo(5);
    }

    @Test
    public void should_take_bulk_size_from_configuration_if_null_in_step() {
        // Given
        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, "", null);

        // When
        Integer bulkSize = processDistributor.findBulkSize(step.getDistribution());

        // Then
        assertThat(bulkSize).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributePauseOK() throws Exception {
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");
        Step step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);
        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        when(
            workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "p" + JSON_EXTENSION)))
        ).thenAnswer(invocation -> {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
            return getMockedItemStatus(StatusCode.OK);
        });

        final ItemStatus is = processDistributor.distribute(workerParameters, step, operationId);

        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter().get(StatusCode.OK.getStatusLevel())).isLessThan(
            VitamConfiguration.getRestoreBulkSize()
        ); // statusCode OK
        // Why 26, because to execute in the file ingestLevelStack we have
        // "level_0" : [], Execute 0
        // "level_1" : [ "a" ], Execute 1
        // "level_2" : [ "a", "b" ], Execute 2
        // "level_3" : [ "a", "b", "c" ], Execute 3
        // "level_4" : [ "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",..., "t"] Execute batchSize = 20
        // Total = 0 + 1 + 2 + 3 + 20 = 26
        assertThat(is.getStatusMeter().stream().mapToInt(o -> o).sum()).isBetween(10, 26);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributePauseAndWorkerTaskExceptionThenPauseOK() throws Exception {
        final File resourceFile = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(resourceFile, any(), any());
        Step step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);

        when(
            workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "d" + JSON_EXTENSION)))
        ).thenThrow(new WorkerServerClientException("Exception While Executing d"));
        when(
            workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "p" + JSON_EXTENSION)))
        ).thenAnswer(invocation -> {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
            return getMockedItemStatus(StatusCode.OK);
        });

        final ItemStatus is = processDistributor.distribute(workerParameters, step, operationId);

        assertThat(is).isNotNull();
        assertThat(is.getItemsStatus().get("FakeStepName")).isNotNull();
        assertThat(is.getStatusMeter().get(StatusCode.OK.getStatusLevel())).isGreaterThan(0); // statusCode OK
        assertThat(is.getStatusMeter().get(StatusCode.FATAL.getStatusLevel())).isEqualTo(1); // statusCode FATAL
        // Why 26, because to execute in the file ingestLevelStack we have
        // "level_0" : [], Execute 0
        // "level_1" : [ "a" ], Execute 1
        // "level_2" : [ "a", "b" ], Execute 2
        // "level_3" : [ "a", "b", "c" ], Execute 3
        // "level_4" : [ "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",...] Execute batchSize = 20
        // Total = 0 + 1 + 2 + 3 + 20 = 26
        assertThat(is.getStatusMeter().stream().mapToInt(o -> o).sum()).isBetween(10, 26);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeCancelOK() throws Exception {
        final File ingestLevelStack = PropertiesUtils.getResourceFile("ingestLevelStack.json");
        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS, 1);
        givenWorkspaceClientReturnsFileContent(ingestLevelStack, any(), any());

        when(
            workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "d" + JSON_EXTENSION)))
        ).thenAnswer(invocation -> {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_CANCEL);
            return getMockedItemStatus(StatusCode.OK);
        });

        ItemStatus is = processDistributor.distribute(workerParameters, step, operationId);
        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter()).isNotNull();

        int processedElements = is.getStatusMeter().stream().mapToInt(o -> o).sum();
        assertThat(processedElements).isEqualTo(step.getElementProcessed().get());
        // Why 10, because to execute in the file ingestLevelStack we have
        // "level_0" : [], Execute 0
        // "level_1" : [ "a" ], Execute 1
        // "level_2" : [ "a", "b" ], Execute 2
        // "level_3" : [ "a", "b", "c" ], Execute 3
        // "level_4" : [ "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",...] when executing "d" we fire the cancel action so Execute 4 ("a","b","c","d")
        // Total = 0 + 1 + 2 + 3 + [at least 1, at most 20] = [7, 26]
        // At least 1 item (the "d" task" that triggerred the cancel)
        // At most 20 items (distributor batch size)
        assertThat(processedElements).isBetween(7, 26);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeOnListPauseThenResumeWithIndexOffsetOK() throws Exception {
        final File ingestLevelStack = PropertiesUtils.getResourceFile("ingestLevelStack.json");
        ProcessStep step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS, 1);
        givenWorkspaceClientReturnsFileContent(ingestLevelStack, any(), any());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<DistributorIndex> distributorIndex = new AtomicReference<>();

        when(
            workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "hh" + JSON_EXTENSION)))
        ).thenAnswer(invocation -> {
            step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_PAUSE);
            countDownLatch.countDown();
            return getMockedItemStatus(StatusCode.OK);
        });

        doAnswer(invocation -> {
            DistributorIndex myDistributorIndex = invocation.getArgument(1);
            distributorIndex.set(myDistributorIndex);
            return myDistributorIndex;
        })
            .when(processDataManagement)
            .persistDistributorIndex(eq(operationId), any(DistributorIndex.class));

        // we run the step
        processDistributor.distribute(workerParameters, step, operationId);

        countDownLatch.await();

        doReturn(Optional.of(distributorIndex.get())).when(processDataManagement).getDistributorIndex(eq(operationId));

        when(workerClient.submitStep(argThat(stepDescription -> matcher(stepDescription, "d")))).thenAnswer(
            invocation -> getMockedItemStatus(StatusCode.OK)
        );

        // simulate resume action
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);
        ItemStatus is = processDistributor.distribute(workerParameters, step, operationId);
        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter()).isNotNull();

        int treatedElements = is.getStatusMeter().stream().mapToInt(o -> o).sum();
        AtomicLong processedElements = step.getElementProcessed();
        assertThat(treatedElements).isEqualTo(processedElements.get());
        assertThat(processedElements.get()).isEqualTo(170L);
    }

    private boolean matcher(DescriptionStep descriptionStep, String elementName) {
        if (Objects.nonNull(descriptionStep)) return descriptionStep
            .getWorkParams()
            .getObjectNameList()
            .contains(elementName);
        else return false;
    }
}
