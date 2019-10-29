/*
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
 */
package fr.gouv.vitam.audit.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
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
import fr.gouv.vitam.worker.core.plugin.audit.AuditExistenceService;
import fr.gouv.vitam.worker.core.plugin.audit.AuditIntegrityService;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.purge.EndToEndEliminationAndTransferReplyIT.prepareVitamSession;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit existence/integrity integration test
 */

public class AuditIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String contractId = "contract";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private WorkFlow workflow = WorkFlow.of(CONTEXT_ID, WORKFLOW_IDENTIFIER, "INGEST");

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
        new DataLoader("integration-ingest-internal").prepareData();
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

    private String doIngest(String zip) throws FileNotFoundException, VitamException {

        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();

        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);

        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(zip);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid, LogbookTypeProcess.INGEST,
                StatusCode.STARTED, ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        waitOperation(NB_TRY, SLEEP_TIME, ingestOperationGuid.getId());
        return ingestOperationGuid.toString();
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
    public void should_execute_audit_workflow_existence_without_error() throws Exception {
        
        String ingestOperationId1 = doIngest("elimination/TEST_ELIMINATION.zip");
        String ingestOperationId2 = doIngest("preservation/OG_with_3_parents.zip");

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            AuditOptions options = new AuditOptions();
            options.setAuditActions(AuditExistenceService.CHECK_EXISTENCE_ID);
            options.setAuditType("tenant");
            options.setObjectId("" + tenantId);

            RequestResponse<JsonNode> response = adminClient.launchAuditWorkflow(options);
            assertThat(response.isOk()).isTrue();
            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                    .selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results").get(0).get("events");
            System.out.println(JsonHandler.prettyPrint(jsonNode));
            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                    .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report
            List<JsonNode> reportLines = null;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                            operationGuid.toString() + ".jsonl", DataCategory.REPORT,
                            AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                    reportLines = getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(3);
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(8);
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").isArray()).isTrue();
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").toString()).contains(ingestOperationId1);
            assertThat(reportLines.get(1).get("extendedInfo").get("opis").toString()).contains(ingestOperationId2);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("OK").asInt()).isEqualTo(7);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("OK").asInt()).isEqualTo(8);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectGroupsCount").get("OK").asInt()).isEqualTo(3);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("RATP").get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(2).get("auditActions").asText()).isEqualTo("AUDIT_FILE_EXISTING");
            assertThat(reportLines.get(2).get("auditType").asText()).isEqualTo("tenant");
            assertThat(reportLines.get(2).get("objectId").asText()).isEqualTo("0");
            
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_audit_workflow_integrity_without_error() throws Exception {

        doIngest("elimination/TEST_ELIMINATION.zip");
        doIngest("preservation/OG_with_3_parents.zip");

        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            AuditOptions options = new AuditOptions();
            options.setAuditActions(AuditIntegrityService.CHECK_INTEGRITY_ID);
            options.setAuditType("originatingagency");
            options.setObjectId("FRAN_NP_009913");

            RequestResponse<JsonNode> response = adminClient.launchAuditWorkflow(options);
            assertThat(response.isOk()).as(response.toString()).isTrue();
            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                    .selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                    .get("$results").get(0).get("events");
            System.out.println(JsonHandler.prettyPrint(jsonNode));
            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                    .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            // Check report
            List<JsonNode> reportLines = null;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                Response reportResponse = null;
                try {
                    reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                            operationGuid.toString() + ".jsonl", DataCategory.REPORT,
                            AccessLogUtils.getNoLogAccessLog());
                    assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
                    reportLines = getReport(reportResponse);
                } finally {
                    consumeAnyEntityAndClose(reportResponse);
                }
            }
            assertThat(reportLines.size()).isEqualTo(3);
            assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjectGroups").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("nbObjects").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("globalResults").get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectGroupsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("OK").asInt()).isEqualTo(4);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("WARNING").asInt()).isEqualTo(0);
            assertThat(reportLines.get(1).get("extendedInfo").get("originatingAgencyResults").get("FRAN_NP_009913").get("objectsCount").get("KO").asInt()).isEqualTo(0);
            assertThat(reportLines.get(2).get("auditActions").asText()).isEqualTo("AUDIT_FILE_INTEGRITY");
            assertThat(reportLines.get(2).get("auditType").asText()).isEqualTo("originatingagency");
            assertThat(reportLines.get(2).get("objectId").asText()).isEqualTo("FRAN_NP_009913");
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
