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

package fr.gouv.vitam.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.gouv.vitam.storage.StorageTestUtils.getLogbookOperation;
import static fr.gouv.vitam.storage.StorageTestUtils.writeFileToOffers;
import static fr.gouv.vitam.storage.engine.server.storagetraceability.LogbookStorageTraceabilityHelper.STP_STORAGE_SECURISATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StorageLogTraceabilityIT extends VitamRuleRunner {

    public static final String STRATEGY_ID = "default";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        StorageLogTraceabilityIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(LogbookMain.class, WorkspaceMain.class, DefaultOfferMain.class, StorageMain.class)
    );

    private static final int TENANT_0 = 0;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void tearDown() {
        runAfterMongo(Sets.newHashSet(LogbookCollections.OPERATION.getName(), OfferCollections.OFFER_LOG.getName()));

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 2)
        );

        VitamServerRunner.cleanOffers();
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageLogTraceabilityNonAdminTenantThenKO() {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(0);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

            // When / Then
            assertThatThrownBy(() -> storageClient.storageLogTraceability(Arrays.asList(0, 1))).isInstanceOf(
                StorageServerClientException.class
            );
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageLogTraceabilityEmptyDataSetThenWarning() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));

            // When
            RequestResponseOK<StorageLogTraceabilityResult> traceabilityResult = storageClient.storageLogTraceability(
                Arrays.asList(0, 1)
            );

            // Then
            assertThat(traceabilityResult.getResults()).hasSize(2);
            assertThat(traceabilityResult.getResults().get(0).getOperationId()).isNotNull();
            assertThat(traceabilityResult.getResults().get(0).getTenantId()).isEqualTo(0);
            assertThat(traceabilityResult.getResults().get(1).getOperationId()).isNotNull();
            assertThat(traceabilityResult.getResults().get(1).getTenantId()).isEqualTo(1);

            // Check logbook operation
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            LogbookOperation logbookOperation = getLogbookOperation(
                traceabilityResult.getResults().get(0).getOperationId()
            );

            checkTraceabilityOperationStatus(logbookOperation, StatusCode.WARNING);

            // Ensure no storage log stored
            try (
                CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                    STRATEGY_ID,
                    null,
                    DataCategory.STORAGELOG
                )
            ) {
                assertThat(storageLogs).isEmpty();
            } catch (StorageNotFoundClientException ignored) {
                // Might be thrown if contain not exists
            }

            // Ensure no storage log traceability zip generated
            try (
                CloseableIterator<ObjectEntry> storageTraceabilityZips = storageClient.listContainer(
                    STRATEGY_ID,
                    null,
                    DataCategory.STORAGETRACEABILITY
                )
            ) {
                assertThat(storageTraceabilityZips).isEmpty();
            } catch (StorageNotFoundClientException ignored) {
                // Might be thrown if contain not exists
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageLogTraceabilityFirstTraceability() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(0);

            int size1 = RandomUtils.nextInt(0, 100_000);
            String objectId1 = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId1, size1);

            // When
            VitamThreadUtils.getVitamSession().setTenantId(1);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            storageClient.storageLogBackup(Arrays.asList(0, 1));

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            RequestResponseOK<StorageLogTraceabilityResult> traceabilityResult = storageClient.storageLogTraceability(
                Arrays.asList(0, 1)
            );

            // Then

            // Check result operations
            assertThat(traceabilityResult.getResults()).hasSize(2);
            assertThat(traceabilityResult.getResults().get(0).getOperationId()).isNotNull();
            assertThat(traceabilityResult.getResults().get(0).getTenantId()).isEqualTo(0);
            assertThat(traceabilityResult.getResults().get(1).getOperationId()).isNotNull();
            assertThat(traceabilityResult.getResults().get(1).getTenantId()).isEqualTo(1);

            // Check logbook operation
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            LogbookOperation logbookOperation = getLogbookOperation(
                traceabilityResult.getResults().get(0).getOperationId()
            );

            checkTraceabilityOperationStatus(logbookOperation, StatusCode.OK);

            assertThat(logbookOperation.getEvDetData()).isNotEmpty();
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(
                logbookOperation.getEvDetData(),
                TraceabilityEvent.class
            );
            assertThat(traceabilityEvent.getHash()).isNotNull();
            assertThat(traceabilityEvent.getTimeStampToken()).isNotNull();
            assertThat(traceabilityEvent.getNumberOfElements()).isEqualTo(1);
            assertThat(traceabilityEvent.getFileName()).isNotNull();
            assertThat(traceabilityEvent.getPreviousLogbookTraceabilityDate()).isNull();

            // Check Traceability ZIP
            downloadZip(traceabilityEvent.getFileName(), tmpFolder.getRoot());
            List<JsonNode> traceabilityEntries = parseTraceabilityDataFile(tmpFolder.getRoot());
            assertThat(traceabilityEntries).hasSize(1);

            // Check storage log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGELOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            String storageLogObjectId = objectEntries.get(0).getObjectId();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGELOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            Digest storageLogDigest = new Digest(DigestType.SHA512).update(inputStream);

            assertThat(traceabilityEntries.get(0).get("FileName").asText()).isEqualTo(storageLogObjectId);
            assertThat(traceabilityEntries.get(0).get("Hash").asText()).isEqualTo(storageLogDigest.digest64());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageLogTraceabilityTraceabilityChaining() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given : Existing traceability
            VitamThreadUtils.getVitamSession().setTenantId(0);

            int size1 = RandomUtils.nextInt(0, 100_000);
            String objectId1 = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId1, size1);

            VitamThreadUtils.getVitamSession().setTenantId(1);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            storageClient.storageLogBackup(Collections.singletonList(0));

            this.logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            RequestResponseOK<StorageLogTraceabilityResult> traceabilityResult1 = storageClient.storageLogTraceability(
                Collections.singletonList(0)
            );

            this.logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            // When : Next traceability
            VitamThreadUtils.getVitamSession().setTenantId(0);

            int size2 = RandomUtils.nextInt(0, 100_000);
            String objectId2 = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId2, size2);

            VitamThreadUtils.getVitamSession().setTenantId(1);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            storageClient.storageLogBackup(Collections.singletonList(0));

            this.logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(1));
            RequestResponseOK<StorageLogTraceabilityResult> traceabilityResult2 = storageClient.storageLogTraceability(
                Collections.singletonList(0)
            );

            // Then

            // Check result operations
            assertThat(traceabilityResult1.getResults()).hasSize(1);
            assertThat(traceabilityResult1.getResults().get(0).getOperationId()).isNotNull();
            assertThat(traceabilityResult1.getResults().get(0).getTenantId()).isEqualTo(0);

            assertThat(traceabilityResult2.getResults()).hasSize(1);
            assertThat(traceabilityResult2.getResults().get(0).getOperationId()).isNotNull();
            assertThat(traceabilityResult2.getResults().get(0).getTenantId()).isEqualTo(0);

            // Check logbook operation status
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

            LogbookOperation logbookOperation1 = getLogbookOperation(
                traceabilityResult1.getResults().get(0).getOperationId()
            );
            LogbookOperation logbookOperation2 = getLogbookOperation(
                traceabilityResult2.getResults().get(0).getOperationId()
            );

            checkTraceabilityOperationStatus(logbookOperation1, StatusCode.OK);
            checkTraceabilityOperationStatus(logbookOperation2, StatusCode.OK);

            // Check event details
            assertThat(logbookOperation1.getEvDetData()).isNotEmpty();
            TraceabilityEvent traceabilityEvent1 = JsonHandler.getFromString(
                logbookOperation1.getEvDetData(),
                TraceabilityEvent.class
            );
            assertThat(traceabilityEvent1.getFileName()).isNotEmpty();

            assertThat(logbookOperation2.getEvDetData()).isNotEmpty();
            TraceabilityEvent traceabilityEvent2 = JsonHandler.getFromString(
                logbookOperation2.getEvDetData(),
                TraceabilityEvent.class
            );
            assertThat(traceabilityEvent2.getHash()).isNotNull();
            assertThat(traceabilityEvent2.getTimeStampToken()).isNotNull();
            assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(1);

            // Check chaining
            assertThat(
                LocalDateUtil.parseMongoFormattedDate(traceabilityEvent2.getPreviousLogbookTraceabilityDate())
            ).isEqualTo(
                LocalDateUtil.parseMongoFormattedDate(traceabilityEvent1.getEndDate())
                    .minus(5, ChronoUnit.MINUTES)
                    .truncatedTo(ChronoUnit.SECONDS)
            );

            // Check Traceability ZIPs
            File zip1Folder = tmpFolder.newFolder("zip1");
            downloadZip(traceabilityEvent1.getFileName(), zip1Folder);
            List<JsonNode> traceabilityEntries1 = parseTraceabilityDataFile(zip1Folder);
            assertThat(traceabilityEntries1).hasSize(1);

            File zip2Folder = tmpFolder.newFolder("zip2");
            downloadZip(traceabilityEvent2.getFileName(), zip2Folder);
            List<JsonNode> traceabilityEntries2 = parseTraceabilityDataFile(zip2Folder);
            assertThat(traceabilityEntries2).hasSize(1);

            // Check storage logs
            String storageLogObjectId1 = traceabilityEntries1.get(0).get("FileName").asText();
            String storageLogObjectId2 = traceabilityEntries2.get(0).get("FileName").asText();
            assertThat(storageLogObjectId1).isNotEqualTo(storageLogObjectId2);

            Response storageLogResponse1 = storageClient.getContainerAsync(
                STRATEGY_ID,
                storageLogObjectId1,
                DataCategory.STORAGELOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream1 = storageLogResponse1.readEntity(InputStream.class);
            Digest storageLogDigest1 = new Digest(DigestType.SHA512).update(inputStream1);
            assertThat(traceabilityEntries1.get(0).get("Hash").asText()).isEqualTo(storageLogDigest1.digest64());

            Response storageLogResponse2 = storageClient.getContainerAsync(
                STRATEGY_ID,
                storageLogObjectId2,
                DataCategory.STORAGELOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream2 = storageLogResponse2.readEntity(InputStream.class);
            Digest storageLogDigest2 = new Digest(DigestType.SHA512).update(inputStream2);
            assertThat(traceabilityEntries2.get(0).get("Hash").asText()).isEqualTo(storageLogDigest2.digest64());
        }
    }

    private void checkTraceabilityOperationStatus(LogbookOperation logbookOperation, StatusCode ok) {
        assertThat(logbookOperation.getEvTypeProc()).isEqualTo(LogbookTypeProcess.TRACEABILITY.name());
        assertThat(logbookOperation.getEvType()).isEqualTo(STP_STORAGE_SECURISATION);
        assertThat(logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()).isEqualTo(
            STP_STORAGE_SECURISATION + "." + ok.name()
        );
    }

    private void downloadZip(String fileName, File folder)
        throws IOException, StorageNotFoundException, StorageServerClientException, StorageUnavailableDataFromAsyncOfferClientException {
        try (
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response containerAsync = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                DataCategory.STORAGETRACEABILITY,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = containerAsync.readEntity(InputStream.class);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(new File(folder, entry.getName()))) {
                    IOUtils.copy(zipInputStream, fileOutputStream);
                }
            }
        }
    }

    private List<JsonNode> parseTraceabilityDataFile(File zip1Folder) throws IOException {
        return FileUtils.readLines(new File(zip1Folder, "data.txt"), StandardCharsets.UTF_8)
            .stream()
            .map(line -> {
                try {
                    return JsonHandler.getFromString(line);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }
}
