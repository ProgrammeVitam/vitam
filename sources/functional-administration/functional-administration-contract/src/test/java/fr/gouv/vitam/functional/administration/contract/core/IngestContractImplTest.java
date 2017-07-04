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
package fr.gouv.vitam.functional.administration.contract.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.model.ProfileModel;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;

import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import org.bson.Document;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class IngestContractImplTest {


    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
            VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "IngestContract";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    static VitamCounterService vitamCounterService;

    static ContractService<IngestContractModel> ingestContractService;
    static int mongoPort;
    private static MongoDbAccessAdminImpl dbImpl;


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

        ingestContractService =
                new IngestContractImpl(dbImpl, vitamCounterService);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        ingestContractService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk());
        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("IC-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("IC-000");


    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_missingName.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileNotInDBReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_not_indb.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void testObjectNode() throws InvalidParseOperationException {
        final ArrayNode object = JsonHandler.createArrayNode();
        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put("Status", "update");
        msg.put("oldStatus", "INACTIF");
        msg.put("newStatus", "ACTIF");
        final ObjectNode msg2 = JsonHandler.createObjectNode();
        msg2.put("FilingParentId", "update");
        msg2.put("oldFilingParentId", "lqskdfjh");
        msg2.put("newFilingParentId", "lqskdfjh");
        object.add(msg);
        object.add(msg2);
        String wellFormedJson = SanityChecker.sanitizeJson(object);
        System.out.println("YOOOOOOOOOOOOOOOOOOOOOOOOOOO" + wellFormedJson);
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileInDBReturnOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        List<ProfileModel> profileModelList =
                JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {
                });

        dbImpl.insertDocuments(JsonHandler.createArrayNode().add(JsonHandler.toJsonNode(profileModelList.iterator().next())), FunctionalAdminCollections.PROFILE);

        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_indb.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk());
        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_duplicate.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try to recreate the same contract but with id
        response = ingestContractService.createContracts(responseCast.getResults());

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestAlreadyExistsContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        // unset ids
        IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find ingestContract with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }"; JsonNode queryDsl =
         * JsonHandler.getFromString(q);
         */
        List<IngestContractModel> IngestContractModelList = ingestContractService.findContracts(queryDsl);

        assertThat(IngestContractModelList).isEmpty();
    }


    /**
     * Check that the created ingest conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        IngestContractModel one = ingestContractService.findOne(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(Long.valueOf(TENANT_ID));

    }


    /**
     * Ingest contract of tenant 1, try to get the same contract with id mongo but with tenant 2 This sgould not return
     * the contract as tenant 2 is not the owner of the Ingest contract
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();


        VitamThreadUtils.getVitamSession().setTenantId(2);

        final IngestContractModel one = ingestContractService.findOne(id1);

        assertThat(one).isNull();

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestfindByIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        IngestContractModel one = ingestContractService.findOne(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<IngestContractModel> IngestContractModelList =
                ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelList).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        List<IngestContractModel> IngestContractModelListSearch =
                ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        final String inactiveStatus = "INACTIVE";
        final String activeStatus = "ACTIVE";
        // Create document
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        List<IngestContractModel> ingestModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();
        ingestModelList = ingestContractService.findContracts(queryDsl);
        assertThat(ingestModelList).isNotEmpty();
        for (IngestContractModel ingestContractModel : ingestModelList) {
            assertThat(activeStatus.equals(ingestContractModel.getStatus()));
        }

        // Test update for access contract Status => inactive
        String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new VarNameAdapter());
        SetAction setActionStatusInactive = UpdateActionHelper.set("Status", inactiveStatus);
        SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
        Update update = new Update();
        update.setQuery(QueryHelper.eq("Name", documentName));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
                ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        List<IngestContractModel> ingestContractModelListForassert =
                ingestContractService.findContracts(queryDsl);
        assertThat(ingestContractModelListForassert).isNotEmpty();
        for (IngestContractModel ingestContractModel : ingestContractModelListForassert) {
            assertThat(inactiveStatus.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(activeStatus.equals(ingestContractModel.getStatus())).isFalse();
            assertThat(ingestContractModel.getDeactivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
        }

        // Test update for access contract Status => Active
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new VarNameAdapter());
        SetAction setActionStatusActive = UpdateActionHelper.set("Status", activeStatus);
        SetAction setActionDesactivationDateActive = UpdateActionHelper.set("ActivationDate", now);
        SetAction setActionLastUpdateActive = UpdateActionHelper.set("LastUpdate", now);
        Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq("Name", documentName));
        updateStatusActive.addActions(setActionStatusActive, setActionDesactivationDateActive,
                setActionLastUpdateActive);
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();
        ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);


        List<IngestContractModel> accessContractModelListForassert2 =
                ingestContractService.findContracts(queryDsl);
        assertThat(accessContractModelListForassert2).isNotEmpty();
        for (IngestContractModel ingestContractModel : accessContractModelListForassert2) {
            assertThat(inactiveStatus.equals(ingestContractModel.getStatus())).isFalse();
            assertThat(activeStatus.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(ingestContractModel.getActivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
        }
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });
        RequestResponse response = ingestContractService.createContracts(IngestContractModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        IngestContractModel acm = IngestContractModelList.iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getId();
        assertThat(id1).isNotNull();

        String name = acm.getName();
        assertThat(name).isNotNull();


        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        JsonNode queryDsl = parser.getRequest().getFinalSelect();


        List<IngestContractModel> IngestContractModelListFound = ingestContractService.findContracts(queryDsl);
        assertThat(IngestContractModelListFound).hasSize(1);

        IngestContractModel acmFound = IngestContractModelListFound.iterator().next();
        assertThat(acmFound).isNotNull();


        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getName()).isEqualTo(name);

    }

}
