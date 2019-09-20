package fr.gouv.vitam.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.integration.test.ProcessingIT;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class replace FunctionalAdminIT
 * As it test all referential import
 */
public class DataLoader {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIT.class);

    int tenantId = 0;
    String dataFodler = null;

    public DataLoader(String dataFodler) {
        this.dataFodler = dataFodler;
    }

    public void prepareData() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        VitamThreadPoolExecutor.getDefaultExecutor().submit(() -> {
            try {
                System.err.println("===============  initialize(); =======================");

                initialize();
            } finally {
                countDownLatch.countDown();
            }

        });
        countDownLatch.await(1, TimeUnit.MINUTES);
    }

    public void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    private void initialize() {

        cleanOffers();

        prepareVitamSession();
        LOGGER.error("++++++++ tryImportFile....");
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importFormat(
                PropertiesUtils.getResourceAsStream(dataFodler + "/DROID_SignatureFile_V94.xml"),
                "DROID_SignatureFile_V94.xml");

            // Import ontologies
            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importOntologies(true, JsonHandler
                .getFromFileAsTypeRefence(
                    PropertiesUtils.getResourceFile("ontology.json"),
                    new TypeReference<List<OntologyModel>>() {
                    }));
            VitamThreadUtils.getVitamSession().setTenantId(initialTenant);

            // Import Rules
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importRulesFile(
                PropertiesUtils.getResourceAsStream(dataFodler + "/jeu_donnees_OK_regles_CSV_regles.csv"),
                "jeu_donnees_OK_regles_CSV_regles.csv");
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream("agencies.csv"), "agencies.csv");
            // lets check evdetdata for rules import
            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evType", "STP_IMPORT_RULES"));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            assertNotNull(logbookResult.get("$results").get(0).get("evDetData"));
            assertTrue(JsonHandler.writeAsString(logbookResult.get("$results").get(0).get("evDetData"))
                .contains("jeu_donnees_OK_regles_CSV_regles"));
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

            File fileProfiles = PropertiesUtils.getResourceFile(dataFodler + "/OK_profil.json");
            List<ProfileModel> profileModelList =
                JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>() {
                });
            client.createProfiles(profileModelList);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            RequestResponseOK<ProfileModel> response =
                (RequestResponseOK<ProfileModel>) client.findProfiles(new Select().getFinalSelect());
            client.importProfileFile(response.getResults().get(0).getIdentifier(),
                PropertiesUtils.getResourceAsStream(dataFodler + "/profil_ok.rng"));


            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            File fileContracts =
                PropertiesUtils.getResourceFile(dataFodler + "/referential_contracts_ok.json");
            List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
                new TypeReference<List<IngestContractModel>>() {
                });
            Response.Status importStatus = client.importIngestContracts(IngestContractModelList);

            // import access contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            File fileAccessContracts = PropertiesUtils
                .getResourceFile(dataFodler + "/access_contracts.json");
            List<AccessContractModel> accessContractModelList = JsonHandler
                .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {
                });
            client.importAccessContracts(accessContractModelList);


            // Import Security Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importSecurityProfiles(JsonHandler
                .getFromFileAsTypeRefence(
                    PropertiesUtils.getResourceFile(dataFodler + "/security_profile_ok.json"),
                    new TypeReference<List<SecurityProfileModel>>() {
                    }));

            // Import Context
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importContexts(JsonHandler
                .getFromFileAsTypeRefence(PropertiesUtils.getResourceFile(dataFodler + "/contexts.json"),
                    new TypeReference<List<ContextModel>>() {
                    }));

            // Import Archive Unit Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.createArchiveUnitProfiles(JsonHandler
                .getFromFileAsTypeRefence(
                    PropertiesUtils.getResourceFile(dataFodler + "/archive-unit-profile.json"),
                    new TypeReference<List<ArchiveUnitProfileModel>>() {
                    }));

        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public String doIngest(String zip) throws FileNotFoundException, VitamException {

        String CONTEXT_ID = "DEFAULT_WORKFLOW";
        String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
        WorkFlow workflow = WorkFlow.of(CONTEXT_ID, WORKFLOW_IDENTIFIER, "INGEST");

        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();

        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);

        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(zip);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        waitOperation(VitamServerRunner.NB_TRY, VitamServerRunner.SLEEP_TIME, ingestOperationGuid.getId());
        return ingestOperationGuid.toString();
    }

    public static final String OFFER_FOLDER = "offer";


    /**
     * Clean offers content.
     */
    public static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }
}
