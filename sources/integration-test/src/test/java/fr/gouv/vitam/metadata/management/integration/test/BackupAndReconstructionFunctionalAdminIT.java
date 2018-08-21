/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.metadata.management.integration.test;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.impl.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.integration.test.IngestInternalIT;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class BackupAndReconstructionFunctionalAdminIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(BackupAndReconstructionFunctionalAdminIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                LogbookMain.class,
                WorkspaceMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                AdminManagementMain.class
            ));

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

    private static final String SECURITY_PROFILE_IDENTIFIER_1 = "SEC_PROFILE-000001";
    private static final String SECURITY_PROFILE_IDENTIFIER_2 = "SEC_PROFILE-000002";


    @AfterClass
    public static void afterClass() throws Exception {
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

        // Import 1 document agencies
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_1_CSV), "agencies_1.csv");
        }

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository agenciesMongo =
            vitamRepository.getVitamMongoRepository(FunctionalAdminCollections.AGENCIES.getVitamCollection());

        final VitamElasticsearchRepository agenciesEs =
            vitamRepository.getVitamESRepository(FunctionalAdminCollections.AGENCIES.getVitamCollection());

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

        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(vitamRepository,
                new RestoreBackupServiceImpl());

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
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                INTEGRATION_RECONSTRUCTION_DATA_AGENCIES_2_CSV), "agencies_2.csv");
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

        // Import 1 document securityProfile.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            File securityProfileFiles =
                PropertiesUtils.getResourceFile(INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_1_JSON);
            List<SecurityProfileModel> securityProfileModelList =
                JsonHandler
                    .getFromFileAsTypeRefence(securityProfileFiles, new TypeReference<List<SecurityProfileModel>>() {
                    });
            client.importSecurityProfiles(securityProfileModelList);
        }
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        final VitamMongoRepository securityProfileMongo =
            vitamRepository.getVitamMongoRepository(FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection());

        final VitamElasticsearchRepository securityProfileEs =
            vitamRepository.getVitamESRepository(FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection());

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

        // Reconstruction service
        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(vitamRepository,
                new RestoreBackupServiceImpl());
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
            File securityProfileFiles =
                PropertiesUtils.getResourceFile(INTEGRATION_RECONSTRUCTION_DATA_SECURITY_PROFILE_2_JSON);
            List<SecurityProfileModel> securityProfileModelList =
                JsonHandler
                    .getFromFileAsTypeRefence(securityProfileFiles, new TypeReference<List<SecurityProfileModel>>() {
                    });
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
}
