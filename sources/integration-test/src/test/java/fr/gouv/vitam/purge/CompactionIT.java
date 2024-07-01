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
package fr.gouv.vitam.purge;

import com.google.common.collect.Sets;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.input.NullInputStream;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.VitamTestHelper.prepareVitamSession;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_SEQUENCE;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.storage.engine.common.model.Order.ASC;
import static fr.gouv.vitam.storage.engine.common.model.Order.DESC;
import static org.assertj.core.api.Assertions.assertThat;

public class CompactionIT extends VitamRuleRunner {

    private static final Integer tenantId = 2;

    private static final String STRATEGY = "default";
    private static final String OFFER_ID = "default";
    private static final String OFFER_URL = "http://localhost:" + VitamServerRunner.PORT_SERVICE_OFFER;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        CompactionIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(WorkspaceMain.class, StorageMain.class, DefaultOfferMain.class)
    );

    private static CompactionRestService compactionRestService;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2));
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(OFFER_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

        compactionRestService = retrofit.create(CompactionRestService.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        ProcessDataAccessImpl.getInstance().clearWorkflow();
        clearOfferLogs();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
        clearOfferLogs();
    }

    private void clearOfferLogs() {
        runAfterMongo(Sets.newHashSet(OFFER_LOG.getName(), COMPACTED_OFFER_LOG.getName(), OFFER_SEQUENCE.getName()));
    }

    @RunWithCustomExecutor
    @Test
    public void should_compact_offer_log() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            prepareVitamSession(tenantId, "aName3", "Context_IT");

            for (int i = 1; i <= 6; i++) {
                storageClient.create(
                    STRATEGY,
                    "unit" + i,
                    UNIT,
                    new NullInputStream(i),
                    (long) i,
                    Collections.singletonList(OFFER_ID)
                );
            }

            List<OfferLog> offerLogsDescResponseBeforeCompaction = getOfferLogsFromStorage(
                storageClient,
                UNIT,
                null,
                6,
                DESC
            );
            List<OfferLog> offerLogsAscResponseBeforeCompaction = getOfferLogsFromStorage(
                storageClient,
                UNIT,
                null,
                6,
                ASC
            );

            logicalClock.logicalSleep(22, ChronoUnit.DAYS);

            compactOfferLogs();

            List<CompactedOfferLog> offerLogsCompaction = getDbCompactedOfferLogs(tenantId + "_unit");
            List<OfferLog> offerLogsFromCompaction = offerLogsCompaction.iterator().next().getLogs();
            List<OfferLog> offerLogs = getDbOfferLogs(tenantId + "_unit");
            assertThat(offerLogsCompaction).as("check offer logs compaction collection is not empty").isNotEmpty();
            assertThat(offerLogs).as("check offer logs compaction collection is empty").isEmpty();

            List<OfferLog> offerLogsDesc = getOfferLogsFromStorage(storageClient, UNIT, null, 6, DESC);
            List<OfferLog> offerLogsAsc = getOfferLogsFromStorage(storageClient, UNIT, null, 6, ASC);

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query is not empty").isNotEmpty();
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query is not empty").isNotEmpty();

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query size").hasSize(6);
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query size").hasSize(6);

            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsAsc);
            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsDesc);

            assertThat(offerLogsDesc).containsExactlyElementsOf(offerLogsDescResponseBeforeCompaction);
            assertThat(offerLogsAsc).containsExactlyElementsOf(offerLogsAscResponseBeforeCompaction);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void should_retrieve_complex_offer_log_from_OfferLog_and_CompactedOfferLog() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");

        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // When
            for (int i = 1; i <= 20; i++) {
                storageClient.create(
                    STRATEGY,
                    "unit" + i,
                    UNIT,
                    new NullInputStream(i),
                    (long) i,
                    Collections.singletonList(OFFER_ID)
                );
            }

            logicalClock.logicalSleep(25, ChronoUnit.DAYS);

            compactOfferLogs();

            for (int i = 21; i <= 40; i++) {
                storageClient.create(
                    STRATEGY,
                    "unit" + i,
                    UNIT,
                    new NullInputStream(i),
                    (long) i,
                    Collections.singletonList(OFFER_ID)
                );
            }
            for (int i = 41; i <= 60; i++) {
                storageClient.create(
                    STRATEGY,
                    "object" + i,
                    OBJECT,
                    new NullInputStream(i),
                    (long) i,
                    Collections.singletonList(OFFER_ID)
                );
            }
            for (int i = 61; i <= 80; i++) {
                storageClient.create(
                    STRATEGY,
                    "unit" + i,
                    UNIT,
                    new NullInputStream(i),
                    (long) i,
                    Collections.singletonList(OFFER_ID)
                );
            }

            logicalClock.logicalSleep(15, ChronoUnit.DAYS);

            for (int i = 81; i <= 90; i++) {
                storageClient.delete(STRATEGY, UNIT, "unit" + i, Collections.singletonList(OFFER_ID));
            }
            for (int i = 91; i <= 100; i++) {
                storageClient.delete(STRATEGY, OBJECT, "object" + i, Collections.singletonList(OFFER_ID));
            }

            logicalClock.logicalSleep(10, ChronoUnit.DAYS);

            compactOfferLogs();

            // Then
            // Technical checks
            List<CompactedOfferLog> offerLogsCompaction = getDbCompactedOfferLogs(
                tenantId + "_unit",
                tenantId + "_object"
            );
            assertThat(offerLogsCompaction).hasSize(3);

            assertThat(offerLogsCompaction.get(0).getContainer()).isEqualTo(tenantId + "_unit");
            assertThat(offerLogsCompaction.get(0).getSequenceStart()).isEqualTo(1L);
            assertThat(offerLogsCompaction.get(0).getSequenceEnd()).isEqualTo(20L);
            assertThat(offerLogsCompaction.get(0).getLogs()).hasSize(20);
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getFileName()).isEqualTo("unit1");
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getSequence()).isEqualTo(1L);
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getAction()).isEqualTo(OfferLogAction.WRITE);
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getFileName()).isEqualTo("unit20");
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getSequence()).isEqualTo(20L);
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getAction()).isEqualTo(OfferLogAction.WRITE);

            assertThat(offerLogsCompaction.get(1).getContainer()).isEqualTo(tenantId + "_unit");
            assertThat(offerLogsCompaction.get(1).getSequenceStart()).isEqualTo(21L);
            assertThat(offerLogsCompaction.get(1).getSequenceEnd()).isEqualTo(80L);
            assertThat(offerLogsCompaction.get(1).getLogs()).hasSize(40);
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getFileName()).isEqualTo("unit21");
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getSequence()).isEqualTo(21L);
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getAction()).isEqualTo(OfferLogAction.WRITE);
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getFileName()).isEqualTo("unit80");
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getSequence()).isEqualTo(80L);
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getAction()).isEqualTo(OfferLogAction.WRITE);

            assertThat(offerLogsCompaction.get(2).getContainer()).isEqualTo(tenantId + "_object");
            assertThat(offerLogsCompaction.get(2).getSequenceStart()).isEqualTo(41L);
            assertThat(offerLogsCompaction.get(2).getSequenceEnd()).isEqualTo(60L);
            assertThat(offerLogsCompaction.get(2).getLogs()).hasSize(20);
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getFileName()).isEqualTo("object45");
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getSequence()).isEqualTo(45L);
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getAction()).isEqualTo(OfferLogAction.WRITE);

            List<OfferLog> offerLogs = getDbOfferLogs(tenantId + "_unit", tenantId + "_object");
            assertThat(offerLogs).hasSize(20);
            for (int i = 0; i < 10; i++) {
                OfferLog offerLog = offerLogs.get(i);
                assertThat(offerLog.getContainer()).isEqualTo(tenantId + "_unit");
                assertThat(offerLog.getFileName()).isEqualTo("unit" + (i + 81));
                assertThat(offerLog.getSequence()).isEqualTo(i + 81);
                assertThat(offerLog.getAction()).isEqualTo(OfferLogAction.DELETE);
            }
            for (int i = 0; i < 10; i++) {
                OfferLog offerLog = offerLogs.get(i + 10);
                assertThat(offerLog.getContainer()).isEqualTo(tenantId + "_object");
                assertThat(offerLog.getFileName()).isEqualTo("object" + (i + 91));
                assertThat(offerLog.getSequence()).isEqualTo(i + 91);
                assertThat(offerLog.getAction()).isEqualTo(OfferLogAction.DELETE);
            }

            // Functional checks
            List<OfferLog> expectedUnitOfferLogs = Stream.concat(
                Stream.concat(
                    IntStream.rangeClosed(1, 40).mapToObj(
                        i -> new OfferLog(i, null, tenantId + "_unit", "unit" + i, OfferLogAction.WRITE)
                    ),
                    IntStream.rangeClosed(61, 80).mapToObj(
                        i -> new OfferLog(i, null, tenantId + "_unit", "unit" + i, OfferLogAction.WRITE)
                    )
                ),
                IntStream.rangeClosed(81, 90).mapToObj(
                    i -> new OfferLog(i, null, tenantId + "_unit", "unit" + i, OfferLogAction.DELETE)
                )
            ).collect(Collectors.toList());

            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 13, Order.DESC);

            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 13, Order.DESC);
        }
    }

    private void compactOfferLogs() throws java.io.IOException {
        retrofit2.Response<Void> response = compactionRestService.launchOfferLogCompaction().execute();

        assertThat(response.isSuccessful()).as("check request compaction successfully execute").isTrue();
    }

    private void checkOfferLogs(
        StorageClient storageClient,
        List<OfferLog> unitOfferLogs,
        Long offset,
        int limit,
        Order order
    ) throws StorageServerClientException {
        List<OfferLog> computedOfferLogs = getOfferLogsFromStorage(
            storageClient,
            DataCategory.UNIT,
            offset,
            limit,
            order
        );
        List<OfferLog> expectedOfferLogs = unitOfferLogs
            .stream()
            .filter(
                log ->
                    offset == null || (order == Order.ASC ? offset <= log.getSequence() : offset >= log.getSequence())
            )
            .sorted(order == Order.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder())
            .limit(limit)
            .collect(Collectors.toList());

        assertThat(computedOfferLogs).hasSameSizeAs(expectedOfferLogs);
        assertThat(computedOfferLogs)
            .usingElementComparatorOnFields("container", "fileName", "action", "sequence")
            .containsExactlyElementsOf(expectedOfferLogs);
    }

    private List<OfferLog> getOfferLogsFromStorage(
        StorageClient storageClient,
        DataCategory dataCategory,
        Long offset,
        int limit,
        Order order
    ) throws StorageServerClientException {
        RequestResponse<OfferLog> response = storageClient.getOfferLogs(
            STRATEGY,
            null,
            dataCategory,
            offset,
            limit,
            order
        );
        if (!response.isOk()) {
            throw new VitamRuntimeException("Could not list offer log");
        }
        return ((RequestResponseOK<OfferLog>) response).getResults();
    }

    private List<OfferLog> getDbOfferLogs(String... containers) {
        return IteratorUtils.toList(
            mongoRule
                .getMongoDatabase()
                .getCollection(OFFER_LOG.getName())
                .find(in(CONTAINER, containers))
                .sort(Sorts.ascending(CompactedOfferLog.SEQUENCE_START))
                .map(this::mapToOfferLog)
                .iterator()
        );
    }

    private OfferLog mapToOfferLog(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public List<CompactedOfferLog> getDbCompactedOfferLogs(String... containers) {
        return IteratorUtils.toList(
            mongoRule
                .getMongoDatabase()
                .getCollection(COMPACTED_OFFER_LOG.getName())
                .find(in(CONTAINER, containers))
                .sort(Sorts.ascending(CompactedOfferLog.SEQUENCE_START))
                .map(this::mapToOfferLogCompaction)
                .iterator()
        );
    }

    private CompactedOfferLog mapToOfferLogCompaction(Document document) {
        try {
            return BsonHelper.fromDocumentToObject(document, CompactedOfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public interface CompactionRestService {
        @POST("/offer/v1/compaction")
        @Headers({ "Accept: application/json" })
        Call<Void> launchOfferLogCompaction();
    }
}
