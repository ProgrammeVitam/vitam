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
package fr.gouv.vitam.logbook.administration.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.impl.LogbookCheckConsistencyServiceImpl;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckError;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckEvent;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.VitamTestHelper.doIngestWithLogbookStatus;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.verifyProcessState;
import static fr.gouv.vitam.common.model.ProcessState.COMPLETED;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.UNKNOWN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of LogbookCheckConsistency services.
 */
public class LogbookCheckConsistencyIT extends VitamRuleRunner {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyIT.class);

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        LogbookCheckConsistencyIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            DefaultOfferMain.class,
            IngestInternalMain.class,
            AccessInternalMain.class,
            StorageMain.class
        )
    );

    private static final String CHECK_LOGBOOK_DATA_AGENCIES = "integration-logbook/data/agencies.csv";
    private static final String ACCESS_CONTRATS_JSON = "integration-logbook/data/access_contrats.json";
    private static final String REFERENTIAL_CONTRACTS_OK_JSON =
        "integration-logbook/data/referential_contracts_ok.json";
    private static final String HECK_LOGBOOK_MGT_RULES_REF_CSV = "integration-logbook/data/MGT_RULES_REF.csv";
    private static final String MGT_RULES_REF_CSV = "jeu_donnees_OK_regles_CSV_regles.csv";
    private static final String AGENCIES_CSV = "agencies.csv";
    private static final String CHECK_LOGBOOK_DROID_SIGNATURE_FILE_V94_XML =
        "integration-logbook/data/DROID_SignatureFile_V94.xml";
    private static final String DROID_SIGNATURE_FILE_V94_XML = "DROID_SignatureFile_V94.xml";

    private static final String SIP_KO_ARBO_RECURSIVE = "integration-logbook/data/KO_ARBO_recursif.zip";
    private static final String EXPECTED_RESULTS_JSON = "integration-logbook/data/expected_results.json";

    private static final int TENANT_0 = 0;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

        ProcessingManagementClientFactory.changeConfigurationUrl(VitamServerRunner.PROCESSING_URL);

        VitamConfiguration.setPurgeTemporaryLFC(false);

        VitamServerRunner.cleanOffers();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        handleAfterClass();
        VitamConfiguration.setPurgeTemporaryLFC(true);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @After
    public void tearDown() {
        runAfter();
    }

    /**
     * Logbook's properties check service.
     */
    private LogbookCheckConsistencyService coherenceCheckService;

    @Test
    @RunWithCustomExecutor
    public void testLogbookCoherenceCheck_withoutIncoherentLogbook() throws Exception {
        LOGGER.debug("Starting integration tests for logbook coherence checks.");

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        // Import of the agencies referential.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DATA_AGENCIES), AGENCIES_CSV);
        }

        // logbook configuration
        final File logbookConfig = PropertiesUtils.findFile(VitamServerRunner.LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        // get vitamRepository instance.
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        // call the logbook coherence check service
        coherenceCheckService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepository);
        LogbookCheckResult logbookCheckResult = coherenceCheckService.logbookCoherenceCheckByTenant(TENANT_0);
        assertNotNull(logbookCheckResult);

        Set<LogbookCheckEvent> logbookCheckedEvents = logbookCheckResult.getCheckedEvents();
        assertNotNull(logbookCheckedEvents);
        assertFalse(logbookCheckedEvents.isEmpty());

        Set<LogbookCheckError> logbookCheckErrors = logbookCheckResult.getCheckErrors();
        assertNotNull(logbookCheckErrors);
        assertTrue(logbookCheckErrors.isEmpty());
    }

    @Test
    @RunWithCustomExecutor
    public void testLogbookCoherenceCheckSIP_KO_withIncoherentLogbook() throws Exception {
        LOGGER.debug("Starting integration tests for logbook coherence checks.");
        // Import of data
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        importFiles();

        String operationId = doIngestWithLogbookStatus(TENANT_0, SIP_KO_ARBO_RECURSIVE, UNKNOWN);
        verifyOperation(operationId, KO);
        verifyProcessState(operationId, TENANT_0, COMPLETED);

        // logbook configuration
        final File logbookConfig = PropertiesUtils.findFile(VitamServerRunner.LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        // get vitamRepository instance.
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.get();

        // call the logbook coherence check service
        coherenceCheckService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepository);
        LogbookCheckResult logbookCheckResult = coherenceCheckService.logbookCoherenceCheckByTenant(TENANT_0);
        assertNotNull(logbookCheckResult);

        Set<LogbookCheckEvent> logbookCheckedEvents = logbookCheckResult.getCheckedEvents();
        assertNotNull(logbookCheckedEvents);
        assertFalse(logbookCheckedEvents.isEmpty());

        Set<LogbookCheckError> logbookCheckErrors = logbookCheckResult.getCheckErrors();
        assertNotNull(logbookCheckErrors);
        assertFalse(logbookCheckErrors.isEmpty());

        ObjectMapper mapper = new ObjectMapper();
        Set<LogbookCheckError> expectedResults = mapper.readValue(
            PropertiesUtils.getResourceAsStream(EXPECTED_RESULTS_JSON),
            new TypeReference<>() {}
        );

        Assertions.assertThat(logbookCheckErrors).containsAll(expectedResults);
    }

    /**
     * import files.
     */
    private void importFiles() {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DATA_AGENCIES), AGENCIES_CSV);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importFormat(
                PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DROID_SIGNATURE_FILE_V94_XML),
                DROID_SIGNATURE_FILE_V94_XML
            );

            // Import Rules
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importRulesFile(
                PropertiesUtils.getResourceAsStream(HECK_LOGBOOK_MGT_RULES_REF_CSV),
                MGT_RULES_REF_CSV
            );

            // import service agent
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DATA_AGENCIES), AGENCIES_CSV);

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File fileContracts = PropertiesUtils.getResourceFile(REFERENTIAL_CONTRACTS_OK_JSON);
            List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeReference(
                fileContracts,
                new TypeReference<>() {}
            );
            client.importIngestContracts(IngestContractModelList);

            // import contrat
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File fileAccessContracts = PropertiesUtils.getResourceFile(ACCESS_CONTRATS_JSON);
            List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
                fileAccessContracts,
                new TypeReference<>() {}
            );
            client.importAccessContracts(accessContractModelList);

            // Import Security Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importSecurityProfiles(
                JsonHandler.getFromFileAsTypeReference(
                    PropertiesUtils.getResourceFile("integration-logbook/data/security_profile_ok.json"),
                    new TypeReference<>() {}
                )
            );

            // Import Context
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importContexts(
                JsonHandler.getFromFileAsTypeReference(
                    PropertiesUtils.getResourceFile("integration-logbook/data/contexts.json"),
                    new TypeReference<>() {}
                )
            );
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }
}
