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
package fr.gouv.vitam.metadata.reconstruction;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.model.PersistentIdentifierReconstructionRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.VitamTestHelper.doIngest;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PersistentIdentifierReconstructionIT extends VitamRuleRunner {

    public static final String ZIP = ".zip";
    public static final String CONTRACT_ID = "aName3";
    public static final String CONTEXT_ID = "Context_IT";
    public static final String ELIMINATION_DATE = "2023-01-01";
    public static final int HTTP_OK = 200;
    public static final String INTEGRATION_INGEST_INTERNAL = "integration-ingest-internal";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        PersistentIdentifierReconstructionIT.class
    );
    private static final String XML = ".xml";
    private static final String TEST_ELIMINATION_V3_SIP = "elimination/TEST_ELIMINATION_V3.zip";
    private static final String TEST_ELIMINATION_V4_SIP = "elimination/TEST_ELIMINATION_V4.zip";
    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        PersistentIdentifierReconstructionIT.class,
        mongoRule.getMongoDatabase().getName(),
        elasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    private static OffsetRepository offsetRepository;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        String configSiegfriedPath = PropertiesUtils.getResourcePath(
            INTEGRATION_INGEST_INTERNAL + "/format-identifiers.conf"
        ).toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(configSiegfriedPath);

        new DataLoader(INTEGRATION_INGEST_INTERNAL).prepareData();

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void testEliminatedPersistentIdentifierReconstructOk() throws Exception {
        prepareVitamSession();
        final String ingestOperationGuid = VitamTestHelper.doIngest(TENANT_0, TEST_ELIMINATION_V3_SIP);
        verifyOperation(ingestOperationGuid, OK);

        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        final String eliminationActionOperationGuid = newOperationLogbookGUID(TENANT_0).toString();
        VitamThreadUtils.getVitamSession().setRequestId(eliminationActionOperationGuid);

        SelectMultiQuery analysisDslRequest = new SelectMultiQuery();
        analysisDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            ELIMINATION_DATE,
            analysisDslRequest.getFinalSelect()
        );

        final RequestResponse<JsonNode> actionResult = accessInternalClient.startEliminationAction(
            eliminationRequestBody
        );

        OperationContextMonitor operationContextMonitor = new OperationContextMonitor();
        assertThat(actionResult.isOk()).isTrue();

        JsonNode info = operationContextMonitor.getInformation(
            VitamConfiguration.getDefaultStrategy(),
            eliminationActionOperationGuid,
            LogbookTypeProcess.ELIMINATION
        );

        assertThat(info).isNotNull();

        awaitForWorkflowTerminationWithStatus(eliminationActionOperationGuid, StatusCode.WARNING);

        TimeUnit.SECONDS.sleep(1);

        info = operationContextMonitor.getInformation(
            VitamConfiguration.getDefaultStrategy(),
            eliminationActionOperationGuid,
            LogbookTypeProcess.ELIMINATION
        );

        assertThat(info).isNotNull();

        PersistentIdentifierReconstructionRequest requestItem = new PersistentIdentifierReconstructionRequest();
        List<Integer> tenants = Lists.list(TENANT_0, TENANT_1);
        requestItem.setTenants(tenants);
        MetaDataClientFactory metaDataClientFactory = MetaDataClientFactory.getInstance();
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> response = metaDataClient.reconstructPersistentIdentifiers(requestItem);
            assertThat(response.getHttpCode()).isEqualTo(HTTP_OK);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeletedGotWithPersistentIdentifierReconstruction_Ok() throws Exception {
        prepareVitamSession();
        String ingestOperationId = doIngest(TENANT_0, TEST_ELIMINATION_V4_SIP);
        verifyOperation(ingestOperationId, OK);

        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            // Prepare Request for delete got versions
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery.addQueries(QueryHelper.eq("Title", "UnitF"));

            DeleteGotVersionsRequest deleteGotVersionsRequest = new DeleteGotVersionsRequest(
                searchDslQuery.getFinalSelect(),
                BINARY_MASTER.getName(),
                List.of(2)
            );

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
            getVitamSession().setRequestId(operationGuid);
            final RequestResponse<JsonNode> actionResult = accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, OK);

            PersistentIdentifierReconstructionRequest requestItem = new PersistentIdentifierReconstructionRequest();
            List<Integer> tenants = Lists.list(TENANT_0, TENANT_1);
            requestItem.setTenants(tenants);
            MetaDataClientFactory metaDataClientFactory = MetaDataClientFactory.getInstance();
            try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
                RequestResponse<JsonNode> response = metaDataClient.reconstructPersistentIdentifiers(requestItem);
                assertThat(response.getHttpCode()).isEqualTo(HTTP_OK);
            }
        }
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {
        waitOperation(operationGuid);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationGuid, TENANT_0);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {} catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            jakarta.ws.rs.core.Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    filename,
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );

                assertThat(reportResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());

                return new VitamAsyncInputStream(reportResponse);
            } catch (
                RuntimeException
                | StorageServerClientException
                | StorageNotFoundException
                | StorageUnavailableDataFromAsyncOfferClientException e
            ) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }

    private Set<String> getBinaryObjectIds(RequestResponseOK<JsonNode> gots) {
        Set<String> objectIds = new HashSet<>();
        for (JsonNode gotJson : gots.getResults()) {
            objectIds.addAll(getBinaryObjectIds(gotJson));
        }
        return objectIds;
    }

    private Set<String> getBinaryObjectIds(JsonNode gotJson) {
        Set<String> objectIds = new HashSet<>();

        try {
            ObjectGroupResponse gotResponse = getFromJsonNode(gotJson, ObjectGroupResponse.class);

            for (QualifiersModel qualifier : gotResponse.getQualifiers()) {
                for (VersionsModel version : qualifier.getVersions()) {
                    if (version.getPhysicalId() == null) {
                        objectIds.add(version.getId());
                    }
                }
            }
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
        return objectIds;
    }

    private RequestResponseOK<JsonNode> selectGotsByOpi(
        String ingestOperationGuid,
        AccessInternalClient accessInternalClient
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationGotDslRequest = new SelectMultiQuery();
        checkEliminationGotDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid)
        );

        return (RequestResponseOK<JsonNode>) accessInternalClient.selectObjects(
            checkEliminationGotDslRequest.getFinalSelect()
        );
    }

    private RequestResponseOK<JsonNode> selectUnitsByOpi(
        String ingestOperationGuid,
        AccessInternalClient accessInternalClient
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationDslRequest = new SelectMultiQuery();
        checkEliminationDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid)
        );

        return (RequestResponseOK<JsonNode>) accessInternalClient.selectUnits(
            checkEliminationDslRequest.getFinalSelect()
        );
    }
}
