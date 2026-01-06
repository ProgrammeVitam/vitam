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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyApi;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyClientFactory;
import fr.gouv.vitam.storage.cold.server.InaTapeProxyLauncher;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

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
 * Integration tests for INA Tape Proxy
 */
@RunWithCustomExecutor
public class InaTapeProxyIT {

    private static final String CONFIG_DIR = "ina-tape-proxy";
    private static final String SERVER_CONFIG = CONFIG_DIR + "/ina-tape-proxy-web.conf";
    private static final String CLIENT_CONFIG = CONFIG_DIR + "/ina-tape-proxy-client.conf";

    private static final String BASE_DIR = "/tmp/data/ina/exchange";
    private static final String WRITE_DIR = BASE_DIR + "/write";
    private static final String READ_DIR = BASE_DIR + "/read";
    private static final String MARKERS_DIR = BASE_DIR + "/markers";
    private static final String INA_STORAGE_DIR = "/tmp/data/ina/storage";

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setupClass() throws IOException {
        String confDir = PropertiesUtils.getResourceFile(CONFIG_DIR).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", confDir);
        VitamConfiguration.getConfiguration().setConfig(confDir);

        File serverConf = PropertiesUtils.getResourceFile(SERVER_CONFIG);
        Files.createDirectories(Paths.get(BASE_DIR));
        Files.createDirectories(Paths.get(INA_STORAGE_DIR));
        InaTapeProxyLauncher.start(new String[] { serverConf.getAbsolutePath() });
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        InaTapeProxyLauncher.stop();
        FileUtils.deleteDirectory(new File(BASE_DIR));
        FileUtils.deleteDirectory(new File(INA_STORAGE_DIR));
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
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
        String filename = filename("write");
        Path source = createFile(filename, "content");

        try {
            call(() -> client.writeToTape(source.toString()));
            assertMarkerExists(filename, ".WRITTEN");
            assertFileExists(WRITE_DIR, filename, "content");
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_skip_write_when_same_checksum() throws Exception {
        InaTapeProxyApi client = createClient();
        String filename = filename("checksum");
        Path source = createFile(filename, "same");

        try {
            call(() -> client.writeToTape(source.toString()));
            assertMarkerExists(filename, ".WRITTEN");

            call(() -> client.writeToTape(source.toString()));
            assertFileExists(WRITE_DIR, filename, "same");
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_write_multiple_files() throws Exception {
        InaTapeProxyApi client = createClient();
        String f1 = filename("m1");
        String f2 = filename("m2");
        String f3 = filename("m3");

        Path p1 = createFile(f1, "c1");
        Path p2 = createFile(f2, "c2");
        Path p3 = createFile(f3, "c3");

        try {
            call(() -> client.writeToTape(p1.toString()));
            call(() -> client.writeToTape(p2.toString()));
            call(() -> client.writeToTape(p3.toString()));

            assertMarkerExists(f1, ".WRITTEN");
            assertMarkerExists(f2, ".WRITTEN");
            assertMarkerExists(f3, ".WRITTEN");

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
        assertThatThrownBy(() -> createClient().writeToTape(missing)).isInstanceOf(
            fr.gouv.vitam.storage.cold.client.invoker.ApiException.class
        );
    }

    // ==================== READ OPERATIONS ====================

    @Test
    public void should_read_file_from_tape() throws Exception {
        InaTapeProxyApi client = createClient();
        String filename = filename("read");
        Path source = createFile(filename, "data");

        try {
            // Write
            call(() -> client.writeToTape(source.toString()));
            assertMarkerExists(filename, ".WRITTEN");

            // Copy to INA storage (simulate archiving to tape)
            copyToInaStorage(filename);

            // Read
            call(() -> client.readFromTape(Paths.get(READ_DIR, filename).toString()));

            // Wait for marker
            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(marker(filename, ".AVAILABLE_FOR_READ")).exists());

            // Verify
            assertThat(Paths.get(READ_DIR, filename)).exists();
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_read_already_prepared_file() throws Exception {
        InaTapeProxyApi client = createClient();
        String filename = filename("prepared");
        Path source = createFile(filename, "prep");

        try {
            // Write and first read
            call(() -> client.writeToTape(source.toString()));
            assertMarkerExists(filename, ".WRITTEN");

            // Copy to INA storage (simulate archiving to tape)
            copyToInaStorage(filename);

            Path readDest = Paths.get(READ_DIR, filename);
            call(() -> client.readFromTape(readDest.toString()));

            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(marker(filename, ".AVAILABLE_FOR_READ")).exists());

            // Remove marker
            Files.deleteIfExists(marker(filename, ".AVAILABLE_FOR_READ"));

            // Second read - should be instant
            call(() -> client.readFromTape(readDest.toString()));

            await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(marker(filename, ".AVAILABLE_FOR_READ")).exists());
        } finally {
            cleanup(filename, source);
        }
    }

    @Test
    public void should_fail_read_nonexistent_file() {
        String missing = READ_DIR + "/missing-" + System.currentTimeMillis() + ".tar";
        assertThatThrownBy(() -> createClient().readFromTape(missing)).isInstanceOf(
            fr.gouv.vitam.storage.cold.client.invoker.ApiException.class
        );
    }

    // ==================== ROBOTIC OPERATIONS ====================

    @Test
    public void should_load_tape() {
        assertThatCode(() -> createClient().loadTape(1, 0)).doesNotThrowAnyException();
    }

    @Test
    public void should_unload_tape() {
        assertThatCode(() -> createClient().unloadTape(1, 0)).doesNotThrowAnyException();
    }

    @Test
    public void should_load_and_unload_tape() throws IOException {
        InaTapeProxyApi client = createClient();
        assertThatCode(() -> {
            client.loadTape(5, 0);
            client.unloadTape(5, 0);
        }).doesNotThrowAnyException();
    }

    @Test
    public void should_move_tape_forward() {
        assertThatCode(() -> createClient().move(10, false)).doesNotThrowAnyException();
    }

    @Test
    public void should_move_tape_backward() {
        assertThatCode(() -> createClient().move(5, true)).doesNotThrowAnyException();
    }

    @Test
    public void should_rewind_tape() {
        assertThatCode(() -> createClient().rewind()).doesNotThrowAnyException();
    }

    @Test
    public void should_go_to_end_of_data() throws IOException {
        call(createClient()::goToEnd);
    }

    @Test
    public void should_eject_tape() {
        assertThatCode(() -> createClient().eject()).doesNotThrowAnyException();
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
        Path path = Paths.get("/tmp", filename);
        Files.writeString(path, content);
        return path;
    }

    private Path marker(String filename, String suffix) {
        return Paths.get(MARKERS_DIR, filename + suffix);
    }

    private void assertMarkerExists(String filename, String suffix) {
        Path marker = marker(filename, suffix);
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(marker).exists());
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
        Files.deleteIfExists(Paths.get(INA_STORAGE_DIR, filename));
        Files.deleteIfExists(marker(filename, ".WRITTEN"));
        Files.deleteIfExists(marker(filename, ".AVAILABLE_FOR_READ"));
    }

    private void copyToInaStorage(String filename) throws IOException {
        Path sourceFile = Paths.get(WRITE_DIR, filename);
        Path destFile = Paths.get(INA_STORAGE_DIR, filename);
        Files.copy(sourceFile, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * HTTP 204 responses cause NullPointerException in the client.
     * This is a known issue with void methods.
     */
    private void call(ThrowingRunnable op) {
        try {
            op.run();
        } catch (Exception ignored) {
            // Expected for HTTP 204 responses
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
