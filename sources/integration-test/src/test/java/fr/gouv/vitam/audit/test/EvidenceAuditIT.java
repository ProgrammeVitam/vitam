/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.audit.test;

import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.iterators.PeekingIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;

import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;

/**
 * Audit evidence integration test
 */

public class EvidenceAuditIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String contractId = "contract";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static DataLoader dataLoader = new DataLoader("integration-ingest-internal");

    private static final HashSet<Class> servers = Sets.newHashSet(AccessInternalMain.class, AdminManagementMain.class,
            ProcessManagementMain.class, LogbookMain.class, WorkspaceMain.class, MetadataMain.class, WorkerMain.class,
            IngestInternalMain.class, StorageMain.class, DefaultOfferMain.class, BatchReportMain.class);

    private static final String mongoName = mongoRule.getMongoDatabase().getName();
    private static final String esName = elasticsearchRule.getClusterName();

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(AuditIT.class, mongoName, esName, servers);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        String configurationPath = PropertiesUtils
                .getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        dataLoader.prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() throws Exception {
        getVitamSession().setRequestId(newOperationLogbookGUID(0));
        getVitamSession().setTenantId(tenantId);

        AccessInternalClientFactory factory = AccessInternalClientFactory.getInstance();
        factory.changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
        factory.setVitamClientType(PRODUCTION);

        AccessContractModel contract = getAccessContractModel();
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();

        client.importAccessContracts(singletonList(contract));

        FormatIdentifierFactory.getInstance().changeConfigurationFile(
                PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());
    }

    private AccessContractModel getAccessContractModel() {
        AccessContractModel contract = new AccessContractModel();
        contract.setName(contractId);
        contract.setIdentifier(contractId);
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setEveryOriginatingAgency(true);
        contract.setCreationdate("10/12/1800");
        contract.setActivationdate("10/12/1800");
        contract.setDeactivationdate("31/12/4200");
        return contract;
    }
    
    @Test
    @RunWithCustomExecutor
    public void should_execute_evidence_audit_workflow_without_error() throws Exception {
        
        // Given
        String ingestOperationId = dataLoader.doIngest("preservation/OG_with_3_parents.zip");
        String traceabilityUnitsOperationId =  doTraceabilityUnits();
        String traceabilityGotsOperationId =  doTraceabilityGots();

        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            
            GUID evidenceAuditOperationGUID = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(evidenceAuditOperationGUID);

            SelectMultiQuery query = new SelectMultiQuery();
            query.setQuery(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));
            RequestResponse<JsonNode> evidenceAuditResponse = adminClient.evidenceAudit(query.getFinalSelect());
            String evidenceAuditOperation = evidenceAuditResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            waitOperation(NB_TRY, SLEEP_TIME, evidenceAuditOperation);

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                    .selectOperationById(evidenceAuditOperation, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results").get(0).get("events");
            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                    .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report exists
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                            evidenceAuditOperationGUID.toString() + ".json", DataCategory.REPORT,
                            AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
        }
    }
    
    @Test
    @RunWithCustomExecutor
    public void should_execute_evidence_audit_workflow_with_ko_report() throws Exception {
        
        // Given
        String ingestOperationId = dataLoader.doIngest("preservation/OG_with_3_parents.zip");
        doTraceabilityUnits();
        doTraceabilityGots();
        String objectIdInError = deleteObjectInStorage(ingestOperationId);

        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            
            // EVIDENCE AUDIT
            
            GUID evidenceAuditOperationGUID = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(evidenceAuditOperationGUID);

            SelectMultiQuery query = new SelectMultiQuery();
            query.setQuery(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));
            RequestResponse<JsonNode> evidenceAuditResponse = adminClient.evidenceAudit(query.getFinalSelect());
            String evidenceAuditOperation = evidenceAuditResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(NB_TRY, SLEEP_TIME, evidenceAuditOperation);

            // When
            ArrayNode evidenceAuditOperationEvents = (ArrayNode) accessClient
                    .selectOperationById(evidenceAuditOperation, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results").get(0).get("events");
            // Then
            assertThat(evidenceAuditOperationEvents.iterator()).extracting(j -> j.get("outcome").asText())
                    .anyMatch(outcome -> outcome.equals(StatusCode.KO.name()));

            // Check report exists
            List<JsonNode> reportLines = null;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                            evidenceAuditOperationGUID.toString() + ".json", DataCategory.REPORT,
                            AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                    reportLines = getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(1);
            assertThat(reportLines.get(0).get("strategyId").asText()).isEqualTo(VitamConfiguration.getDefaultStrategy());
            
            // CORRECTIVE WORKFLOW
            
            // given 
            GUID rectificationAuditOperationGUID = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(rectificationAuditOperationGUID);
            RequestResponse<JsonNode> rectificationAuditResponse = adminClient.rectificationAudit(evidenceAuditOperationGUID.getId());
            String rectificationAuditOperation = rectificationAuditResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(NB_TRY, SLEEP_TIME, rectificationAuditOperation);

            // When
            ArrayNode rectificationAuditOperationEvents = (ArrayNode) accessClient
                    .selectOperationById(rectificationAuditOperation, new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results").get(0).get("events");
            // Then
            assertThat(rectificationAuditOperationEvents.iterator()).extracting(j -> j.get("outcome").asText())
                    .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report exists
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                            rectificationAuditOperationGUID.toString() + ".json", DataCategory.REPORT,
                            AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
        }
    }

    private List<JsonNode> getReport(Response reportResponse) throws IOException, InvalidParseOperationException {
        List<JsonNode> reportLines = new ArrayList<JsonNode>();
        try (InputStream is = reportResponse.readEntity(InputStream.class)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            PeekingIterator<String> linesPeekIterator = new PeekingIterator<>(bufferedReader.lines().iterator());
            while (linesPeekIterator.hasNext()) {
                reportLines.add(JsonHandler.getFromString(linesPeekIterator.next()));
            }
        }
        return reportLines;
    }

    private String doTraceabilityGots() throws VitamException {
        try (LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
                AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();) {
            dataLoader.prepareVitamSession();
            RequestResponseOK traceabilityObjectGroupResponse = logbookOperationsClient.traceabilityLfcObjectGroup();
            String traceabilityGotOperationId = traceabilityObjectGroupResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(NB_TRY, SLEEP_TIME, traceabilityGotOperationId);
            return traceabilityGotOperationId;
        }
    }
    
    private String doTraceabilityUnits() throws VitamException {
        try (LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
                AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();) {
            dataLoader.prepareVitamSession();
            RequestResponseOK traceabilityUnitResponse = logbookOperationsClient.traceabilityLfcUnit();
            String traceabilityUnitOperationId = traceabilityUnitResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(NB_TRY, SLEEP_TIME, traceabilityUnitOperationId);
            return traceabilityUnitOperationId;
        }
    }

    private String deleteObjectInStorage(String initialOperationId) throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
                StorageClient storageClient = StorageClientFactory.getInstance().getClient();) {
            
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            
            GUID accessGuid = GUIDFactory.newRequestIdGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(accessGuid);
            SelectMultiQuery selectQuery = new SelectMultiQuery();
            
            selectQuery.setQuery(QueryHelper.and()
                    .add(QueryHelper.in(VitamFieldsHelper.initialOperation(), initialOperationId))
                    .add(QueryHelper.gte(VitamFieldsHelper.nbobjects(), 1)));
            selectQuery.setLimitFilter(0, 1);
            RequestResponse<JsonNode> gots = accessClient.selectObjects(selectQuery.getFinalSelect());
            if(!gots.isOk()) {
                throw new VitamException("Could not find got with at least one object for operation");
            }
            JsonNode got = ((RequestResponseOK<JsonNode>)gots).getFirstResult();
            ObjectGroupResponse gotPojo = JsonHandler.getFromJsonNode(got, ObjectGroupResponse.class);
            String objectId = gotPojo.getQualifiers().get(0).getVersions().get(0).getId();

            GUID storageGuid = GUIDFactory.newRequestIdGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(storageGuid);
            storageClient.delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, objectId);
            return objectId;
            
        }
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(

                MetadataCollections.UNIT.getName(), MetadataCollections.OBJECTGROUP.getName()));

        runAfterEs(Sets.newHashSet(MetadataCollections.UNIT.getName().toLowerCase() + "_0",
                MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0"));
    }

}
