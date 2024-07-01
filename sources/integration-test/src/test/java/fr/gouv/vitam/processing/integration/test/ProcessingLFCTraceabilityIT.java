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
package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamTestHelper.insertWaitForStepEssentialFiles;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Processing integration test
 */
public class ProcessingLFCTraceabilityIT extends VitamRuleRunner {

    public static final int TEMPORIZATION_IN_SECONDS = 300;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProcessingLFCTraceabilityIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class
        )
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private static final Integer TENANT_ID = 0;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String SIP_3_UNITS_2_GOTS = "integration-processing/3_UNITS_2_GOTS.zip";
    private static final String SIP_12_UNITS_12_GOTS = "integration-processing/12_UNITS_12_GOTS.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String CONFIG_SIEGFRIED_PATH = PropertiesUtils.getResourcePath(
            "integration-processing/format-identifiers.conf"
        ).toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        new DataLoader("integration-processing").prepareData();

        processMonitoring = ProcessMonitoringImpl.getInstance();

        checkServerStatus();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);

        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void before() {
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
    }

    @After
    public void afterTest() {
        handleAfter();
        runAfter();
    }

    private static void checkServerStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGetWarningOnFirstTraceabilityWhenDbIsEmpty() throws Exception {
        ProcessingIT.prepareVitamSession();
        // Given (empty db)

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(traceabilityOperation);
        assertThat(traceabilityEvent).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGetWarnOnFirstTraceabilityWithDataTooFresh() throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(traceabilityOperation);
        assertThat(traceabilityEvent).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGetOkOnFirstTraceabilityWithDataToSecure() throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // When
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String containerName = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();

        // Then
        assertCompletedWithStatus(containerName, StatusCode.OK);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(containerName);
        assertThat(traceabilityEvent).isNotNull();
        assertThat(traceabilityEvent.getLogType()).isEqualTo(TraceabilityType.UNIT_LIFECYCLE);
        assertThat(traceabilityEvent.getMaxEntriesReached()).isFalse();
        assertThat(traceabilityEvent.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGenerateTraceabilityWhenDataToSecure() throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Inject data
        logicalClock.logicalSleep(45, ChronoUnit.MINUTES);
        launchIngest(SIP_3_UNITS_2_GOTS);

        // Ensure traceability is generated after new data is available
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String traceabilityIdWithNewData = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();

        // Then
        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);

        assertThat(traceabilityIdWithNewData).isNotNull();
        assertCompletedWithStatus(traceabilityIdWithNewData, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityIdWithNewData);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent2).isNotNull();

        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldSkipTraceabilityWhenUntilDataToSecureThenTraceabilityOK()
        throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Ensure no traceability next few hours (< 12h)
        for (int i = 0; i < 8; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Inject data
        logicalClock.logicalSleep(45, ChronoUnit.MINUTES);
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);

        // Ensure traceability is generated after new data is available
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String traceabilityIdWithNewData = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();
        assertThat(traceabilityIdWithNewData).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);

        assertThat(traceabilityIdWithNewData).isNotNull();
        assertCompletedWithStatus(traceabilityIdWithNewData, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityIdWithNewData);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent2).isNotNull();

        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldSkipTraceabilityWhenNoDataToSecureUntilLastTraceabilityIsTooOldThenTraceabilityWarning()
        throws Exception {
        ProcessingIT.prepareVitamSession();

        // First traceability
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Ensure no traceability for next 11h
        for (int i = 0; i < 11; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Ensure traceability is generated after 12h
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        String newTraceabilityId = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        assertThat(newTraceabilityId).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.WARNING);
        assertThat(newTraceabilityId).isNotNull();
        assertCompletedWithStatus(newTraceabilityId, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(newTraceabilityId);
        assertThat(traceabilityEvent1).isNull();
        assertThat(traceabilityEvent2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldSkipTraceabilityWhenNoNewDataToSecureUntilLastTraceabilityIsTooOldThenTraceabilityWarning()
        throws Exception {
        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // When / Then

        // Ensure no traceability for next 11h
        for (int i = 0; i < 11; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Ensure traceability is generated after 12h
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        String newTraceabilityId = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        assertThat(newTraceabilityId).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);
        assertThat(newTraceabilityId).isNotNull();
        assertCompletedWithStatus(newTraceabilityId, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(newTraceabilityId);

        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldSkipTraceabilityOnFreshUnitUpdate() throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String traceabilityOperation1 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Update Unit + traceability
        logicalClock.logicalSleep(58, ChronoUnit.MINUTES);
        corruptOneUnitLfcInDb();
        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);

        String traceabilityOperation2 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        assertThat(traceabilityOperation2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGetKoOnCorruptedDb() throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        corruptOneUnitLfcInDb();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_shouldGetOkOnTraceabilityAfterTraceabilityWithMaxEntriesReached()
        throws Exception {
        // Given
        logicalClock.freezeTime();

        launchIngest(SIP_12_UNITS_12_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeIngest2 = LocalDateUtil.now();
        launchIngest(SIP_12_UNITS_12_GOTS);
        LocalDateTime afterIngest2 = LocalDateUtil.now();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        logicalClock.resumeTime();

        // When
        String traceabilityOperation1 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        LocalDateTime beforeTraceability2 = LocalDateUtil.now();
        String traceabilityOperation2 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability2 = LocalDateUtil.now();

        // Then
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        assertCompletedWithStatus(traceabilityOperation2, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getLogType()).isEqualTo(TraceabilityType.UNIT_LIFECYCLE);
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        // EndDate stops at the _lastPersistedDate of the 20th unit LFC
        assertThatDateIsBetween(traceabilityEvent1.getEndDate(), beforeIngest2, afterIngest2);
        assertThat(beforeIngest2).isEqualTo(afterIngest2);
        assertThat(traceabilityEvent1.getMaxEntriesReached()).isTrue();
        assertThat(traceabilityEvent1.getNumberOfElements()).isEqualTo(24);
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbOK()).isEqualTo(24);
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjects()).isNull();

        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityOperation2);
        assertThat(traceabilityEvent2).isNotNull();
        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability2.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability2.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getLogType()).isEqualTo(TraceabilityType.UNIT_LIFECYCLE);
        assertThat(traceabilityEvent2.getMaxEntriesReached()).isFalse();
        assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(15);
        assertThat(traceabilityEvent2.getStatistics().getUnits().getNbOK()).isEqualTo(15);
        assertThat(traceabilityEvent2.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent2.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent2.getStatistics().getObjects()).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_when_corrupted_data_then_all_subsequent_traceabilities_are_ko()
        throws Exception {
        // Given / When

        // Traceability 1 : Empty DB + ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String traceabilityOperation1 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Traceability 2 : No new entries to secure
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        String traceabilityOperation2 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Traceability 3 : Corrupt one unit in DB + traceability
        corruptOneUnitLfcInDb();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability3 = LocalDateUtil.now();
        String traceabilityOperation3 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability3 = LocalDateUtil.now();

        // Traceability 4 : No new data for 12h + empty traceability
        logicalClock.logicalSleep(12, ChronoUnit.HOURS);
        String traceabilityOperation4 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Traceability 5 : ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability5 = LocalDateUtil.now();
        String traceabilityOperation5 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability5 = LocalDateUtil.now();

        // Then

        // Traceability 1 : OK + Zip
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbOK()).isEqualTo(3);
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjects()).isNull();

        // Traceability 2 : Skipped
        assertThat(traceabilityOperation2).isNull();

        // Traceability 3 : KO
        assertCompletedWithStatus(traceabilityOperation3, StatusCode.KO);

        TraceabilityEvent traceabilityEvent3 = getTraceabilityEvent(traceabilityOperation3);
        assertThat(traceabilityEvent3).isNull();

        // Traceability 4 KO
        assertCompletedWithStatus(traceabilityOperation4, StatusCode.KO);

        TraceabilityEvent traceabilityEvent4 = getTraceabilityEvent(traceabilityOperation4);
        assertThat(traceabilityEvent4).isNull();

        // Traceability 5 : KO
        assertCompletedWithStatus(traceabilityOperation5, StatusCode.KO);

        TraceabilityEvent traceabilityEvent5 = getTraceabilityEvent(traceabilityOperation5);
        assertThat(traceabilityEvent5).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowUnitLfcTraceability_multipleChainingTraceabilityOperations() throws Exception {
        // Given / When

        // Traceability 1 : Empty DB + ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String traceabilityOperation1 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Traceability 2 : No new entries to secure
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        String traceabilityOperation2 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Traceability 3 :ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability3 = LocalDateUtil.now();
        String traceabilityOperation3 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability3 = LocalDateUtil.now();

        // Traceability 4 : No new data for 12h + empty traceability
        logicalClock.logicalSleep(12, ChronoUnit.HOURS);
        String traceabilityOperation4 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);

        // Traceability 5 : ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability5 = LocalDateUtil.now();
        String traceabilityOperation5 = launchLogbookLFC(Contexts.UNIT_LFC_TRACEABILITY);
        LocalDateTime afterTraceability5 = LocalDateUtil.now();

        // Then

        // Traceability 1 : OK + Zip
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbOK()).isEqualTo(3);
        assertThat(traceabilityEvent1.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjects()).isNull();

        // Traceability 2 : Skipped
        assertThat(traceabilityOperation2).isNull();

        // Traceability 3 : OK + zip (chained to traceability 1)
        assertCompletedWithStatus(traceabilityOperation3, StatusCode.OK);

        TraceabilityEvent traceabilityEvent3 = getTraceabilityEvent(traceabilityOperation3);
        assertThat(traceabilityEvent3).isNotNull();
        assertThat(traceabilityEvent3.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent3.getEndDate(),
            beforeTraceability3.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability3.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent3.getStatistics().getUnits().getNbOK()).isEqualTo(3);
        assertThat(traceabilityEvent3.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent3.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent3.getStatistics().getObjects()).isNull();

        // Traceability 4 : WARNING without Zip
        assertCompletedWithStatus(traceabilityOperation4, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent4 = getTraceabilityEvent(traceabilityOperation4);
        assertThat(traceabilityEvent4).isNull();

        // Traceability 5 : OK + Zip (chained to traceability 3)
        assertCompletedWithStatus(traceabilityOperation5, StatusCode.OK);

        TraceabilityEvent traceabilityEvent5 = getTraceabilityEvent(traceabilityOperation5);
        assertThat(traceabilityEvent5).isNotNull();
        assertThat(traceabilityEvent5.getStartDate()).isEqualTo(traceabilityEvent3.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent5.getEndDate(),
            beforeTraceability5.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability5.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent5.getStatistics().getUnits().getNbOK()).isEqualTo(3);
        assertThat(traceabilityEvent5.getStatistics().getUnits().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent5.getStatistics().getObjectGroups()).isNull();
        assertThat(traceabilityEvent5.getStatistics().getObjects()).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGetWarningOnFirstTraceabilityWhenDbIsEmpty()
        throws Exception {
        ProcessingIT.prepareVitamSession();
        // Given (empty db)

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(traceabilityOperation);
        assertThat(traceabilityEvent).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGetWarnOnFirstTraceabilityWithDataTooFresh()
        throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(traceabilityOperation);
        assertThat(traceabilityEvent).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGetOkOnFirstTraceabilityWithDataToSecure()
        throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // When
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String containerName = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();

        // Then
        assertCompletedWithStatus(containerName, StatusCode.OK);

        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(containerName);
        assertThat(traceabilityEvent).isNotNull();

        assertThat(traceabilityEvent.getLogType()).isEqualTo(TraceabilityType.OBJECTGROUP_LIFECYCLE);
        assertThat(traceabilityEvent.getMaxEntriesReached()).isFalse();
        assertThat(traceabilityEvent.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGenerateTraceabilityWhenDataToSecure() throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Inject data
        logicalClock.logicalSleep(45, ChronoUnit.MINUTES);
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);

        // Ensure traceability is generated after new data is available
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String traceabilityIdWithNewData = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();
        assertThat(traceabilityIdWithNewData).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);

        assertThat(traceabilityIdWithNewData).isNotNull();
        assertCompletedWithStatus(traceabilityIdWithNewData, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityIdWithNewData);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent2).isNotNull();

        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldSkipTraceabilityWhenUntilDataToSecureThenTraceabilityOK()
        throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Ensure no traceability next few hours (< 12h)
        for (int i = 0; i < 8; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Inject data
        logicalClock.logicalSleep(45, ChronoUnit.MINUTES);
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(15, ChronoUnit.MINUTES);

        // Ensure traceability is generated after new data is available
        LocalDateTime beforeTraceability = LocalDateUtil.now();
        String traceabilityIdWithNewData = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability = LocalDateUtil.now();
        assertThat(traceabilityIdWithNewData).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);

        assertThat(traceabilityIdWithNewData).isNotNull();
        assertCompletedWithStatus(traceabilityIdWithNewData, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityIdWithNewData);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent2).isNotNull();

        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getPreviousLogbookTraceabilityDate()).isEqualTo(
            traceabilityEvent1.getStartDate()
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldSkipTraceabilityWhenNoDataToSecureUntilLastTraceabilityIsTooOldThenTraceabilityWarning()
        throws Exception {
        ProcessingIT.prepareVitamSession();

        // First traceability

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Ensure no traceability for next 12h
        for (int i = 0; i < 11; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Ensure traceability is generated after 12h
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        String newTraceabilityId = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        assertThat(newTraceabilityId).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.WARNING);
        assertThat(newTraceabilityId).isNotNull();
        assertCompletedWithStatus(newTraceabilityId, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(newTraceabilityId);
        assertThat(traceabilityEvent1).isNull();
        assertThat(traceabilityEvent2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldSkipTraceabilityWhenNoNewDataToSecureUntilLastTraceabilityIsTooOldThenTraceabilityWarning()
        throws Exception {
        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);
        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String firstTraceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Ensure no traceability for next 12h
        for (int i = 0; i < 11; i++) {
            logicalClock.logicalSleep(1, ChronoUnit.HOURS);
            String traceabilityId = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
            assertThat(traceabilityId).isNull();
        }

        // Ensure traceability is generated after 12h
        logicalClock.logicalSleep(1, ChronoUnit.HOURS);
        String traceabilityIdWithNewData = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        assertThat(traceabilityIdWithNewData).isNotNull();

        assertThat(firstTraceabilityOperation).isNotNull();
        assertCompletedWithStatus(firstTraceabilityOperation, StatusCode.OK);
        assertThat(traceabilityIdWithNewData).isNotNull();
        assertCompletedWithStatus(traceabilityIdWithNewData, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(firstTraceabilityOperation);
        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityIdWithNewData);

        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );

        assertThat(traceabilityEvent2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldSkipTraceabilityOnFreshObjectGroupUpdate()
        throws Exception {
        // Given / When

        // First ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String traceabilityOperation1 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Update Got + traceability
        logicalClock.logicalSleep(58, ChronoUnit.MINUTES);
        corruptOneUnitLfcInDb();
        logicalClock.logicalSleep(2, ChronoUnit.MINUTES);

        String traceabilityOperation2 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        assertThat(traceabilityOperation2).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGetKoOnCorruptedDb() throws Exception {
        // Given
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        corruptOneObjectGroupLfcInDb();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // When
        String traceabilityOperation = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Then
        assertCompletedWithStatus(traceabilityOperation, StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_shouldGetOkOnTraceabilityAfterTraceabilityWithMaxEntriesReached()
        throws Exception {
        // Given
        logicalClock.freezeTime();

        launchIngest(SIP_12_UNITS_12_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeIngest2 = LocalDateUtil.now();
        launchIngest(SIP_12_UNITS_12_GOTS);
        LocalDateTime afterIngest2 = LocalDateUtil.now();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        logicalClock.resumeTime();

        // When
        String traceabilityOperation1 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        LocalDateTime beforeTraceability2 = LocalDateUtil.now();
        String traceabilityOperation2 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability2 = LocalDateUtil.now();

        // Then
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        assertCompletedWithStatus(traceabilityOperation2, StatusCode.OK);

        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getLogType()).isEqualTo(TraceabilityType.OBJECTGROUP_LIFECYCLE);
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        // EndDate stops at the _lastPersistedDate of the 20th object group LFC
        assertThatDateIsBetween(traceabilityEvent1.getEndDate(), beforeIngest2, afterIngest2);
        assertThat(beforeIngest2).isEqualTo(afterIngest2);
        assertThat(traceabilityEvent1.getMaxEntriesReached()).isTrue();
        assertThat(traceabilityEvent1.getNumberOfElements()).isEqualTo(24);
        assertThat(traceabilityEvent1.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbOK()).isEqualTo(24);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbOK()).isEqualTo(24);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);

        TraceabilityEvent traceabilityEvent2 = getTraceabilityEvent(traceabilityOperation2);
        assertThat(traceabilityEvent2).isNotNull();
        assertThat(traceabilityEvent2.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent2.getEndDate(),
            beforeTraceability2.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability2.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent2.getLogType()).isEqualTo(TraceabilityType.OBJECTGROUP_LIFECYCLE);
        assertThat(traceabilityEvent2.getMaxEntriesReached()).isFalse();
        assertThat(traceabilityEvent2.getNumberOfElements()).isEqualTo(14);
        assertThat(traceabilityEvent2.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent2.getStatistics().getObjectGroups().getNbOK()).isEqualTo(14);
        assertThat(traceabilityEvent2.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent2.getStatistics().getObjects().getNbOK()).isEqualTo(14);
        assertThat(traceabilityEvent2.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_when_corrupted_data_then_subsequent_TraceabilityOperations_are_ko()
        throws Exception {
        // Given / When

        // Traceability 1 : Empty DB + ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String traceabilityOperation1 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Traceability 2 : No new entries to secure
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        String traceabilityOperation2 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Traceability 3 : Corrupt one object group in DB + traceability
        corruptOneObjectGroupLfcInDb();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String traceabilityOperation3 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Traceability 4 : No new data for 12h + empty traceability
        logicalClock.logicalSleep(12, ChronoUnit.HOURS);
        String traceabilityOperation4 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Traceability 5 : ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String traceabilityOperation5 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Then

        // Traceability 1 : OK + Zip
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent1.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);

        // Traceability 2 : Skipped
        assertThat(traceabilityOperation2).isNull();

        // Traceability 3 : KO
        assertCompletedWithStatus(traceabilityOperation3, StatusCode.KO);
        TraceabilityEvent traceabilityEvent3 = getTraceabilityEvent(traceabilityOperation3);
        assertThat(traceabilityEvent3).isNull();

        // Traceability 4 : KO
        assertCompletedWithStatus(traceabilityOperation4, StatusCode.KO);

        TraceabilityEvent traceabilityEvent4 = getTraceabilityEvent(traceabilityOperation4);
        assertThat(traceabilityEvent4).isNull();

        // Traceability 5 : KO
        assertCompletedWithStatus(traceabilityOperation5, StatusCode.KO);

        TraceabilityEvent traceabilityEvent5 = getTraceabilityEvent(traceabilityOperation5);
        assertThat(traceabilityEvent5).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowObjectGroupLfcTraceability_multipleChainingTraceabilityOperations() throws Exception {
        // Given / When

        // Traceability 1 : Empty DB + ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability1 = LocalDateUtil.now();
        String traceabilityOperation1 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability1 = LocalDateUtil.now();

        // Traceability 2 : No new entries to secure
        logicalClock.logicalSleep(1, ChronoUnit.MINUTES);
        String traceabilityOperation2 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Traceability 3 : ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability3 = LocalDateUtil.now();
        String traceabilityOperation3 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability3 = LocalDateUtil.now();

        // Traceability 4 : No new data for 12h + empty traceability
        logicalClock.logicalSleep(12, ChronoUnit.HOURS);
        String traceabilityOperation4 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);

        // Traceability 5 : ingest + traceability
        launchIngest(SIP_3_UNITS_2_GOTS);
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        LocalDateTime beforeTraceability5 = LocalDateUtil.now();
        String traceabilityOperation5 = launchLogbookLFC(Contexts.OBJECTGROUP_LFC_TRACEABILITY);
        LocalDateTime afterTraceability5 = LocalDateUtil.now();

        // Then

        // Traceability 1 : OK + Zip
        assertCompletedWithStatus(traceabilityOperation1, StatusCode.OK);
        TraceabilityEvent traceabilityEvent1 = getTraceabilityEvent(traceabilityOperation1);
        assertThat(traceabilityEvent1).isNotNull();
        assertThat(traceabilityEvent1.getStartDate()).isEqualTo("1970-01-01T00:00:00.000");
        assertThatDateIsBetween(
            traceabilityEvent1.getEndDate(),
            beforeTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability1.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent1.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent1.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent1.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);

        // Traceability 2 : Skipped
        assertThat(traceabilityOperation2).isNull();

        // Traceability 3 : OK + zip (chained to traceability 1)
        assertCompletedWithStatus(traceabilityOperation3, StatusCode.OK);

        TraceabilityEvent traceabilityEvent3 = getTraceabilityEvent(traceabilityOperation3);
        assertThat(traceabilityEvent3).isNotNull();
        assertThat(traceabilityEvent3.getStartDate()).isEqualTo(traceabilityEvent1.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent3.getEndDate(),
            beforeTraceability3.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability3.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent3.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent3.getStatistics().getObjectGroups().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent3.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent3.getStatistics().getObjects().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent3.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);

        // Traceability 4 : warning + empty ZIP
        assertCompletedWithStatus(traceabilityOperation4, StatusCode.WARNING);

        TraceabilityEvent traceabilityEvent4 = getTraceabilityEvent(traceabilityOperation4);
        assertThat(traceabilityEvent4).isNull();

        // Traceability 5 : OK + Zip (chained to traceability 3)
        assertCompletedWithStatus(traceabilityOperation5, StatusCode.OK);

        TraceabilityEvent traceabilityEvent5 = getTraceabilityEvent(traceabilityOperation5);
        assertThat(traceabilityEvent5).isNotNull();
        assertThat(traceabilityEvent5.getStartDate()).isEqualTo(traceabilityEvent3.getEndDate());
        assertThatDateIsBetween(
            traceabilityEvent5.getEndDate(),
            beforeTraceability5.minusSeconds(TEMPORIZATION_IN_SECONDS),
            afterTraceability5.minusSeconds(TEMPORIZATION_IN_SECONDS)
        );
        assertThat(traceabilityEvent5.getStatistics().getUnits()).isNull();
        assertThat(traceabilityEvent5.getStatistics().getObjectGroups().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent5.getStatistics().getObjectGroups().getNbWarnings()).isEqualTo(0);
        assertThat(traceabilityEvent5.getStatistics().getObjects().getNbOK()).isEqualTo(2);
        assertThat(traceabilityEvent5.getStatistics().getObjects().getNbWarnings()).isEqualTo(0);
    }

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        createLogbookOperation(operationId, objectId, null);
    }

    private void createLogbookOperation(GUID operationId, GUID objectId, String type)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = "Process_SIP_unitary";
        }
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            type,
            objectId,
            "Process_SIP_unitary".equals(type) ? LogbookTypeProcess.INGEST : LogbookTypeProcess.TRACEABILITY,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );

        logbookClient.create(initParameters);
    }

    private void launchIngest(String sipFile) throws Exception {
        ProcessingIT.prepareVitamSession();
        final GUID operationGuid2 = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid2);
        final GUID objectGuid2 = GUIDFactory.newManifestGUID(TENANT_ID);
        final String containerName2 = objectGuid2.getId();
        createLogbookOperation(operationGuid2, objectGuid2);

        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(sipFile);
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(containerName2);

        // call processing
        processingClient.initVitamProcess(containerName2, Contexts.DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 = processingClient.executeOperationProcess(
            containerName2,
            Contexts.DEFAULT_WORKFLOW.name(),
            ProcessAction.RESUME.getValue()
        );
        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());
        waitOperation(containerName2);
        assertCompletedWithStatus(containerName2, StatusCode.OK);
    }

    private String launchLogbookLFC(Contexts traceabilityContext) throws Exception {
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK requestResponseOK;
            switch (traceabilityContext) {
                case UNIT_LFC_TRACEABILITY:
                    requestResponseOK = logbookOperationsClient.traceabilityLfcUnit();
                    break;
                case OBJECTGROUP_LFC_TRACEABILITY:
                    requestResponseOK = logbookOperationsClient.traceabilityLfcObjectGroup();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + traceabilityContext);
            }

            if (requestResponseOK.isEmpty()) {
                return null;
            }
            String operationGuid = (String) requestResponseOK.getFirstResult();
            waitOperation(operationGuid);
            return operationGuid;
        }
    }

    private void corruptOneObjectGroupLfcInDb() throws Exception {
        try (
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            // search for got lfc
            final Query parentQuery = QueryHelper.gte("evDateTime", LocalDateTime.MIN.toString());
            final Select select = new Select();
            select.setQuery(parentQuery);
            select.addOrderByAscFilter("evDateTime");
            RequestResponseOK requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookLifeCyclesClient.selectObjectGroupLifeCycle(select.getFinalSelect())
            );
            List<JsonNode> foundObjectGroupLifecycles = requestResponseOK.getResults();
            assertTrue(foundObjectGroupLifecycles != null && foundObjectGroupLifecycles.size() > 0);
            assertThat(foundObjectGroupLifecycles.get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
            // get one got lfc
            String oneGotLfc = foundObjectGroupLifecycles.get(0).get(LogbookDocument.ID).asText();

            // update got lfc
            final GUID updateLfcGuidStart = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            LogbookLifeCycleObjectGroupParameters logbookLifeGotUpdateParameters =
                LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
                    updateLfcGuidStart,
                    VitamLogbookMessages.getEventTypeLfc("AUDIT_CHECK_OBJECT"),
                    updateLfcGuidStart,
                    LogbookTypeProcess.AUDIT,
                    StatusCode.KO,
                    VitamLogbookMessages.getOutcomeDetailLfc("AUDIT_CHECK_OBJECT", StatusCode.KO),
                    VitamLogbookMessages.getCodeLfc("AUDIT_CHECK_OBJECT", StatusCode.KO),
                    GUIDReader.getGUID(oneGotLfc)
                );

            logbookLifeCyclesClient.update(logbookLifeGotUpdateParameters);
            logbookLifeCyclesClient.commit(logbookLifeGotUpdateParameters);
        }
    }

    private void corruptOneUnitLfcInDb() throws Exception {
        try (
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            // search for got lfc
            final Query parentQuery = QueryHelper.gte("evDateTime", LocalDateTime.MIN.toString());
            final Select select = new Select();
            select.setQuery(parentQuery);
            select.addOrderByAscFilter("evDateTime");
            RequestResponseOK requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookLifeCyclesClient.selectUnitLifeCycle(select.getFinalSelect())
            );
            List<JsonNode> foundUnitLifecycles = requestResponseOK.getResults();
            assertTrue(foundUnitLifecycles != null && foundUnitLifecycles.size() > 0);
            assertThat(foundUnitLifecycles.get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
            // get one got lfc
            String oneUnitLfc = foundUnitLifecycles.get(0).get(LogbookDocument.ID).asText();

            // update got lfc
            final GUID updateLfcGuidStart = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            LogbookLifeCycleUnitParameters logbookLifeUnitUpdateParameters =
                LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
                    updateLfcGuidStart,
                    VitamLogbookMessages.getEventTypeLfc("UNIT_UPDATE"),
                    updateLfcGuidStart,
                    LogbookTypeProcess.UPDATE,
                    StatusCode.OK,
                    VitamLogbookMessages.getOutcomeDetailLfc("UNIT_UPDATE", StatusCode.OK),
                    VitamLogbookMessages.getCodeLfc("UNIT_UPDATE", StatusCode.OK),
                    GUIDReader.getGUID(oneUnitLfc)
                );

            logbookLifeCyclesClient.update(logbookLifeUnitUpdateParameters);
            logbookLifeCyclesClient.commit(logbookLifeUnitUpdateParameters);
        }
    }

    private void assertCompletedWithStatus(String containerName, StatusCode expected) {
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, TENANT_ID);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(expected, processWorkflow.getStatus());
    }

    private TraceabilityEvent getTraceabilityEvent(String containerName)
        throws LogbookClientException, InvalidParseOperationException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        JsonNode response = logbookClient.selectOperationById(containerName);
        JsonNode jsonNode = response.get("$results").get(0);
        JsonNode evDetData = jsonNode.get("evDetData");
        if (evDetData == null || evDetData.isNull()) return null;
        return JsonHandler.getFromString(evDetData.textValue(), TraceabilityEvent.class);
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
