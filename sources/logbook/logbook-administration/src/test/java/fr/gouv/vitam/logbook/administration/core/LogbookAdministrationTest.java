package fr.gouv.vitam.logbook.administration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.logbook.operations.core.LogbookOperationsImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

public class LogbookAdministrationTest {

    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    public static final Integer OPERATION_TRACEABILITY_OVERLAP_DELAY = 300;
    static LogbookDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static fr.gouv.vitam.common.junit.JunitHelper junitHelper;
    private static int port;

    // ES
    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private final static String ES_HOST_NAME = "localhost";
    private static ElasticsearchTestConfiguration config = null;

    private static final Integer tenantId = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void init() throws IOException {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(esTempFolder, ES_CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        List<MongoDbNode> nodes = new ArrayList<MongoDbNode>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ES_HOST_NAME, config.getTcpPort()));
        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, DATABASE_NAME, ES_CLUSTER_NAME, esNodes);
        logbookConfiguration.setTenants(tenantList);
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration);
    }


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @After
    public void tearDown() throws DatabaseException {
        mongoDbAccess.deleteCollection(LogbookCollections.OPERATION);
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_without_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        byte[] hash = {1, 2, 3, 4};
        File file = folder.newFolder();

        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        ArgumentCaptor<byte[]> hashCapture = ArgumentCaptor.forClass(byte[].class);

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        doAnswer(invocation -> {
            InputStream argumentAt = invocation.getArgumentAt(2, InputStream.class);
            Files.copy(argumentAt, archive);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));


        given(timestampGenerator.generateToken(hashCapture.capture(),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);

        LogbookOperationsImpl logbookOperations = new LogbookOperationsImpl(mongoDbAccess);

        LogbookAdministration logbookAdministration =
            new LogbookAdministration(logbookOperations, timestampGenerator,
                workspaceClientFactory, file, OPERATION_TRACEABILITY_OVERLAP_DELAY);

        // When
        logbookAdministration.generateSecureLogbook();

        // Then
        assertThat(archive).exists();
        assertThat(hashCapture.getValue()).hasSize(512 / 8);
        validateFile(archive, 1, "null");
    }

    @Test
    @RunWithCustomExecutor
    public void should_populate_logbook_evdetdata() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        byte[] hash = {1, 2, 3, 4};
        File file = folder.newFolder();

        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        ArgumentCaptor<byte[]> hashCapture = ArgumentCaptor.forClass(byte[].class);


        given(timestampGenerator.generateToken(hashCapture.capture(),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);

        LogbookOperationsImpl logbookOperations = new LogbookOperationsImpl(mongoDbAccess);

        LogbookAdministration logbookAdministration =
            new LogbookAdministration(logbookOperations, timestampGenerator,
                workspaceClientFactory, file, OPERATION_TRACEABILITY_OVERLAP_DELAY);

        // When
        logbookAdministration.generateSecureLogbook();
        Select select = new Select();
        select.setQuery(
            QueryHelper.eq(LogbookMongoDbName.eventTypeProcess.getDbname(), LogbookTypeProcess.TRACEABILITY.name()));
        List<LogbookOperation> operations = logbookOperations.select(select.getFinalSelect());

        // Then
        assertThat(operations).extracting("evDetData").doesNotContainNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_one_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        byte[] hash = {1, 2, 3, 4};
        File file = folder.newFolder();

        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        AtomicInteger atomicInteger = new AtomicInteger();
        doAnswer(invocation -> {
            int call = atomicInteger.incrementAndGet();
            if (call == 2) {
                InputStream argumentAt = invocation.getArgumentAt(2, InputStream.class);
                Files.copy(argumentAt, archive);
            }
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        given(timestampGenerator.generateToken(any(byte[].class),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);

        LogbookOperationsImpl logbookOperations = new LogbookOperationsImpl(mongoDbAccess);

        LogbookAdministration logbookAdministration =
            new LogbookAdministration(logbookOperations, timestampGenerator,
                workspaceClientFactory, file, OPERATION_TRACEABILITY_OVERLAP_DELAY);
        // insert initial event
        GUID guid = logbookAdministration.generateSecureLogbook();
        Select select = new Select();
        Query findById = QueryHelper.eq("evIdProc", guid.toString());

        select.setQuery(findById);
        select.setLimitFilter(0, 1);
        TraceabilityEvent firstTraceabilityOperation = extractTraceabilityEvent(logbookOperations, select);

        String lastTimestampToken = extractLastTimestampToken(logbookOperations, select);
        // When
        logbookAdministration.generateSecureLogbook();

        // Then
        assertThat(archive).exists();
        validateFile(archive, 2, BaseXx.getBase64(lastTimestampToken.getBytes()));

        select = new Select();
        select.addOrderByDescFilter("evDateTime");
        select.setLimitFilter(0, 1);
        TraceabilityEvent traceabilityEvent = extractTraceabilityEvent(logbookOperations, select);
        assertEquals(firstTraceabilityOperation.getStartDate(),
            traceabilityEvent.getMinusOneMonthLogbookTraceabilityDate());
        assertEquals(firstTraceabilityOperation.getStartDate(),
            traceabilityEvent.getMinusOneYearLogbookTraceabilityDate());
        assertNotNull(traceabilityEvent.getPreviousLogbookTraceabilityDate());
        assertNotNull(traceabilityEvent.getSize());
        assertEquals(TraceabilityType.OPERATION, traceabilityEvent.getLogType());

        logbookAdministration.generateSecureLogbook();
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_timestamp_token() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        File file = folder.newFolder();

        LogbookAdministration logbookAdministration =
            new LogbookAdministration(logbookOperations, timestampGenerator,
                workspaceClientFactory, file, OPERATION_TRACEABILITY_OVERLAP_DELAY);
        InputStream stream = getClass().getResourceAsStream("/logbookOperationWithToken.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        // When
        byte[] token = logbookAdministration.extractTimestampToken(logbookOperation);

        // Then
        assertThat(Base64.encodeBase64String(token)).isEqualTo(
            "AQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQ=");
    }

    private TraceabilityEvent extractTraceabilityEvent(LogbookOperationsImpl logbookOperations, Select select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        List<LogbookOperation> logbookOperationList = logbookOperations.select(select.getFinalSelect());
        LogbookOperation traceabilityOperation = Iterables.getOnlyElement(logbookOperationList);
        List<Document> documents = (List<Document>) traceabilityOperation.get("events");
        Document lastEvent = Iterables.getLast(documents);
        String evDetData = (String) lastEvent.get("evDetData");

        // a recuperer du dernier event et non pas sur l'event parent
        return JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
    }

    private String extractLastTimestampToken(LogbookOperationsImpl logbookOperations, Select select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        return new String(extractTraceabilityEvent(logbookOperations, select).getTimeStampToken());
    }

    private void validateFile(Path path, int numberOfElement, String previousHash)
        throws IOException, ArchiveException {
        try (ArchiveInputStream archiveInputStream =
            new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.ZIP, Files.newInputStream(path))) {
            //
            ArchiveEntry entry = archiveInputStream.getNextEntry();

            assertThat(entry.getName()).isEqualTo("operations.json");
            byte[] bytes = IOUtils.toByteArray(archiveInputStream);
            assertThat(new String(bytes)).hasLineCount(numberOfElement);

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("merkleTree.json");

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("token.tsp");

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("additional_information.txt");
            Properties properties = new Properties();
            properties.load(archiveInputStream);
            assertThat(properties.getProperty("numberOfElement")).contains(Integer.toString(numberOfElement));

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("computing_information.txt");
            properties = new Properties();
            properties.load(archiveInputStream);
            assertThat(properties.getProperty("currentHash")).isNotNull();
            assertThat(properties.getProperty("previousTimestampToken")).isEqualTo(previousHash);
        }
    }

}
