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
package fr.gouv.vitam.preservation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.griffin.GriffinReport;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadataForAu;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ResultPreservation;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.batch.report.model.PreservationStatus.OK;
import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.VitamTestHelper.doIngest;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.operations;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.PreservationVersion.LAST;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.EXTRACT_AU;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.metadata.client.MetaDataClientFactory.getInstance;
import static fr.gouv.vitam.purge.EndToEndEliminationAndTransferReplyIT.prepareVitamSession;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PreservationIT extends VitamRuleRunner {
    private static final HashSet<Class> SERVERS = Sets.newHashSet(
        AccessInternalMain.class,
        AdminManagementMain.class,
        ProcessManagementMain.class,
        LogbookMain.class,
        WorkspaceMain.class,
        MetadataMain.class,
        WorkerMain.class,
        IngestInternalMain.class,
        StorageMain.class,
        DefaultOfferMain.class,
        BatchReportMain.class
    );

    private static final int TENANT_ID = 0;
    private static final int TENANT_ADMIN = 1;
    private static final String CONTRACT_ID = "contract";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private static final String MONGO_NAME = mongoRule.getMongoDatabase().getName();
    private static final String ES_NAME = ElasticsearchRule.getClusterName();
    private static final String GRIFFIN_LIBREOFFICE = "griffin-libreoffice";
    private static final WorkFlow WORKFLOW = WorkFlow.of(CONTEXT_ID, WORKFLOW_IDENTIFIER, "INGEST");
    private static final TypeReference<List<PreservationScenarioModel>> PRESERVATION_SCENARIO_MODELS = new TypeReference<>() {};
    private static final TypeReference<List<GriffinModel>> GRIFFIN_MODELS_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<ObjectGroupResponse>> OBJECT_GROUP_RESPONSES_TYPE = new TypeReference<>() {};
    private static final TypeReference<OperationSummary> OPERATION_SUMMARY_TYPE = new TypeReference<>() {};

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(PreservationIT.class, MONGO_NAME, ES_NAME, SERVERS);

    private static final String DESCRIPTION_KEY = "Description";
    private static final String DESCRIPTION_VALUE = "This is an awesome description ! Thx Captain.";
    private static final String FOO_KEY = "Foo";
    private static final List<String> FOO_VALUE = Arrays.asList("bar1", "bar2");
    private static final TypeReference<List<String>> TYPE_LIST_STRING = new TypeReference<>() {};

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String configurationPath = PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @Before
    public void setUpBefore() throws Exception {
        getVitamSession().setRequestId(newOperationLogbookGUID(0));
        getVitamSession().setTenantId(TENANT_ID);
        File griffinsExecFolder = PropertiesUtils.getResourceFile("preservation" + File.separator);
        VitamConfiguration.setVitamGriffinExecFolder(griffinsExecFolder.getAbsolutePath());
        VitamConfiguration.setVitamGriffinInputFilesFolder(tmpGriffinFolder.getRoot().getAbsolutePath());

        AccessInternalClientFactory factory = AccessInternalClientFactory.getInstance();
        factory.changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
        factory.setVitamClientType(PRODUCTION);

        Path griffinExecutable = griffinsExecFolder.toPath().resolve("griffin-libreoffice/griffin");
        boolean griffinIsExecutable = griffinExecutable.toFile().setExecutable(true);
        if (!griffinIsExecutable) {
            throw new VitamRuntimeException("Wrong execution right for griffin-libreoffice/griffin.");
        }

        Path griffinExtractionAuExecutable = griffinsExecFolder.toPath().resolve("griffin-extraction-au/griffin");
        boolean griffinExtractAuIsExecutable = griffinExtractionAuExecutable.toFile().setExecutable(true);
        if (!griffinExtractAuIsExecutable) {
            throw new VitamRuntimeException("Wrong execution right for griffin-extraction-au/griffin.");
        }

        prepareVitamSession();
        doIngest(TENANT_ID,"elimination/TEST_ELIMINATION_V2.zip");
        doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);
        ProcessDataAccessImpl.getInstance().clearWorkflow();

        runAfterMongo(Sets.newHashSet(
            FunctionalAdminCollections.PRESERVATION_SCENARIO.getName(),
            FunctionalAdminCollections.GRIFFIN.getName(),
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            "ExtractedMetadata"
        ));

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.PRESERVATION_SCENARIO.getName()),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.GRIFFIN.getName())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_griffin_with_OK() throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
             StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());

            // When
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            // Then
            assertThat(getGriffinReport(storageClient, getVitamSession().getRequestId()).getStatusCode()).isEqualTo(StatusCode.OK);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_scenario_preservation_with_OK() throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());

            // When
            client.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            // Then
            assertThat(getGriffinReport(storageClient, getVitamSession().getRequestId()).getStatusCode()).isEqualTo(StatusCode.OK);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_remove_griffin_with_KO_when_scenario_used() throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
             LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            // Given
            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            client.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());

            // When
            assertThatThrownBy(() -> removeGriffins(client))
            .isInstanceOf(AdminManagementClientServerException.class);

            // Then
            assertThat(getLogbookOperation(logbookClient).getEvents().get(0).getOutcome()).isEqualTo(StatusCode.KO.name());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_griffin_with_WARNING_when_scenario_used() throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
             StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            // Given
            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            client.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());

            // When
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            // Then
            assertThat(getGriffinReport(storageClient, getVitamSession().getRequestId()).getStatusCode()).isEqualTo(StatusCode.WARNING);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_preservation_workflow_without_error() throws Exception {
        // Given
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
             AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            client.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setContractId(CONTRACT_ID);
            getVitamSession().setContextId("Context_IT");
            getVitamSession().setRequestId(operationGuid);

            // Check Accession Register Detail
            List<String> excludeFields = Lists
                .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate",
                    "OperationIds");

            // Get accession register details before start preservation
            long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
            assertThat(countDetails).isEqualTo(4);

            // Assert AccessionRegisterSummary
            assertJsonEquals("preservation/expected/accession_register_ratp_before.json",
                JsonHandler.toJsonNode(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
                excludeFields);

            assertJsonEquals("preservation/expected/accession_register_FRAN_NP_009913_before.json",
                JsonHandler.toJsonNode(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "FRAN_NP_009913")))),
                excludeFields);


            buildAndSavePreservationResultFile();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            PreservationRequest preservationRequest =
                new PreservationRequest(finalSelect, "PSC-000001", "BinaryMaster", LAST, "BinaryMaster");
            accessClient.startPreservation(preservationRequest);

            waitOperation(operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText()).allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            validateAccessionRegisterDetails(excludeFields, operationGuid.toString());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_metadata_for_unit() throws Exception {
        // Given
        Response storageResponse = null;
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient();
             AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
             StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            adminClient.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            adminClient.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setContractId(CONTRACT_ID);
            getVitamSession().setContextId("Context_IT");
            getVitamSession().setRequestId(operationGuid);


            buildAndSavePreservationResultFileForExtractionAU();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            PreservationRequest preservationRequest = new PreservationRequest(finalSelect, "PSC-000042", "BinaryMaster", LAST, "BinaryMaster");

        // When
            accessClient.startPreservation(preservationRequest);

        // Then
            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            SelectMultiQuery selectChangeByOperation = new SelectMultiQuery();
            selectChangeByOperation.setQuery(QueryHelper.in(operations(), operationGuid.getId()));
            ObjectNode selectChangeByOperationFinalSelect = selectChangeByOperation.getFinalSelect();

            RequestResponse<JsonNode> response = accessClient.selectUnits(selectChangeByOperationFinalSelect);
            List<JsonNode> units = ((RequestResponseOK) response).getResults();

            assertThat(units).isNotEmpty();

            for (JsonNode unitFromMetadata : units) {
                String description = unitFromMetadata.get(DESCRIPTION_KEY).asText();
                assertThat(description).isEqualTo(DESCRIPTION_VALUE);
                List<String> foo = JsonHandler.getFromJsonNode(unitFromMetadata.get(FOO_KEY), TYPE_LIST_STRING);
                assertThat(foo).isEqualTo(FOO_VALUE);

                storageResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(), unitFromMetadata.get("#id").asText() + ".json", UNIT, AccessLogUtils.getNoLogAccessLog());
                JsonNode unitFromStorage = getFromIS(storageResponse);
                String descriptionFromStorage = unitFromStorage.get("unit").get(DESCRIPTION_KEY).asText();
                List<String> fooFromStorage = JsonHandler.getFromJsonNode(unitFromStorage.get("unit").get(FOO_KEY), TYPE_LIST_STRING);
                assertThat(descriptionFromStorage).isEqualTo(DESCRIPTION_VALUE);
                assertThat(fooFromStorage).isEqualTo(FOO_VALUE);
            }

        } finally {
            consumeAnyEntityAndClose(storageResponse);
        }
    }

    private JsonNode getFromIS(Response reportResponse) throws IOException, InvalidParseOperationException {
        try (InputStream is = reportResponse.readEntity(InputStream.class)) {
            return JsonHandler.getFromInputStream(is);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_preservation_workflow_with_various_usage() throws Exception {
        // Given
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
             AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
             StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels("preservation/griffins.json"));

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            client.importPreservationScenarios(getPreservationScenarioModels("preservation/scenarios.json"));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setContractId(CONTRACT_ID);
            getVitamSession().setContextId("Context_IT");
            getVitamSession().setRequestId(operationGuid);

            buildAndSavePreservationResultFile();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            PreservationRequest preservationRequest =
                new PreservationRequest(finalSelect, "PSC-000001", "Dissemination", FIRST, "BinaryMaster");
            accessClient.startPreservation(preservationRequest);

            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            try (InputStream inputStream = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(), String.format("%s.jsonl", operationGuid.getId()), DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog()).readEntity(InputStream.class)) {
                    assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText()).allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

                    try (InputStream inputStreamExpected = getClass().getResourceAsStream("/preservation/preservationReport.jsonl")) {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        BufferedReader bufferedReaderExpected = new BufferedReader(new InputStreamReader(inputStreamExpected, StandardCharsets.UTF_8));
                        Optional<String> iteratorExpected = bufferedReaderExpected.lines().findFirst();
                        Optional<String> iterator1 = bufferedReader.lines().findFirst();
                        OperationSummary operationSummaryExpected = JsonHandler.getFromStringAsTypeReference(iteratorExpected.get(), OPERATION_SUMMARY_TYPE);
                        OperationSummary operationSummary = JsonHandler.getFromStringAsTypeReference(iterator1.get(), OPERATION_SUMMARY_TYPE);

                        assertThat(operationSummary.getEvType()).isEqualTo(operationSummaryExpected.getEvType());
                        assertThat(operationSummary.getOutcome()).isEqualTo(operationSummaryExpected.getOutcome());
                        assertThat(operationSummary.getOutMsg()).isEqualTo(operationSummaryExpected.getOutMsg());
                        assertThat(operationSummary.getTenant()).isEqualTo(operationSummaryExpected.getTenant());
                        assertThat(operationSummary.getRightsStatementIdentifier()).isEqualTo(operationSummaryExpected.getRightsStatementIdentifier());
                    }
                }
            JsonNode objectGroup = JsonHandler.toJsonNode(Lists.newArrayList(MetadataCollections.OBJECTGROUP.getCollection().find(new Document("_ops", operationGuid.getId())).sort(Sorts.ascending(ObjectGroup.NBCHILD))));
            assertThat(objectGroup.get(0).get("_qualifiers").get(1).get("qualifier").asText()).isEqualTo("Dissemination");
            assertThat(objectGroup.get(0).get("_qualifiers").get(1).get("versions").get(0).get("_storage").get("strategyId").asText()).isEqualTo(VitamConfiguration.getDefaultStrategy());

            // Ensure evDetData in not set in logbook operation for distributed step STP_PRESERVATION_ACTION / action PRESERVATION_BINARY_HASH
            assertThat(jsonNode.get(8).get("outDetail").asText()).isEqualTo("PRESERVATION_BINARY_HASH.OK");
            assertThat(jsonNode.get(8).get("evDetData").asText()).isEqualTo("{}");
        }
    }

    private void removeGriffins(AdminManagementClient client) throws AdminManagementClientServerException {
        client.importGriffins(Collections.emptyList());
    }

    private void validateAccessionRegisterDetails(List<String> excludeFields, String operationId)
        throws FileNotFoundException, InvalidParseOperationException, ReferentialException,
        InvalidCreateOperationException {
        long countDetails;
        // Get accession register details after start preservation
        countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
        assertThat(countDetails).isEqualTo(6);

        JsonNode query = getQueryDslByOpiType(operationId);

        // when check after preservation, the list of accession register details is not empty
        try (AdminManagementClient mgtClient = AdminManagementClientFactory.getInstance().getClient()) {

            RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterDetailsIngestOpiType =
                (RequestResponseOK<AccessionRegisterDetailModel>) mgtClient
                    .getAccessionRegisterDetail(query);

            assertThat(accessionRegisterDetailsIngestOpiType.getResults().size()).isEqualTo(2);

            // advanced additionnal checks
            verifyExpectedAccessionRegisterDetails(accessionRegisterDetailsIngestOpiType, excludeFields);
        }

        // Assert AccessionRegisterSummary
        assertJsonEquals("preservation/expected/accession_register_ratp_after.json",
            JsonHandler
                .toJsonNode(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        assertJsonEquals("preservation/expected/accession_register_FRAN_NP_009913_after.json",
            JsonHandler
                .toJsonNode(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "FRAN_NP_009913")))),
            excludeFields);
    }

    private void verifyExpectedAccessionRegisterDetails(
        RequestResponseOK<AccessionRegisterDetailModel> accessionRegisterDetailsIngestOpType,
        List<String> excludeFields)
        throws InvalidParseOperationException, FileNotFoundException {
        excludeFields.add("#id");
        // Assert AccessionRegisterDetails of ingest events
        assertJsonEquals("preservation/expected/accession_register_details_ingest_events.json",
            JsonHandler.toJsonNode(accessionRegisterDetailsIngestOpType.getResultsAsJsonNodes()),
            excludeFields);
    }

    private static JsonNode getQueryDslByOpiType(String operationId) throws InvalidCreateOperationException {
        Select select = new Select();
        Query query = QueryHelper.eq(AccessionRegisterDetailModel.OPI, operationId);
        select.setQuery(query);
        return select.getFinalSelect();
    }

    private void assertJsonEquals(String resourcesFile, JsonNode actual, List<String> excludeFields)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonNode expected = getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile));
        if (excludeFields != null) {
            expected.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }
            });
            actual.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }
            });
        }

        JsonAssert
            .assertJsonEquals(expected, actual, JsonAssert.whenIgnoringPaths(excludeFields.toArray(new String[] {})));
    }

    private void buildAndSavePreservationResultFileForExtractionAU() throws IOException, InvalidParseOperationException {

        Map<String, String> objectIdsToFormat = getAllBinariesIds();

        ResultPreservation resultPreservation = new ResultPreservation();

        resultPreservation.setId("batchId");
        resultPreservation.setRequestId(getVitamSession().getRequestId());

        Map<String, List<OutputPreservation>> values = new HashMap<>();

        for (Map.Entry<String, String> entry : objectIdsToFormat.entrySet()) {

            List<OutputPreservation> outputPreservationList = new ArrayList<>();
            for (ActionTypePreservation action : singletonList(EXTRACT_AU)) {
                OutputPreservation outputPreservation = new OutputPreservation();

                outputPreservation.setStatus(OK);
                outputPreservation.setAnalyseResult(EXTRACT_AU.name());
                outputPreservation.setAction(action);

                outputPreservation.setInputPreservation(new InputPreservation(entry.getKey(), entry.getValue()));
                ExtractedMetadataForAu extractedMetadata = new ExtractedMetadataForAu();
                extractedMetadata.put(DESCRIPTION_KEY, DESCRIPTION_VALUE);
                extractedMetadata.put(FOO_KEY, FOO_VALUE);
                outputPreservation.setExtractedMetadataAU(extractedMetadata);
                outputPreservationList.add(outputPreservation);
            }

            values.put(entry.getKey(), outputPreservationList);
        }

        resultPreservation.setOutputs(values);
        Path griffinIdDirectory = tmpGriffinFolder.newFolder("griffin-extraction-au").toPath();
        writeAsFile(resultPreservation, griffinIdDirectory.resolve("result.json").toFile());
    }

    private void buildAndSavePreservationResultFile() throws IOException, InvalidParseOperationException {

        Map<String, String> objectIdsToFormat = getAllBinariesIds();

        ResultPreservation resultPreservation = new ResultPreservation();

        resultPreservation.setId("batchId");
        resultPreservation.setRequestId(getVitamSession().getRequestId());

        Map<String, List<OutputPreservation>> values = new HashMap<>();

        for (Map.Entry<String, String> entry : objectIdsToFormat.entrySet()) {

            List<OutputPreservation> outputPreservationList = new ArrayList<>();
            for (ActionTypePreservation action : singletonList(GENERATE)) {

                OutputPreservation outputPreservation = new OutputPreservation();

                outputPreservation.setStatus(OK);
                outputPreservation.setAnalyseResult("VALID_ALL");
                outputPreservation.setAction(action);

                outputPreservation.setInputPreservation(new InputPreservation(entry.getKey(), entry.getValue()));
                outputPreservation.setOutputName("GENERATE-" + entry.getKey() + ".pdf");
                outputPreservationList.add(outputPreservation);
            }

            values.put(entry.getKey(), outputPreservationList);
        }

        resultPreservation.setOutputs(values);
        Path griffinIdDirectory = tmpGriffinFolder.newFolder(GRIFFIN_LIBREOFFICE).toPath();
        writeAsFile(resultPreservation, griffinIdDirectory.resolve("result.json").toFile());
    }

    private Map<String, String> getAllBinariesIds() {

        List<ObjectGroupResponse> objectModelsForUnitResults = getAllObjectModels();

        Map<String, String> allObjectIds = new HashMap<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {

            Optional<VersionsModel> versionsModelOptional =
                objectGroup.getFirstVersionsModel("BinaryMaster");

            VersionsModel model = versionsModelOptional.get();
            allObjectIds.put(model.getId(), model.getFormatIdentification().getFormatId());
        }
        return allObjectIds;
    }

    private List<ObjectGroupResponse> getAllObjectModels() {

        try (MetaDataClient client = getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = client.selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeReference(results.toString(), OBJECT_GROUP_RESPONSES_TYPE);

        } catch (VitamException | InvalidFormatException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<PreservationScenarioModel> getPreservationScenarioModels(String resourcesFile) throws Exception {
        File resourceFile = PropertiesUtils.getResourceFile(resourcesFile);
        return getFromFileAsTypeReference(resourceFile, PRESERVATION_SCENARIO_MODELS);
    }

    private List<GriffinModel> getGriffinModels(String resourcesFile) throws FileNotFoundException, InvalidParseOperationException {
        File resourceFile = PropertiesUtils.getResourceFile(resourcesFile);
        return getFromFileAsTypeReference(resourceFile, GRIFFIN_MODELS_TYPE);
    }

    private GriffinReport getGriffinReport(StorageClient storageClient, String requestId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException,
        StorageUnavailableDataFromAsyncOfferClientException {
        Response response = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(), requestId + ".json", DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
        return getFromInputStream((InputStream) response.getEntity(), GriffinReport.class);
    }

    private LogbookOperation getLogbookOperation(LogbookOperationsClient logbookClient) throws LogbookClientException, InvalidParseOperationException {
        JsonNode logbookOperationVersionModelResult = logbookClient.selectOperationById(getVitamSession().getRequestId());
        RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(logbookOperationVersionModelResult);
        return JsonHandler.getFromJsonNode(logbookOperationVersionModelResponseOK.getFirstResult(), LogbookOperation.class);
    }
}
