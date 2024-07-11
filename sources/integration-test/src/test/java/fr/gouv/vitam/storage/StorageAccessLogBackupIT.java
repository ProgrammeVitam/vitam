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
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
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
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.collections4.IteratorUtils;
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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.STORAGE_BACKUP;
import static fr.gouv.vitam.storage.StorageTestUtils.getLogbookOperation;
import static fr.gouv.vitam.storage.StorageTestUtils.writeFileToOffers;
import static fr.gouv.vitam.storage.engine.server.storagelog.StorageLogAdministration.STORAGE_ACCESS_BACKUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StorageAccessLogBackupIT extends VitamRuleRunner {

    public static final TypeReference<JsonNode> TYPE_REFERENCE = new TypeReference<>() {};
    public static final String STRATEGY_ID = "default";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        StorageAccessLogBackupIT.class,
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
        cleanup();
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
        cleanup();
    }

    private static void cleanup() {
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
    public void testStorageAccessLogBackupNonAdminTenantThenKO() {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(0);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

            // When / Then
            assertThatThrownBy(() -> storageClient.storageAccessLogBackup(Arrays.asList(0, 1))).isInstanceOf(
                StorageServerClientException.class
            );
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageAccessLogBackupEmptyThenOK() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));

            // When
            RequestResponseOK<StorageLogBackupResult> storageLogBackupResults = storageClient.storageAccessLogBackup(
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
            assertThat(logbookOperation.getEvType()).isEqualTo(STORAGE_ACCESS_BACKUP);
            assertThat(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()
            ).isEqualTo(STORAGE_ACCESS_BACKUP + "." + StatusCode.OK.name());

            // Check storage access log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGEACCESSLOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isNotNull();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGEACCESSLOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            assertThat(inputStream.read()).isEqualTo(IOUtils.EOF);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageAccessLogBackupWithNonLoggedAccessThenOK() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));

            int size = RandomUtils.nextInt(0, 100_000);
            String objectId = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId, size);

            // When
            storageClient.getContainerAsync(
                STRATEGY_ID,
                objectId,
                DataCategory.OBJECT,
                AccessLogUtils.getNoLogAccessLog()
            );

            // When
            RequestResponseOK<StorageLogBackupResult> storageLogBackupResults = storageClient.storageAccessLogBackup(
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
            assertThat(logbookOperation.getEvType()).isEqualTo(STORAGE_ACCESS_BACKUP);
            assertThat(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()
            ).isEqualTo(STORAGE_ACCESS_BACKUP + "." + StatusCode.OK.name());

            // Check storage access log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGEACCESSLOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isNotNull();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGEACCESSLOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            assertThat(inputStream.read()).isEqualTo(IOUtils.EOF);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageAccessLogBackupWithAccessLoggingDisabledInAccessContractThanOK() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));

            int size = RandomUtils.nextInt(0, 100_000);
            String objectId = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId, size);

            // When
            AccessContractModel accessContract = new AccessContractModel().setAccessLog(ActivationStatus.INACTIVE);
            VitamThreadUtils.getVitamSession().setContract(accessContract);
            VitamThreadUtils.getVitamSession().setContextId("contextId");
            VitamThreadUtils.getVitamSession().setApplicationSessionId("applicationId");
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());

            storageClient.getContainerAsync(
                STRATEGY_ID,
                objectId,
                DataCategory.OBJECT,
                AccessLogUtils.getInfoForAccessLog(
                    "BinaryMaster",
                    1,
                    VitamThreadUtils.getVitamSession(),
                    (long) size,
                    "unitId"
                )
            );

            RequestResponseOK<StorageLogBackupResult> storageLogBackupResults = storageClient.storageAccessLogBackup(
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
            assertThat(logbookOperation.getEvType()).isEqualTo(STORAGE_ACCESS_BACKUP);
            assertThat(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()
            ).isEqualTo(STORAGE_ACCESS_BACKUP + "." + StatusCode.OK.name());

            // Check storage access log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGEACCESSLOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isNotNull();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGEACCESSLOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            assertThat(inputStream.read()).isEqualTo(IOUtils.EOF);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testStorageAccessLogBackupWithAccessLoggingEnabledInAccessContractThanOK() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));

            int size = RandomUtils.nextInt(0, 100_000);
            String objectId = GUIDFactory.newGUID().getId();
            writeFileToOffers(objectId, size);

            // When
            AccessContractModel accessContract = new AccessContractModel().setAccessLog(ActivationStatus.ACTIVE);
            VitamThreadUtils.getVitamSession().setContract(accessContract);
            VitamThreadUtils.getVitamSession().setContextId("contextId");
            VitamThreadUtils.getVitamSession().setApplicationSessionId("applicationId");
            GUID requestId = GUIDFactory.newGUID();
            VitamThreadUtils.getVitamSession().setRequestId(requestId);

            LocalDateTime beforeAccess = LocalDateUtil.now();
            storageClient.getContainerAsync(
                STRATEGY_ID,
                objectId,
                DataCategory.OBJECT,
                AccessLogUtils.getInfoForAccessLog(
                    "BinaryMaster",
                    1,
                    VitamThreadUtils.getVitamSession(),
                    (long) size,
                    "unitId"
                )
            );
            LocalDateTime afterAccess = LocalDateUtil.now();

            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(1));
            RequestResponseOK<StorageLogBackupResult> storageLogBackupResults = storageClient.storageAccessLogBackup(
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
            assertThat(logbookOperation.getEvType()).isEqualTo(STORAGE_ACCESS_BACKUP);
            assertThat(
                logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1).getOutDetail()
            ).isEqualTo(STORAGE_ACCESS_BACKUP + "." + StatusCode.OK.name());

            // Check storage access log
            CloseableIterator<ObjectEntry> storageLogs = storageClient.listContainer(
                STRATEGY_ID,
                null,
                DataCategory.STORAGEACCESSLOG
            );
            List<ObjectEntry> objectEntries = IteratorUtils.toList(storageLogs);
            assertThat(objectEntries).hasSize(1);
            assertThat(objectEntries.get(0).getObjectId()).isNotNull();

            Response storageLogResponse = storageClient.getContainerAsync(
                STRATEGY_ID,
                objectEntries.get(0).getObjectId(),
                DataCategory.STORAGEACCESSLOG,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = storageLogResponse.readEntity(InputStream.class);
            List<JsonNode> logs = IteratorUtils.toList(new JsonLineGenericIterator<>(inputStream, TYPE_REFERENCE));
            JsonNode storageLog = logs.get(0);
            assertThat(logs).hasSize(1);

            assertThat(storageLog.get(StorageLogbookParameterName.eventDateTime.name()).asText()).isBetween(
                LocalDateUtil.getFormattedDateForMongo(beforeAccess),
                LocalDateUtil.getFormattedDateForMongo(afterAccess)
            );
            assertThat(storageLog.get(StorageLogbookParameterName.xRequestId.name()).asText()).isEqualTo(
                requestId.getId()
            );
            assertThat(storageLog.get(StorageLogbookParameterName.applicationId.name()).asText()).isEqualTo(
                "applicationId"
            );
            assertThat(storageLog.get(StorageLogbookParameterName.objectIdentifier.name()).asText()).isEqualTo(
                objectId
            );
            assertThat(storageLog.get(StorageLogbookParameterName.size.name()).asLong()).isEqualTo(size);
            assertThat(storageLog.get(StorageLogbookParameterName.qualifier.name()).asText()).isEqualTo("BinaryMaster");
            assertThat(storageLog.get(StorageLogbookParameterName.version.name()).asLong()).isEqualTo(1);
            assertThat(storageLog.get(StorageLogbookParameterName.contextId.name()).asText()).isEqualTo("contextId");
            assertThat(storageLog.get(StorageLogbookParameterName.archivesId.name()).asText()).isEqualTo("unitId");
        }
    }
}
