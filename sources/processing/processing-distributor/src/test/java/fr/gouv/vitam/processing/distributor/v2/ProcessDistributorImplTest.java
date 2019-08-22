/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.processing.distributor.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.data.core.ProcessDataAccess;
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
import org.junit.rules.TemporaryFolder;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.gouv.vitam.common.GlobalDataRest.X_CHUNK_LENGTH;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTENT_LENGTH;
import static fr.gouv.vitam.processing.distributor.api.ProcessDistributor.OBJECTS_LIST_EMPTY;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ProcessDistributorImplTest {

    private static final String FAKE_REQUEST_ID = "FakeRequestId";
    private static final String FAKE_CONTEXT_ID = "FakeContextId";

    private static final String FILE_FULL_GUIDS = "file_full_guids.jsonl";
    private static final String FILE_WITH_GUIDS = "file_with_guids.jsonl";
    private static final String FILE_GUIDS_INVALID = "file_guids_invalid.jsonl";
    private static final String FILE_EMPTY_GUIDS = "file_empty_guids.jsonl";
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private final static String operationId = "FakeOperationId";
    private static final Integer TENANT = 0;
    private WorkerParameters workerParameters;
    private static ProcessDataAccess processDataAccess;
    private static ProcessDataManagement processDataManagement;
    private ProcessWorkflow processWorkflow;
    private static WorkspaceClientFactory workspaceClientFactory;
    private WorkspaceClient workspaceClient;
    private static IWorkerManager workerManager;
    private ProcessDistributorImpl processDistributor;
    private ServerConfiguration serverConfiguration;


    private static final WorkerClientFactory workerClientFactory = mock(WorkerClientFactory.class);
    private static final WorkerClient workerClient = mock(WorkerClient.class);


    @Before
    public void setUp() throws Exception {
        reset(workerClient);
        reset(workerClientFactory);

        when(workerClientFactory.getClient()).thenReturn(workerClient);
        workerParameters = WorkerParametersFactory.newWorkerParameters();
        workerParameters.setWorkerGUID(GUIDFactory.newGUID());
        workerParameters.setContainerName(operationId);

        processWorkflow = mock(ProcessWorkflow.class);
        processDataAccess = mock(ProcessDataAccess.class);
        processDataManagement = mock(ProcessDataManagement.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(processDataAccess.findOneProcessWorkflow(any(), any())).thenReturn(processWorkflow);

        when(workerClient.submitStep(any()))
            .thenAnswer(invocation -> getMockedItemStatus(StatusCode.OK));

        serverConfiguration = new ServerConfiguration()
            .setMaxDistributionInMemoryBufferSize(100_000)
            .setMaxDistributionOnDiskBufferSize(100_000_000);

        processDistributor = new ProcessDistributorImpl(workerManager, serverConfiguration,
            processDataAccess, processDataManagement, workspaceClientFactory, workerClientFactory);

        if (Thread.currentThread() instanceof VitamThreadFactory.VitamThread) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT);
            VitamThreadUtils.getVitamSession().setRequestId(FAKE_REQUEST_ID);
            VitamThreadUtils.getVitamSession().setContextId(FAKE_CONTEXT_ID);
        }

    }

    ItemStatus getMockedItemStatus(StatusCode statusCode) {
        return new ItemStatus("StepId")
            .setItemsStatus("ItemId",
                new ItemStatus("ItemId")
                    .setMessage("message")
                    .increment(statusCode));
    }

    @BeforeClass
    public static void tearUpBeforeClass() throws Exception {
        VitamConfiguration.setWorkerBulkSize(1);
        final WorkerBean workerBean =
            new WorkerBean("DefaultWorker", "DefaultWorker", 2, 0, "status",
                new WorkerRemoteConfiguration("localhost", 8999));
        workerBean.setWorkerId("FakeWorkerId");

        when(workerClientFactory.getClient()).thenReturn(workerClient);
        workerManager = new WorkerManager(workerClientFactory);
        workerManager.registerWorker(workerBean);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VitamConfiguration.setWorkerBulkSize(10);
    }

    /**
     * Test the constructor
     */
    @Test
    public void testConstructor() {

        ServerConfiguration configuration = new ServerConfiguration()
            .setMaxDistributionInMemoryBufferSize(100_000)
            .setMaxDistributionOnDiskBufferSize(100_000_000);

        new ProcessDistributorImpl(mock(IWorkerManager.class), configuration);

        try {
            new ProcessDistributorImpl(null, configuration);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(null, configuration, processDataAccess, processDataManagement,
                workspaceClientFactory, workerClientFactory);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(mock(IWorkerManager.class), null, processDataAccess, processDataManagement,
                workspaceClientFactory, workerClientFactory);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(mock(IWorkerManager.class), configuration, null, processDataManagement,
                workspaceClientFactory, workerClientFactory);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(mock(IWorkerManager.class), configuration, processDataAccess, null,
                workspaceClientFactory, workerClientFactory);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new ProcessDistributorImpl(mock(IWorkerManager.class), configuration, processDataAccess,
                processDataManagement,
                null, workerClientFactory);
            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    private ProcessStep getStep(DistributionKind distributionKind, String distributorElement) {
        return getStep("FakeStepId", "FakeStepName", distributionKind, distributorElement,
            ProcessBehavior.NOBLOCKING, null);
    }

    private ProcessStep getStep(DistributionKind distributionKind, String distributorElement, Integer bulkSize) {
        return getStep("FakeStepId", "FakeStepName", distributionKind, distributorElement,
            ProcessBehavior.NOBLOCKING, bulkSize);
    }

    private ProcessStep getStep(String stepId, String stepName, DistributionKind distributionKind,
        String distributorElement, ProcessBehavior processBehavior, Integer bulkSize) {

        final Distribution distribution = new Distribution();
        distribution.setKind(distributionKind);
        distribution.setElement(distributorElement);
        distribution.setType(DistributionType.Units);
        distribution.setBulkSize(bulkSize);

        final Step step = new Step();
        step.setStepName(stepName);
        step.setDistribution(distribution);
        step.setBehavior(processBehavior);
        return new ProcessStep(step, 0, 0, stepId);
    }

    /**
     * Test parameter required for the method distribute
     */
    @Test
    @RunWithCustomExecutor
    public void whenDistributeRequiredParametersThenOK() {


        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final ProcessDistributor processDistributor =
            new ProcessDistributorImpl(workerManager, serverConfiguration, processDataAccess, processDataManagement,
                workspaceClientFactory, workerClientFactory);

        try {
            processDistributor
                .distribute(null, getStep(DistributionKind.REF, "manifest.xml"), operationId, PauseRecover.NO_RECOVER);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            processDistributor
                .distribute(workerParameters, null, operationId, PauseRecover.NO_RECOVER);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            processDistributor
                .distribute(workerParameters, getStep(DistributionKind.REF, "manifest.xml"), null,
                    PauseRecover.NO_RECOVER);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            processDistributor
                .distribute(workerParameters, getStep(DistributionKind.REF, "manifest.xml"), operationId, null);

            fail("Should throw an exception");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    /**
     * @throws WorkerAlreadyExistsException
     */
    @Test
    @RunWithCustomExecutor
    public void whenDistributeManifestThenOK() throws WorkerAlreadyExistsException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final ProcessDistributor processDistributor =
            new ProcessDistributorImpl(workerManager, serverConfiguration, processDataAccess, processDataManagement,
                workspaceClientFactory, workerClientFactory);

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters, getStep(DistributionKind.REF, "manifest.xml"), operationId,
                PauseRecover.NO_RECOVER);

        assertNotNull(itemStatus);
        assertTrue(StatusCode.OK.equals(itemStatus.getGlobalStatus()));
    }


    /**
     * @throws WorkerAlreadyExistsException
     */
    @Test
    @RunWithCustomExecutor
    public void whenDistributeManifestThenFATAL() throws WorkerAlreadyExistsException {


        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters, getStep(DistributionKind.REF, "manifest.xml"), operationId,
                PauseRecover.NO_RECOVER);
        assertNotNull(itemStatus);
        assertTrue(StatusCode.FATAL.equals(itemStatus.getGlobalStatus()));
    }


    //Exemple here
    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelOK() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        int numberOfObjectIningestLevelStack = 170;
        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS), operationId,
                PauseRecover.NO_RECOVER);
        assertNotNull(itemStatus);

        assertTrue(StatusCode.OK.equals(itemStatus.getGlobalStatus()));

        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are OK
        assertTrue(imap.get("ItemId").getStatusMeter()
            .get(StatusCode.OK.getStatusLevel()) == numberOfObjectIningestLevelStack);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelKO() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        WorkerNotFoundClientException, WorkerServerClientException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

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

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS), operationId,
                PauseRecover.NO_RECOVER);
        assertNotNull(itemStatus);
        assertTrue(StatusCode.KO.equals(itemStatus.getGlobalStatus()));
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are KO
        assertTrue(imap.get("ItemId").getStatusMeter().get(StatusCode.KO.getStatusLevel()) == 1);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelWARNING() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        WorkerNotFoundClientException, WorkerServerClientException {
        ;
        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

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

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS), operationId,
                PauseRecover.NO_RECOVER);
        assertNotNull(itemStatus);
        assertTrue(StatusCode.WARNING.equals(itemStatus.getGlobalStatus()));
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
        // All object status are WARNING
        assertTrue(imap.get("ItemId").getStatusMeter().get(StatusCode.WARNING.getStatusLevel()) == 1);
    }


    @Test
    @RunWithCustomExecutor
    public void whenDistributeDistributionKindListWithLevelFATAL() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        WorkerNotFoundClientException, WorkerServerClientException {


        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());

        when(workerClient.submitStep(any())).thenThrow(new RuntimeException("WorkerException"));

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS), operationId,
                PauseRecover.NO_RECOVER);
        assertNotNull(itemStatus);
        assertTrue(StatusCode.FATAL.equals(itemStatus.getGlobalStatus()));
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindLargeFileOK() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        File file = PropertiesUtils.getResourceFile(FILE_WITH_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_WITH_GUIDS);

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_WITH_GUIDS), operationId,
                PauseRecover.NO_RECOVER);

        assertNotNull(itemStatus);
        assertTrue(StatusCode.OK.equals(itemStatus.getGlobalStatus()));
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindFullLargeFileOK() throws
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {


        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", FILE_FULL_GUIDS);
        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters, getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS), operationId,
                PauseRecover.NO_RECOVER);

        assertThat(itemStatus).isNotNull();

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();
    }


    @Test
    @RunWithCustomExecutor
    public void shouldDistributeOnStream() throws Exception {

        AtomicInteger actualLevel = new AtomicInteger(0);

        File file = createRandomDataSetInfo();

        givenWorkspaceClientReturnsFileContent(file, "FakeOperationId", file.getAbsolutePath());
        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);


        when(workerClient.submitStep(any()))
            .thenAnswer(invocation -> {

                Map<WorkerParameterName, String> mapParameters =
                    ((DescriptionStep) invocation.getArguments()[0])
                        .getWorkParams().getMapParameters();

                JsonNode objectMetadataList =
                    JsonHandler.getFromString(mapParameters.get(WorkerParameterName.objectMetadataList));

                String level = objectMetadataList.get(0).get("distributionNumber").textValue();

                synchronized (this) {

                    if (!String.valueOf(actualLevel.get()).equals(level)) {

                        int newLevel = actualLevel.incrementAndGet();

                        assertThat(level).isEqualTo(String.valueOf(newLevel));
                    }
                }

                return getMockedItemStatus(StatusCode.OK);

            });


        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters, getStep(DistributionKind.LIST_IN_JSONL_FILE, file.getAbsolutePath()),
                operationId,
                PauseRecover.NO_RECOVER);

        verify(workerClient, times(750)).submitStep(any());

        assertThat(itemStatus).isNotNull();

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, ItemStatus> item = itemStatus.getItemsStatus();

        assertThat(item).isNotNull();

        assertThat(item).isNotEmpty();

    }

    private File createRandomDataSetInfo() throws IOException {

        File file = testFolder.newFile();

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {

            for (int distributionLevel = 1; distributionLevel <= 100; distributionLevel++) {

                int nbEntriesPerLevel = distributionLevel % 2 == 0 ? 5 : 10;

                for (int entry = 0; entry < nbEntriesPerLevel; entry++) {

                    writer.append("{ \"id\": \"")
                        .append(String.valueOf(UUID.randomUUID()))
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
    public void whenDistributeKindFullLargeFileResumptionAfterPauseOK() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        InvalidParseOperationException, ProcessingStorageWorkspaceException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        File file = PropertiesUtils.getResourceFile(FILE_FULL_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_FULL_GUIDS);

        Step step = getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_FULL_GUIDS);
        step.setPauseOrCancelAction(PauseOrCancelAction.ACTION_RECOVER);

        String NOLEVEL = "_no_level";
        DistributorIndex distributorIndex =
            new DistributorIndex(NOLEVEL, 7, new ItemStatus(), FAKE_REQUEST_ID, step.getId(), new ArrayList<>());

        String DISTRIBUTOR_INDEX = "distributorIndex";
        when(processDataManagement.getDistributorIndex(DISTRIBUTOR_INDEX, operationId)).thenReturn(distributorIndex);

        ItemStatus itemStatus =
            processDistributor.distribute(workerParameters, step, operationId, PauseRecover.RECOVER_FROM_API_PAUSE);

        assertNotNull(itemStatus);
        assertTrue(StatusCode.OK.equals(itemStatus.getGlobalStatus()));
        Map<String, ItemStatus> imap = itemStatus.getItemsStatus();
        assertNotNull(imap);
        assertFalse(imap.isEmpty());
    }

    private void givenWorkspaceClientReturnsFileContent(File file, String containerName, String objectId)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        when(workspaceClient.getObject(containerName, objectId)).thenAnswer(
            (args) -> Response.ok(Files.newInputStream(file.toPath())).status(Response.Status.OK).build());
        when(workspaceClient.getObject(eq(containerName), eq(objectId), anyLong(), anyLong())).thenAnswer(
            (args) -> {
                long startOffset = args.getArgument(2);
                Long maxSize = args.getArgument(3);
                long actualMaxSize = maxSize == null ? Long.MAX_VALUE : maxSize;

                BoundedInputStream inputStream = new BoundedInputStream(
                    Channels.newInputStream(
                        Files.newByteChannel(file.toPath())
                            .position(startOffset))
                    , actualMaxSize);

                long actualSize = Math.min(file.length() - startOffset, actualMaxSize);

                MultivaluedHashMap headers = new MultivaluedHashMap();
                headers.add(X_CHUNK_LENGTH, actualSize);
                headers.add(X_CONTENT_LENGTH, file.length());
                return new AbstractMockClient.FakeInboundResponse(Response.Status.OK,
                    new BufferedInputStream(inputStream), null, headers);
            });
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindLargeFileFATAL() throws
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        File invalidJsonLFile = PropertiesUtils.getResourceFile(FILE_GUIDS_INVALID);
        givenWorkspaceClientReturnsFileContent(invalidJsonLFile, operationId, FILE_GUIDS_INVALID);

        ItemStatus itemStatus = processDistributor.distribute(workerParameters,
            getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_GUIDS_INVALID), operationId,
            PauseRecover.NO_RECOVER);

        assertNotNull(itemStatus);
        assertTrue(StatusCode.FATAL.equals(itemStatus.getGlobalStatus()));
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeKindEmptyLargeFileThenWarning() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        File file = PropertiesUtils.getResourceFile(FILE_EMPTY_GUIDS);
        givenWorkspaceClientReturnsFileContent(file, operationId, FILE_EMPTY_GUIDS);

        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters, getStep(DistributionKind.LIST_IN_JSONL_FILE, FILE_EMPTY_GUIDS), operationId,
                PauseRecover.NO_RECOVER);

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



        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        File chainedFile = PropertiesUtils.getResourceFile(list_elements);
        givenWorkspaceClientReturnsFileContent(chainedFile, operationId, list_elements);

        final CountDownLatch countDownLatchSubmit = new CountDownLatch(2);
        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            countDownLatchSubmit.countDown();
            return getMockedItemStatus(StatusCode.OK);
        });

        ProcessStep step = getStep(DistributionKind.LIST_IN_FILE, list_elements, 5);

        // When
        ItemStatus itemStatus = processDistributor
            .distribute(workerParameters,
                step, operationId,
                PauseRecover.NO_RECOVER);

        countDownLatchSubmit.await();

        // Then
        assertNotNull(itemStatus);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
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
    public void whenDistributePauseOK() throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, WorkerNotFoundClientException, WorkerServerClientException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final File fileContracts = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(fileContracts, any(), any());
        final CountDownLatch countDownLatchSubmit = new CountDownLatch(9);
        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            System.out.println("submit step");
            countDownLatchSubmit.countDown();
            return getMockedItemStatus(StatusCode.OK);
        });

        Step step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ItemStatus[] itemStatus = new ItemStatus[1];
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> {
            itemStatus[0] = processDistributor.distribute(workerParameters, step, operationId, PauseRecover.NO_RECOVER);

            countDownLatch.countDown();
        });

        try {
            countDownLatchSubmit.await(); // 9 times
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        processDistributor.pause(operationId);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        ItemStatus is = itemStatus[0];
        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter().get(0)).isGreaterThan(0); // statusCode UNkNWON
        assertThat(is.getStatusMeter().get(StatusCode.OK.getStatusLevel())).isGreaterThan(0); // statusCode OK
        // Why 16, because to execute in the file ingestLevelStack we have
        // "level_0" : [], Execute 0
        // "level_1" : [ "a" ], Execute 1
        // "level_2" : [ "a", "b" ], Execute 2
        // "level_3" : [ "a", "b", "c" ], Execute 3
        // "level_4" : [ "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",...] Execute batchSize = 10
        // Total = 0 + 1 + 2 + 3 + 10 = 16
        // We should have at least one in UNkNWON and at least one in OK
        assertThat((is.getStatusMeter().get(StatusCode.UNKNOWN.getStatusLevel()) +
            is.getStatusMeter().get(StatusCode.OK.getStatusLevel()) +
            is.getStatusMeter().get(StatusCode.KO.getStatusLevel())) % 10)
            .isEqualTo(6);
    }


    @Test
    @RunWithCustomExecutor
    public void whenDistributePauseAndWorkerTaskExceptionThenPauseOK() throws WorkerAlreadyExistsException,
        IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        WorkerNotFoundClientException, WorkerServerClientException {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final File resourceFile = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(resourceFile, any(), any());

        final CountDownLatch countDownLatchException = new CountDownLatch(1);

        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            DescriptionStep descriptionStep = invocation.getArgument(0);
            if (descriptionStep.getWorkParams().getObjectNameList().iterator().next().equals("d.json")) {
                countDownLatchException.countDown();
                throw new RuntimeException("Exception While Executing d");
            }
            return getMockedItemStatus(StatusCode.OK);
        });

        Step step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ItemStatus[] itemStatus = new ItemStatus[1];
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> {
            itemStatus[0] = processDistributor
                .distribute(workerParameters, step, operationId,
                    PauseRecover.NO_RECOVER);
            countDownLatch.countDown();
        });
        try {
            countDownLatchException.await();
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        processDistributor.pause(operationId);

        // Wait until distributor responds
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        ItemStatus is = itemStatus[0];

        assertThat(is).isNotNull();
        assertThat(is.getItemsStatus().get(PauseOrCancelAction.ACTION_PAUSE.name())).isNotNull();
        assertThat(is.getItemsStatus().get(ProcessDistributor.WORKER_CALL_EXCEPTION)).isNotNull();
        assertThat(is.getStatusMeter().get(StatusCode.UNKNOWN.getStatusLevel())).isGreaterThan(0); // statusCode UNkNWON
        assertThat(is.getStatusMeter().get(StatusCode.OK.getStatusLevel())).isGreaterThan(0); // statusCode OK
        assertThat(is.getStatusMeter().get(StatusCode.FATAL.getStatusLevel())).isEqualTo(1); // statusCode FATAL
        // Why 16, because to execute in the file ingestLevelStack we have
        // "level_0" : [], Execute 0
        // "level_1" : [ "a" ], Execute 1
        // "level_2" : [ "a", "b" ], Execute 2
        // "level_3" : [ "a", "b", "c" ], Execute 3
        // "level_4" : [ "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k",...] Execute batchSize = 10
        // Total = 0 + 1 + 2 + 3 + 10 = 16
        // We should have at least one in UNkNWON and at least one in OK
        assertThat((is.getStatusMeter().get(StatusCode.UNKNOWN.getStatusLevel()) +
            is.getStatusMeter().get(StatusCode.OK.getStatusLevel()) +
            is.getStatusMeter().get(StatusCode.FATAL.getStatusLevel())) % 10)
            .isEqualTo(6);
    }

    @Test
    @RunWithCustomExecutor
    public void whenDistributeCancelOK() throws Exception {

        when(processWorkflow.getStatus()).thenReturn(StatusCode.STARTED);

        final File ingestLevelStack = PropertiesUtils.getResourceFile("ingestLevelStack.json");

        givenWorkspaceClientReturnsFileContent(ingestLevelStack, any(), any());

        final CountDownLatch countDownLatchSubmit = new CountDownLatch(9);
        when(workerClient.submitStep(any())).thenAnswer(invocation -> {
            countDownLatchSubmit.countDown();
            return getMockedItemStatus(StatusCode.OK);
        });

        Step step = getStep(DistributionKind.LIST_ORDERING_IN_FILE, ProcessDistributor.ELEMENT_UNITS);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ItemStatus[] itemStatus = new ItemStatus[1];
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> {
            itemStatus[0] = processDistributor
                .distribute(workerParameters, step, operationId,
                    PauseRecover.NO_RECOVER);
            countDownLatch.countDown();
        });

        try {
            countDownLatchSubmit.await(); // 9 times
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        processDistributor.cancel(operationId);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        ItemStatus is = itemStatus[0];
        assertThat(is).isNotNull();
        assertThat(is.getStatusMeter()).isNotNull();
        assertThat(is.getStatusMeter().get(0)).isGreaterThan(0); // statusCode UNkNWON
    }

}
