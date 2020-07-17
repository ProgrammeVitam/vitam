/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.ingest.external.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestRequestParameters;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Ingest External integration test
 */
public class IngestExternalIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";
    private static final String INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP = "integration-processing/4_UNITS_2_GOTS.zip";
    private static final String SIP_NOT_ALLOWED_NAME = "integration-processing/KO_FILE_extension_caractere_special.zip";
    private static final String SIP_INCORRECT_OBJECT_SIZE = "integration-processing/KO_SIP_INCORRECT_OBJECT_SIZE.zip";
    private static final String SIP_MISSED_OBJECT_SIZE = "integration-processing/OK_SIP_MISSED_OBJECT_SIZE.zip";
    private static final String ACCESS_CONTRACT = "aName3";
    public static final String OPERATION_ID_REPLACE = "OPERATION_ID_REPLACE";
    public static final String INTEGRATION_INGEST_EXTERNAL_EXPECTED_LOGBOOK_JSON =
        "integration-ingest-external/expected-logbook.json";


    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(IngestExternalIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                AccessExternalMain.class,
                IngestInternalMain.class,
                IngestExternalMain.class));

    private static IngestExternalClient ingestExternalClient;
    private static AdminExternalClient adminExternalClient;
    private static AccessExternalClient accessExternalClient;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        ingestExternalClient = IngestExternalClientFactory.getInstance().getClient();
        adminExternalClient = AdminExternalClientFactory.getInstance().getClient();
        accessExternalClient = AccessExternalClientFactory.getInstance().getClient();

        // TODO: 18/09/2019 should import referential from externals
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @RunWithCustomExecutor
    @Test
    public void test_ingest_ok() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP)) {
            RequestResponse response = ingestExternalClient
                .ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract("aName3"),
                    inputStream, DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name());

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                .wait(tenantId, operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            response = adminExternalClient.getOperationProcessExecutionDetails(new VitamContext(tenantId), operationId);
            assertThat(response.isOk()).isTrue();


            RequestResponseOK<ItemStatus> itemStatusRequestResponse = (RequestResponseOK<ItemStatus>) response;
            assertThat(itemStatusRequestResponse.getResults()).hasSize(1);

            ItemStatus itemStatus = itemStatusRequestResponse.getFirstResult();
            assertThat(itemStatus).isNotNull();
            assertThat(itemStatus.getGlobalState()).isEqualTo(ProcessState.COMPLETED);
            assertThat(itemStatus.getGlobalStatus()).as(JsonHandler
                .unprettyPrint(LogbookCollections.OPERATION.getCollection().find(Filters.eq(operationId))))
                .isEqualTo(StatusCode.OK);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void test_ingest_step_by_step_ok() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream("integration-processing/4_UNITS_2_GOTS.zip")) {
            // Start ingest with step by step mode
            RequestResponse response = ingestExternalClient
                .ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID).setAccessContract(
                        ACCESS_CONTRACT),
                    inputStream, DEFAULT_WORKFLOW.name(), ProcessAction.NEXT.name());

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            // Number of steps in ingest workflow
            int numberOfIngestSteps = 11;

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                .wait(tenantId, operationId, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            RequestResponse itemStatusResponse = null;
            while (null == itemStatusResponse || itemStatusResponse.isOk()) {
                numberOfIngestSteps--;
                // Check workflow state and status
                itemStatusResponse =
                    adminExternalClient.getOperationProcessExecutionDetails(new VitamContext(tenantId), operationId);
                assertThat(itemStatusResponse.isOk()).isTrue();
                RequestResponseOK<ItemStatus> itemStatusRequestResponse =
                    (RequestResponseOK<ItemStatus>) itemStatusResponse;
                assertThat(itemStatusRequestResponse.getResults()).hasSize(1);

                ItemStatus itemStatus = itemStatusRequestResponse.getFirstResult();
                assertThat(itemStatus.getGlobalStatus()).as(JsonHandler
                    .unprettyPrint(LogbookCollections.OPERATION.getCollection().find(Filters.eq(operationId))))
                    .isEqualTo(StatusCode.OK);


                if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                    break;
                }

                // Execute the next step
                RequestResponse<ItemStatus> updateResponse = adminExternalClient.updateOperationActionProcess(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract(ACCESS_CONTRACT),
                    ProcessAction.NEXT.name(),
                    operationId
                );
                assertThat(updateResponse.isOk()).isTrue();

                // Wait the end of execution of the current step
                process_timeout = vitamPoolingClient
                    .wait(tenantId, operationId, 1800, 1_000L, TimeUnit.MILLISECONDS);
                if (!process_timeout) {
                    Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
                }
            }

            // Assert all steps are executed
            assertThat(numberOfIngestSteps).isEqualTo(0);

            // Get logbook and check all events
            Document operation =
                (Document) LogbookCollections.OPERATION.getCollection().find(Filters.eq(operationId), Document.class)
                    .first();
            InputStream expected =
                PropertiesUtils.getResourceAsStream(INTEGRATION_INGEST_EXTERNAL_EXPECTED_LOGBOOK_JSON);
            JsonNode expectedJsonNode = JsonHandler.getFromInputStream(
                expected);
            String found = JsonHandler.prettyPrint(operation).replace(operationId, OPERATION_ID_REPLACE);
            JsonNode foundJsonNode = JsonHandler.getFromString(found);
            JsonAssert.assertJsonEquals(expectedJsonNode, foundJsonNode,
                JsonAssert
                    .whenIgnoringPaths(
                        "evDetData",
                        "evDateTime",
                        "agId",
                        "_lastPersistedDate",
                        "events[*].evDetData",
                        "events[*].evId",
                        "events[*].evDateTime",
                        "events[*].agId",
                        "events[*].evParentId")
            );

        }
    }

    @RunWithCustomExecutor
    @Test
    public void test_ingest_with_manifest_digest_ok() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP)) {
            IngestRequestParameters ingestRequestParameters =
                new IngestRequestParameters(DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name())
                    .setManifestDigestAlgo("SHA-512")
                    .setManifestDigestValue(
                        "3112e4f4f66c70f0565b95ea270c7488f074ace3ab28f74feaa975751b424619ff429490416f1c4b630361ab16f0bb5f16d92f5a867e6f94c886464e95f82ca5");
            RequestResponse response = ingestExternalClient
                .ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract("aName3"),
                    inputStream,
                    ingestRequestParameters);

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                .wait(tenantId, operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            response = adminExternalClient.getOperationProcessExecutionDetails(new VitamContext(tenantId), operationId);
            assertThat(response.isOk()).isTrue();

            RequestResponse<LogbookOperation> logbookOperationRequestResponse = accessExternalClient
                .selectOperationbyId(new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT), operationId, new Select().getFinalSelectById());
            LogbookOperation logbookOperation = ((RequestResponseOK<LogbookOperation>)
                logbookOperationRequestResponse).getFirstResult();
            String logbookOperationStr = JsonHandler.prettyPrint(JsonHandler.toJsonNode(logbookOperation));

            assertThat(logbookOperationStr).contains("PROCESS_SIP_UNITARY.OK");
            assertThat(logbookOperationStr).doesNotContain("MANIFEST_DIGEST_CHECK");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void test_ingest_with_invalid_manifest_digest_ko() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP)) {
            IngestRequestParameters ingestRequestParameters =
                new IngestRequestParameters(DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name())
                    .setManifestDigestAlgo("SHA-512")
                    .setManifestDigestValue("BAD_DIGEST");
            RequestResponse response = ingestExternalClient
                .ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract("aName3"),
                    inputStream,
                    ingestRequestParameters);

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                .wait(tenantId, operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            RequestResponse<LogbookOperation> logbookOperationRequestResponse = accessExternalClient
                .selectOperationbyId(new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT), operationId, new Select().getFinalSelectById());
            LogbookOperation logbookOperation = ((RequestResponseOK<LogbookOperation>)
                logbookOperationRequestResponse).getFirstResult();
            String logbookOperationStr = JsonHandler.prettyPrint(JsonHandler.toJsonNode(logbookOperation));

            assertThat(logbookOperationStr).contains("PROCESS_SIP_UNITARY.KO");
            assertThat(logbookOperationStr).contains("MANIFEST_DIGEST_CHECK.KO");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void test_ingest_with_not_allowed_file_name_ko() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(SIP_NOT_ALLOWED_NAME)) {
            ;
            RequestResponse response = ingestExternalClient
                .ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                        .setAccessContract("aName3"),
                    inputStream,
                    DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name());

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                .wait(tenantId, operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            RequestResponse<LogbookOperation> logbookOperationRequestResponse = accessExternalClient
                .selectOperationbyId(new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT), operationId, new Select().getFinalSelectById());
            LogbookOperation logbookOperation = ((RequestResponseOK<LogbookOperation>)
                logbookOperationRequestResponse).getFirstResult();
            String logbookOperationStr = JsonHandler.prettyPrint(JsonHandler.toJsonNode(logbookOperation));

            assertThat(logbookOperationStr)
                .contains("PROCESS_SIP_UNITARY.KO")
                .contains("un des noms de fichiers contient un caract");
            assertThat(logbookOperationStr).doesNotContain(".FATAL");

        }
    }

    @RunWithCustomExecutor
    @Test
    public void test_tenant_filtering() throws Exception {
        final int ingestTenant = 0;
        final int accessTenant = 1;
        final String tenant1Contract = "newContract";

        String operationId = ingestResource(INTEGRATION_PROCESSING_4_UNITS_2_GOTS_ZIP, ingestTenant);

        // requesting with another tenant than ingest
        assertThatCode(() -> getLogbookOperation(operationId, accessTenant, tenant1Contract))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Not Found");

        // verifying that in the correct tenant we have the logbook (to prevent false-ok test)
        RequestResponse<LogbookOperation> logbookOperationRequestResponse = getLogbookOperation(operationId, ingestTenant, ACCESS_CONTRACT);
        assertThat(((RequestResponseOK<LogbookOperation>) logbookOperationRequestResponse).getResults().size()).isEqualTo(1);

        // retrieve one unit we ingest
        RequestResponse<JsonNode> units = getUnitsFromTitle("UnitA", ingestTenant, ACCESS_CONTRACT);
        List results = ((RequestResponseOK) units).getResults();
        assertNotNull(results);
        assertThat(results.size()).isGreaterThan(0);

        String unitId = ((ObjectNode) results.get(0)).get("#id").asText();
        VitamContext vitamContextIngestTenant = new VitamContext(ingestTenant)
                .setApplicationSessionId(APPLICATION_SESSION_ID)
                .setAccessContract(ACCESS_CONTRACT);

        final String queryDsql =
                "{\n" +
                        "  \"$projection\": {\n" +
                        "    \"$fields\": {\n" +
                        "      \"#id\": 1\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

        // in the correct tenant we should find the unit
        RequestResponse<JsonNode> result = accessExternalClient.selectUnitbyId(vitamContextIngestTenant,
                JsonHandler.getFromString(queryDsql), unitId);
        assertTrue(result.isOk());
        List<JsonNode> resultUnit = ((RequestResponseOK<JsonNode>) result).getResults();
        assertNotNull(resultUnit);
        assertThat(resultUnit.size()).isGreaterThan(0);

        // in the wrong tenant, we should not find the unit
        VitamContext vitamContextAccessTenant = new VitamContext(accessTenant)
                .setApplicationSessionId(APPLICATION_SESSION_ID)
                .setAccessContract("newContract");

        assertThatCode(() -> accessExternalClient.selectUnitbyId(vitamContextAccessTenant,
                JsonHandler.getFromString(queryDsql), unitId))
                .isInstanceOf(VitamClientException.class)
                .hasMessageContaining("Not Found");
    }

    private RequestResponse<JsonNode> getUnitsFromTitle(String title, int tenant, String accessContract)
            throws VitamClientException, InvalidParseOperationException {
        VitamContext vitamContext = new VitamContext(tenant)
                .setApplicationSessionId(APPLICATION_SESSION_ID)
                .setAccessContract(accessContract);
        final String queryDsql =
                "{ \"$query\" : [ { \"$eq\": { \"Title\" : \"" + title + "\" } } ], " +
                        " \"$projection\" : { \"$fields\" : { \"#id\": 1} } " +
                        " }";
        return accessExternalClient.selectUnits(vitamContext, JsonHandler.getFromString(queryDsql));
    }

    private RequestResponse<LogbookOperation> getLogbookOperation(String operationId, int tenantId, String accessContract)
            throws VitamClientException {
        VitamContext vitamContext = new VitamContext(tenantId)
                .setApplicationSessionId(APPLICATION_SESSION_ID)
                .setAccessContract(accessContract);
        return accessExternalClient
                .selectOperationbyId(vitamContext, operationId, new Select().getFinalSelectById());
    }

    private String ingestResource(String resource, int tenantId) throws Exception {
        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(resource)) {
            IngestRequestParameters ingestRequestParameters =
                    new IngestRequestParameters(DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name());
            RequestResponse<Void> response = ingestExternalClient.ingest(
                    new VitamContext(tenantId).setApplicationSessionId(APPLICATION_SESSION_ID)
                            .setAccessContract(ACCESS_CONTRACT),
                    inputStream,
                    ingestRequestParameters);
            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient
                    .wait(tenantId, operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }

            RequestResponse<ItemStatus> operationResponse = adminExternalClient.getOperationProcessExecutionDetails(new VitamContext(tenantId), operationId);
            assertTrue(operationResponse.isOk());
            return operationId;
        }
    }
}
