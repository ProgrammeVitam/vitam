/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */
package fr.gouv.vitam.functionnal.administration.context.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.PushAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.ContextModel;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.context.api.ContextService;
import fr.gouv.vitam.functional.administration.context.core.ContextServiceImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;

public class ContextServiceImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 0;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "Context";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;

    static ContextService contextService;
    static ContractService ingestContractService;
    static ContractService accessContractService;
    static int mongoPort;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
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
        vitamCounterService = new VitamCounterService(dbImpl, tenants);

        contextService =
            new ContextServiceImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)), vitamCounterService);

        ingestContractService = new IngestContractImpl(dbImpl, vitamCounterService);
        accessContractService = new AccessContractImpl(dbImpl, vitamCounterService);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        contextService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }
    
    @Test
    @RunWithCustomExecutor
    public void givenTestWellFormedContextThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        
        File fileIngest = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileIngest, new TypeReference<List<IngestContractModel>>() {});
        ingestContractService.createContracts(IngestContractModelList);
        
        File fileAccess = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        List<AccessContractModel> accessContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileAccess, new TypeReference<List<AccessContractModel>>() {});
        accessContractService.createContracts(accessContractModelList);
        
        File fileContexts = PropertiesUtils.getResourceFile("contexts_ok.json");
        List<ContextModel> ModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {});
        
        RequestResponse response = contextService.createContexts(ModelList);

        assertThat(response.isOk());
        RequestResponseOK<ContextModel> responseCast = (RequestResponseOK<ContextModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
    }
    
    @Test
    @RunWithCustomExecutor
    public void givenContextUpdateTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileIngest = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeRefence(fileIngest, new TypeReference<List<IngestContractModel>>() {});
        ingestContractService.createContracts(IngestContractModelList);
        ingestContractService.findContracts(new Select().getFinalSelect());
        File fileContexts = PropertiesUtils.getResourceFile("contexts_empty.json");
        List<ContextModel> ModelList =
            JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {});
        
        RequestResponse response = contextService.createContexts(ModelList);
        
        PushAction addIdentifierAction = UpdateActionHelper.push("Permissions.0.AccessContracts", "AC-000011");
        Select select = new Select();
        select.setQuery(QueryHelper.eq("Name", "My_Context_1"));
        ContextModel context = contextService.findContexts(select.getFinalSelect()).get(0);
        
        
        Update update = new Update();
        update.addActions(addIdentifierAction);
        update.setQuery(QueryHelper.and().add(QueryHelper.eq("Permissions._tenant", 0))
            .add(QueryHelper.eq("#id", context.getId())));
        update.getActions().get(0).getCurrentAction();

        JsonNode queryDslForUpdate = update.getFinalUpdate();
        contextService.updateContext(context.getIdentifier(), queryDslForUpdate);

       
        queryDslForUpdate = update.getFinalUpdate();     
        
    }

}
