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
package fr.gouv.vitam.functional.administration.test;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.error.VitamError;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.restassured.RestAssured;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;
import fr.gouv.vitam.functional.administration.profile.api.ProfileService;
import fr.gouv.vitam.functional.administration.profile.api.impl.ProfileServiceImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesManagerFileImpl;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;

/**
 * !!! WARNING !!! : in case of modification of class fr.gouv.vitam.driver.fake.FakeDriverImpl, you need to recompile
 * the storage-offer-mock.jar from the storage-offer-mock module and copy it in src/test/resources in place of the
 * previous one.
 *
 *
 */
public class FunctionalAdminIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FunctionalAdminIT.class);
    public static final String CONFIG_WORKSPACE_PATH = "functional-admin/workspace.conf";
    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    private static String TMP_FOLDER;

    private static final Integer TENANT_ID = 1;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    final static String CLUSTER_NAME = "vitam-cluster";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    static int mongoPort;
    static ElasticsearchTestConfiguration config;

    private static final String REST_URI = StorageClientFactory.RESOURCE_PATH;
    private static final String LOGBOOK_REST_URI = LogbookOperationsClientFactory.RESOURCE_PATH;

    private static final String STORAGE_CONF = "functional-admin/storage-engine.conf";
    private static final String LOGBOOK_CONF = "functional-admin/logbook.conf";

    private static int storagePort;
    private static int workspacePort;
    private static int logbookPort;

    private static StorageMain storageMain;
    private static LogbookMain logbookMain;
    private static WorkspaceMain workspaceMain;

    private static VitamCounterService vitamCounterService;
    private static ProfileService profileService;
    private static RulesManagerFileImpl rulesManagerFile;
    private static ReferentialFormatFileImpl referentialFormatFile;
    private static IngestContractImpl ingestContract;
    private static MongoDbAccessAdminImpl dbImpl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();
        File tmpFolder = temporaryFolder.newFolder();
        TMP_FOLDER = tmpFolder.getAbsolutePath();
        SystemPropertyUtil.set("vitam.tmp.folder", TMP_FOLDER);

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));
        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        VitamConfiguration.setTenants(tenants);
        VitamConfiguration.setAdminTenant(1);
        vitamCounterService = new VitamCounterService(dbImpl, tenants, null);

        // ES
        ElasticsearchTestConfiguration config =
            JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME);

        // launch workspace
        File workspaceConfigurationFile = PropertiesUtils.findFile(CONFIG_WORKSPACE_PATH);
        final fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigurationFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(TMP_FOLDER);
        PropertiesUtils.writeYaml(workspaceConfigurationFile, workspaceConfiguration);

        workspacePort = junitHelper.findAvailablePort();
        try {
            SystemPropertyUtil.set("jetty.port", workspacePort);
            workspaceMain = new WorkspaceMain(CONFIG_WORKSPACE_PATH);
            workspaceMain.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start Workspace Server", e);
        }

        // Prepare storage
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(storageConfigurationFile, StorageConfiguration.class);
        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String seg[] = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration
            .setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort));
        serverConfiguration.setZippingDirecorty(TMP_FOLDER);
        serverConfiguration.setLoggingDirectory(TMP_FOLDER);
        storagePort = junitHelper.findAvailablePort();
        PropertiesUtils.writeYaml(storageConfigurationFile, serverConfiguration);
        SystemPropertyUtil.set("jetty.port", storagePort);
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();

        // Prepare logbook
        File logbookConfigurationFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookServerConfiguration =
            PropertiesUtils.readYaml(logbookConfigurationFile, LogbookConfiguration.class);
        logbookServerConfiguration.getMongoDbNodes().get(0).setDbPort(mongoPort);
        logbookServerConfiguration.setWorkspaceUrl("http://localhost:" + Integer.toString(workspacePort));
        logbookServerConfiguration.getElasticsearchNodes().get(0).setTcpPort(config.getTcpPort());
        logbookPort = junitHelper.findAvailablePort();
        PropertiesUtils.writeYaml(logbookConfigurationFile, logbookServerConfiguration);
        SystemPropertyUtil.set("jetty.logbook.port", logbookPort);
        logbookMain = new LogbookMain(LOGBOOK_CONF);
        logbookMain.start();


        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", logbookPort));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", logbookPort));

        FunctionalBackupService functionalBackupService = new FunctionalBackupService(vitamCounterService);

        profileService =
            new ProfileServiceImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
                vitamCounterService, functionalBackupService);

        rulesManagerFile =
            new RulesManagerFileImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
                vitamCounterService);

        referentialFormatFile = new ReferentialFormatFileImpl(
            MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)), vitamCounterService);

        ingestContract = new IngestContractImpl(
            MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
            vitamCounterService);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        if(logbookMain != null) {
            logbookMain.stop();
        }
        if(workspaceMain != null) {
            workspaceMain.stop();
        }
        if(storageMain != null) {
            storageMain.stop();
        }
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        if(mongod != null) {
            mongod.stop();
        }
        if(mongodExecutable != null) {
            mongodExecutable.stop();
        }
        junitHelper.releasePort(mongoPort);
        junitHelper.releasePort(storagePort);
        junitHelper.releasePort(workspacePort);
        junitHelper.releasePort(logbookPort);
        if(client != null) {
            client.close();
        }
        if(profileService != null) {
            profileService.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

    }

    @Test
    @RunWithCustomExecutor
    public final void testUploadDownloadProfileFile() throws Exception {
        RestAssured.port = storagePort;
        RestAssured.basePath = REST_URI;
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileMetadataProfile = PropertiesUtils.getResourceFile("functional-admin/profile_ok.json");
        List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        RequestResponse response = profileService.createProfiles(profileModelList);
        assertThat(response.isOk()).isTrue();
        RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        final ProfileModel profileModel = responseCast.getResults().iterator().next();
        InputStream xsdProfile =
            new FileInputStream(PropertiesUtils.getResourceFile("functional-admin/profile_ok.xsd"));
        RequestResponse requestResponse = profileService.importProfileFile(profileModel.getIdentifier(), xsdProfile);
        assertThat(requestResponse.isOk()).isTrue();
        javax.ws.rs.core.Response responseDown = profileService.downloadProfileFile(profileModel.getIdentifier());
        assertThat(responseDown.hasEntity()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public final void testImportRulesFile() throws Exception {
        RestAssured.port = logbookPort;
        RestAssured.basePath = LOGBOOK_REST_URI;

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        VitamThreadUtils.getVitamSession().setRequestId(eipId);

        InputStream rulesFileStream =
            new FileInputStream(
                PropertiesUtils.getResourceFile("functional-admin/jeu_donnees_OK_regles_CSV_regles.csv"));
        rulesManagerFile.importFile(rulesFileStream, "jeu_donnees_OK_regles_CSV_regles.csv");

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        final Select select = new Select();
        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "STP_IMPORT_RULES"));
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("evDateTime");
        JsonNode result = logbookClient.selectOperation(select.getFinalSelect());
        assertThat(result).isNotNull();
        JsonNode operation = ((ArrayNode) result.get("$results")).get(0);
        assertThat(operation).isNotNull();
        JsonNode lastEvent =
            ((ArrayNode) operation.get("events")).get(((ArrayNode) operation.get("events")).size() - 1);
        assertThat(lastEvent).isNotNull();
        assertThat(lastEvent.has("evDetData")).isTrue();
        JsonNode evDetData = JsonHandler.getFromString(lastEvent.get("evDetData").asText());
        assertThat(evDetData).isNotNull();
        assertThat(evDetData.get("FileName")).isNotNull();
        assertThat(evDetData.get("FileName").asText()).isEqualTo("jeu_donnees_OK_regles_CSV_regles.csv");
    }

    @Test
    @RunWithCustomExecutor
    public final void testImportFormatsFile() throws Exception {
        RestAssured.port = logbookPort;
        RestAssured.basePath = LOGBOOK_REST_URI;

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream rulesFileStream =
            new FileInputStream(
                PropertiesUtils.getResourceFile("functional-admin/DROID_SignatureFile_V88.xml"));
        referentialFormatFile.importFile(rulesFileStream, "DROID_SignatureFile_V88.xml");

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        final Select select = new Select();
        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "STP_REFERENTIAL_FORMAT_IMPORT"));
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("evDateTime");
        JsonNode result = logbookClient.selectOperation(select.getFinalSelect());
        assertThat(result).isNotNull();
        JsonNode operation = ((ArrayNode) result.get("$results")).get(0);
        assertThat(operation).isNotNull();
        JsonNode lastEvent =
            ((ArrayNode) operation.get("events")).get(((ArrayNode) operation.get("events")).size() - 1);
        assertThat(lastEvent).isNotNull();
        assertThat(lastEvent.has("evDetData")).isTrue();
        JsonNode evDetData = JsonHandler.getFromString(lastEvent.get("evDetData").asText());
        assertThat(evDetData).isNotNull();
        assertThat(evDetData.get("FileName")).isNotNull();
        assertThat(evDetData.get("FileName").asText()).isEqualTo("DROID_SignatureFile_V88.xml");
    }

    @Test
    @RunWithCustomExecutor
    public final void testImportAndUpdateIngestContract() throws Exception {
        RestAssured.port = logbookPort;
        RestAssured.basePath = LOGBOOK_REST_URI;

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // do the import
        File fileContracts =
            PropertiesUtils.getResourceFile("integration-ingest-internal/referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
            new TypeReference<List<IngestContractModel>>() {});
        ingestContract.createContracts(IngestContractModelList);

        // check import log
        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        final Select select = new Select();
        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "STP_IMPORT_INGEST_CONTRACT"));
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("evDateTime");
        JsonNode result = logbookClient.selectOperation(select.getFinalSelect());
        assertThat(result).isNotNull();
        JsonNode operation = ((ArrayNode) result.get("$results")).get(0);
        assertThat(operation).isNotNull();
        JsonNode lastEvent =
            ((ArrayNode) operation.get("events")).get(((ArrayNode) operation.get("events")).size() - 1);
        assertThat(lastEvent).isNotNull();
        assertThat(lastEvent.has("outcome")).isTrue();
        assertThat(lastEvent.get("outcome").asText()).isEqualTo("OK");

        // check created contract
        final Select select2 = new Select();
        select2.setQuery(exists("Name"));
        List<IngestContractModel> contractModels = ingestContract.findContracts(select2.getFinalSelect()).getResults();
        assertThat(contractModels).isNotEmpty();
        IngestContractModel contractModel = contractModels.get(0);
        assertThat(contractModel).isNotNull();
        assertThat(contractModel.getStatus().equals("ACTIVE"));
        assertThat(contractModel.getCheckParentLink().equals("INACTIVE"));
        String contractToUpdate = contractModel.getIdentifier();
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        // do an update
        UpdateMultiQuery updateQuery = new UpdateMultiQuery();
        updateQuery.addActions(new SetAction("Status", "INACTIVE"));
        ingestContract.updateContract(contractToUpdate, updateQuery.getFinalUpdate());

        // check update
        IngestContractModel updatedContractModel = ingestContract.findByIdentifier(contractToUpdate);
        assertThat(updatedContractModel).isNotNull();
        assertThat(updatedContractModel.getStatus().equals(ActivationStatus.INACTIVE)).isTrue();

        // check update log
        select.setQuery(new CompareQuery(QUERY.EQ, "evType", "STP_UPDATE_INGEST_CONTRACT"));
        result = logbookClient.selectOperation(select.getFinalSelect());
        assertThat(result).isNotNull();
        operation = ((ArrayNode) result.get("$results")).get(0);
        assertThat(operation).isNotNull();
        lastEvent = ((ArrayNode) operation.get("events")).get(((ArrayNode) operation.get("events")).size() - 1);
        assertThat(lastEvent).isNotNull();
        assertThat(lastEvent.has("outcome")).isTrue();
        assertThat(lastEvent.get("outcome").asText()).isEqualTo("OK");
        assertThat(lastEvent.has("evDetData")).isTrue();
        JsonNode evDetData = JsonHandler.getFromString(lastEvent.get("evDetData").asText());
        assertThat(evDetData).isNotNull();
        assertThat(evDetData.get("IngestContract")).isNotNull();
        assertThat(evDetData.get("IngestContract").get("updatedDiffs")).isNotNull();
        assertThat(evDetData.get("IngestContract").get("updatedDiffs").asText())
            .contains("-  \"Status\" : \"ACTIVE\"+  \"Status\" : \"INACTIVE\"");

        updateQuery = new UpdateMultiQuery();
        updateQuery.addActions(new SetAction("Status", "INACTIVE"));
        RequestResponse response = ingestContract.updateContract("wrongId", updateQuery.getFinalUpdate());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getHttpCode());
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        updateQuery = new UpdateMultiQuery();
        updateQuery.addActions(new SetAction("LinkParentId", "invalid_id"));
        RequestResponse response2 = ingestContract.updateContract(contractToUpdate, updateQuery.getFinalUpdate());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response2.getHttpCode());
        assertThat(response2).isInstanceOf(VitamError.class);
        final VitamError vitamError2 = (VitamError) response2;
        assertThat(vitamError2.toString()).contains("is not in filing nor holding schema");
    }
}
