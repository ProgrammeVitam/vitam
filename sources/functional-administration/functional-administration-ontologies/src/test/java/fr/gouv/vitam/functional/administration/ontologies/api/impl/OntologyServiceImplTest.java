/**
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

package fr.gouv.vitam.functional.administration.ontologies.api.impl;

import com.fasterxml.jackson.core.type.TypeReference;
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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.guid.GUIDFactory.newRequestIdGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OntologyServiceImplTest {
    private final TypeReference<List<OntologyModel>> listOfOntologyType = new TypeReference<List<OntologyModel>>() {};

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 2;
    private static final Integer EXTERNAL_TENANT = 3;
    public static final Integer ADMIN_TENANT = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "Ontology";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;
    static Map<Integer, List<String>> externalIdentifiers;

    static OntologyServiceImpl ontologyService;
    static FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    static int mongoPort;

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
        VitamConfiguration.setAdminTenant(ADMIN_TENANT);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));

        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        final List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        tenants.add(new Integer(EXTERNAL_TENANT));
        tenants.add(new Integer(ADMIN_TENANT));
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("PROFILE");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);

        LogbookOperationsClientFactory.changeMode(null);

        ontologyService =
            new OntologyServiceImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
                vitamCounterService, functionalBackupService);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        ontologyService.close();
    }

    @Before
    public void setUp() throws Exception {
        String operationId = newRequestIdGUID(TENANT_ID).toString();

        VitamThreadUtils.getVitamSession().setRequestId(operationId);
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
        reset(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<OntologyModel> ontologyModelList =
            ontologyService.findOntologies(JsonHandler.createObjectNode());
        assertThat(ontologyModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllForCacheThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<OntologyModel> ontologyModelList =
            ontologyService.findOntologiesForCache(JsonHandler.createObjectNode());
        assertThat(ontologyModelList.getResults()).isEmpty();
    }


    @Test
    @RunWithCustomExecutor
    public void givenWellFormedOntologyMetadataThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        verify(functionalBackupService, times(1))
            .saveCollectionAndSequence(any(), eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT), eq(
                FunctionalAdminCollections.ONTOLOGY), any());
    }

    @Test
    @RunWithCustomExecutor
    public void givenDuplicateIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_duplicate_identifier.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenIdentifierWithWhiteSpaceThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("KO_ontology_vocExt_WithBlank.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response).isInstanceOf(VitamError.class);
    }


    @Test
    @RunWithCustomExecutor
    public void givenDuplicateIdentifiersInDbThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_update_identifiers_ko.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }


    @Test
    @RunWithCustomExecutor
    public void givenNoTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_no_type.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoOriginThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_no_origin.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
    }



    @Test
    @RunWithCustomExecutor
    public void givenInvalidIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_invalid_identifiers.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
        assertThat(ontologyModelList.size()).isEqualTo(5);
    }


    @Test
    @RunWithCustomExecutor
    public void givenSedaFieldEqualsIdentifiersInDbThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        final File fileOntology2 =
            PropertiesUtils.getResourceFile("ontology_Ko_identifier_equals_sedafield_in_DB.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSedaFieldEqualsIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        final File fileOntology = PropertiesUtils.getResourceFile("ontology_Ko_identifier_equals_sedafield.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);

        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTypeInternalNoAdminTenantThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }


    @Test
    @RunWithCustomExecutor
    public void givenUpdateCreateDeleteThenOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_update_ok.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateWrongTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_ko_update_wrong_type.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }



    @Test
    @RunWithCustomExecutor
    public void givenUpdateDoubleTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateLongTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();


        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology3, listOfOntologyType);

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isFalse();
    }



    @Test
    @RunWithCustomExecutor
    public void givenUpdateBooleanTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_boolean.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();


        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology3, listOfOntologyType);

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isFalse();
    }


    @Test
    @RunWithCustomExecutor
    public void givenUpdateTextType() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();


        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology3, listOfOntologyType);

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateKeywordType() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();


        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList3 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology3, listOfOntologyType);

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
        verify(functionalBackupService, times(2)).saveCollectionAndSequence(any(), eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT), eq(
            FunctionalAdminCollections.ONTOLOGY), any());
    }



    @Test
    @RunWithCustomExecutor
    public void givenUpdateWithMissingPropertyOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList =
            JsonHandler.getFromFileAsTypeRefence(fileOntology, listOfOntologyType);
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_empty_property_ok.json");
        final List<OntologyModel> ontologyModelList2 =
            JsonHandler.getFromFileAsTypeRefence(fileOntology2, listOfOntologyType);

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
        verify(functionalBackupService, times(2)).saveCollectionAndSequence(any(), eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT), eq(
            FunctionalAdminCollections.ONTOLOGY), any());
    }

}
