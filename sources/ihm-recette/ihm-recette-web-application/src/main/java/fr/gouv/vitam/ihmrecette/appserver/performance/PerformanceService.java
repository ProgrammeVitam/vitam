/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.performance;

import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.ingest.external.core.Contexts.DEFAULT_WORKFLOW;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;

/**
 *
 */
public class PerformanceService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PerformanceService.class);
    public static final String DEFAULT_CONTRACT_NAME = "test_perf";
    private final IngestExternalClientFactory ingestClientFactory;

    private AtomicBoolean performanceTestInProgress = new AtomicBoolean(false);

    /**
     *
     */
    private final Path sipDirectory;

    /**
     *
     */
    private final Path performanceReportDirectory;

    /**
     * @param sipDirectory base sip directory
     * @param performanceReportDirectory base report directory
     */
    public PerformanceService(Path sipDirectory, Path performanceReportDirectory) {
        this(IngestExternalClientFactory.getInstance(), sipDirectory, performanceReportDirectory);
    }

    public PerformanceService(IngestExternalClientFactory ingestClientFactory, Path sipDirectory,
        Path performanceReportDirectory) {
        this.sipDirectory = sipDirectory;
        this.ingestClientFactory = ingestClientFactory;
        this.performanceReportDirectory = performanceReportDirectory;
    }

    /**
     * indicate if a report is in progress
     *
     * @return
     */
    public boolean inProgress() {
        return performanceTestInProgress.get();
    }

    /**
     * @param model
     * @param fileName report name
     * @param tenantId tenant
     * @throws IOException
     */
    public void launchPerformanceTest(PerformanceModel model, String fileName, int tenantId) throws IOException {
        ExecutorService launcherPerformanceExecutor = Executors.newFixedThreadPool(model.getParallelIngest());
        ExecutorService reportExecutor = Executors.newSingleThreadExecutor();

        LOGGER.info("start performance test");

        ReportGenerator reportGenerator = new ReportGenerator(performanceReportDirectory.resolve(fileName));

        performanceTestInProgress.set(true);

        List<CompletableFuture<Void>> collect = IntStream.range(0, model.getNumberOfIngest())
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> uploadSIP(model, tenantId), launcherPerformanceExecutor))
            .map(
                future -> future.thenAcceptAsync((id) -> generateReport(reportGenerator, id, tenantId), reportExecutor))
            .collect(Collectors.toList());

        CompletableFuture<List<Void>> allDone = sequence(collect);

        allDone.thenRun(() -> {
            try {
                reportGenerator.close();
                launcherPerformanceExecutor.shutdown();
                reportExecutor.shutdown();
                performanceTestInProgress.set(false);
                LOGGER.info("end performance test");
            } catch (IOException e) {
                LOGGER.error("unable to close report", e);
            }
        }).exceptionally((e) -> {
            LOGGER.error("end performance test with error", e);
            performanceTestInProgress.set(false);
            return null;
        });
    }

    private void generateReport(ReportGenerator reportGenerator, String operationId, int tenantId) {
        try {
            LOGGER.debug("generate report");
            final RequestResponse<JsonNode> requestResponse =
                UserInterfaceTransactionManager.selectOperationbyId(operationId, tenantId, DEFAULT_CONTRACT_NAME);

            if (requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;

                final JsonNode logbookOperation = requestResponseOK.getResults().get(0);
                reportGenerator.generateReport(operationId, logbookOperation);
            }
        } catch (IOException | VitamException | ParseException e) {
            LOGGER.error("unable to generate report", e);
        }
    }

    private String uploadSIP(PerformanceModel model, Integer tenantId) {
        // TODO: client is it thread safe ?
        LOGGER.debug("launch unitary test");
        IngestExternalClient client = ingestClientFactory.getClient();
        try (InputStream sipInputStream = Files.newInputStream(sipDirectory.resolve(model.getFileName()),
            StandardOpenOption.READ)) {

            String requestId =
                client.uploadAndWaitFinishingProcess(sipInputStream, tenantId, DEFAULT_WORKFLOW.name(), RESUME.name());

            LOGGER.debug("finish unitary test");
            return requestId;
        } catch (final Exception e) {
            LOGGER.error("unable to upload sip", e);
            return null;
        } finally {
            client.close();
        }
    }

    /**
     * transform a list of future on future of list
     *
     * @param futures list of futures
     * @param <T>
     * @return future of list
     */
    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture
            .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.<T>toList()));
    }

    /**
     * list all the sip
     *
     * @return
     * @throws IOException
     */
    public List<Path> listSipDirectory() throws IOException {
        List<Path> paths = new ArrayList<>();

        Files.walkFileTree(sipDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().toLowerCase().endsWith("zip")) {
                    paths.add(sipDirectory.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    /**
     * list all reports
     *
     * @return
     * @throws IOException
     */
    public List<Path> listReportDirectory() throws IOException {
        return listDirectory(performanceReportDirectory);
    }

    public boolean sipExist(String sipPath) {
        return Files.exists(sipDirectory.resolve(sipPath));
    }

    /**
     * return an InputStream for a reportName
     *
     * @param reportName path of the report
     * @return
     * @throws IOException
     */
    public InputStream readReport(String reportName) throws IOException {
        return Files.newInputStream(performanceReportDirectory.resolve(reportName));
    }

    private List<Path> listDirectory(Path directory) throws IOException {
        return Files.list(directory)
            .collect(Collectors.toList());
    }


}
