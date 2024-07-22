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

package fr.gouv.vitam.logbook.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationTraceabilityIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        OperationTraceabilityIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            LogbookMain.class,
            AdminManagementMain.class,
            WorkspaceMain.class,
            DefaultOfferMain.class,
            StorageMain.class
        )
    );

    private static final int TENANT_0 = 0;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());
        VitamServerRunner.cleanOffers();
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
        runAfterMongo(Sets.newHashSet(LogbookCollections.OPERATION.getName()));

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 2)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenEmptyDataSetWhenFirstTraceabilityThenWarning() throws Exception {
        // Given : Empty DB

        // When : First traceability
        String operationId = runTraceability();

        // Then : Empty traceability operation is generated
        LogbookOperation logbookOperation = getLogbookInformation(operationId);
        assertThat(logbookOperation.getEvDetData()).isNull();
        LogbookEventOperation lastEvent = logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.WARNING");

        assertThat(logbookOperation.getEvDetData()).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenFreshOperationsWhenFirstTraceabilityThenWarning() throws Exception {
        // Given :
        // - A recent operation (less than 5 minutes old)
        injectTestLogbookOperation();
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);

        // When : First traceability
        String traceabilityOperationId = runTraceability();

        // Then : Empty traceability operation is generated
        LogbookOperation logbookOperation = getLogbookInformation(traceabilityOperationId);

        LogbookEventOperation lastEvent = logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.WARNING");

        assertThat(logbookOperation.getEvDetData()).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenOperationsToSecureWhenFirstTraceabilityThenOK() throws Exception {
        // Given :
        // - An operation to secure
        String operation1 = injectTestLogbookOperation();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // When : First traceability
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String traceabilityOperationId = runTraceability();
        LocalDateTime afterTraceability = LocalDateUtil.now();

        // Then : Traceability OK
        LogbookOperation logbookOperation = getLogbookInformation(traceabilityOperationId);

        LogbookEventOperation lastEvent = logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.OK");

        assertThat(logbookOperation.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(
            logbookOperation.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(traceabilityEvent.getLogType()).isEqualTo(TraceabilityType.OPERATION);
        assertThat(traceabilityEvent.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent.getEndDate(),
            beforeTraceability.minusMinutes(5),
            afterTraceability.minusMinutes(5)
        );

        assertThat(traceabilityEvent.getHash()).isNotNull();
        assertThat(traceabilityEvent.getTimeStampToken()).isNotNull();
        assertThat(traceabilityEvent.getNumberOfElements()).isEqualTo(1);
        assertThat(traceabilityEvent.getFileName()).isNotNull();

        downloadZip(traceabilityEvent.getFileName(), tmpFolder.getRoot());

        List<String> lines = FileUtils.readLines(new File(tmpFolder.getRoot(), "data.txt"), StandardCharsets.UTF_8);
        List<String> ids = parseLines(lines);
        assertThat(ids).containsExactly(operation1);
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenNoNewEntriesThenSkipTraceabilityUntilLastTraceabilityIsTooOld()
        throws Exception {
        // Given
        // - An already secured operation1
        injectTestLogbookOperation();
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);

        // - A traceability operation that secures operation1
        String firstTraceabilityOperation = runTraceability();

        // When / Then

        // Ensure no traceability for next 12h
        for (int i = 0; i < 11; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = runTraceability();
            assertThat(traceabilityId).isNull();
        }

        // Ensure traceability is generated after 12h
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        LocalDateTime beforeTraceability2 = LocalDateUtil.now();
        String newTraceabilityId = runTraceability();
        LocalDateTime afterTraceability2 = LocalDateUtil.now();
        assertThat(newTraceabilityId).isNotNull();

        LogbookOperation logbookOperation1 = getLogbookInformation(firstTraceabilityOperation);
        LogbookOperation logbookOperation2 = getLogbookInformation(newTraceabilityId);

        LogbookEventOperation lastEvent = logbookOperation2.getEvents().get(logbookOperation2.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.OK");

        assertThat(logbookOperation1.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent1 = JsonHandler.getFromString(
            logbookOperation1.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(logbookOperation2.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent2 = JsonHandler.getFromString(
            logbookOperation2.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(traceabilityEvent2.getLogType()).isEqualTo(TraceabilityType.OPERATION);
        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());

        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability2.minusMinutes(5),
            afterTraceability2.minusMinutes(5)
        );

        assertThat(traceabilityEvent2.getHash()).isNotNull();
        assertThat(traceabilityEvent2.getTimeStampToken()).isNotNull();
        assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(1);
        assertThat(traceabilityEvent2.getFileName()).isNotNull();
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );

        downloadZip(traceabilityEvent2.getFileName(), tmpFolder.getRoot());

        List<String> lines = FileUtils.readLines(new File(tmpFolder.getRoot(), "data.txt"), StandardCharsets.UTF_8);
        List<String> ids = parseLines(lines);
        assertThat(ids).containsExactly(firstTraceabilityOperation);
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenNoNewEntriesThenSkipTraceabilityUntilNewDataToSecure() throws Exception {
        // Given
        // - An already secured operation1
        injectTestLogbookOperation();
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);

        // - A traceability operation that secures operation1
        String firstTraceabilityOperation = runTraceability();

        // When / Then

        // Ensure no traceability for next new hours (< 12h)
        for (int i = 0; i < 8; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = runTraceability();
            assertThat(traceabilityId).isNull();
        }

        // New LFCs
        String operation2 = injectTestLogbookOperation();

        // Ensure traceability is generated
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        LocalDateTime beforeTraceability2 = LocalDateUtil.now();
        String newTraceabilityId = runTraceability();
        LocalDateTime afterTraceability2 = LocalDateUtil.now();
        assertThat(newTraceabilityId).isNotNull();

        LogbookOperation logbookOperation1 = getLogbookInformation(firstTraceabilityOperation);
        LogbookOperation logbookOperation2 = getLogbookInformation(newTraceabilityId);

        LogbookEventOperation lastEvent = logbookOperation2.getEvents().get(logbookOperation2.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.OK");

        assertThat(logbookOperation1.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent1 = JsonHandler.getFromString(
            logbookOperation1.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(logbookOperation2.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent2 = JsonHandler.getFromString(
            logbookOperation2.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(traceabilityEvent2.getLogType()).isEqualTo(TraceabilityType.OPERATION);
        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());

        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability2.minusMinutes(5),
            afterTraceability2.minusMinutes(5)
        );

        assertThat(traceabilityEvent2.getHash()).isNotNull();
        assertThat(traceabilityEvent2.getTimeStampToken()).isNotNull();
        assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(2);
        assertThat(traceabilityEvent2.getFileName()).isNotNull();
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );

        downloadZip(traceabilityEvent2.getFileName(), tmpFolder.getRoot());

        List<String> lines = FileUtils.readLines(new File(tmpFolder.getRoot(), "data.txt"), StandardCharsets.UTF_8);
        List<String> ids = parseLines(lines);
        assertThat(ids).containsExactly(firstTraceabilityOperation, operation2);
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenOldEntriesSinceRecentTraceabilityWhenNewTraceabilityThenTraceabilityOK()
        throws Exception {
        // Given :
        // - An already secured operation
        String operation1 = injectTestLogbookOperation();
        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

        // - An operation than was not covered by last traceability operation
        String operation2 = injectTestLogbookOperation();
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);

        // - An traceability operation that secures operation1, but not operation2
        String firstTraceabilityOperation = runTraceability();

        // - An operation that will be secured by next traceability operation
        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);
        String operation3 = injectTestLogbookOperation();

        // - A fresh operation than will be ignored by next traceability operation (less than 5 minutes old)
        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);
        String operation4 = injectTestLogbookOperation();

        // When
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        LocalDateTime beforeNewTraceability = LocalDateUtil.now();
        String newTraceabilityOperationId = runTraceability();
        LocalDateTime afterNewTraceability = LocalDateUtil.now();

        // Then : New traceability operation generated
        LogbookOperation logbookOperation1 = getLogbookInformation(firstTraceabilityOperation);
        LogbookOperation logbookOperation2 = getLogbookInformation(newTraceabilityOperationId);

        LogbookEventOperation lastEvent = logbookOperation2.getEvents().get(logbookOperation2.getEvents().size() - 1);
        assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.OK");

        assertThat(logbookOperation2.getEvDetData()).isNotNull();
        TraceabilityEvent traceabilityEvent1 = JsonHandler.getFromString(
            logbookOperation1.getEvDetData(),
            TraceabilityEvent.class
        );
        TraceabilityEvent traceabilityEvent2 = JsonHandler.getFromString(
            logbookOperation2.getEvDetData(),
            TraceabilityEvent.class
        );

        assertThat(traceabilityEvent2.getLogType()).isEqualTo(TraceabilityType.OPERATION);
        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(
            LocalDateUtil.getFormattedDateTimeForMongo(traceabilityEvent1.getEndDate())
        );
        assertThat(traceabilityEvent2.getEndDate()).isGreaterThanOrEqualTo(
            LocalDateUtil.getFormattedDateTimeForMongo(beforeNewTraceability.minusMinutes(5))
        );
        assertThat(traceabilityEvent2.getEndDate()).isLessThanOrEqualTo(
            LocalDateUtil.getFormattedDateTimeForMongo(afterNewTraceability.minusMinutes(5))
        );

        assertThat(traceabilityEvent2.getHash()).isNotNull();
        assertThat(traceabilityEvent2.getTimeStampToken()).isNotNull();
        assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(3);
        assertThat(traceabilityEvent2.getFileName()).isNotNull();
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );

        downloadZip(traceabilityEvent2.getFileName(), tmpFolder.getRoot());

        List<String> lines = FileUtils.readLines(new File(tmpFolder.getRoot(), "data.txt"), StandardCharsets.UTF_8);
        List<String> ids = parseLines(lines);
        assertThat(ids).containsExactly(operation2, firstTraceabilityOperation, operation3);
        assertThat(ids).doesNotContain(operation1, operation4);
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationTraceability_GivenMultipleTenantsThenTraceabilityOKForAllTenants() throws Exception {
        // Give : Data set for all tenants
        for (int tenantId = 0; tenantId < 3; tenantId++) {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            injectTestLogbookOperation();
        }
        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

        // When : Run traceability for all tenants
        Map<Integer, String> operationIds = runTraceabilityOperations(0, 1, 2);

        // Then
        for (int tenantId = 0; tenantId < 3; tenantId++) {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            LogbookOperation logbookOperation = getLogbookInformation(operationIds.get(tenantId));
            assertThat(logbookOperation).isNotNull();
            LogbookEventOperation lastEvent = logbookOperation.getEvents().get(logbookOperation.getEvents().size() - 1);
            assertThat(lastEvent.getOutDetail()).isEqualTo("STP_OP_SECURISATION.OK");
        }
    }

    private String injectTestLogbookOperation() throws Exception {
        String id = GUIDFactory.newGUID().getId();
        VitamThreadUtils.getVitamSession().setRequestId(id);
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            SecurityProfileModel securityProfileModel = new SecurityProfileModel();
            securityProfileModel.setIdentifier("Identifier" + id);
            securityProfileModel.setName("Name" + id);
            securityProfileModel.setFullAccess(true);
            adminManagementClient.importSecurityProfiles(Collections.singletonList(securityProfileModel));
        }
        return id;
    }

    private String runTraceability() throws LogbookClientServerException, InvalidParseOperationException {
        return runTraceabilityOperations(TENANT_0).get(TENANT_0);
    }

    private Map<Integer, String> runTraceabilityOperations(Integer... tenants)
        throws LogbookClientServerException, InvalidParseOperationException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try (final LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            RequestResponseOK<TenantLogbookOperationTraceabilityResult> result = client.traceability(
                Arrays.asList(tenants)
            );
            // Collect to map with null values throws exceptions (https://bugs.openjdk.java.net/browse/JDK-8148463)
            return result
                .getResults()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getTenantId(), v.getOperationId()), HashMap::putAll);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        }
    }

    private LogbookOperation getLogbookInformation(String operationId)
        throws InvalidParseOperationException, LogbookClientException {
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode response = client.selectOperationById(operationId);
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
        }
    }

    private void downloadZip(String fileName, File folder)
        throws IOException, StorageNotFoundException, StorageServerClientException, StorageUnavailableDataFromAsyncOfferClientException {
        try (
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response containerAsync = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                DataCategory.LOGBOOK,
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

    private List<String> parseLines(List<String> lines) {
        return lines
            .stream()
            .map(line -> {
                try {
                    return JsonHandler.getFromString(line);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(json -> json.get("_id").asText())
            .collect(Collectors.toList());
    }

    private void assertThatDateIsBetween(String mongoDate, LocalDateTime expectedMin, LocalDateTime expectedMax) {
        assertThatDateIsAfterOrEqualTo(mongoDate, expectedMin);
        assertThatDateIsBeforeOrEqualTo(mongoDate, expectedMax);
    }

    private void assertThatDateIsBeforeOrEqualTo(String mongoDate, LocalDateTime expectedMax) {
        LocalDateTime dateTime = LocalDateUtil.parseMongoFormattedDate(mongoDate);
        assertThat(dateTime).isBeforeOrEqualTo(expectedMax);
    }

    private void assertThatDateIsAfterOrEqualTo(String mongoDate, LocalDateTime expectedMin) {
        LocalDateTime dateTime = LocalDateUtil.parseMongoFormattedDate(mongoDate);
        assertThat(dateTime).isAfterOrEqualTo(expectedMin);
    }
}
