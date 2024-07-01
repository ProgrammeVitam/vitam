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
package fr.gouv.vitam.ihmrecette.appserver.performance;

import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static fr.gouv.vitam.ihmrecette.appserver.performance.PerformanceModel.createPerformanceTestInSequence;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class PerformanceServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IngestExternalClientFactory ingestClientFactory;

    @Mock
    private AdminExternalClientFactory adminClientFactory;

    @Test
    public void should_list_file_in_sip_directory() throws IOException {
        // Given
        Path sipDirectory = generateThreeZipFileSipWithSubDirectory();
        Path reportDirectory = temporaryFolder.newFolder().toPath();
        PerformanceService performanceService = new PerformanceService(
            ingestClientFactory,
            adminClientFactory,
            sipDirectory,
            reportDirectory,
            UserInterfaceTransactionManager.getInstance()
        );

        // When
        List<Path> files = performanceService.listSipDirectory();

        // Then
        assertThat(files).hasSize(3);
    }

    @Test
    public void should_list_file_in_report_directory() throws IOException {
        // Given
        Path reportDirectory = generateFileReport();
        PerformanceService performanceService = new PerformanceService(
            ingestClientFactory,
            adminClientFactory,
            null,
            reportDirectory,
            UserInterfaceTransactionManager.getInstance()
        );

        // When
        List<Path> files = performanceService.listReportDirectory();

        // Then
        assertThat(files).hasSize(2);
    }

    @Test
    public void should_read_generated_report() throws Exception {
        // Given
        Path reportDirectory = temporaryFolder.newFolder().toPath();
        String reportName = "1.txt";
        Files.write(reportDirectory.resolve(reportName), "test".getBytes());
        PerformanceService performanceService = new PerformanceService(
            ingestClientFactory,
            adminClientFactory,
            null,
            reportDirectory,
            UserInterfaceTransactionManager.getInstance()
        );

        // When
        InputStream inputStream = performanceService.readReport(reportName);

        // Then
        assertThat(inputStream).hasSameContentAs(new ByteArrayInputStream("test".getBytes()));
    }

    @Test
    public void should_launch_performance_test_and_generate_non_empty_file() throws Exception {
        // Given
        Path reportDirectory = temporaryFolder.newFolder().toPath();
        Path sipDirectory = temporaryFolder.newFolder().toPath();
        String reportName = "1.txt";
        String fileName = "result.csv";
        Path file = sipDirectory.resolve(fileName);
        Files.write(file, "test".getBytes());

        AdminExternalClient adminExternalClient = mock(AdminExternalClient.class);
        RequestResponseOK<ItemStatus> adminClientResponse = new RequestResponseOK<>();
        ItemStatus itemStatus = new ItemStatus();
        itemStatus.setGlobalState(ProcessState.COMPLETED);
        adminClientResponse.addResult(itemStatus);
        given(adminExternalClient.getOperationProcessStatus(any(), anyString())).willReturn(adminClientResponse);
        given(adminClientFactory.getClient()).willReturn(adminExternalClient);

        PerformanceService performanceService = new PerformanceService(
            ingestClientFactory,
            adminClientFactory,
            sipDirectory,
            reportDirectory,
            UserInterfaceTransactionManager.getInstance()
        );
        IngestExternalClient ingestExternalClient = mock(IngestExternalClient.class);
        given(ingestClientFactory.getClient()).willReturn(ingestExternalClient);
        RequestResponseOK<Void> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.getVitamHeaders().put(GlobalDataRest.X_REQUEST_ID, "operationId");
        given(ingestExternalClient.ingest(any(), any(), anyString(), anyString())).willReturn(requestResponseOK);

        // When
        PerformanceModel model = createPerformanceTestInSequence(fileName, 1, 1, 1);
        performanceService.launchPerformanceTest(model, reportName, 0);

        // Then
        assertThat(performanceService.inProgress()).isTrue();
        Thread.sleep(1500);
        assertThat(performanceService.inProgress()).isFalse();

        Path reportFile = reportDirectory.resolve(reportName);
        assertThat(Files.exists(reportFile)).isTrue();
        assertThat(reportFile.toFile().length()).isGreaterThan(0);
    }

    private Path generateFileReport() throws IOException {
        Path sipDirectory = temporaryFolder.newFolder().toPath();
        Files.createFile(sipDirectory.resolve("1.txt"));
        Files.createFile(sipDirectory.resolve("2.txt"));
        return sipDirectory;
    }

    private Path generateThreeZipFileSipWithSubDirectory() throws IOException {
        Path sipDirectory = temporaryFolder.newFolder().toPath();
        Files.createFile(sipDirectory.resolve("1.zip"));
        Files.createFile(sipDirectory.resolve("2.zip"));
        Path directories = Files.createDirectories(sipDirectory.resolve("data"));
        Files.createFile(directories.resolve("3.zip"));
        Files.createFile(directories.resolve("4.txt"));

        return sipDirectory;
    }
}
