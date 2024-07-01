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
package fr.gouv.vitam.functional.administration.core.ontologies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newRequestIdGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OntologyServiceImplTest {

    private static final Integer TENANT_ID = 2;
    private static final Integer ADMIN_TENANT = 1;
    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final String DATABASE_HOST = "localhost";
    private static final FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(Ontology.class));

    private static OntologyServiceImpl ontologyService;
    private final TypeReference<List<OntologyModel>> listOfOntologyType = new TypeReference<>() {};

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
                indexManager
            ),
            Collections.singletonList(FunctionalAdminCollections.ONTOLOGY)
        );

        VitamConfiguration.setAdminTenant(ADMIN_TENANT);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, MongoRule.getDataBasePort()));

        LogbookOperationsClientFactory.changeMode(null);

        ontologyService = new OntologyServiceImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            functionalBackupService
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    @Before
    public void setUp() {
        String operationId = newRequestIdGUID(TENANT_ID).toString();

        VitamThreadUtils.getVitamSession().setRequestId(operationId);
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest();
        reset(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<OntologyModel> ontologyModelList = ontologyService.findOntologies(
            JsonHandler.createObjectNode()
        );
        assertThat(ontologyModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllForCacheThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<OntologyModel> ontologyModelList = ontologyService.findOntologiesForCache(
            JsonHandler.createObjectNode()
        );
        assertThat(ontologyModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenWellFormedOntologyMetadataThenImportOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(false, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenWellFormedOntologyMetadataThenForcedImportOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testSimulateUpgradeOntologyThenOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("upgrade/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        final RequestResponseOK<OntologyModel> responseCast1 = (RequestResponseOK<OntologyModel>) response;
        List<OntologyModel> results1 = responseCast1.getResults();
        List<Ontology> actualExternals = getExternalOntologies();

        // Then
        assertThat(response.isOk()).isTrue();
        assertThat(results1).hasSize(6);
        assertThat(actualExternals).hasSize(3);

        JsonNode expected = JsonHandler.toJsonNode(actualExternals);

        // import an upgraded version of ontology , this is the most probable OK Scenarios
        final File fileOntology2 = PropertiesUtils.getResourceFile("upgrade/ontology_new_version.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        // When
        final RequestResponse<OntologyModel> response2 = ontologyService.importInternalOntologies(ontologyModelList2);
        final RequestResponseOK<OntologyModel> responseCast2 = (RequestResponseOK<OntologyModel>) response2;
        List<OntologyModel> results2 = responseCast2.getResults();
        List<Ontology> afterUpgrade = getExternalOntologies();

        // Then
        assertThat(response2.isOk()).isTrue();
        assertThat(results2).hasSize(4);
        assertThat(afterUpgrade).hasSize(3);
        verify(functionalBackupService, times(2)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );

        JsonNode actual = JsonHandler.toJsonNode(afterUpgrade);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @RunWithCustomExecutor
    public void testSimulateUpgradeOntologyWhenConflictThenKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("upgrade/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        // import an upgraded version of ontology , this is the most probable OK Scenarios
        final File fileOntology2 = PropertiesUtils.getResourceFile(
            "upgrade/ontology_new_version_conflict_external_with_existing.json"
        );
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );
        final RequestResponse<OntologyModel> response2 = ontologyService.importInternalOntologies(ontologyModelList2);

        // Then
        assertThat(response2.isOk()).isFalse();
        assertThat(((VitamError) response2).getDescription()).contains(
            "Import/upgrade ontologies error : There is conflict between Ontologies being imported and those already exists in database"
        );
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testSimulateUpgradeWithExternalOntologiesInListThenKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("upgrade/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        // Then
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile(
            "upgrade/ontology_new_version_with_externals_included.json"
        );
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        // When
        final RequestResponse<OntologyModel> response2 = ontologyService.importInternalOntologies(ontologyModelList2);

        // Then
        assertThat(response2.isOk()).isFalse();
        assertThat(((VitamError) response2).getDescription()).contains(
            "Import ontologies error : The imported ontologies contains EXTERNAL fields."
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenDuplicateIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_duplicate_identifier.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenIdentifierWithWhiteSpaceThenImportKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("KO_ontology_vocExt_WithBlank.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response).isInstanceOf(VitamError.class);
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_no_type.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoOriginThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_no_origin.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenInvalidIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ko_invalid_identifiers.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);

        assertThat(response.isOk()).isFalse();
        assertThat(ontologyModelList.size()).isEqualTo(5);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSedaFieldEqualsIdentifiersThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        final File fileOntology = PropertiesUtils.getResourceFile("ontology_Ko_identifier_equals_sedafield.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTypeInternalNoAdminTenantThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedDeleteThenOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_update_ok.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenDeleteInternalOntologyThenNonForcedImportKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(false, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_update_ok.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenDeleteExternalOntologyThenNonForcedImportKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_external_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(false, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_external_update_ok.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateWrongTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_ko_update_wrong_type.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateWrongTypeThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_ko_update_wrong_type.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateDoubleTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateDoubleTypeThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateLongToDoubleTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateLongToDoubleTypeThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateLongToKeywordTypeThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(false, ontologyModelList3);
        assertThat(response3.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateLongToKeywordTypeThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_long.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateBooleanToTextThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_boolean.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateBooleanToTextThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_boolean.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateBooleanToKeywordThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_boolean.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(false, ontologyModelList3);
        assertThat(response3.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateBooleanToKeywordThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_boolean.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(true, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateTextToDouble() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateTextToDouble() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateTextToKeywordOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(false, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateKeywordToDoubleKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenForcedUpdateKeywordToDoubleOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_double.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(true, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateKeywordToTextOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_keyword.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();

        final File fileOntology3 = PropertiesUtils.getResourceFile("ontology_text.json");
        final List<OntologyModel> ontologyModelList3 = JsonHandler.getFromFileAsTypeReference(
            fileOntology3,
            listOfOntologyType
        );

        final RequestResponse response3 = ontologyService.importOntologies(false, ontologyModelList3);
        assertThat(response3.isOk()).isTrue();
        verify(functionalBackupService, times(2)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenUpdateWithMissingPropertyOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("ontology_ok.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        assertThat(response.isOk()).isTrue();
        //Insert same file (same identifier= update different identifier=create)
        final File fileOntology2 = PropertiesUtils.getResourceFile("ontology_empty_property_ok.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.importOntologies(false, ontologyModelList2);
        assertThat(response2.isOk()).isTrue();
        verify(functionalBackupService, times(2)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportRightOntology() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        File fileOntology = PropertiesUtils.getResourceFile("ok_ontology.json");
        List<OntologyModel> ontologyModelListOk = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        RequestResponse response = ontologyService.importOntologies(true, ontologyModelListOk);

        // Then
        assertThat(response).isInstanceOf(RequestResponseOK.class);
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldNotImportWrongOntologyWithUnknownCollection() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        File fileOntologyKo = PropertiesUtils.getResourceFile("KO_ontology_unknown_collection.json");
        List<OntologyModel> ontologyModelListKo = JsonHandler.getFromFileAsTypeReference(
            fileOntologyKo,
            listOfOntologyType
        );

        // When
        RequestResponse response = ontologyService.importOntologies(true, ontologyModelListKo);

        // Then
        assertThat(response.toString()).contains("Import ontologies schema error");
        assertThat(response.toString()).contains("instance value (\\\\\\\"BlablaCollection\\\\\\\") not found in enum");

        assertThat(response).isNotInstanceOf(RequestResponseOK.class);
        verify(functionalBackupService, times(0)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkUpgradeOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("upgrade/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        final RequestResponseOK<OntologyModel> responseCast1 = (RequestResponseOK<OntologyModel>) response;
        List<OntologyModel> results1 = responseCast1.getResults();
        List<Ontology> actualExternals = getExternalOntologies();

        assertThat(response.isOk()).isTrue();
        assertThat(results1).hasSize(6);
        assertThat(actualExternals).hasSize(3);

        final File fileOntology2 = PropertiesUtils.getResourceFile("upgrade/ontology_new_version.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.checkUpgradeOntologies(ontologyModelList2);

        // Then
        assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkUpgradeKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("dryrun/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        final RequestResponseOK<OntologyModel> responseCast1 = (RequestResponseOK<OntologyModel>) response;
        List<OntologyModel> results1 = responseCast1.getResults();
        List<Ontology> actualExternals = getExternalOntologies();

        assertThat(response.isOk()).isTrue();
        assertThat(results1).hasSize(6);
        assertThat(actualExternals).hasSize(3);

        final File fileOntology2 = PropertiesUtils.getResourceFile("dryrun/ontology_new_version_dry_run.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.checkUpgradeOntologies(ontologyModelList2);

        // Then
        assertThat(response2.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
        JsonAssert.assertJsonEquals(
            "{\"httpCode\":400,\"code\":\"20\",\"context\":\"FunctionalModule-Ontology\",\"state\":\"KO\",\"message\":\"Ontology service error\",\"description\":\"Check ontology errors : % : The field Type is mandatory,BirthDate : Forbidden field id\"}",
            response2.toString()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkAnotherUpgradeKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileOntology = PropertiesUtils.getResourceFile("dryrun/ontology_in_database.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            fileOntology,
            listOfOntologyType
        );

        // When
        final RequestResponse response = ontologyService.importOntologies(true, ontologyModelList);
        final RequestResponseOK<OntologyModel> responseCast1 = (RequestResponseOK<OntologyModel>) response;
        List<OntologyModel> results1 = responseCast1.getResults();
        List<Ontology> actualExternals = getExternalOntologies();

        assertThat(response.isOk()).isTrue();
        assertThat(results1).hasSize(6);
        assertThat(actualExternals).hasSize(3);

        final File fileOntology2 = PropertiesUtils.getResourceFile("dryrun/ontology_new_version_dry_run_conflict.json");
        final List<OntologyModel> ontologyModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileOntology2,
            listOfOntologyType
        );

        final RequestResponse response2 = ontologyService.checkUpgradeOntologies(ontologyModelList2);

        // Then
        assertThat(response2.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response2.toString()).contains(
            "There is conflict between Ontologies being imported and those already exists in database"
        );
        verify(functionalBackupService, times(1)).saveCollectionAndSequence(
            any(),
            eq(OntologyServiceImpl.BACKUP_ONTOLOGY_EVENT),
            eq(FunctionalAdminCollections.ONTOLOGY),
            any()
        );
    }

    private List<Ontology> getExternalOntologies() throws InvalidParseOperationException {
        final List<Ontology> models = new ArrayList<>();

        FindIterable<Ontology> documents = FunctionalAdminCollections.ONTOLOGY.<Ontology>getCollection()
            .find(Filters.eq(OntologyModel.TAG_ORIGIN, "EXTERNAL"));
        for (Document document : documents) {
            models.add(BsonHelper.fromDocumentToObject(document, Ontology.class));
        }

        return models;
    }
}
