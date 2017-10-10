/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.functional.administration.agencies.api;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.exception.AgencyImportDeletionException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.FilesSecurisator;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.mockito.Matchers;


public class AgenciesServiceTest {

    private static VitamCounterService vitamCounterService;
    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Integer TENANT_ID = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "Agency";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static MongoDbAccessAdminImpl dbImpl;
    private static ElasticsearchTestConfiguration esConfig = null;
    private final static String HOST_NAME = "127.0.0.1";
    private final static String CLUSTER_NAME = "vitam-cluster";


    static AgenciesService agencyService;
    private static FilesSecurisator securisator;

    private static LogbookOperationsClientFactory logbookOperationsClientFactory;

    private static WorkspaceClientFactory workspaceClientFactory;
    private static StorageClientFactory storageClientFactory;

    static int mongoPort;

    @RunWithCustomExecutor
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
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
        try {
            esConfig = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        securisator = mock(FilesSecurisator.class);
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));
        final List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        vitamCounterService = new VitamCounterService(dbImpl, tenants, new HashMap<>());

        LogbookOperationsClientFactory.changeMode(null);
        FilesSecurisator securisator = mock(FilesSecurisator.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        storageClientFactory = mock(StorageClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        securisator = mock(FilesSecurisator.class);
        agencyService = new AgenciesService(dbImpl, vitamCounterService, securisator, logbookOperationsClientFactory,
            storageClientFactory, workspaceClientFactory);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        agencyService.close();
    }

    @After
    public void afterTest() throws ReferentialException {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAgencyImportTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        JsonNode contractToPersist = JsonHandler.getFromString(contract);

        dbImpl.insertDocument(contractToPersist, FunctionalAdminCollections.ACCESS_CONTRACT).close();

        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(StatusCode.OK.name(), TENANT_ID));

        agencyService = new AgenciesService(dbImpl, vitamCounterService, securisator, logbookOperationsClientFactory,
            storageClientFactory, workspaceClientFactory);

        File fileAgencies = getResourceFile("agencies.csv");

        RequestResponse<AgenciesModel> response = agencyService.importAgencies(new FileInputStream(fileAgencies));
        assertThat(response.isOk()).isTrue();
        fileAgencies = getResourceFile("agencies2.csv");

        agencyService = new AgenciesService(dbImpl, vitamCounterService, securisator, logbookOperationsClientFactory,
            storageClientFactory, workspaceClientFactory);

        response = agencyService.importAgencies(new FileInputStream(fileAgencies));
        assertThat(response.isOk()).isTrue();

        Agencies doc = (Agencies) agencyService.findDocumentById("AG-000001");

        assertThat(doc.getName()).isEqualTo("agency222");
        fileAgencies = getResourceFile("agencies_delete.csv");
        response = agencyService.importAgencies(new FileInputStream(fileAgencies));
        assertThat(response.isOk()).isFalse();



    }



    @Test
    @RunWithCustomExecutor
    public void givenFileTestChekFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(StatusCode.OK.name(), TENANT_ID));

        agencyService = new AgenciesService(dbImpl, vitamCounterService, securisator, logbookOperationsClientFactory,
            storageClientFactory, workspaceClientFactory);
        File file = getResourceFile("agencies_delete.csv");
        agencyService.checkFile(new FileInputStream(file));
    }

    static String contract = "{ \"_tenant\": 1,\n" +
        "    \"Name\": \"contract_with_field_EveryDataObjectVersion\",\n" +
        "    \"Identifier\": \"AC-000018\",\n" +
        "    \"Description\": \"aDescription of the contract\",\n" +
        "    \"Status\": \"ACTIVE\",\n" +
        "    \"CreationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"LastUpdate\": \"2017-10-06T01:53:22.544\",\n" +
        "    \"ActivationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"DeactivationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"DataObjectVersion\": [],\n" +
        "    \"OriginatingAgencies\": [\n" +
        "        \"FRAN_NP_005568\",\n" +
        "        \"AG-000001\"\n" +
        "    ],\n" +
        "    \"WritingPermission\": true,\n" +
        "    \"EveryOriginatingAgency\": true,\n" +
        "    \"EveryDataObjectVersion\": false,\n" +
        "    \"_v\": 0\n" +
        "}";
    private JsonNode getJsonResult(String outcome, int tenantId) throws Exception {
        return JsonHandler.getFromString(String.format("{\n" +
            "     \"httpCode\": 200,\n" +
            "     \"$hits\": {\n" +
            "          \"total\": 1,\n" +
            "          \"offset\": 0,\n" +
            "          \"limit\": 1,\n" +
            "          \"size\": 1\n" +
            "     },\n" +
            "     \"$results\": [\n" +
            "          {\n" +
            "               \"_id\": \"aecaaaaaacgbcaacaa76eak44s3of6iaaaaq\",\n" +
            "               \"events\": [\n" +
            "                    {\n" +
            "                         \"outcome\": \"%s\"\n" +
            "                    }\n" +
            "               ],\n" +
            "               \"_v\": 0,\n" +
            "               \"_tenant\": %d\n" +
            "          }\n" +
            "     ],\n" +
            "     \"$context\": {\n" +
            "          \"$query\": {\n" +
            "               \"$eq\": {\n" +
            "                    \"events.evType\": \"STP_IMPORT_AGENCIES\"\n" +
            "               }\n" +
            "          },\n" +
            "          \"$filter\": {\n" +
            "               \"$limit\": 1,\n" +
            "               \"$orderby\": {\n" +
            "                    \"evDateTime\": -1\n" +
            "               }\n" +
            "          },\n" +
            "          \"$projection\": {\n" +
            "               \"$fields\": {\n" +
            "                    \"#id\": 1,\n" +
            "                    \"events.outcome\": 1\n" +
            "               }\n" +
            "          }\n" +
            "     }\n" +
            "}", outcome, tenantId));
    }
}
