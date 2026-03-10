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
package fr.gouv.vitam.storage.offer;

import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyApi;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyClientFactory;
import fr.gouv.vitam.storage.cold.server.InaTapeProxyApplication;
import fr.gouv.vitam.storage.cold.server.InaTapeProxyServer;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeCatalogRepository;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeDriveRepository;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.MarkerType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for INA Tape Proxy.
 * These tests validate the interaction between the client and the INA Tape Proxy server,
 * including write operations, read operations, and tape robotic controls.
 */
@RunWithCustomExecutor
public class InaTapeProxyIT {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyIT.class);

    private static final Integer DRIVE_INDEX = 0;

    // Configuration files
    private static final String CONFIG_DIR = "ina-tape-proxy";
    private static final String SERVER_CONFIG = CONFIG_DIR + "/ina-tape-proxy-web.conf";
    private static final String CLIENT_CONFIG = CONFIG_DIR + "/ina-tape-proxy-client.conf";

    private static String WRITE_DIR; // INA writes files here
    private static String READ_DIR; // INA copies read files directly here
    private static String MARKERS_DIR;
    private static String VITAM_STORAGE_DIR;
    private static String INA_STORAGE_DIR; // INA tape storage (simulated)

    // Marker file suffixes
    private static final String WRITE_OK = MarkerType.WRITE_OK.getSuffix();
    private static final String READ_OK = MarkerType.READ_OK.getSuffix();

    // Timeouts and intervals
    private static final Duration DEFAULT_MARKER_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_MARKER_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration FAST_READ_MARKER_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration POLL_INTERVAL_READ = Duration.ofMillis(200);

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        "admin",
        MongoDbAccess.getMongoClientSettingsBuilder(),
        TapeCatalogRepository.COLLECTION_NAME,
        TapeDriveRepository.COLLECTION_NAME
    );

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static InaTapeProxyServer server;

    @BeforeClass
    public static void setupClass() throws Exception {
        String confDir = PropertiesUtils.getResourceFile(CONFIG_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", confDir);
        VitamConfiguration.getConfiguration().setConfig(confDir);

        // Set Jetty port BEFORE starting the server
        SystemPropertyUtil.set("jetty.port", "8216");

        File serverConf = PropertiesUtils.getResourceFile(SERVER_CONFIG);

        // Directory structure
        File exchangeRoot = temporaryFolder.newFolder();
        String exchangeDirectory = exchangeRoot.getAbsolutePath() + "/data/ina/exchange";
        INA_STORAGE_DIR = exchangeRoot.getAbsolutePath() + "/data/ina/storage";
        File offerStorageRoot = temporaryFolder.newFolder();
        VITAM_STORAGE_DIR = offerStorageRoot.getAbsolutePath() + "/data/offer";

        // FileSystemManager will create write, read, markers subdirectories under exchangeDirectory
        WRITE_DIR = exchangeDirectory + "/write";
        READ_DIR = exchangeDirectory + "/read";
        MARKERS_DIR = exchangeDirectory + "/markers";

        // Create the base directories (FileSystemManager will create subdirectories)
        Files.createDirectories(Paths.get(VITAM_STORAGE_DIR));

        InaTapeProxyConfiguration inaTapeProxyConfiguration = PropertiesUtils.readYaml(
            serverConf,
            InaTapeProxyConfiguration.class
        );
        inaTapeProxyConfiguration.setExchangeDirectory(exchangeDirectory);
        inaTapeProxyConfiguration.setInaStorageDirectory(INA_STORAGE_DIR);
        inaTapeProxyConfiguration.setOfferStorageDirectory(VITAM_STORAGE_DIR);

        // Write modified configuration to a temporary file
        File tempConfigFile = temporaryFolder.newFile("ina-tape-proxy-temp.conf");
        PropertiesUtils.writeYaml(tempConfigFile, inaTapeProxyConfiguration);

        // Create and start server instance (keep reference to prevent GC)
        server = new InaTapeProxyServer(
            InaTapeProxyConfiguration.class,
            tempConfigFile.getAbsolutePath(),
            InaTapeProxyApplication.class,
            AdminApplication.class
        );
        server.start();

        // Wait for server to be fully started
        LOGGER.info("Waiting for server to start...");
        Uninterruptibles.sleepUninterruptibly(3, java.util.concurrent.TimeUnit.SECONDS);

        // Clear system properties after server start
        SystemPropertyUtil.clear("jetty.port");
        SystemPropertyUtil.clear("vitam.conf.folder");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
    }

    @org.junit.After
    public void tearDown() {
        // Try to unload any loaded tape to clean up state
        try {
            InaTapeProxyApi client = createClient();
            client.unloadTape(1, DRIVE_INDEX);
            LOGGER.debug("Tape unloaded successfully in tearDown");
        } catch (Exception e) {
            // No tape was loaded, or unload failed - this is OK
            LOGGER.debug("Could not unload tape in tearDown: {}", e.getMessage());
        }
    }

    // ==================== CONNECTIVITY ====================

    @Test
    public void should_connect_to_proxy() throws IOException {
        InaTapeProxyApi client = createClient();
        assertThat(client).isNotNull();
    }

    // ==================== WRITE OPERATIONS ====================

    @Test
    public void should_write_file_to_tape() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX); // Load tape before writing

        String filename = filename("write");
        Path source = createFile(filename, "content");

        try {
            client.writeToTape(DRIVE_INDEX, source.toString());
            assertMarkerExists(filename);
            assertFileExists(WRITE_DIR, filename, "content");
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_skip_write_when_same_checksum() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX); // Load tape before writing

        String filename = filename("checksum");
        Path source = createFile(filename, "same");

        try {
            client.writeToTape(DRIVE_INDEX, source.toString());
            assertMarkerExists(filename);

            client.writeToTape(DRIVE_INDEX, source.toString());
            assertFileExists(WRITE_DIR, filename, "same");
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_write_multiple_files() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX); // Load tape before writing

        String f1 = filename("m1");
        String f2 = filename("m2");
        String f3 = filename("m3");

        Path p1 = createFile(f1, "c1");
        Path p2 = createFile(f2, "c2");
        Path p3 = createFile(f3, "c3");

        try {
            client.writeToTape(DRIVE_INDEX, p1.toString());
            client.writeToTape(DRIVE_INDEX, p2.toString());
            client.writeToTape(DRIVE_INDEX, p3.toString());

            assertMarkerExists(f1);
            assertMarkerExists(f2);
            assertMarkerExists(f3);

            assertFileExists(WRITE_DIR, f1, "c1");
            assertFileExists(WRITE_DIR, f2, "c2");
            assertFileExists(WRITE_DIR, f3, "c3");
        } finally {
            cleanup(f1, p1);
            cleanup(f2, p2);
            cleanup(f3, p3);
        }
    }

    @Test
    public void should_fail_write_nonexistent_file() {
        String missing = "/tmp/missing-" + System.currentTimeMillis() + ".tar";
        assertThatThrownBy(() -> createClient().writeToTape(DRIVE_INDEX, missing)).isInstanceOf(
            fr.gouv.vitam.storage.cold.client.invoker.ApiException.class
        );
    }

    // ==================== READ OPERATIONS ====================

    @Test
    public void should_read_file_from_tape() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX); // Load tape before I/O operations

        String filename = filename("read");
        Path source = createFile(filename, "data");

        try {
            // Write
            client.writeToTape(DRIVE_INDEX, source.toString());
            assertMarkerExists(filename);

            // Copy to INA storage (simulate archiving to tape)
            copyToInaStorage(filename);

            // Read
            Path destination = Paths.get(VITAM_STORAGE_DIR, filename);
            client.readFromTape(DRIVE_INDEX, destination.toString());

            // Wait for marker
            assertMarkerExists(filename, READ_OK, READ_MARKER_TIMEOUT, POLL_INTERVAL_READ);

            // Verify
            assertThat(destination).exists();
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_read_already_prepared_file() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX); // Load tape before I/O operations

        String filename = filename("prepared");
        Path source = createFile(filename, "prep");

        try {
            // Write and first read
            client.writeToTape(DRIVE_INDEX, source.toString());
            assertMarkerExists(filename);

            // Copy to INA storage (simulate archiving to tape)
            copyToInaStorage(filename);

            // First read - pass destination path in Vitam storage
            Path readDest = Paths.get(VITAM_STORAGE_DIR, filename);
            client.readFromTape(DRIVE_INDEX, readDest.toString());

            assertMarkerExists(filename, READ_OK, READ_MARKER_TIMEOUT, POLL_INTERVAL_READ);

            // Remove marker
            Files.deleteIfExists(getMarkerPath(filename, READ_OK));

            // Second read - should be instant (file already prepared)
            client.readFromTape(DRIVE_INDEX, readDest.toString());

            assertMarkerExists(filename, READ_OK, FAST_READ_MARKER_TIMEOUT, POLL_INTERVAL);
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_fail_read_nonexistent_file() {
        String missing = VITAM_STORAGE_DIR + "/missing-" + System.currentTimeMillis() + ".tar";
        assertThatThrownBy(() -> createClient().readFromTape(0, missing)).isInstanceOf(
            fr.gouv.vitam.storage.cold.client.invoker.ApiException.class
        );
    }

    // ==================== ROBOTIC OPERATIONS ====================

    @Test
    public void should_load_and_unload_tape() throws IOException {
        InaTapeProxyApi client = createClient();
        assertThatCode(() -> {
            client.loadTape(1, DRIVE_INDEX);
            client.unloadTape(1, DRIVE_INDEX);
        }).doesNotThrowAnyException();
    }

    @Test
    public void should_move_tape_backword_forward() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX);
        writeMultipleFiles(10, client);
        assertThatCode(() -> {
            client.move(DRIVE_INDEX, 5, true);
            client.move(DRIVE_INDEX, 3, false);
        }).doesNotThrowAnyException();
    }

    @Test
    public void should_rewind_tape() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX);
        writeMultipleFiles(5, client);
        assertThatCode(() -> client.rewind(DRIVE_INDEX)).doesNotThrowAnyException();
    }

    @Test
    public void should_go_to_end_of_data() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX);
        writeMultipleFiles(5, client);
        assertThatCode(() -> client.goToEnd(DRIVE_INDEX)).doesNotThrowAnyException();
    }

    @Test
    public void should_eject_tape() throws Exception {
        InaTapeProxyApi client = createClient();
        client.loadTape(1, DRIVE_INDEX);
        assertThatCode(() -> client.eject(DRIVE_INDEX)).doesNotThrowAnyException();
    }

    // ==================== HELPERS ====================

    private InaTapeProxyApi createClient() throws IOException {
        File conf = PropertiesUtils.getResourceFile(CLIENT_CONFIG);
        ClientConfiguration config = PropertiesUtils.readYaml(conf, SecureClientConfigurationImpl.class);
        return new InaTapeProxyClientFactory(config).getInaTapeProxyClient();
    }

    private String filename(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + ".tar";
    }

    private Path createFile(String filename, String content) throws IOException {
        Path path = Paths.get(VITAM_STORAGE_DIR, filename);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }

    private Path getMarkerPath(String filename, String suffix) {
        return Paths.get(MARKERS_DIR, filename + suffix);
    }

    private void assertMarkerExists(String filename) {
        assertMarkerExists(filename, InaTapeProxyIT.WRITE_OK, DEFAULT_MARKER_TIMEOUT, POLL_INTERVAL);
    }

    private void assertMarkerExists(String filename, String suffix, Duration timeout, Duration pollInterval) {
        Path marker = getMarkerPath(filename, suffix);
        await().atMost(timeout).pollInterval(pollInterval).untilAsserted(() -> assertThat(marker).exists());
    }

    private void assertFileExists(String dir, String filename, String expectedContent) throws IOException {
        Path file = Paths.get(dir, filename);
        assertThat(file).exists();
        assertThat(Files.readString(file)).isEqualTo(expectedContent);
    }

    private void cleanup(String filename, Path source) throws IOException {
        Files.deleteIfExists(source);
        Files.deleteIfExists(Paths.get(WRITE_DIR, filename));
        Files.deleteIfExists(Paths.get(READ_DIR, filename));
        Files.deleteIfExists(Paths.get(VITAM_STORAGE_DIR, filename));
        Files.deleteIfExists(getMarkerPath(filename, WRITE_OK));
        Files.deleteIfExists(getMarkerPath(filename, READ_OK));
    }

    private void writeMultipleFiles(int nbrFiles, InaTapeProxyApi client) throws Exception {
        for (int i = 0; i < nbrFiles; i++) {
            String file = filename("filename_" + i);
            Path path = createFile(file, "content_" + i);
            client.writeToTape(DRIVE_INDEX, path.toString());
        }
    }

    private void copyToInaStorage(String filename) throws IOException {
        Path sourceFile = Paths.get(WRITE_DIR, filename);
        Path destFile = Paths.get(INA_STORAGE_DIR, filename);
        Files.copy(sourceFile, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
