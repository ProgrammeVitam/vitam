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
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.processing.integration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.RequestResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

public class PausedProcessingIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PausedProcessingIT.class);

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static final Integer TENANT_ID = 0;
    private static final int DATABASE_PORT = 12346;

    private static final String WORFKLOW_NAME = "DefaultIngestWorkflow";

    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;

    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String SIP_FOLDER = "SIP";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static String CONFIG_WORKER_PATH;
    private static String CONFIG_WORKSPACE_PATH;
    private static String CONFIG_METADATA_PATH;
    private static String CONFIG_PROCESSING_PATH;
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH;
    private static String CONFIG_LOGBOOK_PATH;
    private static String CONFIG_SIEGFRIED_PATH;
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static MetaDataApplication metadataApplication;
    private static WorkerApplication workerApplication;
    private static AdminManagementApplication adminManagementApplication;
    private static LogbookApplication logbookApplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;

    private static JunitHelper.ElasticsearchTestConfiguration configES;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    private static String SIP_FILE_OK_NAME = "integration-processing/SIP-test.zip";

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;

    private boolean imported = false;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.getConfiguration()
            .setData(PropertiesUtils.getResourcePath("integration-processing/").toString());
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-processing/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-processing/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-processing/processing.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-processing/functional-administration.conf").toString();
        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-processing/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();

        configES = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // launch metadata
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        metadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        metadataApplication.start();
        SystemPropertyUtil.clear(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT);

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication(CONFIG_WORKSPACE_PATH);
        workspaceApplication.start();
        SystemPropertyUtil.clear(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch logbook
        SystemPropertyUtil
            .set(LogbookApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        logbookApplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookApplication.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        workerApplication = new WorkerApplication(CONFIG_WORKER_PATH);
        workerApplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin server
        adminManagementApplication = new AdminManagementApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminManagementApplication.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.deleteContainer("process", true);
        if (configES != null) {
            JunitHelper.stopElasticsearchForTest(configES);
        }
        mongod.stop();
        mongodExecutable.stop();
        try {
            workspaceApplication.stop();
            adminManagementApplication.stop();
            workerApplication.stop();
            logbookApplication.stop();
            processManagementApplication.stop();
            metadataApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    private void tryImportFile() {
        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                client
                    .importFormat(
                        PropertiesUtils.getResourceAsStream("integration-processing/DROID_SignatureFile_V88.xml"));

                // Import Rules
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-processing/jeu_donnees_OK_regles_CSV_regles.csv"));

                // import contract
                File fileContracts = PropertiesUtils.getResourceFile("integration-processing/referential_contracts_ok.json");
                List<IngestContractModel> IngestContractModelList = JsonHandler
                    .getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>(){});

                client.importIngestContracts(IngestContractModelList);
            } catch (final Exception e) {
                LOGGER.error(e);
            }
            imported = true;
        }
    }

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, "Process_SIP_unitary", objectId,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Test
    public void testPausedAndPersistedWorkflow() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(TENANT_ID);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
       processingClient.initVitamProcess(LogbookTypeProcess.INGEST.name(), containerName,
            WORFKLOW_NAME);
        // wait a little bit
        Thread.sleep(5000);

        RequestResponse<JsonNode> resp = processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
            LogbookTypeProcess.INGEST.toString(), ProcessAction.NEXT.getValue());
        // wait a little bit
        Thread.sleep(10000);
        assertNotNull(resp);
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        assertEquals(ProcessExecutionStatus.PAUSE.toString(), resp.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS));
        // wait a little bit
        Thread.sleep(5000);

        // shutdown processing
        processManagementApplication.stop();
        // wait a little bit
        Thread.sleep(5000);
        LOGGER.info("After STOP");
        // restart processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        // wait a little bit
        Thread.sleep(5000);
        LOGGER.info("After RE-START");

        // Next on the old paused ans persisted workflow
        Response ret = processingClient.updateOperationActionProcess(ProcessAction.NEXT.getValue(),
            containerName);
        assertNotNull(ret);
        assertEquals(ProcessExecutionStatus.PAUSE.toString(), ret.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS));
        assertEquals(Response.Status.PARTIAL_CONTENT.getStatusCode(), ret.getStatus());
        // wait a little bit
        Thread.sleep(1000);

        ret =  processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(),
            containerName);
        assertNotNull(ret);
        assertEquals(ProcessExecutionStatus.COMPLETED.toString(), ret.getHeaderString(GlobalDataRest
            .X_GLOBAL_EXECUTION_STATUS));
        assertEquals(Response.Status.PARTIAL_CONTENT.getStatusCode(), ret.getStatus());
    }
}
