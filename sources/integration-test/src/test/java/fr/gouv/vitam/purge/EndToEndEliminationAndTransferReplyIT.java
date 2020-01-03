/*
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
package fr.gouv.vitam.purge;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.IngestCleanupObjectGroupReportEntry;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.IngestCleanupUnitReportEntry;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeUnitStatus;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.PRESERVATION;
import static fr.gouv.vitam.worker.core.plugin.dip.StoreExports.TRANSFER_CONTAINER;
import static io.restassured.RestAssured.get;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EndToEndEliminationAndTransferReplyIT extends VitamRuleRunner {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EndToEndEliminationAndTransferReplyIT.class);

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
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BATCH_REPORT_PATH = "/batchreport/v1";
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private static final String SAINT_DENIS_UNIVERSITE_LIGNE_13 = "1_Saint Denis Université (ligne 13)";
    private static final String SAINT_DENIS_BASILIQUE = "Saint Denis Basilique";
    private static final String CARREFOUR_PLEYEL = "Carrefour Pleyel";
    private static final String SAINT_LAZARE = "Saint-Lazare";
    private static final String MARX_DORMOY = "Marx Dormoy";
    private static final String MONTPARNASSE = "Montparnasse.txt";
    private static final String ORIGINATING_AGENCY = "RATP";
    private static final String JSONL = ".jsonl";
    private static final String XML = ".xml";
    private static String link_to_manifest_and_existing_unit =
        "ingestCleanup/link_to_manifest_and_existing_unit";
    private static String link_to_manifest_and_existing_object_group =
        "ingestCleanup/link_to_manifest_and_existing_object_group";
    private static String add_object_to_existing_object_group =
        "ingestCleanup/add_object_to_existing_object_group";
    private static final String ADMIN_MANAGEMENT_URL =
        "http://localhost:" + VitamServerRunner.PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN;
    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";
    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(EndToEndEliminationAndTransferReplyIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                IngestInternalMain.class,
                StorageMain.class,
                DefaultOfferMain.class,
                BatchReportMain.class
            ));
    private static String CONFIG_SIEGFRIED_PATH = "";
    private static String TEST_ELIMINATION_SIP =
        "elimination/TEST_ELIMINATION.zip";
    private static String ELIMINATION_ACCESSION_REGISTER_DETAIL =
        "elimination/accession_regoister_detail.json";
    private static String ELIMINATION_ACCESSION_REGISTER_SUMMARY =
        "elimination/accession_regoister_summary.json";
    private WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");
    private TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE =
        new TypeReference<JsonNode>() {
        };
    private TypeReference<UnitReportEntry> UNIT_REPORT_TYPE_REFERENCE =
        new TypeReference<UnitReportEntry>() {
        };
    private TypeReference<ObjectGroupReportEntry> OG_REPORT_TYPE_REFERENCE =
        new TypeReference<ObjectGroupReportEntry>() {
        };
    private static IngestCleanupAdminService ingestCleanupAdminService;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);


        new DataLoader("integration-ingest-internal").prepareData();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit_metadata =
            new Retrofit.Builder().client(okHttpClient).baseUrl(ADMIN_MANAGEMENT_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();

        ingestCleanupAdminService = retrofit_metadata.create(IngestCleanupAdminService.class);
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()

        ));

        runAfterEs(Sets.newHashSet(
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_0",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_1",
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase()
        ));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isNotRunning(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_BATCH_REPORT;
        RestAssured.basePath = BATCH_REPORT_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void testEliminationAction() throws Exception {
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // elimination action
        final String eliminationActionOperationGuid = newOperationLogbookGUID(tenantId).toString();
        VitamThreadUtils.getVitamSession().setRequestId(eliminationActionOperationGuid);

        SelectMultiQuery analysisDslRequest = new SelectMultiQuery();
        analysisDslRequest
            .addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2018-01-01", analysisDslRequest.getFinalSelect());

        final RequestResponse<JsonNode> actionResult =
            accessInternalClient.startEliminationAction(eliminationRequestBody);

        assertThat(actionResult.isOk()).isTrue();

        awaitForWorkflowTerminationWithStatus(eliminationActionOperationGuid, StatusCode.WARNING);

        // DSL check
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(remainingUnits.getResults()).hasSize(3);
        assertThat(remainingGots.getResults()).hasSize(2);

        Set<String> remainingObjectIds = getBinaryObjectIds(remainingGots);
        assertThat(remainingObjectIds).hasSize(2);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals(ELIMINATION_ACCESSION_REGISTER_DETAIL, JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find())),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals(ELIMINATION_ACCESSION_REGISTER_SUMMARY,
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
            excludeFields);

        // Check remaining units / gots & objects ids (low level)

        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Map<String, JsonNode> ingestedUnitsByTitle = mapByField(ingestedUnits, "Title");

        Set<String> expectedRemainingUnitIds = new HashSet<>(Arrays.asList(
            getId(ingestedUnitsByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13)),
            getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE)),
            getId(ingestedUnitsByTitle.get(CARREFOUR_PLEYEL))
        ));
        Set<String> expectedDeletedUnitIds = SetUtils.difference(ingestedUnitIds, expectedRemainingUnitIds);

        for (String id : expectedRemainingUnitIds) {
            checkUnitExistence(id, true);
        }

        for (String id : expectedDeletedUnitIds) {
            checkUnitExistence(id, false);
        }

        Set<String> ingestedGotIds = getIds(ingestedGots);
        Set<String> expectedRemainingGotIds = new HashSet<>(Arrays.asList(
            getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13)),
            getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE))
        ));
        Set<String> expectedDeletedGotIds = SetUtils.difference(ingestedGotIds, expectedRemainingGotIds);

        for (String id : expectedRemainingGotIds) {
            checkObjectGroupExistence(id, true);
        }

        for (String id : expectedDeletedGotIds) {
            checkObjectGroupExistence(id, false);
        }

        Set<String> expectedRemainingObjectIds = ingestedGots.getResults().stream()
            .filter(got -> expectedRemainingGotIds.contains(getId(got)))
            .flatMap(got -> getBinaryObjectIds(got).stream())
            .collect(Collectors.toSet());
        Set<String> expectedDeletedObjectIds = SetUtils.difference(ingestedObjectIds, expectedRemainingObjectIds);


        for (String id : expectedRemainingObjectIds) {
            checkObjectExistence(id, true);
        }

        for (String id : expectedDeletedObjectIds) {
            checkObjectExistence(id, false);
        }

        // Check detached GOT

        String detachedGotId = getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE));

        JsonNode detachedGotBeforeElimination = getById(ingestedGots, detachedGotId);
        assertThat(detachedGotBeforeElimination.get(VitamFieldsHelper.unitups())).hasSize(2);
        assertThat(detachedGotBeforeElimination.get(VitamFieldsHelper.allunitups())).hasSize(6);

        JsonNode detachedGotAfterElimination = getById(remainingGots, detachedGotId);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.version()).asInt()).isEqualTo(
            detachedGotBeforeElimination.get(VitamFieldsHelper.version()).asInt() + 1);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.unitups())).hasSize(1);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.allunitups())).hasSize(2);

        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.unitups()).get(0).asText()).isEqualTo(
            getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE)));

        // Check report
        try (InputStream reportInputStream = readStoredReport(eliminationActionOperationGuid + JSONL)) {

            checkEliminationReport(reportInputStream, ingestedUnits, ingestedUnitsByTitle, ingestOperationGuid,
                ingestedGots,
                detachedGotId);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferReply() throws Exception {
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Set<String> ingestedObjectGroupIds = getIds(ingestedObjectGroups);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedObjectGroups.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedObjectGroups);
        assertThat(ingestedObjectIds).hasSize(3);

        // Export transfer archive
        String transferOperation = transfer(ingestedUnitIds, StatusCode.OK);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation, ingestedUnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation, ingestedUnitIds, Collections.emptyList());

        // Get exported SIP from transfer request and ingest it
        String transferredSipIngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation)) {
            transferredSipIngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Check ingested transferred metadata match initial exported ones
        RequestResponseOK<JsonNode> ingestedTransferredUnits =
            selectUnitsByOpi(transferredSipIngestOperationId, accessInternalClient);
        RequestResponseOK<JsonNode> ingestedTransferredObjectGroups =
            selectGotsByOpi(transferredSipIngestOperationId, accessInternalClient);

        compareMetadata(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, transferredSipIngestOperationId,
            ingestedTransferredUnits, ingestedTransferredObjectGroups);

        // Send ATR back for transfer reply workflow
        String transferReplyOperationId;
        try (InputStream atrInputStream = readStoredReport(transferredSipIngestOperationId + XML)) {
            transferReplyOperationId = startTransferReplyWorkflow(atrInputStream, StatusCode.OK);
        }

        List<String> transferPurgedTransferUnitIds = new ArrayList<>(ingestedUnitIds);
        List<String> transferAlreadyDeletedUnitIds = Collections.emptyList();
        List<String> transferNonDeletableUnitIds = Collections.emptyList();
        List<String> transferDeletedObjectGroups = new ArrayList<>(ingestedObjectGroupIds);
        List<String> transferDetachedObjectGroups = Collections.emptyList();

        // Check transfer result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds,
            transferReplyOperationId, transferReplyOperationId, transferPurgedTransferUnitIds,
            transferAlreadyDeletedUnitIds,
            transferNonDeletableUnitIds, transferDeletedObjectGroups, transferDetachedObjectGroups,
            accessInternalClient);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("transfer/reply/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("transfer/reply/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
            excludeFields);
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferReplyComplex() throws Exception {
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Set<String> ingestedObjectGroupIds = getIds(ingestedObjectGroups);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedObjectGroups.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedObjectGroups);
        assertThat(ingestedObjectIds).hasSize(3);

        Map<String, JsonNode> ingestedUnitsByTitle = mapByField(ingestedUnits, "Title");

        // Transfer operation 1 : 1 single unit with 1 object group
        String transfer1UnitId = getId(ingestedUnitsByTitle.get(MONTPARNASSE));
        List<String> transfer1UnitIds = Collections.singletonList(transfer1UnitId);
        String transferOperation1 = transfer(transfer1UnitIds, StatusCode.OK);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation1, transfer1UnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation1, transfer1UnitIds, Collections.emptyList());

        // Export transfer archive ==> WARNING (contains 1 unit already being transferred)
        String transfer2UnitId1 = getId(ingestedUnitsByTitle.get(MONTPARNASSE));
        String transfer2UnitId2 = getId(ingestedUnitsByTitle.get(MARX_DORMOY));
        String transfer2UnitId3 = getId(ingestedUnitsByTitle.get(SAINT_LAZARE));
        String transfer2UnitId4 = getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE));
        List<String> transfer2UnitIds =
            Arrays.asList(transfer2UnitId1, transfer2UnitId2, transfer2UnitId3, transfer2UnitId4);
        String transferOperation2 = transfer(transfer2UnitIds, StatusCode.WARNING);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation2, transfer2UnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation2,
            Arrays.asList(transfer2UnitId2, transfer2UnitId3, transfer2UnitId4),
            Collections.singletonList(transfer2UnitId1));

        // Get exported SIP from transfer request 1 and ingest it
        String transferredSip1IngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation1)) {
            transferredSip1IngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Send ATR back for transfer reply workflow 1 ==> Will delete 1 unit & detach 1 object group
        String transferReplyOperationId1;
        try (InputStream atrInputStream = readStoredReport(transferredSip1IngestOperationId + XML)) {
            transferReplyOperationId1 = startTransferReplyWorkflow(atrInputStream, StatusCode.OK);
        }

        List<String> transfer1PurgedTransferUnitIds = transfer1UnitIds;
        List<String> transfer1AlreadyDeletedUnitIds = Collections.emptyList();
        List<String> transfer1NonDeletableUnitIds = Collections.emptyList();
        List<String> transfer1DeletedObjectGroups = Collections.emptyList();
        List<String> transfer1DetachedObjectGroups =
            Collections.singletonList(getObjectGroupId(getById(ingestedUnits, transfer1UnitId)));

        // Check transfer 1 result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds,
            transferReplyOperationId1, transferReplyOperationId1, transfer1PurgedTransferUnitIds,
            transfer1AlreadyDeletedUnitIds,
            transfer1NonDeletableUnitIds, transfer1DeletedObjectGroups, transfer1DetachedObjectGroups,
            accessInternalClient);

        // Get exported SIP from transfer request 2 and ingest it
        String transferredSip2IngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation2)) {
            transferredSip2IngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Send ATR back for transfer reply workflow 2 ==> Will delete 2/3 units & 1 object group
        String transferReplyOperationId2;
        try (InputStream atrInputStream = readStoredReport(transferredSip2IngestOperationId + XML)) {
            // 1 units could not be deleted ==> WARNING
            transferReplyOperationId2 = startTransferReplyWorkflow(atrInputStream, StatusCode.WARNING);
        }

        List<String> transfer2PurgedTransferUnitIds = Arrays.asList(transfer2UnitId2, transfer2UnitId3);
        List<String> transfer2AlreadyDeletedUnitIds = Collections.singletonList(transfer2UnitId1);
        List<String> transfer2NonDeletableUnitIds = Collections.singletonList(transfer2UnitId4);
        List<String> transfer2DeletedObjectGroups =
            Collections.singletonList(getObjectGroupId(getById(ingestedUnits, transfer2UnitId2)));
        List<String> transfer2DetachedObjectGroups = Collections.emptyList();

        // Check transfer 2 result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds, transferReplyOperationId1,
            transferReplyOperationId2, transfer2PurgedTransferUnitIds, transfer2AlreadyDeletedUnitIds,
            transfer2NonDeletableUnitIds, transfer2DeletedObjectGroups, transfer2DetachedObjectGroups,
            accessInternalClient);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("transfer/reply/accession_register_detail_complex_test.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);
        // Check Accession Register Summary
        assertJsonEquals("transfer/reply/accession_register_summary_complex_test.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
            excludeFields);
    }

    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestRunningIngestThenKO() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        // Then
        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // No report
        checkNoIngestCleanupReport(ingestCleanupActionOperationGuid);

        // Cleanup
        killProcess(ingestOperationGuid);
    }

    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestCompletedOKIngestThenKO() throws Exception {
        // Given
        String ingestOperationGuid =
            doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP), StatusCode.OK);

        // When : Run ingest cleanup process

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        // Then
        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // No report
        checkNoIngestCleanupReport(ingestCleanupActionOperationGuid);
    }

    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestKilledIngestAfterAccessionRegistersThenOK() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Check ingested units / gots / object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // When : Run ingest cleanup process

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.OK);

        // Ensure data purged via DSL
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingUnits.getResults()).isEmpty();
        assertThat(remainingObjectGroups.getResults()).isEmpty();

        // Low level check of units / object groups & objects existence
        for (String id : getIds(ingestedUnits)) {
            checkUnitExistence(id, false);
        }

        for (String id : getIds(ingestedGots)) {
            checkObjectGroupExistence(id, false);
            for (String objectId : getBinaryObjectIds(getById(ingestedGots, id))) {
                checkObjectExistence(objectId, false);
            }
        }

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            getIds(ingestedUnits).stream().collect(Collectors.toMap(i -> i, i -> StatusCode.OK)),
            getIds(ingestedGots).stream().collect(Collectors.toMap(i -> i, i -> StatusCode.OK)),
            ingestedGots.getResults().stream().collect(toMap(this::getId, this::getBinaryObjectIds))
        );
    }

    /**
     * checkChildUnitsFromOtherIngests
     * My unit has been update by another process (batch update, preservation...)
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestUpdatedUnitsThenWarning() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Check ingested units / gots / object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // When : Run ingest cleanup process

        updateUnits(ingestOperationGuid);

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.WARNING);

        // Ensure data purged via DSL
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingUnits.getResults()).isEmpty();
        assertThat(remainingObjectGroups.getResults()).isEmpty();

        // Low level check of units / object groups & objects existence
        for (String id : getIds(ingestedUnits)) {
            checkUnitExistence(id, false);
        }

        for (String id : getIds(ingestedGots)) {
            checkObjectGroupExistence(id, false);
            for (String objectId : getBinaryObjectIds(getById(ingestedGots, id))) {
                checkObjectExistence(objectId, false);
            }
        }

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            getIds(ingestedUnits).stream().collect(Collectors.toMap(i -> i, i -> StatusCode.WARNING)),
            getIds(ingestedGots).stream().collect(Collectors.toMap(i -> i, i -> StatusCode.OK)),
            ingestedGots.getResults().stream().collect(toMap(this::getId, this::getBinaryObjectIds))
        );
    }

    /**
     * testCleanupIngestUnitsWithChildUnitAttachmentThenKO
     * Another unit has been attached to my unit (ingest, reclassification...)
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestUnitsWithChildUnitAttachmentThenKO() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Check ingested units / gots / object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // Create SIP with attachment to unit
        String montparnasseUnitId = ingestedUnits.getResults().stream()
            .filter(unit -> getTitle(unit).equals(MONTPARNASSE))
            .map(this::getId)
            .findFirst().orElseThrow(() -> new IllegalStateException("Unkown unit " + MONTPARNASSE));

        String zipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        replaceStringInFile(link_to_manifest_and_existing_unit + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            montparnasseUnitId);
        zipFolder(PropertiesUtils.getResourcePath(link_to_manifest_and_existing_unit), zipName);

        // Ingest "child" SIP
        doIngest(new FileInputStream(zipName), StatusCode.WARNING);

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final RequestResponseOK<JsonNode> remainingIngestedUnits =
            selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingIngestedGots =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(remainingIngestedUnits.getResults()).hasSize(6);
        assertThat(remainingIngestedGots.getResults()).hasSize(3);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            ImmutableMap.of(montparnasseUnitId, StatusCode.KO),
            emptyMap(), emptyMap()
        );
    }

    /**
     * checkObjectGroupUpdatesFromOtherOperations [INGEST]
     * An ingest attached a unit to my object group
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestObjectGroupWithAttachedUnitFromAnotherIngestThenKO() throws Exception {

        // Given
        String ingestOperationGuid = doIngestStepByStepUntilStepReached(
            PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP), "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        String objectGroupId = getId(selectGotsByOpi(ingestOperationGuid, accessInternalClient)
            .getFirstResult());

        // Ingest attached unit to my object group
        String zipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        replaceStringInFile(link_to_manifest_and_existing_object_group + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            objectGroupId);
        zipFolder(PropertiesUtils.getResourcePath(link_to_manifest_and_existing_object_group), zipName);

        doIngest(new FileInputStream(zipName), StatusCode.WARNING);

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(remainingUnits.getResults()).hasSize(6);
        assertThat(remainingGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(remainingGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_5_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_5_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            emptyMap(),
            ImmutableMap.of(objectGroupId, StatusCode.KO),
            emptyMap()
        );
    }

    /**
     * checkObjectGroupUpdatesFromOtherOperations [INGEST]
     * An ingest added a binary to my object group
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestObjectGroupWithAttachedObjectFromAnotherIngestToExistingObjectGroupThenKO()
        throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Attach an object to one of its object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode objectGroup = selectGotsByOpi(ingestOperationGuid, accessInternalClient)
            .getFirstResult();
        String objectGroupId = getId(objectGroup);
        String unitId = getParentUnitIds(objectGroup).iterator().next();

        String zipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        replaceStringInFile(add_object_to_existing_object_group + "/manifest.xml",
            "(?<=<SystemId>).*?(?=</SystemId>)",
            unitId);
        zipFolder(PropertiesUtils.getResourcePath(add_object_to_existing_object_group), zipName);

        doIngest(new FileInputStream(zipName), StatusCode.WARNING);

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(remainingUnits.getResults()).hasSize(6);
        assertThat(remainingGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(remainingGots);
        // 3 initial objects + 1 attached object from 2nd SIP
        assertThat(ingestedObjectIds).hasSize(4);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_4_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_4_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            emptyMap(),
            ImmutableMap.of(objectGroupId, StatusCode.KO),
            emptyMap()
        );
    }

    /**
     * checkObjectAttachmentsToExistingObjectGroups
     * My ingest added a binary to another existing object group
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestObjectAttachedToExistingObjectGroupThenKO() throws Exception {

        // Given
        String initialIngestOperationGuid =
            doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP), StatusCode.OK);

        // Ingest another SIP that add an object to an existing object group
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode initialObjectGroup = selectGotsByOpi(initialIngestOperationGuid, accessInternalClient)
            .getFirstResult();
        String initialObjectGroupId = getId(initialObjectGroup);
        String initialUnitId = getParentUnitIds(initialObjectGroup).iterator().next();

        String zipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        replaceStringInFile(add_object_to_existing_object_group + "/manifest.xml",
            "(?<=<SystemId>).*?(?=</SystemId>)",
            initialUnitId);
        zipFolder(PropertiesUtils.getResourcePath(add_object_to_existing_object_group), zipName);

        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(new FileInputStream(zipName), "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        final RequestResponseOK<JsonNode> ingestedUnits =
            selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(ingestedUnits.getResults()).hasSize(1);

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final RequestResponseOK<JsonNode> remainingIngestedUnits =
            selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingIngestedUnits.getResults()).hasSize(1);


        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_3_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_3_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            emptyMap(),
            ImmutableMap.of(initialObjectGroupId, StatusCode.KO),
            emptyMap()
        );
    }

    /**
     * checkObjectAttachmentsToExistingObjectGroups
     * My ingest attached a unit to another existing object group
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestUnitAttachedToExistingObjectGroupThenKO() throws Exception {

        // Given
        String initialIngestOperationGuid =
            doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP), StatusCode.OK);

        // Initial ingest of an object group
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        String initialObjectGroupId = getId(selectGotsByOpi(initialIngestOperationGuid, accessInternalClient)
            .getFirstResult());

        // Ingest attached unit to existing object group
        String zipName = tempFolder.getRoot().getAbsolutePath() + "/" + GUIDFactory.newGUID().getId() + ".zip";
        replaceStringInFile(link_to_manifest_and_existing_object_group + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            initialObjectGroupId);
        zipFolder(PropertiesUtils.getResourcePath(link_to_manifest_and_existing_object_group), zipName);

        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(new FileInputStream(zipName), "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        final RequestResponseOK<JsonNode> ingestedUnits =
            selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(ingestedUnits.getResults()).hasSize(1);

        // When : Run ingest cleanup process
        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.KO);

        // Ensure no cleanup occurred
        final RequestResponseOK<JsonNode> remainingIngestedUnits =
            selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingIngestedUnits.getResults()).hasSize(1);


        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail_2_no_cleanup.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary_2_no_cleanup.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "Service_producteur")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            emptyMap(),
            ImmutableMap.of(initialObjectGroupId, StatusCode.KO),
            emptyMap()
        );
    }

    /**
     * checkObjectGroupUpdatesFromOtherOperations [NOT AN INGEST]
     * My object group has been updated by some preservation workflow (metadata extract, binary conversion...)
     */
    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestObjectGroupUpdatedByAnotherProcessThenWarning() throws Exception {
        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Update object group by another operation (simulate preservation operation)
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode objectGroup = selectGotsByOpi(ingestOperationGuid, accessInternalClient)
            .getFirstResult();
        String objectGroupId = getId(objectGroup);

        final String dummyPreservationOperationGuid = newOperationLogbookGUID(tenantId).toString();
        VitamThreadUtils.getVitamSession().setRequestId(dummyPreservationOperationGuid);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()) {

            String message = VitamLogbookMessages.getLabelOp(PRESERVATION.getEventType() + ".STARTED") + " : " +
                GUIDReader.getGUID(dummyPreservationOperationGuid);

            LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                GUIDReader.getGUID(dummyPreservationOperationGuid),
                PRESERVATION.getEventType(),
                GUIDReader.getGUID(dummyPreservationOperationGuid),
                LogbookTypeProcess.PRESERVATION,
                STARTED,
                message,
                GUIDReader.getGUID(dummyPreservationOperationGuid)
            );
            logbookOperationsClient.create(initParameters);

            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addHintFilter(OBJECTGROUPS.exactToken());
            query.addActions(UpdateActionHelper.set("var", "some value"));
            query.addActions(UpdateActionHelper.set(VitamFieldsHelper.operations(), dummyPreservationOperationGuid));
            metaDataClient.updateObjectGroupById(query.getFinalUpdate(), objectGroupId);
        }

        // Check ingested units / gots / object groups
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // When : Run ingest cleanup process

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.WARNING);

        // Ensure data purged via DSL
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingUnits.getResults()).isEmpty();
        assertThat(remainingObjectGroups.getResults()).isEmpty();

        // Low level check of units / object groups & objects existence
        for (String id : getIds(ingestedUnits)) {
            checkUnitExistence(id, false);
        }

        for (String id : getIds(ingestedGots)) {
            checkObjectGroupExistence(id, false);
            for (String objectId : getBinaryObjectIds(getById(ingestedGots, id))) {
                checkObjectExistence(objectId, false);
            }
        }

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        // Check report
        checkIngestCleanupReport(ingestCleanupActionOperationGuid, ingestOperationGuid,
            getIds(ingestedUnits).stream().collect(Collectors.toMap(i -> i, i -> StatusCode.OK)),
            ingestedGots.getResults().stream().collect(Collectors.toMap(
                this::getId, i-> getId(i).equals(objectGroupId) ? StatusCode.WARNING : StatusCode.OK)),
            ingestedGots.getResults().stream().collect(toMap(this::getId, this::getBinaryObjectIds))
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testDoubleIngestCleanupThenWarning() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_ACCESSION_REGISTRATION");
        killProcess(ingestOperationGuid);

        // Check ingested units / gots / object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // When : Run 2x ingest cleanup process

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();
        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.OK);

        retrofit2.Response<Void> actionResult2 =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();
        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid2 = actionResult2.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid2, StatusCode.WARNING);

        // Ensure data purged via DSL
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingUnits.getResults()).isEmpty();
        assertThat(remainingObjectGroups.getResults()).isEmpty();

        // Low level check of units / object groups & objects existence
        for (String id : getIds(ingestedUnits)) {
            checkUnitExistence(id, false);
        }

        for (String id : getIds(ingestedGots)) {
            checkObjectGroupExistence(id, false);
            for (String objectId : getBinaryObjectIds(getById(ingestedGots, id))) {
                checkObjectExistence(objectId, false);
            }
        }

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("ingestCleanup/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("ingestCleanup/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP")))),
            excludeFields);

        checkIngestCleanupReport(ingestCleanupActionOperationGuid2, ingestOperationGuid, emptyMap(), emptyMap(),
            emptyMap());
    }

    @RunWithCustomExecutor
    @Test
    public void testCleanupIngestKilledIngestBeforeAccessionRegistersThenOK() throws Exception {

        // Given
        String ingestOperationGuid =
            doIngestStepByStepUntilStepReached(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
                "STP_UPDATE_OBJECT_GROUP");
        killProcess(ingestOperationGuid);

        // Check ingested units / gots / object groups
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getBinaryObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // When : Run ingest cleanup process

        retrofit2.Response<Void> actionResult =
            ingestCleanupAdminService.startIngestCleanupWorkflow(ingestOperationGuid, tenantId,
                getBasicAuthnToken()).execute();

        assertThat(actionResult.isSuccessful()).isTrue();
        String ingestCleanupActionOperationGuid = actionResult.headers().get(GlobalDataRest.X_REQUEST_ID);

        // Then
        awaitForWorkflowTerminationWithStatus(ingestCleanupActionOperationGuid, StatusCode.OK);

        // Ensure data purged via DSL
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        assertThat(remainingUnits.getResults()).isEmpty();
        assertThat(remainingObjectGroups.getResults()).isEmpty();

        // Low level check of units / object groups & objects existence
        for (String id : getIds(ingestedUnits)) {
            checkUnitExistence(id, false);
        }

        for (String id : getIds(ingestedGots)) {
            checkObjectGroupExistence(id, false);
            for (String objectId : getBinaryObjectIds(getById(ingestedGots, id))) {
                checkObjectExistence(objectId, false);
            }
        }

        // Check Accession Register Detail
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments()).isEqualTo(0);
        assertThat(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().countDocuments()).isEqualTo(0);
    }

    private void checkIngestCleanupReport(String ingestCleanupOperationId, String opi,
        Map<String, StatusCode> expectedUnitStatusMap, Map<String, StatusCode> expectedObjectGroupStatusMap,
        Map<String, Set<String>> expectedObjectIdsByObjectGroupIds)
        throws IOException, StorageNotFoundException, StorageServerClientException, InvalidParseOperationException {

        try (InputStream reportInputStream = readStoredReport(ingestCleanupOperationId + JSONL);
            JsonLineGenericIterator<JsonNode> reportIterator = new JsonLineGenericIterator<>(reportInputStream,
                JSON_NODE_TYPE_REFERENCE)) {

            JsonAssert.assertJsonEquals(
                reportIterator.next(), JsonHandler.createObjectNode().put("ingestOperationId", opi));

            Map<String, StatusCode> actualUnitStatusMap = new HashMap<>();
            Map<String, StatusCode> actualObjectGroupStatusMap = new HashMap<>();
            Map<String, Set<String>> actualObjectIdsByObjectGroupIds = new HashMap<>();

            for (int i = 0; i < expectedUnitStatusMap.size(); i++) {
                IngestCleanupUnitReportEntry entry =
                    JsonHandler
                        .getFromJsonNode(reportIterator.next().get("params"), IngestCleanupUnitReportEntry.class);
                actualUnitStatusMap.put(entry.getId(), entry.getStatus());
                if (entry.getStatus() == StatusCode.KO) {
                    assertThat(entry.getErrors()).isNotEmpty();
                }
                if (entry.getStatus() == StatusCode.WARNING) {
                    assertThat(entry.getWarnings()).isNotEmpty();
                }
            }
            assertThat(actualUnitStatusMap).isEqualTo(expectedUnitStatusMap);

            for (int i = 0; i < expectedObjectGroupStatusMap.size(); i++) {
                IngestCleanupObjectGroupReportEntry entry =
                    JsonHandler.getFromJsonNode(reportIterator.next().get("params"),
                        IngestCleanupObjectGroupReportEntry.class);
                actualObjectGroupStatusMap.put(entry.getId(), entry.getStatus());
                if (entry.getObjects() != null) {
                    actualObjectIdsByObjectGroupIds.put(entry.getId(), new HashSet<>(entry.getObjects()));
                }

                if (entry.getStatus() == StatusCode.KO) {
                    assertThat(entry.getErrors()).isNotEmpty();
                }
                if (entry.getStatus() == StatusCode.WARNING) {
                    assertThat(entry.getWarnings()).isNotEmpty();
                }
            }
            assertThat(actualObjectGroupStatusMap).isEqualTo(expectedObjectGroupStatusMap);
            assertThat(actualObjectIdsByObjectGroupIds).isEqualTo(expectedObjectIdsByObjectGroupIds);
        }
    }

    private String doIngestStepByStepUntilStepReached(InputStream zipInputStreamSipObject, String targetStepName)
        throws VitamException, InterruptedException {
        final GUID ingestOperationGuid = newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.NEXT.name());

        runStepByStepUntilStepReached(ingestOperationGuid.getId(), targetStepName);
        return ingestOperationGuid.getId();
    }

    private void replaceStringInFile(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        java.nio.file.Path path = PropertiesUtils.getResourcePath(targetFilename);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(charset));
    }


    private void zipFolder(final java.nio.file.Path path, final String zipFilePath) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(path, new SimpleFileVisitor<java.nio.file.Path>() {
                public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
                    throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs)
                    throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(dir).toString() + "/"));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void runStepByStepUntilStepReached(String operationGuid, String targetStepName)
        throws VitamClientException, InternalServerException, InterruptedException {

        try (ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {

            while (true) {

                ProcessQuery processQuery = new ProcessQuery();
                processQuery.setId(operationGuid);
                RequestResponse<ProcessDetail> response = processingClient.listOperationsDetails(processQuery);

                assertThat(response.isOk()).isTrue();
                ProcessDetail processDetail = ((RequestResponseOK<ProcessDetail>) response).getResults().get(0);

                switch (ProcessState.valueOf(processDetail.getGlobalState())) {

                    case PAUSE:

                        if (processDetail.getPreviousStep().equals(targetStepName)) {
                            LOGGER.info(operationGuid + " finished step " + targetStepName);
                            return;
                        }

                        processingClient
                            .executeOperationProcess(operationGuid, Contexts.DEFAULT_WORKFLOW.name(),
                                ProcessAction.NEXT.getValue());

                        break;
                    case RUNNING:

                        // Sleep and retry
                        TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);

                        break;
                    case COMPLETED:
                        tryLogLogbookOperation(operationGuid);
                        tryLogATR(operationGuid);
                        fail("Process completion unexpected " + JsonHandler.unprettyPrint(processDetail));
                        break;
                }
            }
        }
    }

    private void updateUnits(String ingestOperationGuid)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        NoWritingPermissionException, AccessUnauthorizedException {
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));
        updateMultiQuery.addActions(UpdateActionHelper.set("var", "value"));
        String updateRequestId = GUIDFactory.newRequestIdGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(updateRequestId);
        RequestResponse<JsonNode> updateResponse =
            accessInternalClient.updateUnits(updateMultiQuery.getFinalUpdate());
        assertThat(updateResponse.isOk()).isTrue();
        awaitForWorkflowTerminationWithStatus(updateRequestId, StatusCode.OK);
    }

    private void killProcess(String operationGuid)
        throws VitamClientException, InternalServerException {

        try (ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {
            processingClient.cancelOperationProcessExecution(operationGuid);
            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.FATAL);
        }
    }

    private void checkNoIngestCleanupReport(String ingestCleanupActionOperationGuid) {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            assertThatThrownBy(() -> storageClient
                .getContainerAsync(VitamConfiguration.getDefaultStrategy(), ingestCleanupActionOperationGuid + JSONL,
                    DataCategory.REPORT, AccessLogUtils
                        .getNoLogAccessLog()))
                .isInstanceOf(StorageNotFoundException.class);
        }
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    private void checkTransferResult(String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedObjectGroups, Set<String> ingestedUnitIds,
        Set<String> ingestedObjectGroupIds, String transferReplyOperationId1, String transferReplyOperationId,
        List<String> expectedDeletedUnitIds, List<String> expectedAlreadyDeletedUnitIds,
        List<String> expectedNonDeletableUnitIds, List<String> expectedDeletedObjectGroups,
        List<String> expectedDetachedObjectGroups,
        AccessInternalClient accessInternalClient)
        throws StorageNotFoundClientException, StorageServerClientException, IOException, StorageNotFoundException,
        BadRequestException, InvalidParseOperationException, AccessUnauthorizedException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidCreateOperationException,
        LogbookClientException {

        // Check remaining units / object groups
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(getIds(remainingUnits)).containsExactlyInAnyOrderElementsOf(
            ListUtils.removeAll(getIds(ingestedUnits),
                ListUtils.union(expectedAlreadyDeletedUnitIds, expectedDeletedUnitIds)));
        assertThat(getIds(remainingObjectGroups)).containsExactlyInAnyOrderElementsOf(
            ListUtils.removeAll(getIds(ingestedObjectGroups), expectedDeletedObjectGroups)
        );

        // Low level check of units / object groups & objects existence

        for (String id : ingestedUnitIds) {
            boolean shouldBePurged = expectedDeletedUnitIds.contains(id)
                || expectedAlreadyDeletedUnitIds.contains(id);
            checkUnitExistence(id, !shouldBePurged);
        }

        for (String id : ingestedObjectGroupIds) {
            boolean shouldBePurged = expectedDeletedObjectGroups.contains(id);
            checkObjectGroupExistence(id, !shouldBePurged);
            for (String objectId : getBinaryObjectIds(getById(ingestedObjectGroups, id))) {
                checkObjectExistence(objectId, !shouldBePurged);
            }
        }

        // Ensure exported units LFC modified
        LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        List<JsonNode> unitLifeCycles =
            logbookLifeCyclesClient.getRawUnitLifeCycleByIds(new ArrayList<>(expectedNonDeletableUnitIds));
        for (JsonNode unitLifeCycle : unitLifeCycles) {
            assertThat(unitLifeCycle.get("events").get(unitLifeCycle.get("events").size() - 1)
                .get("outDetail").asText()).isEqualTo("LFC.UNIT_DELETION_ABORT.OK");
        }

        // Check SIP transfer is deleted after atr reception
        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        assertThatThrownBy(() ->
            workspaceClient.getObject(TRANSFER_CONTAINER, tenantId + "/" + transferReplyOperationId1))
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // Check transfer reply report
        checkTransferReplyReport(transferReplyOperationId, ingestOperationGuid,
            expectedDeletedUnitIds, expectedAlreadyDeletedUnitIds, expectedNonDeletableUnitIds,
            expectedDeletedObjectGroups, expectedDetachedObjectGroups,
            ingestedUnits, ingestedObjectGroups);
    }

    private void checkTransferReplyReport(String transferReplyOperationId, String ingestOperationGuid,
        List<String> expectedDeletedUnitIds, List<String> expectedAlreadyDeletedUnitIds,
        List<String> expectedNonDeletableUnitIds, List<String> expectedDeletedObjectGroupIds,
        List<String> expectedPartiallyDeletedObjectGroupIds, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedObjectGroups)

        throws IOException, StorageNotFoundException, StorageServerClientException {

        try (InputStream reportInputStream = readStoredReport(transferReplyOperationId + JSONL);
            JsonLineGenericIterator<JsonNode> reportIterator = new JsonLineGenericIterator<>(reportInputStream,
                JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            int unitCount = expectedDeletedUnitIds.size() + expectedAlreadyDeletedUnitIds.size() +
                expectedNonDeletableUnitIds.size();

            Map<String, UnitReportEntry> unitReportById = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, unitCount),
                    entry -> asTypeReference(entry, UNIT_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> entry.id, entry -> entry));

            assertThat(unitReportById.values()).allMatch(i -> "Unit".equals(i.params.type));
            assertThat(unitReportById.keySet()).containsExactlyInAnyOrderElementsOf(
                ListUtils.union(expectedDeletedUnitIds,
                    ListUtils.union(expectedAlreadyDeletedUnitIds, expectedNonDeletableUnitIds)));
            for (Map.Entry<String, UnitReportEntry> entry : unitReportById.entrySet()) {
                assertThat(entry.getValue().params.id).isEqualTo(entry.getKey());

                String expectedStatus = expectedAlreadyDeletedUnitIds.contains(entry.getKey()) ? "ALREADY_DELETED" :
                    expectedDeletedUnitIds.contains(entry.getKey()) ? "DELETED" : "NON_DESTROYABLE_HAS_CHILD_UNITS";
                assertThat(entry.getValue().params.status).isEqualTo(expectedStatus);

                if (!expectedAlreadyDeletedUnitIds.contains(entry.getKey())) {
                    assertThat(entry.getValue().params.opi).isEqualTo(ingestOperationGuid);
                    assertThat(entry.getValue().params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
                    assertThat(entry.getValue().params.objectGroupId).isEqualTo(
                        getById(ingestedUnits, entry.getKey()).has("#object") ?
                            getById(ingestedUnits, entry.getKey()).get("#object").asText() : null
                    );
                }
            }

            // Check report object groups
            int objectGroupCount = expectedDeletedObjectGroupIds.size() + expectedPartiallyDeletedObjectGroupIds.size();
            Map<String, ObjectGroupReportEntry> objectGroupReportById = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, objectGroupCount),
                    entry -> asTypeReference(entry, OG_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> entry.id, entry -> entry));

            assertThat(objectGroupReportById.values()).allMatch(i -> "ObjectGroup".equals(i.params.type));
            assertThat(objectGroupReportById.keySet()).containsExactlyInAnyOrderElementsOf(
                ListUtils.union(expectedDeletedObjectGroupIds, expectedPartiallyDeletedObjectGroupIds));

            for (Map.Entry<String, ObjectGroupReportEntry> entry : objectGroupReportById.entrySet()) {
                assertThat(entry.getValue().params.id).isEqualTo(entry.getKey());

                if (expectedDeletedObjectGroupIds.contains(entry.getKey())) {
                    assertThat(entry.getValue().params.status).isEqualTo("DELETED");
                    assertThat(entry.getValue().params.objectIds).containsExactlyInAnyOrderElementsOf(
                        getBinaryObjectIds(getById(ingestedObjectGroups, entry.getKey()))
                    );
                    assertThat(entry.getValue().params.deletedParentUnitIds).isNullOrEmpty();
                } else {
                    assertThat(entry.getValue().params.status).isEqualTo("PARTIAL_DETACHMENT");
                    assertThat(entry.getValue().params.objectIds).isNullOrEmpty();
                    assertThat(entry.getValue().params.deletedParentUnitIds).containsExactlyInAnyOrderElementsOf(
                        SetUtils.intersection(
                            getParentUnitIds(getById(ingestedObjectGroups, entry.getKey())),
                            new HashSet<>(expectedDeletedUnitIds)));
                }

                assertThat(entry.getValue().params.opi).isEqualTo(ingestOperationGuid);
                assertThat(entry.getValue().params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            }

            // Check end of report
            assertThat(reportIterator.hasNext()).isFalse();
        }
    }

    private String startTransferReplyWorkflow(InputStream atrInputStream, StatusCode expectedStatusCode)
        throws AccessInternalClientServerException {
        String transferReplyWorkflowGuid = GUIDFactory.newOperationLogbookGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(transferReplyWorkflowGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.startTransferReplyWorkflow(atrInputStream);
            awaitForWorkflowTerminationWithStatus(transferReplyWorkflowGuid, expectedStatusCode);
        }
        return transferReplyWorkflowGuid;
    }

    private void checkTransferOperationsInExportedUnits(AccessInternalClient accessInternalClient,
        String transferOperation, Collection<String> expectedUnitIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException,
        LogbookClientException {
        SelectMultiQuery selectUnitsWithTransferOperations = new SelectMultiQuery();
        selectUnitsWithTransferOperations.addQueries(
            QueryHelper.eq(VitamFieldsHelper.opts(), transferOperation));
        RequestResponseOK<JsonNode> response =
            (RequestResponseOK<JsonNode>) accessInternalClient
                .selectUnits(selectUnitsWithTransferOperations.getFinalSelect());
        assertThat(response.getResults().stream()
            .map(this::getId)
            .collect(Collectors.toList())
        ).containsExactlyInAnyOrderElementsOf(expectedUnitIds);

        // Ensure exported units LFC not modified (opts is a non persisted / secured field)
        LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        List<JsonNode> unitLifeCycles =
            logbookLifeCyclesClient.getRawUnitLifeCycleByIds(new ArrayList<>(expectedUnitIds));
        for (JsonNode unitLifeCycle : unitLifeCycles) {
            assertThat(unitLifeCycle.get("events").get(unitLifeCycle.get("events").size() - 1)
                .get("outDetail").asText()).isEqualTo("LFC.UNIT_METADATA_INDEXATION.OK");
        }
    }

    private void checkTransferRequestReport(String transferOperationId, Collection<String> okUnitIds,
        Collection<String> alredyTransferedUnitIds)
        throws IOException, StorageNotFoundException, StorageServerClientException {

        try (InputStream is = readStoredReport(transferOperationId + JSONL);
            JsonLineGenericIterator<JsonNode> reportIterator =
                new JsonLineGenericIterator<>(is, JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            Map<String, String> exportedUnitReportStatus =
                Streams.stream(reportIterator)
                    .collect(toMap(
                        entry -> entry.get("id").asText(),
                        entry -> entry.get("status").asText()
                    ));

            assertThat(exportedUnitReportStatus).hasSize(okUnitIds.size() + alredyTransferedUnitIds.size());
            assertThat(exportedUnitReportStatus.entrySet())
                .allMatch(entry -> entry.getValue()
                    .equals(okUnitIds.contains(entry.getKey()) ? "OK" : "ALREADY_IN_TRANSFER"));
        }
    }

    private void compareMetadata(String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedGots, String transferredSipIngestOperationId,
        RequestResponseOK<JsonNode> ingestedTransferredUnits,
        RequestResponseOK<JsonNode> ingestedTransferredObjectGroups) throws InvalidParseOperationException {
        Map<String, String> initialIngestReplacements = getReplacementsForNormalization(ingestOperationGuid,
            ingestedUnits.getResults(), ingestedGots.getResults());
        List<JsonNode> initialUnits = normalize(ingestedUnits.getResults(), initialIngestReplacements);
        List<JsonNode> initialObjectGroups = normalize(ingestedGots.getResults(), initialIngestReplacements);

        Map<String, String> transferredIngestReplacements =
            getReplacementsForNormalization(transferredSipIngestOperationId,
                ingestedTransferredUnits.getResults(), ingestedTransferredObjectGroups.getResults());
        List<JsonNode> transferredUnits =
            normalize(ingestedTransferredUnits.getResults(), transferredIngestReplacements);
        List<JsonNode> transferredObjectGroups =
            normalize(ingestedTransferredObjectGroups.getResults(), transferredIngestReplacements);

        assertThat(initialUnits).hasSameSizeAs(transferredUnits);
        for (int i = 0; i < initialUnits.size(); i++) {
            JsonAssert.assertJsonEquals(initialUnits.get(i), transferredUnits.get(i),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        }

        assertThat(initialObjectGroups).hasSameSizeAs(transferredObjectGroups);
        for (int i = 0; i < initialObjectGroups.size(); i++) {

            JsonAssert.assertJsonEquals(initialObjectGroups.get(i), transferredObjectGroups.get(i),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        }
    }

    private Map<String, String> getReplacementsForNormalization(String opi, List<JsonNode> units,
        List<JsonNode> objectGroups)
        throws InvalidParseOperationException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(opi, "{ingest_operation}");
        for (JsonNode unit : units) {
            replacements.put(getId(unit), "{unit:" + getTitle(unit) + "}");
        }
        for (JsonNode objectGroup : objectGroups) {
            String gotLabel =
                "{og_of:" +
                    JsonHandler.getFromJsonNode(objectGroup.get("#unitups"), new TypeReference<List<String>>() {
                    }).stream()
                        .map(replacements::get)
                        .sorted()
                        .collect(Collectors.joining(",", "{og_of:", "}"));
            replacements.put(getId(objectGroup), gotLabel);

            objectGroup.get("#qualifiers").elements().forEachRemaining(
                qualifier -> qualifier.get("versions").elements().forEachRemaining(
                    version -> replacements.put(version.get("#id").asText(),
                        "{binary:" + version.get("DataObjectVersion").asText() + "_of_" + gotLabel + "}")
                )
            );
        }
        return replacements;
    }

    private List<JsonNode> normalize(List<JsonNode> resultSet, Map<String, String> replacements)
        throws InvalidParseOperationException {

        List<JsonNode> normalizedMetadata = resultSet.stream()
            .map(metadata -> (JsonNode) metadata.deepCopy())
            .collect(Collectors.toList());

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            replaceIds(normalizedMetadata, entry.getKey(), entry.getValue());
        }

        List<String> excludedFields = Arrays.asList("#version", "_graph", "_uds", "_glpd", "#allunitups", "_us_sp",
            "Uri", "DateCreatedByApplication");
        for (JsonNode md : normalizedMetadata) {
            purgeIgnoredFields(md, excludedFields);
        }

        return normalizedMetadata.stream()
            .sorted(Comparator.comparing(this::getId))
            .collect(Collectors.toList());
    }

    private void replaceIds(List<JsonNode> metadataList, String id, String replacement)
        throws InvalidParseOperationException {
        for (int i = 0; i < metadataList.size(); i++) {
            metadataList.set(i, JsonHandler.getFromString(
                JsonHandler.unprettyPrint(metadataList.get(i))
                    .replaceAll(id, replacement))
            );
        }
    }

    private void purgeIgnoredFields(JsonNode jsonNode, List<String> ignoredFields) {
        if (jsonNode.isValueNode()) {
            return;
        }
        if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                purgeIgnoredFields(jsonNode.get(i), ignoredFields);
            }
            return;
        }
        if (jsonNode.isObject()) {
            ((ObjectNode) jsonNode).remove(ignoredFields);
            for (JsonNode node : jsonNode) {
                purgeIgnoredFields(node, ignoredFields);
            }
            return;
        }
        throw new IllegalStateException("Unknown type " + jsonNode);
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                    filename, DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog());

                assertThat(reportResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());

                return new VitamAsyncInputStream(reportResponse);


            } catch (RuntimeException | StorageServerClientException | StorageNotFoundException e) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }

    private String doIngest(InputStream zipInputStreamSipObject, StatusCode expectedStatusCode) throws VitamException {
        final GUID ingestOperationGuid = newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid.getId(), expectedStatusCode);
        return ingestOperationGuid.getId();
    }

    private String transfer(Collection<String> unitIds, StatusCode expectedStatusCode) throws Exception {

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), unitIds.toArray(new String[0])));

        ExportRequest dipExportRequest = new ExportRequest(
            new DataObjectVersions(),
            select.getFinalSelect(),
            true
        );

        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("RATP");
        exportRequestParameters.setSubmissionAgencyIdentifier("RATP");
        exportRequestParameters
            .setRelatedTransferReference(Arrays.asList("RelatedTransferReference1", "RelatedTransferReference2"));

        dipExportRequest.setExportType(ExportType.ArchiveTransfer);
        dipExportRequest.setExportRequestParameters(exportRequestParameters);


        String transferOperation = GUIDFactory.newGUID().getId();
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(transferOperation);

        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(dipExportRequest);

            awaitForWorkflowTerminationWithStatus(transferOperation, expectedStatusCode);

            return transferOperation;
        }
    }

    private InputStream getTransferSip(String operationId)
        throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            return client.findTransferSIPByID(operationId).readEntity(InputStream.class);
        }
    }

    private void checkEliminationReport(InputStream reportInputStream, RequestResponseOK<JsonNode> ingestedUnits,
        Map<String, JsonNode> ingestedUnitsByTitle,
        String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedGots, String detachedGotId) {

        try (JsonLineGenericIterator<JsonNode> reportIterator = new JsonLineGenericIterator<>(reportInputStream,
            JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            Map<String, UnitReportEntry> unitReportByTitle = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, 6),
                    entry -> asTypeReference(entry, UNIT_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> getTitle(getById(ingestedUnits, entry.id)), entry -> entry));

            assertThat(unitReportByTitle).hasSize(6);

            assertThat(unitReportByTitle.get(CARREFOUR_PLEYEL).params.status).isEqualTo(
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP.name());
            assertThat(unitReportByTitle.get(SAINT_DENIS_BASILIQUE).params.status).isEqualTo(
                PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
            assertThat(unitReportByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13).params.status).isEqualTo(
                PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
            assertThat(unitReportByTitle.get(SAINT_LAZARE).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());
            assertThat(unitReportByTitle.get(MARX_DORMOY).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());
            assertThat(unitReportByTitle.get(MONTPARNASSE).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());

            for (UnitReportEntry unitReport : unitReportByTitle.values()) {

                JsonNode unit = getById(ingestedUnits, unitReport.id);

                assertThat(unitReport.params.opi).isEqualTo(ingestOperationGuid.toString());
                assertThat(unitReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
                assertThat(unitReport.params.objectGroupId).isEqualTo(getObjectGroupId(unit));
                assertThat(unitReport.params.type).isEqualTo("Unit");
            }

            //Check report object groups
            List<ObjectGroupReportEntry> objectGroupReports = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, 2),
                    entry -> asTypeReference(entry, OG_REPORT_TYPE_REFERENCE))
            ).collect(Collectors.toList());

            assertThat(objectGroupReports).hasSize(2);
            assertThat(objectGroupReports).allMatch(i -> "ObjectGroup".equals(i.params.type));

            String deletedGotId = getObjectGroupId(ingestedUnitsByTitle.get(MARX_DORMOY));
            ObjectGroupReportEntry deletedGotReport = objectGroupReports.stream()
                .filter(got -> got.id.equals(deletedGotId))
                .findFirst().get();
            assertThat(deletedGotReport.params.status).isEqualTo(PurgeObjectGroupStatus.DELETED.name());
            assertThat(deletedGotReport.params.opi).isEqualTo(ingestOperationGuid);
            assertThat(deletedGotReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            assertThat(deletedGotReport.params.objectIds).containsExactlyInAnyOrderElementsOf(
                getBinaryObjectIds(getById(ingestedGots, deletedGotId)));
            assertThat(deletedGotReport.params.deletedParentUnitIds).isNullOrEmpty();

            ObjectGroupReportEntry detachedGotReport = objectGroupReports.stream()
                .filter(got -> got.id.equals(detachedGotId))
                .findFirst().get();
            assertThat(detachedGotReport.params.status)
                .isEqualTo(PurgeObjectGroupStatus.PARTIAL_DETACHMENT.name());
            assertThat(detachedGotReport.params.opi).isEqualTo(ingestOperationGuid);
            assertThat(detachedGotReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            assertThat(detachedGotReport.params.objectIds).isNullOrEmpty();
            assertThat(detachedGotReport.params.deletedParentUnitIds).containsExactlyInAnyOrder(
                getId(ingestedUnitsByTitle.get(MONTPARNASSE)));

            // Check end of report
            assertThat(reportIterator.hasNext()).isFalse();
        }
    }

    private <T> T asTypeReference(JsonNode entry, TypeReference<T> typeReference) {
        try {
            return JsonHandler.getFromJsonNode(entry, typeReference);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertJsonEquals(String resourcesFile, JsonNode actual, List<String> excludeFields)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonNode expected = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile));
        if (excludeFields != null) {
            expected.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }
            });
            actual.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }

            });
        }

        JsonAssert
            .assertJsonEquals(expected, actual, JsonAssert.whenIgnoringPaths(excludeFields.toArray(new String[] {})));
    }

    private String getId(JsonNode unit) {
        return unit.get(VitamFieldsHelper.id()).asText();
    }

    private String getObjectGroupId(JsonNode unit) {
        return unit.has(VitamFieldsHelper.object()) ? unit.get(VitamFieldsHelper.object()).asText() : null;
    }

    private String getTitle(JsonNode unit) {
        return unit.get("Title").asText();
    }

    private JsonNode getById(RequestResponseOK<JsonNode> metadata, String id) {
        return metadata.getResults().stream()
            .filter(got -> id.equals(got.get(VitamFieldsHelper.id()).asText()))
            .findFirst().get();
    }

    private void checkUnitExistence(String unitId, boolean shouldExist)
        throws StorageNotFoundClientException, StorageServerClientException {
        checkDocumentExistence(MetadataCollections.UNIT.getVitamCollection(), unitId, shouldExist);
        checkDocumentExistence(LogbookCollections.LIFECYCLE_UNIT.getVitamCollection(), unitId, shouldExist);
        checkFileInStorage(DataCategory.UNIT, unitId + ".json", shouldExist);
    }

    private void checkObjectGroupExistence(String gotId, boolean shouldExist)
        throws StorageNotFoundClientException, StorageServerClientException {
        checkDocumentExistence(MetadataCollections.OBJECTGROUP.getVitamCollection(), gotId, shouldExist);
        checkDocumentExistence(LogbookCollections.LIFECYCLE_OBJECTGROUP.getVitamCollection(), gotId, shouldExist);
        checkFileInStorage(DataCategory.OBJECTGROUP, gotId + ".json", shouldExist);
    }

    private void checkObjectExistence(String objectId, boolean shouldExist)
        throws StorageNotFoundClientException, StorageServerClientException {
        checkFileInStorage(DataCategory.OBJECT, objectId, shouldExist);
    }

    private void checkFileInStorage(DataCategory dataCategory, String filename, boolean shouldExist)
        throws StorageNotFoundClientException, StorageServerClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            List<String> offers = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
            JsonNode information =
                storageClient
                    .getInformation(VitamConfiguration.getDefaultStrategy(), dataCategory, filename, offers, false);
            boolean fileFound = information.size() > 0;
            assertThat(fileFound).isEqualTo((boolean) shouldExist);
        }
    }

    private void checkDocumentExistence(VitamCollection collection, String documentId,
        boolean shouldExist) {

        int expectedHits = shouldExist ? 1 : 0;

        // Logbook LFCs are not persisted in ES
        if (collection.getEsClient() != null) {
            long totalHits = collection.getEsClient().getClient()
                .prepareSearch(getAliasName(collection, VitamThreadUtils.getVitamSession().getTenantId()))
                .setTypes(VitamCollection.getTypeunique())
                .setQuery(QueryBuilders.termQuery(VitamDocument.ID, documentId))
                .get()
                .getHits()
                .getTotalHits();
            assertThat(totalHits).isEqualTo(expectedHits);
        }

        assertThat(collection.getCollection().find(Filters.eq(VitamDocument.ID, documentId))
            .iterator()).hasSize(expectedHits);
    }

    private Set<String> getBinaryObjectIds(RequestResponseOK<JsonNode> gots) {
        Set<String> objectIds = new HashSet<>();
        for (JsonNode gotJson : gots.getResults()) {
            objectIds.addAll(getBinaryObjectIds(gotJson));
        }
        return objectIds;
    }

    private Set<String> getBinaryObjectIds(JsonNode gotJson) {
        Set<String> objectIds = new HashSet<>();

        try {
            ObjectGroupResponse gotResponse = JsonHandler.getFromJsonNode(gotJson, ObjectGroupResponse.class);

            for (QualifiersModel qualifier : gotResponse.getQualifiers()) {
                for (VersionsModel version : qualifier.getVersions()) {
                    if (version.getPhysicalId() == null) {
                        objectIds.add(version.getId());
                    }
                }
            }
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
        return objectIds;
    }

    private Set<String> getParentUnitIds(JsonNode gotJson) {

        try {
            ObjectGroupResponse gotResponse = JsonHandler.getFromJsonNode(gotJson, ObjectGroupResponse.class);
            return new HashSet<>(gotResponse.getUp());

        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getIds(RequestResponseOK<JsonNode> ingestedGots) {
        return mapByField(ingestedGots, VitamFieldsHelper.id()).keySet();
    }

    private String getAliasName(final VitamCollection collection, Integer tenantId) {
        return collection.getName().toLowerCase() + "_" + tenantId.toString();
    }

    private Map<String, JsonNode> mapByField(RequestResponseOK<JsonNode> requestResponseOK, String title) {
        return requestResponseOK.getResults()
            .stream()
            .collect(toMap(node -> node.get(title).asText(), node -> node));
    }

    private RequestResponseOK<JsonNode> selectGotsByOpi(String ingestOperationGuid,
        AccessInternalClient accessInternalClient)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationGotDslRequest = new SelectMultiQuery();
        checkEliminationGotDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient
            .selectObjects(checkEliminationGotDslRequest.getFinalSelect());
    }

    private RequestResponseOK<JsonNode> selectUnitsByOpi(String ingestOperationGuid,
        AccessInternalClient accessInternalClient)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationDslRequest = new SelectMultiQuery();
        checkEliminationDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient
            .selectUnits(checkEliminationDslRequest.getFinalSelect());
    }

    private RequestResponseOK<JsonNode> selectUnitsByTitle(String ingestOperationGuid, String title,
        AccessInternalClient accessInternalClient)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationDslRequest = new SelectMultiQuery();
        checkEliminationDslRequest.addQueries(
            QueryHelper.and().add(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid),
                QueryHelper.match("Title", title)
            ));

        return (RequestResponseOK<JsonNode>) accessInternalClient
            .selectUnits(checkEliminationDslRequest.getFinalSelect());
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {

        wait(operationGuid);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid, tenantId);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {
        } catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }

    private static class UnitReportEntry {
        @JsonProperty("id")
        String id;
        @JsonProperty("params")
        UnitReportParams params;
    }


    private static class UnitReportParams {
        @JsonProperty("id")
        String id;
        @JsonProperty("status")
        String status;
        @JsonProperty("opi")
        String opi;
        @JsonProperty("originatingAgency")
        String originatingAgency;
        @JsonProperty("objectGroupId")
        String objectGroupId;
        @JsonProperty("type")
        String type;
    }


    private static class ObjectGroupReportEntry {
        @JsonProperty("id")
        String id;
        @JsonProperty("params")
        ObjectGroupReportParams params;
    }


    private static class ObjectGroupReportParams {
        @JsonProperty("id")
        String id;
        @JsonProperty("status")
        String status;
        @JsonProperty("opi")
        String opi;
        @JsonProperty("originatingAgency")
        String originatingAgency;
        @JsonProperty("objectIds")
        List<String> objectIds;
        @JsonProperty("deletedParentUnitIds")
        List<String> deletedParentUnitIds;
        @JsonProperty("type")
        String type;
    }


    public interface IngestCleanupAdminService {
        @POST("/adminmanagement/v1/invalidIngestCleanup/{opi}")
        @Headers({
            "Accept: application/json"
        })
        Call<Void> startIngestCleanupWorkflow(
            @Path("opi") String opi,
            @Header("X-Tenant-Id") Integer tenant,
            @Header("Authorization") String basicAuthnToken);
    }

}
