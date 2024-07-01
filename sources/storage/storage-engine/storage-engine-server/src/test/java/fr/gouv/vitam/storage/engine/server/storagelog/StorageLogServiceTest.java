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
package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.assertj.core.api.AbstractLongAssert;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.gouv.vitam.storage.engine.server.storagelog.StorageLogService.WRITE_LOG_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageLogServiceTest {

    private static final int TENANTS = 3;

    private StorageLog storageLogService;

    private static List<Integer> tenants;

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        tenants = new ArrayList<>();
        for (int i = 0; i < TENANTS; i++) {
            tenants.add(i);
        }
    }

    @After
    public void cleanUp() {
        storageLogService.close();
    }

    @Test
    public void appendTest() throws Exception {
        File workingDir = folder.newFolder();
        storageLogService = new StorageLogService(tenants, Paths.get(workingDir.getAbsolutePath()));
        storageLogService.appendWriteLog(0, buildStorageParameters("tenant0-param1"));
        storageLogService.appendWriteLog(1, buildStorageParameters("tenant1-param1"));
        storageLogService.appendWriteLog(0, buildStorageParameters("tenant0-param2"));

        storageLogService.close();

        Path path = Paths.get(workingDir.getAbsolutePath()).resolve(WRITE_LOG_DIR);
        List<Path> files = Files.list(path).sorted().collect(Collectors.toList());

        assertThat(files).hasSize(TENANTS);

        Path file1 = files.get(0);
        assertThat(file1.getFileName().toString()).matches("0_\\d+_.*\\.log");
        assertFileContent(
            file1,
            "{\"objectIdentifier\":\"tenant0-param1\"}\n{\"objectIdentifier\":\"tenant0-param2\"}\n"
        );

        Path file2 = files.get(1);
        assertThat(file2.getFileName().toString()).matches("1_\\d+_.*\\.log");
        assertFileContent(file2, "{\"objectIdentifier\":\"tenant1-param1\"}\n");

        Path file3 = files.get(2);
        assertEmptyFile(file3);
    }

    @Test
    public void rotateLogsTest() throws IOException {
        File workingDir = folder.newFolder();

        storageLogService = new StorageLogService(tenants, Paths.get(workingDir.getAbsolutePath()));

        // Given / when
        LocalDateTime date1 = LocalDateUtil.now();

        storageLogService.appendWriteLog(0, buildStorageParameters("tenant0-param1"));
        storageLogService.appendWriteLog(1, buildStorageParameters("tenant1-param1"));
        storageLogService.appendWriteLog(0, buildStorageParameters("tenant0-param2"));

        LocalDateTime date2 = LocalDateUtil.now();

        List<LogInformation> logInformation = storageLogService
            .rotateLogFile(0, true)
            .stream()
            .sorted(Comparator.comparing(i -> i.getPath().getFileName().toString()))
            .collect(Collectors.toList());

        storageLogService.close();

        LocalDateTime date3 = LocalDateUtil.now();

        // Assert

        assertThat(logInformation).hasSize(1);

        assertThat(logInformation.get(0).getBeginTime()).isBeforeOrEqualTo(date1);
        assertThat(logInformation.get(0).getEndTime()).isBetween(date2, date3);

        Path path = Paths.get(workingDir.getAbsolutePath()).resolve(WRITE_LOG_DIR);
        List<Path> files = Files.list(path).sorted().collect(Collectors.toList());

        assertThat(files).hasSize(4);

        assertThat(logInformation.get(0).getPath().toAbsolutePath().toString()).isEqualTo(
            files.get(0).toAbsolutePath().toString()
        );

        assertFileContent(
            files.get(0),
            "{\"objectIdentifier\":\"tenant0-param1\"}\n{\"objectIdentifier\":\"tenant0-param2\"}\n"
        );
        assertFileContent(files.get(1), "");
        assertFileContent(files.get(2), "{\"objectIdentifier\":\"tenant1-param1\"}\n");

        assertEmptyFile(files.get(3));
    }

    @Test
    public void multiThreadedAppendRotateLogsTest() throws Exception {
        File workingDir = folder.newFolder();

        /*
         * For each tenant, run "NB_THREADS_PER_TENANT" threads that continuously append log entries
         * For each tenant, run a thread that rotate logs every "INTERVAL_BETWEEN_LOG_ROTATION"
         * Wait for "TEST_DURATION_IN_MILLISECONDS" and stop threads
         * Ensure that all messages have been logged
         */
        storageLogService = new StorageLogService(tenants, Paths.get(workingDir.getAbsolutePath()));

        int TEST_DURATION_IN_MILLISECONDS = 1500;
        int INTERVAL_BETWEEN_LOG_ROTATION = 30;
        int NB_THREADS_PER_TENANT = 10;

        List<AtomicInteger> tenantCpt = new ArrayList<>();
        for (int i = 0; i < TENANTS; i++) {
            tenantCpt.add(new AtomicInteger());
        }

        MultiValuedMap<Integer, String> loggedDataByTenant = new ArrayListValuedHashMap<>();

        CountDownLatch stopSignal = new CountDownLatch(1);

        ExecutorService executorService = Executors.newFixedThreadPool(
            NB_THREADS_PER_TENANT * TENANTS + TENANTS,
            VitamThreadFactory.getInstance()
        );

        // Threads for appending messages
        for (int i = 0; i < NB_THREADS_PER_TENANT * TENANTS; i++) {
            final int tenant = i % TENANTS;

            executorService.submit(() -> {
                try {
                    while (!stopSignal.await(0, TimeUnit.MILLISECONDS)) {
                        storageLogService.appendWriteLog(
                            tenant,
                            buildStorageParameters(
                                "tenant" + tenant + "-param" + tenantCpt.get(tenant).getAndIncrement()
                            )
                        );
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Threads for rotating logs every INTERVAL_BETWEEN_LOG_ROTATION
        for (int i = 0; i < TENANTS; i++) {
            final int tenant = i;

            executorService.submit(() -> {
                try {
                    while (!stopSignal.await(INTERVAL_BETWEEN_LOG_ROTATION, TimeUnit.MILLISECONDS)) {
                        List<LogInformation> logInformation = storageLogService.rotateLogFile(tenant, true);
                        for (LogInformation logInfo : logInformation) {
                            synchronized (loggedDataByTenant) {
                                loggedDataByTenant.putAll(
                                    tenant,
                                    Files.readAllLines(logInfo.getPath(), StandardCharsets.UTF_8)
                                );
                            }
                            Files.delete(logInfo.getPath());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Wait for TEST_DURATION_IN_MILLISECONDS and send stop signal
        stopSignal.await(TEST_DURATION_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        stopSignal.countDown();

        // Await termination
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Flush any buffered data to disk
        storageLogService.close();

        // Read non remaining log files (non rotated)
        Path path = Paths.get(workingDir.getAbsolutePath()).resolve(WRITE_LOG_DIR);
        List<Path> files = Files.list(path).sorted().collect(Collectors.toList());

        assertThat(files).hasSize(TENANTS);

        for (int i = 0; i < TENANTS; i++) {
            loggedDataByTenant.putAll(i, Files.readAllLines(files.get(i), StandardCharsets.UTF_8));
        }

        // Ensure all data have been written to disk
        for (int tenant = 0; tenant < TENANTS; tenant++) {
            List<String> sortedTenantLog = loggedDataByTenant
                .get(tenant)
                .stream()
                .sorted(
                    Comparator.comparing(
                        (String s) -> Integer.parseInt(s.substring(s.lastIndexOf("-param") + 6, s.lastIndexOf("\"")))
                    )
                )
                .collect(Collectors.toList());

            assertThat(sortedTenantLog).hasSize(tenantCpt.get(tenant).get());
            System.out.println("Nb message for tenant " + tenant + "=" + sortedTenantLog.size());

            for (int i = 0; i < sortedTenantLog.size(); i++) {
                assertThat(sortedTenantLog.get(i)).isEqualTo(
                    "{\"objectIdentifier\":\"tenant" + tenant + "-param" + i + "\"}"
                );
            }
        }
    }

    private StorageLogbookParameters buildStorageParameters(String str) {
        StorageLogbookParameters params = mock(StorageLogbookParameters.class);
        Map<StorageLogbookParameterName, String> mapParameters = new HashMap<>();
        mapParameters.put(StorageLogbookParameterName.objectIdentifier, str);
        when(params.getMapParameters()).thenReturn(mapParameters);
        return params;
    }

    private void assertFileContent(Path file, String expectedContent) throws IOException {
        assertThat(Files.readAllBytes(file)).isEqualTo(expectedContent.getBytes(StandardCharsets.UTF_8));
    }

    private AbstractLongAssert<?> assertEmptyFile(Path filePath) throws IOException {
        return assertThat(Files.size(filePath)).isEqualTo(0);
    }
}
