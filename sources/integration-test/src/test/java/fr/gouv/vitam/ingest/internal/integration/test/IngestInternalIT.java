/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.ingest.internal.integration.test;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.jayway.restassured.RestAssured;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client2.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalApplication;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.integration.test.ProcessingIT;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Ingest Internal integration test
 */
public class IngestInternalIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIT.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    static JunitHelper junitHelper;
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_INGEST_INTERNAL = 8095;

    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";
    private static String CONFIG_INGEST_INTERNAL_PATH = "";

    // private static VitamServer workerApplication;
    private static MetaDataApplication medtadataApplication;
    private static WorkerApplication wkrapplication;
    private static AdminManagementApplication adminApplication;
    private static LogbookApplication lgbapplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;
    private static IngestInternalApplication ingestInternalApplication;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private static String SIP_FILE_OK_NAME = "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-ingest-internal/SIP_Conformity_KO.zip";

    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/worker.conf").toString();
        CONFIG_WORKSPACE_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/workspace.conf").toString();
        CONFIG_PROCESSING_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/processing.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/functional-administration.conf").toString();

        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        CONFIG_INGEST_INTERNAL_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/ingest-internal.conf").toString();

        // ES
        config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // launch metadata
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        medtadataApplication.start();
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
        lgbapplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        lgbapplication.start();
        SystemPropertyUtil.clear(LogbookApplication.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch ingest-internal
        SystemPropertyUtil.set("jetty.ingest-internal.port", Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        ingestInternalApplication = new IngestInternalApplication(CONFIG_INGEST_INTERNAL_PATH);
        ingestInternalApplication.start();
        SystemPropertyUtil.clear("jetty.ingest-internal.port");

        // launch functional Admin server
        adminApplication = new AdminManagementApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminApplication.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
        AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient();
        // VitamClientFactory;
        adminClient
            .importFormat(
                PropertiesUtils.getResourceAsStream("integration-ingest-internal/DROID_SignatureFile_V88.xml"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        mongod.stop();
        mongodExecutable.stop();
        try {
            ingestInternalApplication.stop();
            workspaceApplication.stop();
            wkrapplication.stop();
            lgbapplication.stop();
            processManagementApplication.stop();
            medtadataApplication.stop();
            adminApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }


    @Test
    public void testServersStatus() throws Exception {
        try {
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_METADATA;
            RestAssured.basePath = METADATA_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_WORKER;
            RestAssured.basePath = WORKER_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_LOGBOOK;
            RestAssured.basePath = LOGBOOK_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_INGEST_INTERNAL;
            RestAssured.basePath = INGEST_INTERNAL_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testIngestInternal() throws Exception {
        try {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);

            // init default logbook operation
            List<LogbookOperationParameters> params = new ArrayList<>();
            LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            Response response = client.upload(operationGuid, params, zipInputStreamSipObject, CommonMediaType.ZIP);
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @Test(expected = VitamException.class)
    public void testIngestWithManifestIncorrectObjectNumber() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGuid = GUIDFactory.newManifestGUID(0);
        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);

        List<LogbookOperationParameters> params = new ArrayList<>();
        LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", objectGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);
        LOGGER.error(initParameters.toString());

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
        IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.upload(operationGuid, params, zipInputStreamSipObject, CommonMediaType.ZIP);
    }

}
