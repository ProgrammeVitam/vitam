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
package fr.gouv.vitam.metadata.management.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.AddAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.ReconstructionRequestItem;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientBadRequestException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.core.reconstruction.FunctionalAdministrationReconstructionMetricsCache;
import fr.gouv.vitam.functional.administration.core.reconstruction.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.core.reconstruction.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class BackupAndReconstructionFunctionalAdminIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        BackupAndReconstructionFunctionalAdminIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AdminManagementMain.class
        )
    );

    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;
    private static final String AGENCY_IDENTIFIER_1 = "FR_ORG_AGEN";
    private static final String AGENCY_IDENTIFIER_2 = "FRAN_NP_005568";
    private static final String INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_1_CSV =
        "integration-metadata-management/data/agencies_1.csv";

    private static final String INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_2_CSV =
        "integration-metadata-management/data/agencies_2.csv";

    private static final String INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON =
        "integration-metadata-management/data/security_profile_1.json";
    private static final String INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_2_JSON =
        "integration-metadata-management/data/security_profile_2.json";
    private static final String IMPORT_SECURITY_PROFILE_3_JSON =
        "integration-metadata-management/data/security_profile_3.json";

    private static final String SECURITY_PROFILE_IDENTIFIER_1 = "SEC_PROFILE-000001";
    private static final String SECURITY_PROFILE_IDENTIFIER_2 = "SEC_PROFILE-000002";

    private static final String ACCESSION_REGISTER_DETAIL_DATA_1 =
        "functional-admin/accession-register/accession-register-detail-1.json";
    private static final String ACCESSION_REGISTER_DETAIL_DATA_2 =
        "functional-admin/accession-register/accession-register-detail-2.json";
    private static final String ACCESSION_REGISTER_DETAIL_DATA_3 =
        "functional-admin/accession-register/accession-register-detail-3.json";
    private static final String ACCESSION_REGISTER_DETAIL_DATA_4 =
        "functional-admin/accession-register/accession-register-detail-4.json";

    private static final String NAME = "Name";
    public static final String PERMISSIONS = "Permissions";

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @BeforeClass
    public static void beforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void tearDown() {
        runAfter();
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    /**
     * Test reconstruction of agencies For tenant 0 1. Import one agency using import service 2. Check that imported 3.
     * purge mongo and es 4. Check that purged 5. reconstruct 6. Check that document reconstructed 7. check that initial
     * document is equal to the reconstructed one 8. import agencies containing the first one + an other agency 9. Check
     * that imported two documents 10. purge mongo and es 11. Check that purged 12. reconstruct 13. Check that two
     * documents are reconstructed 14. check that initial documents are equal to the reconstructed documents
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void testReconstructionAgenciesOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        OffsetRepository offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        // Import 1 document agencies
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(
                PropertiesUtils.getResourceAsStream(INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_1_CSV),
                "agencies_1.csv"
            );
        }

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository agenciesMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.AGENCIES.getVitamCollection()
        );

        final VitamElasticsearchRepository agenciesEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.AGENCIES.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.AGENCIES)
        );

        Optional<Document> agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo11 = agencyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs11 = agencyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("agency 1");

        agenciesMongo.purge(TENANT_0);
        agenciesEs.purge(TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache =
            new FunctionalAdministrationReconstructionMetricsCache(10, TimeUnit.MINUTES);
        ReconstructionServiceImpl reconstructionService = new ReconstructionServiceImpl(
            vitamRepository,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            functionalAdminIndexManager,
            reconstructionMetricsCache
        );

        reconstructionService.reconstruct(FunctionalAdminCollections.AGENCIES, TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo11Reconstructed = agencyDoc.get();
        assertThat(inMogo11Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo11Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs11Reconstructed = agencyDoc.get();
        assertThat(inEs11Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs11Reconstructed.getString("Name")).isEqualTo("agency 1");

        assertThat(inMogo11).isEqualTo(inMogo11Reconstructed);
        assertThat(inEs11).isEqualTo(inEs11Reconstructed);

        // Import 2 documents agencies
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        // Create and save some backup files for reconstruction.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(
                PropertiesUtils.getResourceAsStream(INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_2_CSV),
                "agencies_2.csv"
            );
        }

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo12 = agencyDoc.get();
        assertThat(inMogo12.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo12.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs12 = agencyDoc.get();
        assertThat(inEs12.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs12.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo22 = agencyDoc.get();
        assertThat(inMogo22.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inMogo22.getString("Name")).isEqualTo("agency 2");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs22 = agencyDoc.get();
        assertThat(inEs22.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inEs22.getString("Name")).isEqualTo("agency 2");

        agenciesMongo.purge(TENANT_0);
        agenciesEs.purge(TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isEmpty();

        reconstructionService.reconstruct(FunctionalAdminCollections.AGENCIES, TENANT_0);

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo12Reconstructed = agencyDoc.get();
        assertThat(inMogo12Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inMogo12Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_1, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs12Reconstructed = agencyDoc.get();
        assertThat(inEs12Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_1);
        assertThat(inEs12Reconstructed.getString("Name")).isEqualTo("agency 1");

        agencyDoc = agenciesMongo.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inMogo22Reconstructed = agencyDoc.get();
        assertThat(inMogo22Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inMogo22Reconstructed.getString("Name")).isEqualTo("agency 2");

        agencyDoc = agenciesEs.findByIdentifierAndTenant(AGENCY_IDENTIFIER_2, TENANT_0);
        assertThat(agencyDoc).isPresent();
        Document inEs22Reconstructed = agencyDoc.get();
        assertThat(inEs22Reconstructed.getString("Identifier")).isEqualTo(AGENCY_IDENTIFIER_2);
        assertThat(inEs22Reconstructed.getString("Name")).isEqualTo("agency 2");

        assertThat(inMogo12).isEqualTo(inMogo12Reconstructed);
        assertThat(inEs12).isEqualTo(inEs12Reconstructed);
        assertThat(inMogo22).isEqualTo(inMogo22Reconstructed);
        assertThat(inEs22).isEqualTo(inEs22Reconstructed);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionSecurityProfileOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        OffsetRepository offsetRepository;

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        // Import 1 document securityProfile.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(
                INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON
            );
            List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            client.importSecurityProfiles(securityProfileModelList);
        }
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository securityProfileMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection()
        );

        final VitamElasticsearchRepository securityProfileEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11 = securityProfileyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11 = securityProfileyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache =
            new FunctionalAdministrationReconstructionMetricsCache(10, TimeUnit.MINUTES);

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService = new ReconstructionServiceImpl(
            vitamRepository,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            functionalAdminIndexManager,
            reconstructionMetricsCache
        );
        reconstructionService.reconstruct(FunctionalAdminCollections.SECURITY_PROFILE, TENANT_1);

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo11Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11Reconstructed = securityProfileyDoc.get();
        assertThat(inEs11Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        assertThat(inMogo11).isEqualTo(inMogo11Reconstructed);
        assertThat(inEs11).isEqualTo(inEs11Reconstructed);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        // Import 2 document securityProfile.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(
                INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_2_JSON
            );
            List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            client.importSecurityProfiles(securityProfileModelList);
        }

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo12 = securityProfileyDoc.get();
        assertThat(inMogo12.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo12.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs12 = securityProfileyDoc.get();
        assertThat(inEs12.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs12.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo22 = securityProfileyDoc.get();
        assertThat(inMogo22.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inMogo22.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs22 = securityProfileyDoc.get();
        assertThat(inEs22.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inEs22.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isEmpty();

        reconstructionService.reconstruct(FunctionalAdminCollections.SECURITY_PROFILE, TENANT_1);

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo12Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo12Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo12Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs12Reconstructed = securityProfileyDoc.get();
        assertThat(inEs12Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs12Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_1");

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo22Reconstructed = securityProfileyDoc.get();
        assertThat(inMogo22Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inMogo22Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_2");

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs22Reconstructed = securityProfileyDoc.get();
        assertThat(inEs22Reconstructed.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_2);
        assertThat(inEs22Reconstructed.getString("Name")).isEqualTo("SEC_PROFILE_2");

        assertThat(inMogo12).isEqualTo(inMogo12Reconstructed);
        assertThat(inEs12).isEqualTo(inEs12Reconstructed);
        assertThat(inMogo22).isEqualTo(inMogo22Reconstructed);
        assertThat(inEs22).isEqualTo(inEs22Reconstructed);
    }

    @Test
    @RunWithCustomExecutor
    public void testBackupAndReconstructAccessionRegisterDetailOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_1));
        AccessionRegisterDetailModel register1;
        AccessionRegisterDetailModel register2;
        AccessionRegisterDetailModel register3;
        AccessionRegisterDetailModel register4;

        OffsetRepository offsetRepository;

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        logicalClock.freezeTime();

        // Insert new register detail
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            register1 = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_DATA_1),
                AccessionRegisterDetailModel.class
            );
            client.createOrUpdateAccessionRegister(register1);
            logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            register2 = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_DATA_2),
                AccessionRegisterDetailModel.class
            );
            client.createOrUpdateAccessionRegister(register2);
            logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            register3 = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_DATA_3),
                AccessionRegisterDetailModel.class
            );
            client.createOrUpdateAccessionRegister(register3);
            logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

            VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
            register4 = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_DATA_4),
                AccessionRegisterDetailModel.class
            );
            client.createOrUpdateAccessionRegister(register4);
            logicalClock.logicalSleep(10, ChronoUnit.MINUTES);
        }

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository ardMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getVitamCollection()
        );

        final VitamElasticsearchRepository ardEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL
            )
        );

        final VitamMongoRepository arsMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getVitamCollection()
        );

        final VitamElasticsearchRepository arsEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY
            )
        );

        Optional<Document> registerDetailDoc = ardMongo.getByID(register1.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        Document inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register1.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register1.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register1.getEndDate());

        registerDetailDoc = ardEs.getByID(register1.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register1.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register1.getEndDate());

        registerDetailDoc = ardMongo.getByID(register2.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register2.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register2.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register2.getEndDate());

        registerDetailDoc = ardEs.getByID(register2.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register2.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register2.getEndDate());

        registerDetailDoc = ardMongo.getByID(register3.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register3.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register3.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register3.getEndDate());

        registerDetailDoc = ardEs.getByID(register3.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register3.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register3.getEndDate());

        registerDetailDoc = ardMongo.getByID(register4.getId(), TENANT_0);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register4.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register4.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register4.getEndDate());

        registerDetailDoc = ardEs.getByID(register4.getId(), TENANT_0);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register4.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register4.getEndDate());

        ArrayNode registerSummaryDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())
        );

        assertThat(registerSummaryDocs.size()).isEqualTo(3);

        for (JsonNode doc : registerSummaryDocs) {
            if (doc.get("_tenant").asInt() == TENANT_0) {
                assertEquals(1000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(1000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(9999, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(9999, doc.get("ObjectSize").get("remained").asInt());
            } else if (doc.get("OriginatingAgency").asText().equals("OG_1")) {
                assertEquals(2000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(2000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(19998, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(19998, doc.get("ObjectSize").get("remained").asInt());
            } else if (doc.get("OriginatingAgency").asText().equals("OG_2")) {
                assertEquals(1000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(1000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(9999, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(9999, doc.get("ObjectSize").get("remained").asInt());
            }
        }

        ardMongo.purge();
        ardEs.purge();
        arsMongo.purge();
        arsEs.purge();

        LocalDateTime accessionRegisterDetailReconstructionDate = LocalDateUtil.now();

        registerDetailDoc = ardMongo.getByID(register1.getId(), TENANT_1);
        assertThat(registerDetailDoc).isEmpty();

        registerDetailDoc = ardMongo.getByID(register2.getId(), TENANT_1);
        assertThat(registerDetailDoc).isEmpty();

        registerDetailDoc = ardMongo.getByID(register3.getId(), TENANT_1);
        assertThat(registerDetailDoc).isEmpty();

        registerDetailDoc = ardMongo.getByID(register4.getId(), TENANT_0);
        assertThat(registerDetailDoc).isEmpty();

        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache =
            new FunctionalAdministrationReconstructionMetricsCache(10, TimeUnit.MINUTES);

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService = new ReconstructionServiceImpl(
            vitamRepository,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            functionalAdminIndexManager,
            reconstructionMetricsCache
        );

        // First reconstruct Accession Register Details + summary (with limit 2)

        ReconstructionRequestItem reconstructionItemTenant0 = new ReconstructionRequestItem();
        reconstructionItemTenant0.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        reconstructionItemTenant0.setTenant(TENANT_0);
        reconstructionItemTenant0.setLimit(2);
        reconstructionService.reconstructAccessionRegister(reconstructionItemTenant0);

        ReconstructionRequestItem reconstructionItemTenant1 = new ReconstructionRequestItem();
        reconstructionItemTenant1.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        reconstructionItemTenant1.setTenant(TENANT_1);
        reconstructionItemTenant1.setLimit(2);
        reconstructionService.reconstructAccessionRegister(reconstructionItemTenant1);

        registerDetailDoc = ardMongo.getByID(register1.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register1.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register1.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register1.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerDetailDoc = ardMongo.getByID(register2.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register2.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register2.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register2.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerDetailDoc = ardMongo.getByID(register3.getId(), TENANT_1);
        assertThat(registerDetailDoc).isEmpty();

        registerDetailDoc = ardMongo.getByID(register4.getId(), TENANT_0);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register4.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register4.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register4.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerSummaryDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())
        );

        assertThat(registerSummaryDocs.size()).isEqualTo(3);

        for (JsonNode doc : registerSummaryDocs) {
            assertEquals(0, doc.get("_v").asInt());
            assertEquals(1000, doc.get("TotalObjectGroups").get("ingested").asInt());
            assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
            assertEquals(1000, doc.get("TotalObjectGroups").get("remained").asInt());

            assertEquals(9999, doc.get("ObjectSize").get("ingested").asInt());
            assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
            assertEquals(9999, doc.get("ObjectSize").get("remained").asInt());
        }

        long offset0 = offsetRepository.findOffsetBy(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
        );
        long offset1 = offsetRepository.findOffsetBy(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
        );
        assertThat(offset0).isEqualTo(4L);
        assertThat(offset1).isEqualTo(2L);

        // Another reconstruction for Accession Register Details + summary (with limit 2)

        reconstructionItemTenant0 = new ReconstructionRequestItem();
        reconstructionItemTenant0.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        reconstructionItemTenant0.setTenant(TENANT_0);
        reconstructionItemTenant0.setLimit(2);
        reconstructionService.reconstructAccessionRegister(reconstructionItemTenant0);

        reconstructionItemTenant1 = new ReconstructionRequestItem();
        reconstructionItemTenant1.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        reconstructionItemTenant1.setTenant(TENANT_1);
        reconstructionItemTenant1.setLimit(2);
        reconstructionService.reconstructAccessionRegister(reconstructionItemTenant1);

        registerDetailDoc = ardMongo.getByID(register1.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register1.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register1.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register1.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerDetailDoc = ardMongo.getByID(register2.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register2.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register2.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register2.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerDetailDoc = ardMongo.getByID(register3.getId(), TENANT_1);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register3.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register3.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register3.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerDetailDoc = ardMongo.getByID(register4.getId(), TENANT_0);
        assertThat(registerDetailDoc).isNotNull();
        assertThat(registerDetailDoc).isNotEmpty();
        inMogo22Reconstructed = registerDetailDoc.get();
        assertThat(inMogo22Reconstructed.getString("_id")).isEqualTo(register4.getId());
        assertThat(inMogo22Reconstructed.getString("Opc")).isEqualTo(register4.getOpc());
        assertThat(inMogo22Reconstructed.getString("EndDate")).isEqualTo(register4.getEndDate());
        assertThat(inMogo22Reconstructed.getInteger("_v")).isEqualTo(0);

        registerSummaryDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())
        );

        assertThat(registerSummaryDocs.size()).isEqualTo(3);

        for (JsonNode doc : registerSummaryDocs) {
            assertEquals(0, doc.get("_v").asInt());

            if (doc.get("_tenant").asInt() == TENANT_0) {
                assertEquals(1000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(1000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(9999, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(9999, doc.get("ObjectSize").get("remained").asInt());
            } else if (doc.get("OriginatingAgency").asText().equals("OG_1")) {
                assertEquals(2000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(2000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(19998, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(19998, doc.get("ObjectSize").get("remained").asInt());
            } else if (doc.get("OriginatingAgency").asText().equals("OG_2")) {
                assertEquals(1000, doc.get("TotalObjectGroups").get("ingested").asInt());
                assertEquals(0, doc.get("TotalObjectGroups").get("deleted").asInt());
                assertEquals(1000, doc.get("TotalObjectGroups").get("remained").asInt());

                assertEquals(9999, doc.get("ObjectSize").get("ingested").asInt());
                assertEquals(0, doc.get("ObjectSize").get("deleted").asInt());
                assertEquals(9999, doc.get("ObjectSize").get("remained").asInt());
            }
        }

        long newOffset0 = offsetRepository.findOffsetBy(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
        );
        long newOffset1 = offsetRepository.findOffsetBy(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
        );
        assertThat(newOffset0).isEqualTo(4L);
        assertThat(newOffset1).isEqualTo(3L);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, 0)
        ).isEqualTo(Duration.between(accessionRegisterDetailReconstructionDate, LocalDateUtil.now()));

        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, 1)
        ).isEqualTo(Duration.between(accessionRegisterDetailReconstructionDate, LocalDateUtil.now()));

        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, 2)
        ).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void testBackupAndReconstructAccessionRegisterSymbolicOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_1));

        OffsetRepository offsetRepository;

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName(),
            0L
        );

        initializeDbWithUnitAndObjectGroupData();

        logicalClock.freezeTime();

        // Compute Accession Register Symbolic

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            adminManagementClient.createAccessionRegisterSymbolic(Collections.singletonList(TENANT_1));
        }

        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);

        ArrayNode registerSymbolicDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection().find())
        );

        assertThat(registerSymbolicDocs.size()).isEqualTo(3);
        checkSymbolicRegisterResult(registerSymbolicDocs);

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();
        final VitamMongoRepository arsMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getVitamCollection()
        );

        final VitamElasticsearchRepository arsEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC
            )
        );

        arsMongo.purge();
        arsEs.purge();

        registerSymbolicDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection().find())
        );

        assertThat(registerSymbolicDocs.size()).isEqualTo(0);

        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache =
            new FunctionalAdministrationReconstructionMetricsCache(10, TimeUnit.MINUTES);

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService = new ReconstructionServiceImpl(
            vitamRepository,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            functionalAdminIndexManager,
            reconstructionMetricsCache
        );

        LocalDateTime accessionRegisterReconstructionDate = LocalDateUtil.now();

        // Reconstruct Accession Register Detail
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName());
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItem.setLimit(1000);
        reconstructionService.reconstructAccessionRegister(reconstructionItem);

        registerSymbolicDocs = (ArrayNode) JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection().find())
        );

        assertThat(registerSymbolicDocs.size()).isEqualTo(3);
        checkSymbolicRegisterResult(registerSymbolicDocs);

        logicalClock.logicalSleep(5, ChronoUnit.MINUTES);

        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC,
                0
            )
        ).isNull();
        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC,
                1
            )
        ).isEqualTo(Duration.between(accessionRegisterReconstructionDate, LocalDateUtil.now()));
        assertThat(
            reconstructionMetricsCache.getReconstructionLatency(
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC,
                2
            )
        ).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void testBackupAndReconstructAccessionRegisterSymbolicWithManyAgenciesOk() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_1));

        OffsetRepository offsetRepository;

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName(),
            0L
        );

        initializeDbWithManyUnitsAndObjectGroups();

        // Compute Accession Register Symbolic

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            adminManagementClient.createAccessionRegisterSymbolic(Collections.singletonList(TENANT_0));
        }

        long registerSymbolicSize = FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection()
            .countDocuments();

        assertThat(registerSymbolicSize).isEqualTo(16);

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();
        final VitamMongoRepository arsMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getVitamCollection()
        );

        final VitamElasticsearchRepository arsEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(
                FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC
            )
        );

        arsMongo.purge();
        arsEs.purge();

        registerSymbolicSize = FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection().countDocuments();

        assertThat(registerSymbolicSize).isEqualTo(0);

        FunctionalAdministrationReconstructionMetricsCache reconstructionMetricsCache =
            new FunctionalAdministrationReconstructionMetricsCache(10, TimeUnit.MINUTES);

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService = new ReconstructionServiceImpl(
            vitamRepository,
            new RestoreBackupServiceImpl(),
            offsetRepository,
            functionalAdminIndexManager,
            reconstructionMetricsCache
        );

        // Reconstruct Accession Register Detail
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getName());
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItem.setLimit(1000);
        reconstructionService.reconstructAccessionRegister(reconstructionItem);

        registerSymbolicSize = FunctionalAdminCollections.ACCESSION_REGISTER_SYMBOLIC.getCollection().countDocuments();

        assertThat(registerSymbolicSize).isEqualTo(16);
    }

    private void checkSymbolicRegisterResult(ArrayNode docs) {
        for (JsonNode doc : docs) {
            if (doc.get("OriginatingAgency").asText().equals("OA1")) {
                assertEquals(doc.get("_tenant").asInt(), TENANT_1);
                assertEquals(doc.get("ArchiveUnit").asInt(), 4);
            } else if (doc.get("OriginatingAgency").asText().equals("OA2")) {
                assertEquals(doc.get("_tenant").asInt(), TENANT_1);
                assertEquals(doc.get("ArchiveUnit").asInt(), 2);
            } else {
                assertEquals(doc.get("_tenant").asInt(), TENANT_1);
                assertEquals(doc.get("ArchiveUnit").asInt(), 2);
            }
        }
    }

    private void initializeDbWithUnitAndObjectGroupData() throws DatabaseException {
        // Create units with or without graph data
        Document au1 = new Document(Unit.ID, "AU_1")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA1")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA1"));

        Document au2 = new Document(Unit.ID, "AU_2")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au3 = new Document(Unit.ID, "AU_3")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_1"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA1")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA1"));

        Document au4 = new Document(Unit.ID, "AU_4")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_4")
            .append(Unit.UP, Lists.newArrayList("AU_1", "AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA4")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au5 = new Document(Unit.ID, "AU_5")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList("AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au6 = new Document(Unit.ID, "AU_6")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_6")
            .append(Unit.UP, Lists.newArrayList("AU_2", "AU_5"))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au7 = new Document(Unit.ID, "AU_7")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_4"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA4")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au8 = new Document(Unit.ID, "AU_8")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_6", "AU_4"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au9 = new Document(Unit.ID, "AU_9")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_9")
            .append(Unit.UP, Lists.newArrayList("AU_5", "AU_6"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au10 = new Document(Unit.ID, "AU_10")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_10")
            .append(Unit.UP, Lists.newArrayList("AU_8", "AU_9"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        List<Document> units = Lists.newArrayList(au1, au2, au3, au4, au5, au6, au7, au8, au9, au10);
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection()).save(units);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.UNIT.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
            )
            .save(units);

        ////////////////////////////////////////////////
        // Create corresponding ObjectGroup (only 4 GOT subject of compute graph as no _glpd defined on them)
        ///////////////////////////////////////////////
        Document got4 = new Document(ObjectGroup.ID, "GOT_4")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_4"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA4")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA4"));

        // Got 6 have Graph Data
        Document got6 = new Document(ObjectGroup.ID, "GOT_6")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.UP, Lists.newArrayList("AU_6"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2")
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA4", "OA1", "OA2"));

        //Unit "AU_8", "AU_3", "AU_7" attached to got 8
        Document got8 = new Document(ObjectGroup.ID, "GOT_8")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_8", "AU_3", "AU_7"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got9 = new Document(ObjectGroup.ID, "GOT_9")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_9"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got10 = new Document(ObjectGroup.ID, "GOT_10")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_10"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        List<Document> gots = Lists.newArrayList(got4, got6, got8, got9, got10);
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
            .save(gots);
        VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
            )
            .save(gots);
    }

    private void initializeDbWithManyUnitsAndObjectGroups() throws IOException, DatabaseException {
        try (InputStream is = PropertiesUtils.getResourceAsStream("reconstruction/units.jsonl")) {
            final Iterator<List<Document>> iterator = Iterators.partition(
                new JsonLineGenericIterator<>(is, new TypeReference<>() {}),
                1000
            );
            while (iterator.hasNext()) {
                List<Document> units = iterator.next();
                VitamRepositoryFactory.get()
                    .getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
                    .save(units);
                VitamRepositoryFactory.get()
                    .getVitamESRepository(
                        MetadataCollections.UNIT.getVitamCollection(),
                        metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT)
                    )
                    .save(units);
            }
        }
        try (InputStream is = PropertiesUtils.getResourceAsStream("reconstruction/objectgroups.jsonl")) {
            final Iterator<List<Document>> iterator = Iterators.partition(
                new JsonLineGenericIterator<>(is, new TypeReference<>() {}),
                1000
            );
            while (iterator.hasNext()) {
                List<Document> objectgroups = iterator.next();
                VitamRepositoryFactory.get()
                    .getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
                    .save(objectgroups);
                VitamRepositoryFactory.get()
                    .getVitamESRepository(
                        MetadataCollections.OBJECTGROUP.getVitamCollection(),
                        metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
                    )
                    .save(objectgroups);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void importSecurityProfile_OK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        OffsetRepository offsetRepository = new OffsetRepository(mongoDbAccess);
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        List<SecurityProfileModel> securityProfileModelList;

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(
                INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON
            );
            securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            client.importSecurityProfiles(securityProfileModelList);
        }
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository securityProfileMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection()
        );

        final VitamElasticsearchRepository securityProfileEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11 = securityProfileyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inMogo11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11 = securityProfileyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inEs11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void importSecurityProfileUnknownPermission_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        OffsetRepository offsetRepository = new OffsetRepository(mongoDbAccess);
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        List<SecurityProfileModel> securityProfileModelList;

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(IMPORT_SECURITY_PROFILE_3_JSON);
            securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            assertThatThrownBy(() -> client.importSecurityProfiles(securityProfileModelList))
                .isInstanceOf(AdminManagementClientServerException.class)
                .hasMessageContaining("SecurityProfile service error")
                .hasMessageContaining("Au moins une permission n'existe pas.");
        }
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository securityProfileMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection()
        );
        final VitamElasticsearchRepository securityProfileEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void importSecurityProfileAndUpdatePermission_OK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        OffsetRepository offsetRepository = new OffsetRepository(mongoDbAccess);
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        List<SecurityProfileModel> securityProfileModelList;
        final String newPermission = "units:read";
        // import security profile
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(
                INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON
            );
            securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            client.importSecurityProfiles(securityProfileModelList);

            // update permissions in security profile
            VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));
            final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
            final Update update = new Update();
            update.setQuery(QueryHelper.eq(NAME, "SEC_PROFILE_1"));
            final AddAction setActionAddPermission = UpdateActionHelper.add(PERMISSIONS, newPermission);
            update.addActions(setActionAddPermission);
            updateParser.parse(update.getFinalUpdate());
            final JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

            client.updateSecurityProfile(SECURITY_PROFILE_IDENTIFIER_1, queryDslForUpdate);
            securityProfileModelList.get(0).getPermissions().add(newPermission);
        }

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();
        final VitamMongoRepository securityProfileMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection()
        );
        final VitamElasticsearchRepository securityProfileEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11 = securityProfileyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inMogo11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11 = securityProfileyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inEs11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void importSecurityProfileAndUpdateUnknownPermission_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        OffsetRepository offsetRepository = new OffsetRepository(mongoDbAccess);
        offsetRepository.createOrUpdateOffset(
            TENANT_1,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        List<SecurityProfileModel> securityProfileModelList;
        final String newPermission = "test:test";
        // import security profile
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles = PropertiesUtils.getResourceFile(
                INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON
            );
            securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
                securityProfileFiles,
                new TypeReference<>() {}
            );
            client.importSecurityProfiles(securityProfileModelList);

            // update permissions in security profile
            VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));
            final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
            final Update update = new Update();
            update.setQuery(QueryHelper.eq(NAME, "SEC_PROFILE_1"));
            final AddAction setActionAddPermission = UpdateActionHelper.add(PERMISSIONS, newPermission);
            update.addActions(setActionAddPermission);
            updateParser.parse(update.getFinalUpdate());
            final JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

            assertThatThrownBy(() -> client.updateSecurityProfile(SECURITY_PROFILE_IDENTIFIER_1, queryDslForUpdate))
                .isInstanceOf(AdminManagementClientBadRequestException.class)
                .hasMessageContaining("Update security profile error")
                .hasMessageContaining("Au moins une permission n'existe pas.");
        }

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();
        final VitamMongoRepository securityProfileMongo = vitamRepository.getVitamMongoRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection()
        );
        final VitamElasticsearchRepository securityProfileEs = vitamRepository.getVitamESRepository(
            FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection(),
            functionalAdminIndexManager.getElasticsearchIndexAliasResolver(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        Optional<Document> securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inMogo11 = securityProfileyDoc.get();
        assertThat(inMogo11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inMogo11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inMogo11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isPresent();
        Document inEs11 = securityProfileyDoc.get();
        assertThat(inEs11.getString("Identifier")).isEqualTo(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(inEs11.getString("Name")).isEqualTo("SEC_PROFILE_1");
        assertThat(inEs11.getList("Permissions", String.class).toString()).isEqualTo(
            securityProfileModelList.get(0).getPermissions().toString()
        );

        securityProfileMongo.purge();
        securityProfileEs.purge();

        securityProfileyDoc = securityProfileMongo.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();

        securityProfileyDoc = securityProfileEs.findByIdentifier(SECURITY_PROFILE_IDENTIFIER_1);
        assertThat(securityProfileyDoc).isEmpty();
    }
}
