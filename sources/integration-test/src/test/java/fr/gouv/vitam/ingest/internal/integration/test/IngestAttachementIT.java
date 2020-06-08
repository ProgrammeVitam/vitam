/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.ingest.internal.integration.test;

import com.google.common.collect.Sets;
import com.mongodb.client.MongoIterable;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_LOGBOOK;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IngestAttachementIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestAttachementIT.class);

    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";

    private static final String SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET = "integration-processing";


    private static final String SIP_BASE_INIT = "integration-ingest-internal/base_init.zip";
    private static final String SIP_BASE_WITH_UNIT = "integration-ingest-internal/base_with_unit";
    private static final String SIP_BASE_WITH_GOT = "integration-ingest-internal/base_with_got";
    private static final String LINK_AU_TO_EXISTING_GOT = "integration-ingest-internal/LINK_AU_TO_EXISTING_GOT";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(IngestInternalIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                IngestInternalMain.class));

    private static IngestInternalClient client;
    // run workflow until storing OG
    private static ProcessingManagementClient processingManagementClient;

    private final WorkFlow ingestSip =
        WorkFlow.of(Contexts.DEFAULT_WORKFLOW.name(), Contexts.DEFAULT_WORKFLOW.getEventType(), "INGEST");


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);

        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        client = IngestInternalClientFactory.getInstance().getClient();

        processingManagementClient = ProcessingManagementClientFactory.getInstance().getClient();

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass(0, 1);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static void prepareVitamSession(Integer tenantId, String contractId, String contextId) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId(contractId);
        VitamThreadUtils.getVitamSession().setContextId(contextId);
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    private void ingestToStepThenKO() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        initialize(operationGuid);

        InputStream ingestStream = PropertiesUtils.getResourceAsStream(SIP_BASE_INIT);

        client.upload(ingestStream, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.NEXT.getValue());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK, ProcessState.PAUSE);

        for (int i = 0; i <= 5; i++) {
            processingManagementClient
                .executeOperationProcess(operationGuid.toString(), Contexts.DEFAULT_WORKFLOW.toString(),
                    ProcessAction.NEXT.getValue());
            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK, ProcessState.PAUSE);
        }

        // stop workflow
        processingManagementClient.cancelOperationProcessExecution(operationGuid.toString());
        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO, ProcessState.COMPLETED);

    }

    private void initialize(GUID operationGuid) throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        // init default logbook operation
        List<LogbookOperationParameters> params = new ArrayList<>();
        LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);

        // call ingest
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);
    }

    private GUID ingestSip(InputStream sipStream) throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        initialize(operationGuid);
        client.upload(sipStream, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.getValue());
        return operationGuid;
    }

    @RunWithCustomExecutor
    @Test
    public void given_unit_with_opi_KO_when_test_link_unit_to_unit_then_KO() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");

        // simulate a corrupted ingest
        ingestToStepThenKO();

        // prepare 2nd zip to ingest
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        Document unit = resultUnits.first();
        String idUnit = (String) unit.get("_id");
        assertThat(idUnit).isNotNull();

        replaceStringInFile(SIP_BASE_WITH_UNIT + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        InputStream streamSip = createZipFile(SIP_BASE_WITH_UNIT);


        GUID ingestOperationGuid = ingestSip(streamSip);

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid, StatusCode.KO);
        verifyLogbook(ingestOperationGuid.toString());
    }


    @RunWithCustomExecutor
    @Test
    public void given_got_with_opi_KO_when_test_link_got_to_unitthen_KO() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");

        // simulate a corrupted ingest
        ingestToStepThenKO();

        // prepare 2nd zip to ingest
        MongoIterable<Document> resultGots = MetadataCollections.OBJECTGROUP.getCollection().find();
        Document got = resultGots.first();
        String idGOT = (String) got.get("_id");
        assertThat(idGOT).isNotNull();

        replaceStringInFile(SIP_BASE_WITH_GOT + "/manifest.xml",
            "(?<=<MetadataName>).*?(?=</MetadataName>)", "#object");
        replaceStringInFile(SIP_BASE_WITH_GOT + "/manifest.xml",
            "(?<=<MetadataValue>).*?(?=</MetadataValue>)", idGOT);

        InputStream streamSip = createZipFile(SIP_BASE_WITH_GOT);

        GUID ingestOperationGuid = ingestSip(streamSip);

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid, StatusCode.KO);
        verifyLogbook(ingestOperationGuid.toString());
    }

    @RunWithCustomExecutor
    @Test
    public void given_og_with_opi_KO_when_test_link_got_to_unit_then_KO() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");

        // simulate a corrupted ingest
        ingestToStepThenKO();

        // prepare 2nd zip to ingest
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find().skip(1);
        Document unit = resultUnits.first();
        String idGot = (String) unit.get("_og");
        assertThat(idGot).isNotNull();


        replaceStringInFile(LINK_AU_TO_EXISTING_GOT + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);

        InputStream streamSip = createZipFile(LINK_AU_TO_EXISTING_GOT);

        GUID ingestOperationGuid = ingestSip(streamSip);

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid, StatusCode.KO);
        verifyLogbook(ingestOperationGuid.toString());
    }

    private void verifyLogbook(String operationId) {
        Document operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_ATTACHEMENT.KO"));
    }

    private InputStream createZipFile(final String folderToZip) throws IOException {
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";
        String zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(folderToZip), zipPath);

        return new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));
    }

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status) {
        awaitForWorkflowTerminationWithStatus(operationGuid, status, ProcessState.COMPLETED);
    }

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status,
        ProcessState processState) {

        waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(processState, processWorkflow.getState());
        assertEquals(status, processWorkflow.getStatus());
    }

    private void replaceStringInFile(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        Path path = PropertiesUtils.getResourcePath(targetFilename);
        Charset charset = StandardCharsets.UTF_8;

        String content = Files.readString(path, charset);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(charset));
    }

    private void zipFolder(final Path path, final String zipFilePath) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(dir).toString() + "/"));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
