package fr.gouv.vitam.logbook.administration.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

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
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.core.LogbookOperationsImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class LogbookAdministrationTest {

    private static final String DATABASE_HOST = "localhost";
    static LogbookDbAccess mongoDbAccess;
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static fr.gouv.vitam.common.junit.JunitHelper junitHelper;
    private static int port;

    private static final Integer tenantId = 0;
    
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    
    @BeforeClass
    public static void init() throws IOException {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        List<MongoDbNode> nodes = new ArrayList<MongoDbNode>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));
        mongoDbAccess =
            LogbookMongoDbAccessFactory.create(
                new DbConfigurationImpl(nodes,
                    "vitam-test"));

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
                workspaceClientFactory, file);

        // When
        logbookAdministration.generateSecureLogbook();

        // Then
        assertThat(archive).exists();
        assertThat(hashCapture.getValue()).hasSize(512 / 8);
        validateFile(archive, 1, "null");
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
                workspaceClientFactory, file);
        // insert initial event
        GUID guid = logbookAdministration.generateSecureLogbook();
        Select select = new Select();
        Query findById = QueryHelper.eq("evIdProc", guid.toString());

        select.setQuery(findById);
        select.setLimitFilter(0, 1);

        String lastTimestampToken = extractLastTimestampToken(logbookOperations, select);
        // When
        logbookAdministration.generateSecureLogbook();

        // Then
        assertThat(archive).exists();
        validateFile(archive, 2, BaseXx.getBase64(lastTimestampToken.getBytes()));
    }

    private String extractLastTimestampToken(LogbookOperationsImpl logbookOperations, Select select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        List<LogbookOperation> logbookOperationList = logbookOperations.select(select.getFinalSelect());
        LogbookOperation traceabilityOperation = Iterables.getOnlyElement(logbookOperationList);
        List<Document> documents = (List<Document>) traceabilityOperation.get("events");
        Document lastEvent = Iterables.getLast(documents);
        String evDetData = (String) lastEvent.get("evDetData");

        // a recuperer du dernier event et non pas sur l'event parent
        TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
        return new String(traceabilityEvent.getTimeStampToken());

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
