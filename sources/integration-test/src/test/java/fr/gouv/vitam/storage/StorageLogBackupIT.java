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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.collections4.IteratorUtils;
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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.STORAGE_BACKUP;
import static fr.gouv.vitam.storage.StorageTestUtils.deleteFile;
import static fr.gouv.vitam.storage.StorageTestUtils.getLogbookOperation;
import static fr.gouv.vitam.storage.StorageTestUtils.writeFileToOffers;
import static fr.gouv.vitam.storage.engine.server.storagelog.StorageLogAdministration.STORAGE_WRITE_BACKUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StorageLogBackupIT extends VitamRuleRunner {

    public static final TypeReference<JsonNode> TYPE_REFERENCE = new TypeReference<>() {};
    public static final String STRATEGY_ID = "default";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        StorageLogBackupIT.class,
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
    public void testStorageLogBackupNonAdminTenantThenKO() {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(0);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

            // When / Then
            assertThatThrownBy(() -> storageClient.storageLogBackup(Arrays.asList(0, 1))).isInstanceOf(
                StorageServerClientException.class
            );
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageLogBackupOK() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(0);

            int size1 = RandomUtils.nextInt(0, 100_000);
            String objectId1 = GUIDFactory.newGUID().getId();

            int size2 = RandomUtils.nextInt(0, 100_000);
            String objectId2 = GUIDFactory.newGUID().getId();

            LocalDateTime beforeWrite1 = LocalDateUtil.now();
            Digest digest1 = writeFileToOffers(objectId1, size1);
            LocalDateTime afterWrite1 = LocalDateUtil.now();

            LocalDateTime beforeWrite2 = LocalDateUtil.now();
            Digest digest2 = writeFileToOffers(objectId2, size2);
            LocalDateTime afterWrite2 = LocalDateUtil.now();

            LocalDateTime beforeDelete1 = LocalDateUtil.now();
            deleteFile(objectId1);
            LocalDateTime afterDelete1 = LocalDateUtil.now();

            // When
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));
            RequestResponseOK<StorageLogBackupResult> storageLogBackupResults = storageClient.storageLogBackup(
                Arrays.asList(0, 1)
            );

            // Then

            // Check result operations
            assertThat(storageLogBackupResults.getResults()).hasSize(2);
            assertThat(storageLogBackupResults.getResults().get(0).getOperationId()).isNotNull();
            assertThat(storageLogBackupResults.getResults().get(0).getTenantId()).isEqualTo(0);
            assertThat(storageLogBackupResults.getResults().get(1).getOperationId()).isNotNull();
            assertThat(storageLogBackupResults.getResults().get(1).getTenantId()).isEqualTo(1);

            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            LogbookOperation logbookOperation = getLogbookOperation(
                storageLogBackupResults.getResults().get(0).getOperationId()
            );

            assertThat(logbookOperation.getEvTypeProc()).isEqualTo(STORAGE_BACKUP.name());
            assertThat(logbookOperation.getEvType()).isEqualTo(STORAGE_WRITE_BACKUP);
            assertThat(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()
            ).isEqualTo(STORAGE_WRITE_BACKUP + "." + StatusCode.OK.name());

            // Check storage log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGELOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isNotNull();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGELOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            List<JsonNode> logs = IteratorUtils.toList(new JsonLineGenericIterator<>(inputStream, TYPE_REFERENCE));

            // Check storage log content
            JsonNode object1WriteLog = logs
                .stream()
                .filter(log -> log.get(StorageLogbookParameterName.objectIdentifier.name()).asText().equals(objectId1))
                .filter(log -> log.get(StorageLogbookParameterName.eventType.name()).asText().equals("CREATE"))
                .findFirst()
                .orElseThrow(AssertionError::new);
            JsonNode object2WriteLog = logs
                .stream()
                .filter(log -> log.get(StorageLogbookParameterName.objectIdentifier.name()).asText().equals(objectId2))
                .filter(log -> log.get(StorageLogbookParameterName.eventType.name()).asText().equals("CREATE"))
                .findFirst()
                .orElseThrow(AssertionError::new);
            JsonNode object1DeleteLog = logs
                .stream()
                .filter(log -> log.get(StorageLogbookParameterName.objectIdentifier.name()).asText().equals(objectId1))
                .filter(log -> log.get(StorageLogbookParameterName.eventType.name()).asText().equals("DELETE"))
                .findFirst()
                .orElseThrow(AssertionError::new);

            checkStorageLogWriteEntry(object1WriteLog, objectId1, size1, digest1, beforeWrite1, afterWrite1);
            checkStorageLogWriteEntry(object2WriteLog, objectId2, size2, digest2, beforeWrite2, afterWrite2);
            checkStorageLogDeleteEntry(object1DeleteLog, objectId1, beforeDelete1, afterDelete1);
        }
    }

    private void checkStorageLogWriteEntry(
        JsonNode storageLog,
        String objectId,
        int size,
        Digest digest,
        LocalDateTime beforeWrite,
        LocalDateTime afterWrite
    ) {
        assertThat(storageLog.get(StorageLogbookParameterName.objectIdentifier.name()).asText()).isEqualTo(objectId);
        assertThat(storageLog.get(StorageLogbookParameterName.outcome.name()).asText()).isEqualTo(
            StorageLogbookOutcome.OK.name()
        );
        assertThat(storageLog.get(StorageLogbookParameterName.digest.name()).asText()).isEqualTo(digest.digestHex());
        assertThat(storageLog.get(StorageLogbookParameterName.size.name()).asLong()).isEqualTo(size);
        assertThat(storageLog.get(StorageLogbookParameterName.eventType.name()).asText()).isEqualTo("CREATE");
        assertThat(storageLog.get(StorageLogbookParameterName.tenantId.name()).asInt()).isEqualTo(TENANT_0);
        assertThat(storageLog.get(StorageLogbookParameterName.eventDateTime.name()).asText()).isBetween(
            LocalDateUtil.getFormattedDateTimeForMongo(beforeWrite),
            LocalDateUtil.getFormattedDateTimeForMongo(afterWrite)
        );
    }

    private void checkStorageLogDeleteEntry(
        JsonNode storageLog,
        String objectId,
        LocalDateTime beforeWrite,
        LocalDateTime afterWrite
    ) {
        assertThat(storageLog.get(StorageLogbookParameterName.objectIdentifier.name()).asText()).isEqualTo(objectId);
        assertThat(storageLog.get(StorageLogbookParameterName.outcome.name()).asText()).isEqualTo(
            StorageLogbookOutcome.OK.name()
        );
        assertThat(storageLog.get(StorageLogbookParameterName.eventType.name()).asText()).isEqualTo("DELETE");
        assertThat(storageLog.get(StorageLogbookParameterName.tenantId.name()).asInt()).isEqualTo(TENANT_0);
        assertThat(storageLog.get(StorageLogbookParameterName.eventDateTime.name()).asText()).isBetween(
            LocalDateUtil.getFormattedDateTimeForMongo(beforeWrite),
            LocalDateUtil.getFormattedDateTimeForMongo(afterWrite)
        );
    }
}
