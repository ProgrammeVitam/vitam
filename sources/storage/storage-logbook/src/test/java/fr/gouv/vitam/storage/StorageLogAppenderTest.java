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
package fr.gouv.vitam.storage;

import fr.gouv.vitam.storage.logbook.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.logbook.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.logbook.parameters.StorageLogbookParameters;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        "{eventDateTime=2016-07-29T11:56:35.914, objectIdentifier=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, objectGroupIdentifier=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, digest=aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq, digestAlgorithm=" +
            "SHA-256, size=1024, agentIdentifiers=agentIdentifiers, agentIdentifierRequester=agentIdentifierRequester, outcome=OK}\n";

    @Test
    public void appenderTest() throws Exception {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        File currentFolder = folder.newFolder();
        StorageLogAppender storageLogAppender = new StorageLogAppender(list, currentFolder.toPath());
        assertThat(currentFolder.list().length).isEqualTo(list.size());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LogInformation logInformation = storageLogAppender.secureAndCreateNewlogByTenant(2);
        assertThat(logInformation.getPath().toString()).startsWith(currentFolder.toString() + "/2_");
        assertThat(logInformation.getPath().toString()).contains("_" + LocalDateTime.now().format(formatter));
        assertThat(currentFolder.list().length).isEqualTo(list.size() + 1);
       // assertThat(logInformation.getBeginTime()).isBefore(logInformation.getEndTime());
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
        list.add(1);
        list.add(2);

        // When
        StorageLogAppender storageLogAppender = new StorageLogAppender(list, currentFolder.toPath());

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.submit(() -> {
            int i = 10;
            while (i > 0) {
                try {
                    storageLogAppender.append(1, getParameters());
                    Thread.sleep(50L);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                i--;
            }
        });
        //execute 10 times
        executorService.submit(() -> {
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
        });
        //execute 10 times

        executorService.submit(() -> {
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
        });
        executorService.shutdown();
        Thread.sleep(2000L);
        String[] lines = currentFolder.list();
        assertThat(lines.length).isEqualTo(7);

        LogInformation info = storageLogAppender.secureAndCreateNewlogByTenant(1);
        int nbLines = 0;
        for (String file : lines) {
            nbLines += Files.lines(Paths.get(currentFolder.getAbsolutePath() + "/" + file)).count();
        }
        assertThat(nbLines).isEqualTo(20);
    }

    private StorageLogbookParameters getParameters() {
        final Map<StorageLogbookParameterName, String> parameters = new TreeMap<>();
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
