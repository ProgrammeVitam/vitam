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
package fr.gouv.vitam.audit.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.model.ReportStatus;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.TraceabilityObjectModel;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.logbook.common.model.TraceabilityType.OBJECTGROUP_LIFECYCLE;
import static fr.gouv.vitam.logbook.common.model.TraceabilityType.OPERATION;
import static fr.gouv.vitam.logbook.common.model.TraceabilityType.STORAGE;
import static fr.gouv.vitam.logbook.common.model.TraceabilityType.UNIT_LIFECYCLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

public class LinkedCheckTraceabilityIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "Context_IT";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        LinkedCheckTraceabilityIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            BatchReportMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class
        )
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        StorageClientFactory.getInstance().setVitamClientType(PRODUCTION);
        IngestInternalClientFactory.getInstance().setVitamClientType(PRODUCTION);

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_linked_check_traceability_audit_workflow_on_traceability_event_without_error() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);

        // Inject test data
        injectTestLogbookOperation();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        // run operations to audit
        String secureTenantOpId = secureTenant();
        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(Collections.singletonList(secureTenantOpId));
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.OK);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 4);
        assertEquals(ReportStatus.OK.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get("OK").asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbOperations").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertEquals(
            StatusCode.OK.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText()
        );
        assertEquals(
            OPERATION.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_linked_check_traceability_audit_workflow_on_storage_traceability_event_without_error() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        // storage logbackup
        storageLogBackup();
        // run operations to audit
        String secureStorageDataOpId = secureStorageData();
        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(Collections.singletonList(secureStorageDataOpId));
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.OK);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 4);
        assertEquals(ReportStatus.OK.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get("OK").asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbStorage").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertEquals(
            StatusCode.OK.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText()
        );
        assertEquals(
            STORAGE.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_linked_check_traceability_audit_workflow_unit_lfc_traceability_event_without_error() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        try {
            VitamTestHelper.doIngest(TENANT_ID, "integration-processing/4_UNITS_2_GOTS.zip");
        } catch (VitamException e) {
            fail("error while ingest", e);
        }

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String secureUnitLFCDataOpId = secureUnitLFCData();
        VitamTestHelper.verifyOperation(secureUnitLFCDataOpId, StatusCode.OK);

        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(Collections.singletonList(secureUnitLFCDataOpId));
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.OK);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 4);
        assertEquals(ReportStatus.OK.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get("OK").asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbUnitLFC").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertEquals(
            StatusCode.OK.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText()
        );
        assertEquals(
            UNIT_LIFECYCLE.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_linked_check_traceability_audit_workflow_got_lfc_traceability_event_without_error() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        try {
            VitamTestHelper.doIngest(TENANT_ID, "integration-processing/4_UNITS_2_GOTS.zip");
        } catch (VitamException e) {
            fail("error while ingest", e);
        }

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String secureGOTLFCDataOpId = secureGOTLFCData();
        VitamTestHelper.verifyOperation(secureGOTLFCDataOpId, StatusCode.OK);

        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(Collections.singletonList(secureGOTLFCDataOpId));
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.OK);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 4);
        assertEquals(ReportStatus.OK.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get("OK").asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbGotLFC").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertEquals(
            StatusCode.OK.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText()
        );
        assertEquals(
            OBJECTGROUP_LIFECYCLE.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void give_an_operation_with_null_data_should_execute_linked_check_traceability_audit_workflow_with_warning() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        List<String> secureOpGUID = new ArrayList<>();
        // run operations to audit
        secureOpGUID.add(secureStorageData());

        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(secureOpGUID);
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.WARNING);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 3);
        assertEquals(ReportStatus.WARNING.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(
            String.format("%s.%s", ProcessDistributor.OBJECTS_LIST_EMPTY, StatusCode.WARNING),
            report.get(0).get(LogbookEvent.OUT_DETAIL).asText()
        );
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_query_result_should_execute_linked_check_traceability_audit_workflow_with_warning() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        List<String> secureOpGUID = new ArrayList<>();
        secureOpGUID.add("");
        JsonNode query = buildQuery(secureOpGUID);
        String opId = runLinkedCheckTraceability(query);

        VitamTestHelper.verifyOperation(opId, StatusCode.WARNING);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 3);
        assertEquals(ReportStatus.WARNING.name(), report.get(0).get(LogbookEvent.OUTCOME).asText());
        assertEquals(
            String.format("%s.%s", ProcessDistributor.OBJECTS_LIST_EMPTY, StatusCode.WARNING),
            report.get(0).get(LogbookEvent.OUT_DETAIL).asText()
        );
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_offer_should_execute_linked_check_traceability_audit_workflow_with_ko() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        List<String> secureOpGUID = new ArrayList<>();
        // storage logbackup
        storageLogBackup();

        // run operations to audit
        secureOpGUID.add(secureStorageData());

        VitamServerRunner.cleanOffers();

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        secureOpGUID.add(secureTenant());

        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(secureOpGUID);
        String opId = runLinkedCheckTraceability(query);
        VitamTestHelper.verifyOperation(opId, StatusCode.KO);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 5);
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get(StatusCode.OK.name()).asInt());
        assertEquals(1, report.get(1).get("vitamResults").get(StatusCode.KO.name()).asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbOperations").asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbStorage").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertThat(report.subList(3, 5))
            .extracting(
                t -> t.get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
            )
            .contains(STORAGE.name(), OPERATION.name());
        assertThat(report.subList(3, 5))
            .extracting(t -> t.get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText())
            .contains(StatusCode.OK.name(), StatusCode.KO.name());
    }

    @Test
    @RunWithCustomExecutor
    public void given_incorrect_hash_should_execute_linked_check_traceability_audit_workflow_with_ko() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);

        // Inject test data
        injectTestLogbookOperation();
        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        String secureTenantOpId = secureTenant();

        updateHash(secureTenantOpId);

        // run LinkedCheckTraceabilityWorkflow
        JsonNode query = buildQuery(Collections.singletonList(secureTenantOpId));
        String opId = runLinkedCheckTraceability(query);
        VitamTestHelper.verifyOperation(opId, StatusCode.KO);

        List<JsonNode> report = VitamTestHelper.getReports(opId);
        assertEquals(report.size(), 4);
        assertEquals(ReportType.TRACEABILITY.name(), report.get(1).get("reportType").asText());
        assertEquals(1, report.get(1).get("vitamResults").get(StatusCode.KO.name()).asInt());
        assertEquals(1, report.get(1).get("extendedInfo").get("nbOperations").asInt());
        assertEquals(JsonHandler.unprettyPrint(query), JsonHandler.unprettyPrint(report.get(2).get("query")));
        assertEquals(
            OPERATION.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.OPERATION_TYPE).asText()
        );
        assertEquals(
            StatusCode.KO.name(),
            report.get(3).get(TraceabilityObjectModel.METADATA).get(TraceabilityReportEntry.STATUS).asText()
        );
    }

    private void updateHash(String secureTenantOpId) {
        try {
            Document operation = LogbookCollections.OPERATION.getCollection().find(eq("_id", secureTenantOpId)).first();
            LogbookOperation logbookOperation = BsonHelper.fromDocumentToObject(operation, LogbookOperation.class);
            ObjectNode evData = (ObjectNode) JsonHandler.getFromString(logbookOperation.getEvDetData());
            evData.put("Hash", "fake");
            logbookOperation.setEvDetData(JsonHandler.unprettyPrint(evData));
            logbookOperation.getEvents().get(1).setEvDetData(JsonHandler.unprettyPrint(evData));
            logbookOperation.getEvents().get(2).setEvDetData(JsonHandler.unprettyPrint(evData));
            LogbookCollections.OPERATION.getCollection()
                .updateMany(
                    eq("_id", secureTenantOpId),
                    new Document("$set", VitamDocument.parse(JsonHandler.unprettyPrint(logbookOperation)))
                );
        } catch (InvalidParseOperationException e) {
            fail("Error while parsing json", e);
        }
    }

    private void storageLogBackup() {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            storageClient.storageLogBackup(Collections.singletonList(TENANT_ID));
        } catch (StorageServerClientException | InvalidParseOperationException e) {
            fail("Cannot run storage backup");
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        }
    }

    private String runLinkedCheckTraceability(JsonNode query) {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient()) {
            accessInternalClient.linkedCheckTraceability(query);
            waitOperation(operationGuid.toString());
            return operationGuid.toString();
        } catch (InvalidParseOperationException | AccessUnauthorizedException | LogbookClientException e) {
            fail("cannot run linked check workflow", e);
        }
        return null;
    }

    private String secureStorageData() {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            RequestResponseOK<StorageLogTraceabilityResult> response = storageClient.storageLogTraceability(
                Collections.singletonList(TENANT_ID)
            );
            return response.getResults().get(0).getOperationId();
        } catch (StorageServerClientException | InvalidParseOperationException e) {
            fail("Error while securing data", e);
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        }
        return null;
    }

    private String secureGOTLFCData() {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK<String> response = logbookOperationsClient.traceabilityLfcObjectGroup();
            String opId = response.getResults().get(0);
            waitOperation(opId);
            return opId;
        } catch (InvalidParseOperationException | LogbookClientServerException e) {
            fail("Error while securing GOT data", e);
        }
        return null;
    }

    private String secureUnitLFCData() {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK<String> response = logbookOperationsClient.traceabilityLfcUnit();
            String opId = response.getResults().get(0);
            waitOperation(opId);
            return opId;
        } catch (InvalidParseOperationException | LogbookClientServerException e) {
            fail("Error while securing UNIT data", e);
        }
        return null;
    }

    private String secureTenant() {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            RequestResponseOK<TenantLogbookOperationTraceabilityResult> response = client.traceability(
                Collections.singletonList(TENANT_ID)
            );
            assertThat(response.getResults().size()).isEqualTo(1);
            return response.getResults().get(0).getOperationId();
        } catch (InvalidParseOperationException | LogbookClientServerException e) {
            fail("Error while securing tenant", e);
            return null;
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        }
    }

    private JsonNode buildQuery(List<String> secureOpGUID) {
        try {
            final Select select = new Select();
            select.setQuery(in(LogbookEvent.EV_ID, secureOpGUID.toArray(String[]::new)));
            return select.getFinalSelect();
        } catch (InvalidCreateOperationException e) {
            fail("error while building query", e);
            return null;
        }
    }

    private String injectTestLogbookOperation() {
        String id = GUIDFactory.newGUID().getId();
        VitamThreadUtils.getVitamSession().setRequestId(id);
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            SecurityProfileModel securityProfileModel = new SecurityProfileModel();
            securityProfileModel.setIdentifier("Identifier" + id);
            securityProfileModel.setName("Name" + id);
            securityProfileModel.setFullAccess(true);
            adminManagementClient.importSecurityProfiles(Collections.singletonList(securityProfileModel));
        } catch (Exception e) {
            fail("error while injecting test data", e);
        }
        return id;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }
}
