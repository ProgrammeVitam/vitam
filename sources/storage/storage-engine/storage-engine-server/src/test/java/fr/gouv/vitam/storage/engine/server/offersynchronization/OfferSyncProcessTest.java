package fr.gouv.vitam.storage.engine.server.offersynchronization;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWithCustomExecutor
public class OfferSyncProcessTest {

    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;
    private static final String CONTAINER = "2_unit";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private RestoreOfferBackupService restoreOfferBackupService;

    @Mock
    private StorageDistribution distribution;

    private List<OfferLog> sourceOfferLogs;
    private Map<String, byte[]> sourceDataFiles;
    private Map<String, byte[]> targetDataFiles;

    @Before
    public void setup() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));

        sourceOfferLogs = new ArrayList<>();
        sourceDataFiles = new HashMap<>();
        targetDataFiles = new HashMap<>();

        doAnswer((args) -> {

            Long offsetLong = args.getArgument(3);
            long offset = (offsetLong == null) ? 0L : offsetLong;
            int limit = args.getArgument(4);

            return sourceOfferLogs.stream()
                .filter(offerLog -> offerLog.getSequence() >= offset)
                .limit(limit)
                .collect(Collectors.toList());

        }).when(restoreOfferBackupService).getListing(
            eq(VitamConfiguration.getDefaultStrategy()), eq(SOURCE), eq(DATA_CATEGORY), any(), anyInt(), eq(Order.ASC));

        doAnswer((args) -> {

            String filename = args.getArgument(1);
            byte[] data = sourceDataFiles.get(filename);
            if (data == null) {
                throw new StorageNotFoundException("not found");
            }
            return Response.ok(data).build();

        }).when(distribution).getContainerByCategory(eq(VitamConfiguration.getDefaultStrategy()), anyString(), eq(DATA_CATEGORY), eq(SOURCE));

        doAnswer((args) -> {

            String filename = args.getArgument(1);
            Response response = args.getArgument(5);

            targetDataFiles.put(filename, (byte[]) response.getEntity());
            return null;

        }).when(distribution)
            .storeDataInOffers(eq(VitamConfiguration.getDefaultStrategy()), anyString(), eq(DATA_CATEGORY), eq(null),
                eq(singletonList(TARGET)), any());

        doAnswer((args) -> {

            DataContext dataContext = args.getArgument(1);
            assertThat(dataContext.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(dataContext.getRequester()).isNull();
            assertThat(dataContext.getCategory()).isEqualTo(DATA_CATEGORY);

            targetDataFiles.remove(dataContext.getObjectId());
            return null;

        }).when(distribution)
            .deleteObjectInOffers(eq(VitamConfiguration.getDefaultStrategy()), any(), eq(singletonList(TARGET)));
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeEmptyOffer() throws Exception {

        // Given
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService,
            distribution, 10, 4, 1, 1, 1);

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, null);

        verify(distribution, never()).storeDataInOffers(anyString(), anyString(), any(), any(), any(), any());
        verify(distribution, never()).deleteObjectInOffers(any(), any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchSingleBatch() throws Exception {

        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService,
            distribution, 100, 4, 1, 1, 1);

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, null);

        // Then
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
        verifySynchronizationStatus(instance, null, 12L);

        // First batch [sequence 1..12] :
        //  - Written   : file2, file3, file4, file6
        //  - Deleted   : file1, file5
        verify(distribution, times(4)).storeDataInOffers(anyString(), anyString(), any(), any(), any(), any());
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromScratchMultiBatch() throws Exception {

        // Given
        givenDataSetInSourceOffer();

        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService,
            distribution, 10, 4, 1, 1, 1);

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, null);

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

        verify(distribution, times(4)).storeDataInOffers(anyString(), anyString(), any(), any(), any(), any());
        verify(distribution, times(2)).deleteObjectInOffers(any(), any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromOffsetWithNewChanges() throws Exception {

        /*
         * Synchronize part 1
         */

        // Given
        givenDataSetInSourceOfferPart1();
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService,
            distribution, 100, 4, 1, 1, 1);

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, null);

        // Then
        verifySynchronizationStatus(instance, null, 2L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);

        /*
         * Synchronize part 2
         */

        // Given
        givenDataSetInSourceOfferPart2();

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, 3L);

        // Then
        verifySynchronizationStatus(instance, 3L, 12L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeFromOffsetWithoutNewChanges() throws Exception {

        /*
         * Synchronize part 1
         */

        // Given
        givenDataSetInSourceOfferPart1();
        OfferSyncProcess instance = new OfferSyncProcess(restoreOfferBackupService,
            distribution, 100, 4, 1, 1, 1);

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, null);

        // Then
        verifySynchronizationStatus(instance, null, 2L);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);

        /*
         * Synchronize part 2
         */

        // Given not updates

        // When
        instance.synchronize(SOURCE, TARGET, VitamConfiguration.getDefaultStrategy(), DATA_CATEGORY, 3L);

        // Then
        verifySynchronizationStatus(instance, 3L, null);
        assertThat(targetDataFiles).isEqualTo(sourceDataFiles);
    }

    private void givenDataSetInSourceOffer() {
        givenDataSetInSourceOfferPart1();
        givenDataSetInSourceOfferPart2();
    }

    private void givenDataSetInSourceOfferPart1() {
        givenFileWriteOrder("file1", 1L, "data1".getBytes());
        givenFileWriteOrder("file2", 2L, "data2".getBytes());
    }

    private void givenDataSetInSourceOfferPart2() {
        givenFileDeleteOrder("file1", 3L);
        givenFileWriteOrder("file2", 4L, "data2-v2".getBytes());
        givenFileWriteOrder("file3", 5L, "data3".getBytes());
        givenFileWriteOrder("file2", 6L, "data2-v3".getBytes());
        givenFileWriteOrder("file4", 7L, "data4".getBytes());
        givenFileWriteOrder("file4", 8L, "data4-v2".getBytes());
        givenFileWriteOrder("file2", 9L, "data2-v4".getBytes());
        givenFileWriteOrder("file5", 10L, "data5".getBytes());
        givenFileWriteOrder("file6", 11L, "data6".getBytes());
        givenFileDeleteOrder("file5", 12L);
    }

    private void givenFileWriteOrder(String filename, long sequence, byte[] bytes) {
        sourceOfferLogs.add(new OfferLog(CONTAINER, filename, OfferLogAction.WRITE).setSequence(sequence));
        sourceDataFiles.put(filename, bytes);
    }

    private void givenFileDeleteOrder(String filename, long sequence) {
        sourceOfferLogs.add(new OfferLog(CONTAINER, filename, OfferLogAction.DELETE).setSequence(sequence));
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
        assertThat(instance.getOfferSyncStatus().getRequestId())
            .isEqualTo(VitamThreadUtils.getVitamSession().getRequestId());
        assertThat(instance.getOfferSyncStatus().getStartOffset()).isEqualTo(startOffset);
        assertThat(instance.getOfferSyncStatus().getCurrentOffset()).isEqualTo(endOffset);
    }
}
