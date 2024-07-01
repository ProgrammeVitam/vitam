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
package fr.gouv.vitam.functional.administration.core.archiveunitprofiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ArchiveUnitProfileServiceImplTest {

    public static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(ArchiveUnitProfile.class)
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    static ArchiveUnitProfileServiceImpl archiveUnitProfileService;
    static FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;
    private static ElasticsearchAccessFunctionalAdmin esClient;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));

        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
        final List<Integer> tenants = Arrays.asList(TENANT_ID, EXTERNAL_TENANT);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.name());
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);

        LogbookOperationsClientFactory.changeMode(null);

        archiveUnitProfileService = new ArchiveUnitProfileServiceImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            vitamCounterService,
            functionalBackupService,
            false
        );

        esClient = new ElasticsearchAccessFunctionalAdmin(
            ElasticsearchRule.VITAM_CLUSTER,
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
            indexManager
        );
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            esClient,
            Collections.singletonList(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE)
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        archiveUnitProfileService.close();
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest();
        reset(functionalBackupService);
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<ArchiveUnitProfileModel> profileModelList =
            archiveUnitProfileService.findArchiveUnitProfiles(JsonHandler.createObjectNode());
        assertThat(profileModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenWellFormedArchiveUnitProfileMetadataThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenATestMissingIdentifierReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_missing_identifier.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        assertThat(response.isOk()).isFalse();
        verifyZeroInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenATestMissingSchemaReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_missing_schema.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse response = archiveUnitProfileService.createArchiveUnitProfiles(profileModelList);

        assertThat(response.isOk()).isFalse();
        verifyZeroInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void importAnAUPWithInvalidControlSchemaThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_invalid_schema.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        List<VitamError<ArchiveUnitProfileModel>> errors = ((VitamError<ArchiveUnitProfileModel>) response).getErrors();
        assertThat(errors.get(0).getDescription().equals("The field ControlSchema is not a json schema")).isTrue();
        assertThat(errors.get(0).getMessage().equals("IMPORT_ARCHIVEUNITPROFILE.INVALID_JSON_SCHEMA.KO")).isTrue();

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestIdentifiers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_duplicate_identifier.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_duplicate_name.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDuplicateNamesInDb() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        assertThat(response.isOk()).isTrue();

        final List<ArchiveUnitProfileModel> profileModelList2 = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response2 = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList2
        );

        assertThat(response2.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");

        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // Try to recreate the same profile but with id
        response = archiveUnitProfileService.createArchiveUnitProfiles(responseCast.getResults());

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find profile with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }"; JsonNode queryDsl =
         * JsonHandler.getFromString(q);
         */
        final RequestResponseOK<ArchiveUnitProfileModel> profileModelList =
            archiveUnitProfileService.findArchiveUnitProfiles(queryDsl);

        assertThat(profileModelList.getResults()).isEmpty();
    }

    /**
     * Check that the created access conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenTestTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We juste test the first profile
        final ArchiveUnitProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        ArchiveUnitProfileModel one = archiveUnitProfileService.findByIdentifier(id1);
        assertThat(one).isNotNull();
        assertThat(one.getName()).isEqualTo(acm.getName());
        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(TENANT_ID);
    }

    /**
     * Profile of tenant 1, try to get the same profile with id mongo but with tenant 2 This sgould not return the
     * profile as tenant 2 is not the owner of the profile
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenTestNotTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We just test the first profile
        final ArchiveUnitProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();
        assertThat(acm.getFields()).isNotNull();
        assertEquals(acm.getFields().size(), 0);

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        VitamThreadUtils.getVitamSession().setTenantId(2);
        final ArchiveUnitProfileModel one = archiveUnitProfileService.findByIdentifier(id1);
        assertThat(one).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestWithSchema() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_with_schema.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        String id3 =
            ((RequestResponseOK<ArchiveUnitProfileModel>) response).getResults().iterator().next().getIdentifier();
        final ArchiveUnitProfileModel acm = archiveUnitProfileService.findByIdentifier(id3);
        assertEquals(acm.getFields().size(), 14);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We juste test the first profile
        final ArchiveUnitProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        ArchiveUnitProfileModel one = archiveUnitProfileService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We juste test the first profile
        final ArchiveUnitProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        // External identifier
        String id1 = "aIdentifier2";

        ArchiveUnitProfileModel one = archiveUnitProfileService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifierSlaveMode() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("aup_without_identifier_slave_mode.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );
        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError<ArchiveUnitProfileModel>) response).getErrors().get(0).getDescription()).isEqualTo(
            "The field Identifier is mandatory"
        );
        assertThat(((VitamError<ArchiveUnitProfileModel>) response).getErrors().get(0).getMessage()).isEqualTo(
            "IMPORT_ARCHIVEUNITPROFILE.EMPTY_REQUIRED_FIELD.KO"
        );
        verifyZeroInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifierMasterMode() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("aup_without_identifier_master_mode.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );
        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We juste test the first profile
        final ArchiveUnitProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();
        assertThat(acm.getIdentifier()).startsWith("AUP-");
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnTwoProfiles() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        final RequestResponseOK<ArchiveUnitProfileModel> profileModelListSearch =
            archiveUnitProfileService.findArchiveUnitProfiles(JsonHandler.createObjectNode());
        assertThat(profileModelListSearch.getResults()).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindByIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("AUP_ok_id.json");
        final List<ArchiveUnitProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ArchiveUnitProfileModel> response = archiveUnitProfileService.createArchiveUnitProfiles(
            profileModelList
        );

        final RequestResponseOK<ArchiveUnitProfileModel> responseCast = (RequestResponseOK<
                ArchiveUnitProfileModel
            >) response;
        assertThat(responseCast.getResults()).hasSize(1);

        final ArchiveUnitProfileModel acm = profileModelList.iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        final String identifier = acm.getIdentifier();
        assertThat(identifier).isNotNull();

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", identifier));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();

        final RequestResponseOK<ArchiveUnitProfileModel> profileModelListFound =
            archiveUnitProfileService.findArchiveUnitProfiles(queryDsl);
        assertThat(profileModelListFound.getResults()).hasSize(1);

        final ArchiveUnitProfileModel acmFound = profileModelListFound.getResults().iterator().next();
        assertThat(acmFound).isNotNull();
        assertThat(acmFound.getIdentifier()).isEqualTo(identifier);
    }
}
