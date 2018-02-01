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
package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class StorageLogAppenderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
    }

    private static final StorageLogbookOutcome OK_STATUS = StorageLogbookOutcome.OK;
    private static final StorageLogbookOutcome KO_STATUS = StorageLogbookOutcome.KO;
    private static final String DATE = "2016-07-29T11:56:35.914";
    private static final String LOG =
        "{eventDateTime=2016-07-29T11:56:35.914, xRequestId=abcd, tenantId=0, eventType=CREATE, objectIdentifier=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, objectGroupIdentifier=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, digest=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, digestAlgorithm=" +
            "SHA-256, size=1024, agentIdentifiers=agentIdentifiers, agentIdentifierRequester=agentIdentifierRequester, outcome=OK}\n";

    @Test
    public void appenderTest() throws Exception {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        File currentFolder = folder.newFolder();
        StorageLogAppender storageLogAppender = new StorageLogAppender(list, currentFolder.toPath());
        assertThat(currentFolder.list()).hasSize(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LogInformation logInformation = storageLogAppender.secureAndCreateNewlogByTenant(2);
        assertThat(logInformation.getPath().toString()).startsWith(currentFolder.toString() + "/2_");
        assertThat(logInformation.getPath().toString()).contains("_" + LocalDateTime.now().format(formatter));
        assertThat(currentFolder.list()).hasSize(4);
    }

    @Test
    public void soulTestAppendindLog() throws IOException {
        // Given
        File currentFolder = folder.newFolder();
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);

        // When
        StorageLogAppender storageLogAppender = new StorageLogAppender(list, currentFolder.toPath());
        storageLogAppender.append(1, getParameters());
        LogInformation info = storageLogAppender.secureAndCreateNewlogByTenant(1);

        // Then
        assertThat(Files.readAllBytes(info.getPath().toAbsolutePath())).isEqualTo(LOG.getBytes());
    }

    @Test
    public void testParallelism() throws IOException, InterruptedException {
        File currentFolder = folder.newFolder();
        final List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);

        // When
        StorageLogAppender storageLogAppender = new StorageLogAppender(list, currentFolder.toPath());

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(4);
        executorService.submit(() -> {
            try {
                startSignal.await();
                int i = 10;
                while (i > 0) {
                    try {
                        storageLogAppender.append(1, getParameters());
                        Thread.sleep(50L);
                    } catch (IOException | InterruptedException e) {
                        fail("should not raise execption");
                    }
                    i--;
                }
                doneSignal.countDown();
            } catch (InterruptedException ex) {
            } // return;

        });
        //execute 10 times on tenant 0
        executorService.submit(() -> {
            try {
                startSignal.await();
                int i = 10;
                while (i > 0) {
                    try {
                        storageLogAppender.append(0, getParameters());
                        Thread.sleep(50L);
                    } catch (IOException | InterruptedException e) {
                        fail("should not raise execption");
                    }
                    i--;
                }
                doneSignal.countDown();
            } catch (InterruptedException ex) {
            } // return;

        });
        //execute 10 times
        executorService.submit(() -> {
            try {
                startSignal.await();
                int i = 10;
                while (i > 0) {
                    try {
                        storageLogAppender.append(1, getParameters());
                        Thread.sleep(75L);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    i--;
                }
                doneSignal.countDown();
            } catch (InterruptedException ex) {
            } //
        });
        //execute 10 times

        executorService.submit(() -> {
            try {
                startSignal.await();
                int i = 5;
                while (i > 0) {
                    try {
                        LogInformation info = storageLogAppender.secureAndCreateNewlogByTenant(1);

                        Thread.sleep(100L);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    i--;
                }
                doneSignal.countDown();
            } catch (InterruptedException ex) {
            } //
        });
        executorService.shutdown();
        startSignal.countDown();      // let all threads proceed
        doneSignal.await();           // wait for all to finish
        String[] lines = currentFolder.list();
        assertThat(lines.length).isEqualTo(8);

        LogInformation info = storageLogAppender.secureAndCreateNewlogByTenant(1);
        int nbLines = 0;
        for (String file : lines) {
            if (file.startsWith("1")) {
                nbLines += Files.lines(Paths.get(currentFolder.getAbsolutePath() + "/" + file)).count();
            }
        }
        assertThat(nbLines).isEqualTo(20);
    }



    private StorageLogbookParameters getParameters() {
        final Map<StorageLogbookParameterName, String> parameters = new TreeMap<>();

        parameters.put(StorageLogbookParameterName.eventType, "CREATE");
        parameters.put(StorageLogbookParameterName.xRequestId, "abcd");
        parameters.put(StorageLogbookParameterName.tenantId, "0");
        parameters.put(StorageLogbookParameterName.objectIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.objectGroupIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digest, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digestAlgorithm, "SHA-256");
        parameters.put(StorageLogbookParameterName.size, "1024");
        parameters.put(StorageLogbookParameterName.agentIdentifiers, "agentIdentifiers");
        parameters.put(StorageLogbookParameterName.agentIdentifierRequester, "agentIdentifierRequester");
        parameters.put(StorageLogbookParameterName.eventDateTime, DATE);
        parameters.put(StorageLogbookParameterName.outcome, OK_STATUS.name());

        return new StorageLogbookParameters(parameters);
    }

}
