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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWithCustomExecutor
public class OfferSyncProcessTest {

    private static final String STRATEGY = VitamConfiguration.getDefaultStrategy();
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;
    private static final String CONTAINER_1 = "2_unit";
    private static final String CONTAINER_2 = "2_object";
    public static final String ACCESS_REQUEST_1 = "accessRequest1";
    public static final String ACCESS_REQUEST_2 = "accessRequest2";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private RestoreOfferBackupService restoreOfferBackupService;

    @Mock
    private StorageDistribution distribution;

    private List<OfferLog> sourceOfferLogs;
    private Map<String, byte[]> sourceDataFiles;
    private Map<String, byte[]> targetDataFiles;

    private static ExecutorService executorService;

    @BeforeClass
    public static void beforeClass() {
        executorService = Executors.newFixedThreadPool(4, VitamThreadFactory.getInstance());
    }

    @AfterClass
    public static void afterClass() {
        executorService.shutdown();
    }

    @Before
    public void setup() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));

        sourceOfferLogs = new ArrayList<>();
        sourceDataFiles = new ConcurrentHashMap<>();
        targetDataFiles = new ConcurrentHashMap<>();

        doAnswer(args -> {
            Long offsetLong = args.getArgument(3);
            long offset = (offsetLong == null) ? 0L : offsetLong;
            int limit = args.getArgument(4);

            return sourceOfferLogs
                .stream()
                .filter(offerLog -> offerLog.getSequence() >= offset)
                .limit(limit)
                .collect(Collectors.toList());
        })
            .when(restoreOfferBackupService)
            .getListing(eq(STRATEGY), eq(SOURCE), eq(DATA_CATEGORY), any(), anyInt(), eq(Order.ASC));

        doAnswer(args -> {
            String filename = args.getArgument(2);
            byte[] data = sourceDataFiles.get(filename);
            if (data == null) {
                throw new StorageNotFoundException("not found");
            }
            return Response.ok(data).build();
        })
            .when(distribution)
            .getContainerByCategory(eq(STRATEGY), anyString(), anyString(), eq(DATA_CATEGORY), eq(SOURCE));

        doAnswer(args -> {
            String filename = args.getArgument(2);
            Response response = args.getArgument(6);

            targetDataFiles.put(filename, (byte[]) response.getEntity());
            return null;
        })
            .when(distribution)
            .storeDataInOffers(
                eq(STRATEGY),
                eq(OfferSyncProcess.OFFER_SYNC_ORIGIN),
                anyString(),
                eq(DATA_CATEGORY),
                eq(null),
                eq(singletonList(TARGET)),
                any()
            );

        doAnswer(args -> {
            DataContext dataContext = args.getArgument(1);
            assertThat(dataContext.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(dataContext.getRequester()).isNull();
            assertThat(dataContext.getCategory()).isEqualTo(DATA_CATEGORY);

            targetDataFiles.remove(dataContext.getObjectId());
            return null;
        })
            .when(distribution)
            .deleteObjectInOffers(eq(STRATEGY), any(), eq(singletonList(TARGET)));
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeEmptyOffer() throws Exception {
        // Given
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 10, 1, 1, 1, 1);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, null);

        verify(distribution, never()).storeDataInOffers(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any()
        );
        verify(distribution, never()).deleteObjectInOffers(any(), any(), any());
        verify(distribution, never()).createAccessRequestIfRequired(any(), any(), any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchSingleBatch() throws Exception {
        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);
        doReturn(Optional.empty())
            .when(distribution)
            .createAccessRequestIfRequired(eq(STRATEGY), eq(SOURCE), eq(DATA_CATEGORY), anyList());

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, 12L);

        // First batch [sequence 1..12] :
        //  - Written   : file2, file3, file4, file6
        //  - Deleted   : file1, file5
        verify(distribution, times(4)).storeDataInOffers(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any()
        );
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file4", "file6", "file2", "file3")
        );
        verify(distribution, never()).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(distribution, never()).removeAccessRequest(any(), any(), any(), anyBoolean());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchSingleBatchAsyncOffer() throws Exception {
        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);
        doReturn(Optional.of(ACCESS_REQUEST_1))
            .when(distribution)
            .createAccessRequestIfRequired(eq(STRATEGY), eq(SOURCE), eq(DATA_CATEGORY), anyList());
        doReturn(
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.READY)
        )
            .when(distribution)
            .checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, 12L);

        // First batch [sequence 1..12] :
        //  - Written   : file2, file3, file4, file6
        //  - Deleted   : file1, file5
        verify(distribution, times(4)).storeDataInOffers(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any()
        );
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file4", "file6", "file2", "file3")
        );
        verify(distribution, times(3)).checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);
        verify(distribution, times(1)).removeAccessRequest(STRATEGY, SOURCE, ACCESS_REQUEST_1, false);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchMultiBatch() throws Exception {
        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 10, 1, 1, 1, 1);
        doReturn(Optional.empty())
            .when(distribution)
            .createAccessRequestIfRequired(eq(STRATEGY), eq(SOURCE), eq(DATA_CATEGORY), anyList());

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, 12L);

        // First batch [sequence 1..10] :
        //  - Written   : file2, file3, file4
        //  - Deleted   : file1
        //  - Not found : file 5
        // Second batch [sequence 11..12] :
        //  - Written   : file6
        //  - Deleted   : file5 (silently)

        verify(distribution, times(4)).storeDataInOffers(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any()
        );
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file4", "file5", "file2", "file3")
        );
        verify(distribution, times(1)).createAccessRequestIfRequired(STRATEGY, SOURCE, DATA_CATEGORY, List.of("file6"));
        verify(distribution, never()).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(distribution, never()).removeAccessRequest(any(), any(), any(), anyBoolean());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchMultiBatchAsyncOffer() throws Exception {
        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 10, 1, 1, 1, 1);
        doReturn(Optional.of(ACCESS_REQUEST_1))
            .when(distribution)
            .createAccessRequestIfRequired(
                STRATEGY,
                SOURCE,
                DATA_CATEGORY,
                List.of("file4", "file5", "file2", "file3")
            );
        doReturn(Optional.of(ACCESS_REQUEST_2))
            .when(distribution)
            .createAccessRequestIfRequired(STRATEGY, SOURCE, DATA_CATEGORY, List.of("file6"));
        doReturn(
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.READY)
        )
            .when(distribution)
            .checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);

        doReturn(
            Map.of(ACCESS_REQUEST_2, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_2, AccessRequestStatus.READY)
        )
            .when(distribution)
            .checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_2), false);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, 12L);

        // First batch [sequence 1..10] :
        //  - Written   : file2, file3, file4
        //  - Deleted   : file1
        //  - Not found : file 5
        // Second batch [sequence 11..12] :
        //  - Written   : file6
        //  - Deleted   : file5 (silently)

        verify(distribution, times(4)).storeDataInOffers(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any(),
            any(),
            any()
        );
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file4", "file5", "file2", "file3")
        );
        verify(distribution, times(1)).createAccessRequestIfRequired(STRATEGY, SOURCE, DATA_CATEGORY, List.of("file6"));

        verify(distribution, times(3)).checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);
        verify(distribution, times(1)).removeAccessRequest(STRATEGY, SOURCE, ACCESS_REQUEST_1, false);

        verify(distribution, times(2)).checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_2), false);
        verify(distribution, times(1)).removeAccessRequest(STRATEGY, SOURCE, ACCESS_REQUEST_2, false);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromOffsetWithNewChanges() {
        /*
         * Synchronize part 1
         */

        // Given
        givenDataSetInSourceOfferPart1(CONTAINER_1);
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        verifySynchronizationStatus(instance, null, 2L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);

        /*
         * Synchronize part 2
         */

        // Given
        givenDataSetInSourceOfferPart2(CONTAINER_1);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, 3L);

        // Then
        verifySynchronizationStatus(instance, 3L, 12L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromOffsetWithoutNewChanges() {
        /*
         * Synchronize part 1
         */

        // Given
        givenDataSetInSourceOfferPart1(CONTAINER_1);
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, null);

        // Then
        verifySynchronizationStatus(instance, null, 2L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);

        /*
         * Synchronize part 2
         */

        // Given not updates

        // When
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, DATA_CATEGORY, 3L);

        // Then
        verifySynchronizationStatus(instance, 3L, null);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
    }

    @Test
    @RunWithCustomExecutor
    public void partial_synchronize_existing_and_delete_not_found() throws StorageException {
        // Given
        givenDataSetInSourceOffer();
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);
        doReturn(Optional.empty())
            .when(distribution)
            .createAccessRequestIfRequired(eq(STRATEGY), eq(SOURCE), eq(DATA_CATEGORY), anyList());

        // When
        List<OfferPartialSyncItem> items = new ArrayList<>();
        OfferPartialSyncItem offerPartialSyncItem = new OfferPartialSyncItem();
        offerPartialSyncItem.setContainer(DATA_CATEGORY.getCollectionName());
        offerPartialSyncItem.setTenantId(TENANT_ID);
        offerPartialSyncItem.setFilenames(Lists.newArrayList("file2", "file6", "file5"));

        items.add(offerPartialSyncItem);
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, items);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferSyncStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferSyncStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getSourceOffer()).isEqualTo(SOURCE);
        assertThat(instance.getOfferSyncStatus().getTargetOffer()).isEqualTo(TARGET);
        assertThat(instance.getOfferSyncStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );

        assertThat(targetDataFiles).containsKeys("file2", "file6");

        ArgumentCaptor<DataContext> contextArgumentCaptor = forClass(DataContext.class);
        ArgumentCaptor<String> strategyCaptor = forClass(String.class);
        ArgumentCaptor<List<String>> offersCaptor = forClass(List.class);
        verify(distribution, times(1)).deleteObjectInOffers(
            strategyCaptor.capture(),
            contextArgumentCaptor.capture(),
            offersCaptor.capture()
        );
        assertThat(contextArgumentCaptor.getValue().getObjectId()).isEqualTo("file5");

        ArgumentCaptor<String> fileName = forClass(String.class);

        verify(distribution, times(2)).storeDataInOffers(
            forClass(String.class).capture(),
            forClass(String.class).capture(),
            fileName.capture(),
            forClass(DataCategory.class).capture(),
            forClass(String.class).capture(),
            forClass(List.class).capture(),
            forClass(Response.class).capture()
        );

        assertThat(fileName.getAllValues()).contains("file2", "file6");
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file2", "file6", "file5")
        );
        verify(distribution, never()).checkAccessRequestStatuses(any(), any(), any(), anyBoolean());
        verify(distribution, never()).removeAccessRequest(any(), any(), any(), anyBoolean());
    }

    @Test
    @RunWithCustomExecutor
    public void partial_synchronize_existing_and_delete_not_found_from_async_offer() throws StorageException {
        // Given
        givenDataSetInSourceOffer();
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);
        doReturn(Optional.of(ACCESS_REQUEST_1))
            .when(distribution)
            .createAccessRequestIfRequired(STRATEGY, SOURCE, DATA_CATEGORY, List.of("file2", "file6", "file5"));
        doReturn(
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.NOT_READY),
            Map.of(ACCESS_REQUEST_1, AccessRequestStatus.READY)
        )
            .when(distribution)
            .checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);

        // When
        List<OfferPartialSyncItem> items = new ArrayList<>();
        OfferPartialSyncItem offerPartialSyncItem = new OfferPartialSyncItem();
        offerPartialSyncItem.setContainer(DATA_CATEGORY.getCollectionName());
        offerPartialSyncItem.setTenantId(TENANT_ID);
        offerPartialSyncItem.setFilenames(Lists.newArrayList("file2", "file6", "file5"));

        items.add(offerPartialSyncItem);
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, items);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferSyncStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferSyncStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getSourceOffer()).isEqualTo(SOURCE);
        assertThat(instance.getOfferSyncStatus().getTargetOffer()).isEqualTo(TARGET);
        assertThat(instance.getOfferSyncStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );

        assertThat(targetDataFiles).containsKeys("file2", "file6");

        ArgumentCaptor<DataContext> contextArgumentCaptor = forClass(DataContext.class);
        ArgumentCaptor<String> strategyCaptor = forClass(String.class);
        ArgumentCaptor<List<String>> offersCaptor = forClass(List.class);
        verify(distribution, times(1)).deleteObjectInOffers(
            strategyCaptor.capture(),
            contextArgumentCaptor.capture(),
            offersCaptor.capture()
        );
        assertThat(contextArgumentCaptor.getValue().getObjectId()).isEqualTo("file5");

        ArgumentCaptor<String> fileName = forClass(String.class);

        verify(distribution, times(2)).storeDataInOffers(
            forClass(String.class).capture(),
            forClass(String.class).capture(),
            fileName.capture(),
            forClass(DataCategory.class).capture(),
            forClass(String.class).capture(),
            forClass(List.class).capture(),
            forClass(Response.class).capture()
        );

        assertThat(fileName.getAllValues()).contains("file2", "file6");
        verify(distribution, times(1)).createAccessRequestIfRequired(
            STRATEGY,
            SOURCE,
            DATA_CATEGORY,
            List.of("file2", "file6", "file5")
        );

        verify(distribution, times(3)).checkAccessRequestStatuses(STRATEGY, SOURCE, List.of(ACCESS_REQUEST_1), false);
        verify(distribution, times(1)).removeAccessRequest(STRATEGY, SOURCE, ACCESS_REQUEST_1, false);
    }

    @Test
    @RunWithCustomExecutor
    public void partial_synchronize_delete_in_target_files_not_in_source_offer() throws Exception {
        // Given
        givenDataSetInSourceOfferPart1(CONTAINER_1);
        givenDataSetInTargetOffer();

        assertThat(targetDataFiles).hasSize(1);
        assertThat(targetDataFiles).containsKeys("file11");

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService, distribution, 100, 1, 1, 1, 1);

        // When
        List<OfferPartialSyncItem> items = new ArrayList<>();
        OfferPartialSyncItem offerPartialSyncItem = new OfferPartialSyncItem();
        offerPartialSyncItem.setContainer(DATA_CATEGORY.getCollectionName());
        offerPartialSyncItem.setTenantId(TENANT_ID);
        offerPartialSyncItem.setFilenames(Lists.newArrayList("file11", "file1", "file2"));

        items.add(offerPartialSyncItem);
        instance.synchronize(executorService, SOURCE, TARGET, STRATEGY, items);

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferSyncStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferSyncStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getSourceOffer()).isEqualTo(SOURCE);
        assertThat(instance.getOfferSyncStatus().getTargetOffer()).isEqualTo(TARGET);
        assertThat(instance.getOfferSyncStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );

        assertThat(targetDataFiles).containsKeys("file1", "file2");

        ArgumentCaptor<DataContext> contextArgumentCaptor = forClass(DataContext.class);
        ArgumentCaptor<String> strategyCaptor = forClass(String.class);
        ArgumentCaptor<List<String>> offersCaptor = forClass(List.class);
        verify(distribution, times(1)).deleteObjectInOffers(
            strategyCaptor.capture(),
            contextArgumentCaptor.capture(),
            offersCaptor.capture()
        );
        assertThat(contextArgumentCaptor.getValue().getObjectId()).isEqualTo("file11");

        ArgumentCaptor<String> fileName = forClass(String.class);

        verify(distribution, times(2)).storeDataInOffers(
            forClass(String.class).capture(),
            forClass(String.class).capture(),
            fileName.capture(),
            forClass(DataCategory.class).capture(),
            forClass(String.class).capture(),
            forClass(List.class).capture(),
            forClass(Response.class).capture()
        );

        assertThat(fileName.getAllValues()).contains("file1", "file2");
    }

    private void givenDataSetInSourceOffer() {
        givenDataSetInSourceOfferPart1(CONTAINER_1);
        givenDataSetInSourceOfferPart2(CONTAINER_1);
    }

    private void givenDataSetInSourceOfferPart1(String container) {
        givenFileWriteOrder(container, "file1", 1L, "data1".getBytes());
        givenFileWriteOrder(container, "file2", 2L, "data2".getBytes());
    }

    private void givenDataSetInTargetOffer() {
        targetDataFiles.put("file11", "data11".getBytes());
    }

    private void givenDataSetInSourceOfferPart2(String container) {
        givenFileDeleteOrder("file1", 3L, container);
        givenFileWriteOrder(container, "file2", 4L, "data2-v2".getBytes());
        givenFileWriteOrder(container, "file3", 5L, "data3".getBytes());
        givenFileWriteOrder(container, "file2", 6L, "data2-v3".getBytes());
        givenFileWriteOrder(container, "file4", 7L, "data4".getBytes());
        givenFileWriteOrder(container, "file4", 8L, "data4-v2".getBytes());
        givenFileWriteOrder(container, "file2", 9L, "data2-v4".getBytes());
        givenFileWriteOrder(container, "file5", 10L, "data5".getBytes());
        givenFileWriteOrder(container, "file6", 11L, "data6".getBytes());
        givenFileDeleteOrder("file5", 12L, container);
    }

    private void givenFileWriteOrder(String container, String filename, long sequence, byte[] bytes) {
        sourceOfferLogs.add(new OfferLog(container, filename, OfferLogAction.WRITE).setSequence(sequence));
        sourceDataFiles.put(filename, bytes);
    }

    private void givenFileDeleteOrder(String filename, long sequence, String container) {
        sourceOfferLogs.add(new OfferLog(container, filename, OfferLogAction.DELETE).setSequence(sequence));
        sourceDataFiles.remove(filename);
    }

    private void verifySynchronizationStatus(OfferSyncProcess instance, Long startOffset, Long endOffset) {
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferSyncStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferSyncStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferSyncStatus().getSourceOffer()).isEqualTo(SOURCE);
        assertThat(instance.getOfferSyncStatus().getTargetOffer()).isEqualTo(TARGET);
        assertThat(instance.getOfferSyncStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferSyncStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
        assertThat(instance.getOfferSyncStatus().getStartOffset()).isEqualTo(startOffset);
        assertThat(instance.getOfferSyncStatus().getCurrentOffset()).isEqualTo(endOffset);
    }
}
