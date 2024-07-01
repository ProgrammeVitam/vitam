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

package fr.gouv.vitam.update;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextModel;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.integration.test.ProcessingIT;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.model.WorkspaceConstants.OPTIONS_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RevertEssentialMetadataIT extends VitamRuleRunner {

    private static final int TENANT_0 = 0;
    private static final String CONTEXT_ID = "fakeContextId";
    private static final String CONTRACT_ID = "AC-000001";

    private static final String CASE1_UNITS_FILE = "update/revert/case_1/Units.json";
    private static final String CASE1_LOGBOOK_LIFECYCLE_UNIT_FILE = "update/revert/case_1/LogbookLifeCycleUnit.json";

    private static final String CASE2_UNITS_FILE = "update/revert/case_2/Units.json";
    private static final String CASE2_LOGBOOK_LIFECYCLE_UNIT_FILE = "update/revert/case_2/LogbookLifeCycleUnit.json";

    private static final String OPTIONS_JSON_FILE = "update/revert/options.json";
    private static final String FORCE_OPTIONS_JSON_FILE = "update/revert/options_force.json";

    private static final String TITLE = "Title";
    private static final String TITLE_ = "Title_";
    private static final String FR = "fr";
    private static final String TITLE_FR = TITLE_ + "." + FR;

    private static final String OLD_VALUE = "Toto";

    private static WorkspaceClient workspaceClient;
    private static ProcessingManagementClient processingClient;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ProcessingIT.class,
        MongoRule.getDatabaseName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Collections.singletonList(TENANT_0), Collections.emptyMap());
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        new DataLoader("integration-processing").prepareData();
    }

    private void populateData(String unitsFile, String lfcFile) throws Exception {
        List<Unit> units = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream(unitsFile),
            new TypeReference<>() {}
        );
        MetadataCollections.UNIT.<Unit>getCollection().insertMany(units);
        MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_0, units);

        List<LogbookLifeCycleUnit> logbookLifeCycleUnits = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream(lfcFile),
            new TypeReference<>() {}
        );
        LogbookCollections.LIFECYCLE_UNIT.<LogbookLifeCycleUnit>getCollection().insertMany(logbookLifeCycleUnits);
    }

    @Test
    @RunWithCustomExecutor
    public void given_last_operation_should_run_without_error() throws Exception {
        populateData(CASE1_UNITS_FILE, CASE1_LOGBOOK_LIFECYCLE_UNIT_FILE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid);

        workspaceClient.createContainer(containerName);
        workspaceClient.putObject(containerName, OPTIONS_FILE, PropertiesUtils.getResourceAsStream(OPTIONS_JSON_FILE));
        workspaceClient.putObject(
            containerName,
            OperationContextMonitor.OperationContextFileName,
            writeToInpustream(
                OperationContextModel.get(
                    JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OPTIONS_JSON_FILE))
                )
            )
        );

        OperationContextMonitor.compressInWorkspace(
            WorkspaceClientFactory.getInstance(),
            containerName,
            Contexts.REVERT_ESSENTIAL_METADATA.getLogbookTypeProcess(),
            OperationContextMonitor.OperationContextFileName
        );

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.REVERT_ESSENTIAL_METADATA.name());
        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);

        VitamTestHelper.verifyOperation(containerName, StatusCode.OK);

        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        Select select = new Select();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), containerName));
        select.setProjection(
            createObjectNode()
                .set(
                    BuilderToken.PROJECTION.FIELDS.exactToken(),
                    createObjectNode()
                        .put(VitamFieldsHelper.id(), 1)
                        .put(VitamFieldsHelper.version(), 1)
                        .put(TITLE, 1)
                        .put(TITLE_FR, 1)
                        .put(VitamFieldsHelper.operations(), 1)
                )
        );

        JsonNode unitByIdRaw = metadataClient.selectUnits(select.getFinalSelect());

        unitByIdRaw
            .get(RequestResponseOK.TAG_RESULTS)
            .forEach(u -> {
                if (u.has(TITLE)) {
                    assertNotEquals(u.get(TITLE).asText(), OLD_VALUE);
                } else {
                    assertNotEquals(u.get(TITLE_).get(FR).asText(), OLD_VALUE);
                }
                assertThat(u.get(VitamFieldsHelper.version()).asInt()).isGreaterThan(1);
                assertThat(u.get(VitamFieldsHelper.operations()).iterator())
                    .extracting(JsonNode::asText)
                    .contains(containerName);
            });

        List<JsonNode> reportLines = VitamTestHelper.getReports(containerName);
        assertThat(reportLines).isNotNull();
        assertThat(reportLines.size()).isEqualTo(3);
        assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(12);
        assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
        assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
    }

    @Test
    @RunWithCustomExecutor
    public void given_not_last_operation_and_force_is_false_then_should_run_with_KO() throws Exception {
        populateData(CASE2_UNITS_FILE, CASE2_LOGBOOK_LIFECYCLE_UNIT_FILE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid);

        workspaceClient.createContainer(containerName);
        workspaceClient.putObject(containerName, OPTIONS_FILE, PropertiesUtils.getResourceAsStream(OPTIONS_JSON_FILE));
        workspaceClient.putObject(
            containerName,
            OperationContextMonitor.OperationContextFileName,
            writeToInpustream(
                OperationContextModel.get(
                    JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OPTIONS_JSON_FILE))
                )
            )
        );

        OperationContextMonitor.compressInWorkspace(
            WorkspaceClientFactory.getInstance(),
            containerName,
            Contexts.REVERT_ESSENTIAL_METADATA.getLogbookTypeProcess(),
            OperationContextMonitor.OperationContextFileName
        );

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.REVERT_ESSENTIAL_METADATA.name());
        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);

        VitamTestHelper.verifyOperation(containerName, StatusCode.KO);

        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        Select select = new Select();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), containerName));
        select.setProjection(
            createObjectNode()
                .set(
                    BuilderToken.PROJECTION.FIELDS.exactToken(),
                    createObjectNode()
                        .put(VitamFieldsHelper.id(), 1)
                        .put(VitamFieldsHelper.version(), 1)
                        .put(TITLE, 1)
                        .put(TITLE_FR, 1)
                        .put(VitamFieldsHelper.operations(), 1)
                )
        );

        JsonNode unitByIdRaw = metadataClient.selectUnits(select.getFinalSelect());

        unitByIdRaw
            .get(RequestResponseOK.TAG_RESULTS)
            .forEach(u -> {
                if (u.has(TITLE)) {
                    assertEquals(u.get(TITLE).asText(), OLD_VALUE);
                } else {
                    assertEquals(u.get(TITLE_).get(FR).asText(), OLD_VALUE);
                }
                assertThat(u.get(VitamFieldsHelper.version()).asInt()).isGreaterThan(1);
                assertThat(u.get(VitamFieldsHelper.operations()).iterator())
                    .extracting(JsonNode::asText)
                    .doesNotContain(containerName);
            });

        List<JsonNode> reportLines = VitamTestHelper.getReports(containerName);
        assertThat(reportLines).isNotNull();
        assertThat(reportLines.size()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void given_not_last_operation_and_force_is_true_then_should_run_with_OK() throws Exception {
        populateData(CASE2_UNITS_FILE, CASE2_LOGBOOK_LIFECYCLE_UNIT_FILE);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        final String containerName = operationGuid.getId();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        createLogbookOperation(operationGuid);

        workspaceClient.createContainer(containerName);
        workspaceClient.putObject(
            containerName,
            OPTIONS_FILE,
            PropertiesUtils.getResourceAsStream(FORCE_OPTIONS_JSON_FILE)
        );
        workspaceClient.putObject(
            containerName,
            OperationContextMonitor.OperationContextFileName,
            writeToInpustream(
                OperationContextModel.get(
                    JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FORCE_OPTIONS_JSON_FILE))
                )
            )
        );

        OperationContextMonitor.compressInWorkspace(
            WorkspaceClientFactory.getInstance(),
            containerName,
            Contexts.REVERT_ESSENTIAL_METADATA.getLogbookTypeProcess(),
            OperationContextMonitor.OperationContextFileName
        );

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.REVERT_ESSENTIAL_METADATA.name());
        processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);

        waitOperation(containerName);

        VitamTestHelper.verifyOperation(containerName, StatusCode.OK);

        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        Select select = new Select();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), containerName));
        select.setProjection(
            createObjectNode()
                .set(
                    BuilderToken.PROJECTION.FIELDS.exactToken(),
                    createObjectNode()
                        .put(VitamFieldsHelper.id(), 1)
                        .put(VitamFieldsHelper.version(), 1)
                        .put(TITLE, 1)
                        .put(TITLE_FR, 1)
                        .put(VitamFieldsHelper.operations(), 1)
                )
        );

        JsonNode unitByIdRaw = metadataClient.selectUnits(select.getFinalSelect());

        unitByIdRaw
            .get(RequestResponseOK.TAG_RESULTS)
            .forEach(u -> {
                if (u.has(TITLE)) {
                    assertNotEquals(u.get(TITLE).asText(), OLD_VALUE);
                } else {
                    assertNotEquals(u.get(TITLE_).get(FR).asText(), OLD_VALUE);
                }
                assertThat(u.get(VitamFieldsHelper.version()).asInt()).isGreaterThan(1);
                assertThat(u.get(VitamFieldsHelper.operations()).iterator())
                    .extracting(JsonNode::asText)
                    .contains(containerName);
            });

        List<JsonNode> reportLines = VitamTestHelper.getReports(containerName);
        assertThat(reportLines).isNotNull();
        assertThat(reportLines.size()).isEqualTo(3);
        assertThat(reportLines.get(1).get("vitamResults").get("OK").asInt()).isEqualTo(12);
        assertThat(reportLines.get(1).get("vitamResults").get("WARNING").asInt()).isEqualTo(0);
        assertThat(reportLines.get(1).get("vitamResults").get("KO").asInt()).isEqualTo(0);
    }

    @After
    public void tearDown() {
        runAfterMongo(Set.of(MetadataCollections.UNIT.getName(), LogbookCollections.LIFECYCLE_UNIT.getName()));
        runAfterEs(ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), TENANT_0));
    }

    private void createLogbookOperation(GUID operationId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            Contexts.REVERT_ESSENTIAL_METADATA.getEventType(),
            operationId,
            LogbookTypeProcess.MASS_UPDATE,
            STARTED,
            VitamLogbookMessages.getCodeOp(Contexts.REVERT_ESSENTIAL_METADATA.getEventType(), STARTED),
            operationId
        );
        logbookClient.create(initParameters);
    }
}
