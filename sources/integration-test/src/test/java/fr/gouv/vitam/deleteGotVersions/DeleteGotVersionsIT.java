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
package fr.gouv.vitam.deleteGotVersions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
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
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModelCustomized;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ResultPreservation;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamTestHelper.doIngest;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.DISSEMINATION;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.metadata.client.MetaDataClientFactory.getInstance;
import static fr.gouv.vitam.purge.EndToEndEliminationAndTransferReplyIT.prepareVitamSession;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeleteGotVersionsIT extends VitamRuleRunner {
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
    private static final String CONTEXT_ID = "Context_IT";
    private static final String MONGO_NAME = mongoRule.getMongoDatabase().getName();
    private static final String ES_NAME = ElasticsearchRule.getClusterName();
    private static final String GRIFFIN_LIBREOFFICE = "griffin-libreoffice";
    private static final TypeReference<List<PreservationScenarioModel>> PRESERVATION_SCENARIO_MODELS =
        new TypeReference<>() {
        };
    private static final TypeReference<List<GriffinModel>> GRIFFIN_MODELS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ObjectGroupResponse>> OBJECT_GROUP_RESPONSES_TYPE = new TypeReference<>() {
    };

    String ingestOperationId;

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(DeleteGotVersionsIT.class, MONGO_NAME, ES_NAME, SERVERS);

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        String configurationPath =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
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
        ingestOperationId = doIngest(TENANT_ID, "preservation/OG_with_3_parents.zip");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

            getVitamSession().setTenantId(TENANT_ADMIN);
            getVitamSession().setRequestId(newGUID());
            client.importGriffins(getGriffinModels());

            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setRequestId(newGUID());
            client.importPreservationScenarios(getPreservationScenarioModels());
        }
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


    @RunWithCustomExecutor
    @Test
    public void givenBinaryMasterThenDeleteGotVersions_OK() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> gotsBeforePreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforePreservation =
                (Set<Integer>) ((RequestResponseOK) gotsBeforePreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforePreservation.size());
            long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
            assertThat(countDetails).isEqualTo(1);

            // Launch 3 Preservation
            for (int i = 0; i < 3; i++) {
                launchPreservation(BINARY_MASTER, BINARY_MASTER);
            }

            RequestResponse<JsonNode> gotsAfterThirdPreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforeThirdPreservation =
                (Set<Integer>) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforeThirdPreservation.size());
            assertTrue(gotsQualifiersSizeBeforeThirdPreservation.contains(4));

            // Prepare Request for delete got versions
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    BINARY_MASTER.getName(), List.of(2));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);
            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, OK);

            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .allMatch(outcome -> outcome.equals(OK.name()));

            RequestResponse<JsonNode> gotsAfterDelete =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeAfterDelete =
                (Set<Integer>) ((RequestResponseOK) gotsAfterDelete).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeAfterDelete.size());
            assertTrue(gotsQualifiersSizeAfterDelete.contains(3));

            // Check report
            JsonNode reportsNode =
                JsonHandler.toJsonNode(VitamTestHelper.getReports(getVitamSession().getRequestId()).stream()
                    .filter(elmt -> elmt.has("objectGroupGlobal")).collect(Collectors.toList()));
            List<DeleteGotVersionsReportEntry> reportsList =
                getFromJsonNode(reportsNode, new TypeReference<>() {
                });
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getStatus(), OK);
            assertNull(reportsList.get(0).getObjectGroupGlobal().get(0).getOutcome());
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getDeletedVersions().size(), 1);
            assertThat(reportsList.get(0).getObjectGroupGlobal().get(0).getDeletedVersions().stream().map(
                VersionsModelCustomized::getDataObjectVersion).collect(Collectors.toList()))
                .contains("BinaryMaster_2");

            JsonNode accessRegisterDetailNode = JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()));
            assertNotNull(accessRegisterDetailNode);
            List<AccessionRegisterDetail> accessRegisterDetailList =
                getFromJsonNode(accessRegisterDetailNode, new TypeReference<>() {
                });
            assertTrue(
                accessRegisterDetailList.get(0).getEvents().stream().anyMatch(elmt -> elmt.getOperationType().equals(
                    ReportType.DELETE_GOT_VERSIONS.name())));
            assertThat(accessRegisterDetailList.get(0).getTotalObjectSize().getDeleted()).isGreaterThan(0);
            assertThat(accessRegisterDetailList.get(0).getTotalObjects().getDeleted()).isGreaterThan(0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenForbiddenVersionThenDeleteGotVersions_Warning() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> gotsBeforePreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforePreservation =
                (Set<Integer>) ((RequestResponseOK) gotsBeforePreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforePreservation.size());
            long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
            assertThat(countDetails).isGreaterThan(0);

            // Launch 3 Preservation
            for (int i = 0; i < 3; i++) {
                launchPreservation(BINARY_MASTER, BINARY_MASTER);
            }

            RequestResponse<JsonNode> gotsAfterThirdPreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforeThirdPreservation =
                (Set<Integer>) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforeThirdPreservation.size());
            assertTrue(gotsQualifiersSizeBeforeThirdPreservation.contains(4));

            // Prepare Request
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    BINARY_MASTER.getName(), List.of(1, 2));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);

            // WHEN

            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, WARNING);

            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .anyMatch(outcome -> outcome.equals(WARNING.name()));

            RequestResponse<JsonNode> gotsAfterDelete =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeAfterDelete =
                (Set<Integer>) ((RequestResponseOK) gotsAfterDelete).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeAfterDelete.size());
            assertTrue(gotsQualifiersSizeAfterDelete.contains(3));

            // Check report
            JsonNode reportsNode =
                JsonHandler.toJsonNode(VitamTestHelper.getReports(getVitamSession().getRequestId()).stream()
                    .filter(elmt -> elmt.has("objectGroupGlobal")).collect(Collectors.toList()));
            List<DeleteGotVersionsReportEntry> reportsList =
                getFromJsonNode(reportsNode, new TypeReference<>() {
                });
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getStatus(), WARNING);
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getOutcome(),
                "Qualifier with forbidden version 1 has been detected!");
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getStatus(), OK);
            assertNull(reportsList.get(0).getObjectGroupGlobal().get(1).getOutcome());
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getDeletedVersions().size(), 1);
            assertThat(reportsList.get(0).getObjectGroupGlobal().get(1).getDeletedVersions().stream().map(
                VersionsModelCustomized::getDataObjectVersion).collect(Collectors.toList()))
                .contains("BinaryMaster_2");

            // Check AccesRegisterDetail
            JsonNode accessRegisterDetailNode = JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()));
            assertNotNull(accessRegisterDetailNode);
            List<AccessionRegisterDetail> accessRegisterDetailList =
                getFromJsonNode(accessRegisterDetailNode, new TypeReference<>() {
                });
            assertTrue(
                accessRegisterDetailList.get(0).getEvents().stream().anyMatch(elmt -> elmt.getOperationType().equals(
                    ReportType.DELETE_GOT_VERSIONS.name())));
            assertThat(accessRegisterDetailList.get(0).getTotalObjectSize().getDeleted()).isGreaterThan(0);
            assertThat(accessRegisterDetailList.get(0).getTotalObjects().getDeleted()).isGreaterThan(0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenForbiddenAndInexistantVersionsThenDeleteGotVersions_Warning() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> gotsBeforePreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforePreservation =
                (Set<Integer>) ((RequestResponseOK) gotsBeforePreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforePreservation.size());
            long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
            assertThat(countDetails).isGreaterThan(0);

            // Launch 3 Preservation
            for (int i = 0; i < 3; i++) {
                launchPreservation(BINARY_MASTER, BINARY_MASTER);
            }

            RequestResponse<JsonNode> gotsAfterThirdPreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforeThirdPreservation =
                (Set<Integer>) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforeThirdPreservation.size());
            assertTrue(gotsQualifiersSizeBeforeThirdPreservation.contains(4));

            // Prepare Request
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    BINARY_MASTER.getName(), List.of(1, 2, 5000));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);

            // WHEN

            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, WARNING);

            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .anyMatch(outcome -> outcome.equals(WARNING.name()));

            RequestResponse<JsonNode> gotsAfterDelete =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeAfterDelete =
                (Set<Integer>) ((RequestResponseOK) gotsAfterDelete).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeAfterDelete.size());
            assertTrue(gotsQualifiersSizeAfterDelete.contains(3));

            // Check report
            JsonNode reportsNode =
                JsonHandler.toJsonNode(VitamTestHelper.getReports(getVitamSession().getRequestId()).stream()
                    .filter(elmt -> elmt.has("objectGroupGlobal")).collect(Collectors.toList()));
            List<DeleteGotVersionsReportEntry> reportsList =
                getFromJsonNode(reportsNode, new TypeReference<>() {
                });
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getStatus(), WARNING);
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getOutcome(),
                "Qualifier with forbidden version 1 has been detected!");
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getStatus(), WARNING);
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getOutcome(),
                "Qualifier with this specific version 5000 is inexistant!");
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(2).getStatus(), OK);
            assertNull(reportsList.get(0).getObjectGroupGlobal().get(2).getOutcome());
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(2).getDeletedVersions().size(), 1);
            assertThat(reportsList.get(0).getObjectGroupGlobal().get(2).getDeletedVersions().stream().map(
                VersionsModelCustomized::getDataObjectVersion).collect(Collectors.toList()))
                .contains("BinaryMaster_2");

            // Check AccesRegisterDetail
            JsonNode accessRegisterDetailNode = JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()));
            assertNotNull(accessRegisterDetailNode);
            List<AccessionRegisterDetail> accessRegisterDetailList =
                getFromJsonNode(accessRegisterDetailNode, new TypeReference<>() {
                });
            assertTrue(
                accessRegisterDetailList.get(0).getEvents().stream().anyMatch(elmt -> elmt.getOperationType().equals(
                    ReportType.DELETE_GOT_VERSIONS.name())));
            assertThat(accessRegisterDetailList.get(0).getTotalObjectSize().getDeleted()).isGreaterThan(0);
            assertThat(accessRegisterDetailList.get(0).getTotalObjects().getDeleted()).isGreaterThan(0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenInvalidUsageNameThenDeleteGotVersions_KO() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            // Prepare Request
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    "I DONT EXIST", List.of(2));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);

            // WHEN

            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, KO);

            // THEN
            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .anyMatch(outcome -> outcome.equals(KO.name()));
            assertThat(logbookOperation.getEvents().get(3).getEvDetData()).contains("Usage name is unknown.");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenEmptySpecificVersionsThenDeleteGotVersions_KO() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            // Prepare Request
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    BINARY_MASTER.getName(), Collections.emptyList());

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);

            // WHEN

            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, KO);

            // THEN
            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .anyMatch(outcome -> outcome.equals(KO.name()));
            assertThat(logbookOperation.getEvents().get(3).getEvDetData()).contains("Specific versions list is empty.");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenDissemniationThenDeleteGotVersions_OK() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> gotsBeforePreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforePreservation =
                (Set<Integer>) ((RequestResponseOK) gotsBeforePreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforePreservation.size());

            // Launch 3 Preservation
            launchPreservation(BINARY_MASTER, DISSEMINATION);
            for (int i = 0; i < 2; i++) {
                launchPreservation(DISSEMINATION, DISSEMINATION);
            }

            RequestResponse<JsonNode> gotsAfterThirdPreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            assertEquals(2,
                ((ObjectNode) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().get(0)).get("#qualifiers")
                    .size());
            Set<Integer> gotsQualifiersSizeBeforeThirdPreservation =
                (Set<Integer>) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(1).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforeThirdPreservation.size());
            assertTrue(gotsQualifiersSizeBeforeThirdPreservation.contains(3));

            // Prepare Request for delete got versions
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    DISSEMINATION.getName(), List.of(1, 2));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);
            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, OK);

            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .allMatch(outcome -> outcome.equals(OK.name()));

            RequestResponse<JsonNode> gotsAfterDelete =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeAfterDelete =
                (Set<Integer>) ((RequestResponseOK) gotsAfterDelete).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeAfterDelete.size());
            assertTrue(gotsQualifiersSizeAfterDelete.contains(1));

            // Check report
            JsonNode reportsNode =
                JsonHandler.toJsonNode(VitamTestHelper.getReports(getVitamSession().getRequestId()).stream()
                    .filter(elmt -> elmt.has("objectGroupGlobal")).collect(Collectors.toList()));
            List<DeleteGotVersionsReportEntry> reportsList =
                getFromJsonNode(reportsNode, new TypeReference<>() {
                });
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getStatus(), OK);
            assertNull(reportsList.get(0).getObjectGroupGlobal().get(0).getOutcome());
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getDeletedVersions().size(), 2);
            assertThat(reportsList.get(0).getObjectGroupGlobal().get(0).getDeletedVersions().stream().map(
                VersionsModelCustomized::getDataObjectVersion).collect(Collectors.toList()))
                .contains("Dissemination_1", "Dissemination_2");

            JsonNode accessRegisterDetailNode = JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()));
            assertNotNull(accessRegisterDetailNode);
            List<AccessionRegisterDetail> accessRegisterDetailList =
                getFromJsonNode(accessRegisterDetailNode, new TypeReference<>() {
                });
            assertTrue(
                accessRegisterDetailList.get(0).getEvents().stream().anyMatch(elmt -> elmt.getOperationType().equals(
                    ReportType.DELETE_GOT_VERSIONS.name())));
            assertThat(accessRegisterDetailList.get(0).getTotalObjectSize().getDeleted()).isGreaterThan(0);
            assertThat(accessRegisterDetailList.get(0).getTotalObjects().getDeleted()).isGreaterThan(0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void givenDissemniationAndInexistantVersionThenDeleteGotVersions_Warning() throws Exception {
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            // GIVEN
            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            RequestResponse<JsonNode> gotsBeforePreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeBeforePreservation =
                (Set<Integer>) ((RequestResponseOK) gotsBeforePreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforePreservation.size());

            // Launch 3 Preservation
            launchPreservation(BINARY_MASTER, DISSEMINATION);
            for (int i = 0; i < 2; i++) {
                launchPreservation(DISSEMINATION, DISSEMINATION);
            }

            RequestResponse<JsonNode> gotsAfterThirdPreservation =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            assertEquals(2,
                ((ObjectNode) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().get(0)).get("#qualifiers")
                    .size());
            Set<Integer> gotsQualifiersSizeBeforeThirdPreservation =
                (Set<Integer>) ((RequestResponseOK) gotsAfterThirdPreservation).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(1).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeBeforeThirdPreservation.size());
            assertTrue(gotsQualifiersSizeBeforeThirdPreservation.contains(3));

            // Prepare Request for delete got versions
            SelectMultiQuery searchDslQuery = new SelectMultiQuery();
            searchDslQuery
                .addQueries(QueryHelper.exists(VitamFieldsHelper.id()));

            DeleteGotVersionsRequest deleteGotVersionsRequest =
                new DeleteGotVersionsRequest(searchDslQuery.getFinalSelect(),
                    DISSEMINATION.getName(), List.of(1, 2, 1818));

            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setRequestId(operationGuid);
            final RequestResponse<JsonNode> actionResult =
                accessClient.deleteGotVersions(deleteGotVersionsRequest);
            assertThat(actionResult.isOk()).isTrue();
            VitamTestHelper.awaitForWorkflowTerminationWithStatus(operationGuid, WARNING);

            LogbookOperation logbookOperation = getLogbookOperation();
            assertThat(logbookOperation.getEvents().stream().map(LogbookEventOperation::getOutcome)
                .collect(Collectors.toList()))
                .anyMatch(outcome -> outcome.equals(WARNING.name()));

            RequestResponse<JsonNode> gotsAfterDelete =
                accessClient.selectObjects(getGotsRequest.getFinalSelect());
            Set<Integer> gotsQualifiersSizeAfterDelete =
                (Set<Integer>) ((RequestResponseOK) gotsAfterDelete).getResults().stream()
                    .map(elmt -> ((ObjectNode) elmt).get("#qualifiers").get(0).get("#nbc").asInt())
                    .collect(Collectors.toSet());
            assertEquals(1, gotsQualifiersSizeAfterDelete.size());
            assertTrue(gotsQualifiersSizeAfterDelete.contains(1));

            // Check report
            JsonNode reportsNode =
                JsonHandler.toJsonNode(VitamTestHelper.getReports(getVitamSession().getRequestId()).stream()
                    .filter(elmt -> elmt.has("objectGroupGlobal")).collect(Collectors.toList()));
            List<DeleteGotVersionsReportEntry> reportsList =
                getFromJsonNode(reportsNode, new TypeReference<>() {
                });
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getStatus(), WARNING);
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(0).getOutcome(),
                "Qualifier with this specific version 1818 is inexistant!");
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getStatus(), OK);
            assertNull(reportsList.get(0).getObjectGroupGlobal().get(1).getOutcome());
            assertEquals(reportsList.get(0).getObjectGroupGlobal().get(1).getDeletedVersions().size(), 2);
            assertThat(reportsList.get(0).getObjectGroupGlobal().get(1).getDeletedVersions().stream().map(
                VersionsModelCustomized::getDataObjectVersion).collect(Collectors.toList()))
                .contains("Dissemination_1", "Dissemination_2");

            JsonNode accessRegisterDetailNode = JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()));
            assertNotNull(accessRegisterDetailNode);
            List<AccessionRegisterDetail> accessRegisterDetailList =
                getFromJsonNode(accessRegisterDetailNode, new TypeReference<>() {
                });
            assertTrue(
                accessRegisterDetailList.get(0).getEvents().stream().anyMatch(elmt -> elmt.getOperationType().equals(
                    ReportType.DELETE_GOT_VERSIONS.name())));
            assertThat(accessRegisterDetailList.get(0).getTotalObjectSize().getDeleted()).isGreaterThan(0);
            assertThat(accessRegisterDetailList.get(0).getTotalObjects().getDeleted()).isGreaterThan(0);
        }
    }

    public void launchPreservation(DataObjectVersionType sourceType, DataObjectVersionType targetType)
        throws Exception {
        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
            getVitamSession().setTenantId(TENANT_ID);
            getVitamSession().setContractId("contract");
            getVitamSession().setContextId(CONTEXT_ID);
            getVitamSession().setRequestId(operationGuid);

            SelectMultiQuery getGotsRequest = new SelectMultiQuery();
            getGotsRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            buildAndSavePreservationResultFile(sourceType.getName());

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));
            ObjectNode finalSelect = select.getFinalSelect();

            PreservationRequest preservationRequest =
                new PreservationRequest(finalSelect, "PSC-000001", targetType.getName(),
                    FIRST, sourceType.getName());

            // when
            accessClient.startPreservation(preservationRequest);
            waitOperation(operationGuid.toString());

            // Then
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(OK.name()));

            tmpGriffinFolder.delete();
        }
    }

    private void buildAndSavePreservationResultFile(String sourceUsageName)
        throws IOException, InvalidParseOperationException {

        Map<String, String> objectIdsToFormat = getAllBinariesIds(sourceUsageName);

        ResultPreservation resultPreservation = new ResultPreservation();

        resultPreservation.setId("batchId");
        resultPreservation.setRequestId(getVitamSession().getRequestId());

        Map<String, List<OutputPreservation>> values = new HashMap<>();

        for (Map.Entry<String, String> entry : objectIdsToFormat.entrySet()) {

            List<OutputPreservation> outputPreservationList = new ArrayList<>();
            for (ActionTypePreservation action : singletonList(GENERATE)) {

                OutputPreservation outputPreservation = new OutputPreservation();

                outputPreservation.setStatus(PreservationStatus.OK);
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

    private Map<String, String> getAllBinariesIds(String sourceUsageName) {

        List<ObjectGroupResponse> objectModelsForUnitResults = getAllObjectModels();

        Map<String, String> allObjectIds = new HashMap<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {

            Optional<VersionsModel> versionsModelOptional =
                objectGroup.getFirstVersionsModel(sourceUsageName);

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

    private List<PreservationScenarioModel> getPreservationScenarioModels() throws Exception {
        File resourceFile = PropertiesUtils.getResourceFile("preservation/scenarios.json");
        return getFromFileAsTypeReference(resourceFile, PRESERVATION_SCENARIO_MODELS);
    }

    private List<GriffinModel> getGriffinModels()
        throws FileNotFoundException, InvalidParseOperationException {
        File resourceFile = PropertiesUtils.getResourceFile("preservation/griffins.json");
        return getFromFileAsTypeReference(resourceFile, GRIFFIN_MODELS_TYPE);
    }

    private LogbookOperation getLogbookOperation()
        throws LogbookClientException, InvalidParseOperationException {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperationVersionModelResult =
                logbookClient.selectOperationById(getVitamSession().getRequestId());
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK =
                RequestResponseOK.getFromJsonNode(logbookOperationVersionModelResult);
            return JsonHandler
                .getFromJsonNode(logbookOperationVersionModelResponseOK.getFirstResult(), LogbookOperation.class);
        }
    }
}
