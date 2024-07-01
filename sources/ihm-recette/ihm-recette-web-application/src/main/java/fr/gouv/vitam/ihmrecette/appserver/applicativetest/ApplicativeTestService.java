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
package fr.gouv.vitam.ihmrecette.appserver.applicativetest;

import com.google.common.base.Throwables;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * service to manage cucumber test
 */
public class ApplicativeTestService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ApplicativeTestResource.class);

    /**
     * custom formatter
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * name of the package
     */
    private static final String GLUE_CODE_PACKAGE = "fr.gouv.vitam.functionaltest.cucumber";
    private static final String PROCESS_EXIT_STATUS = "process exit status ";

    /**
     * flag to indicate if a test is in progress or not.
     */
    private AtomicBoolean inProgress;

    /**
     * executor to launch test in a separate thread
     */
    private Executor executor;

    /**
     * flag to indicate if  TnrMasterActived  .
     */
    private AtomicBoolean isTnrMasterActived;

    private String tnrBranch = "master";
    /**
     * cucumber launcher
     */
    private CucumberLauncher cucumberLauncher;

    /**
     * path of the tnr report directory
     */
    private Path tnrReportDirectory;

    public ApplicativeTestService(Path tnrReportDirectory) {
        this.tnrReportDirectory = tnrReportDirectory;
        this.inProgress = new AtomicBoolean(false);
        this.executor = Executors.newSingleThreadExecutor(VitamThreadFactory.getInstance());
        this.cucumberLauncher = new CucumberLauncher(tnrReportDirectory);
        this.isTnrMasterActived = new AtomicBoolean(false);
    }

    /**
     * launch cucumber test.
     *
     * @param featurePath path to the feature
     */
    String launchCucumberTest(Path featurePath) {
        if (!featurePath.toFile().exists()) {
            throw new VitamRuntimeException("path does not exist: " + featurePath);
        }
        String fileName = String.format("report_%s.json", LocalDateUtil.now().format(DATE_TIME_FORMATTER));

        inProgress.set(true);
        executor.execute(() -> {
            List<String> arguments = cucumberLauncher.buildCucumberArgument(GLUE_CODE_PACKAGE, featurePath, fileName);
            try {
                cucumberLauncher.launchCucumberTest(arguments);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            inProgress.set(false);
        });

        return fileName;
    }

    String launchPiecesCucumberTest(String pieces) throws IOException {
        File reportFile = File.createTempFile("tmp", ".json", new File(VitamConfiguration.getVitamTmpFolder()));
        File featureFile = File.createTempFile("tmp", ".feature", new File(VitamConfiguration.getVitamTmpFolder()));

        try {
            Files.write(featureFile.toPath(), pieces.getBytes());
            List<String> arguments = cucumberLauncher.buildCucumberArgument(
                GLUE_CODE_PACKAGE,
                featureFile.toPath(),
                reportFile.getAbsolutePath()
            );
            cucumberLauncher.launchCucumberTest(arguments);
            return String.join(System.lineSeparator(), Files.readAllLines(reportFile.toPath()));
        } finally {
            Files.deleteIfExists(featureFile.toPath());
            Files.deleteIfExists(reportFile.toPath());
        }
    }

    /**
     * @return if a tnr is in progress.
     */
    boolean inProgress() {
        return inProgress.get();
    }

    /**
     * @return the list of reports.
     */
    List<Path> reports() throws IOException {
        try (Stream<Path> pathStream = Files.list(tnrReportDirectory)) {
            return pathStream.collect(Collectors.toList());
        }
    }

    /**
     * @param fileName name of the report.
     * @return stream on the report.
     * @throws IOException if the report is not found.
     */
    InputStream readReport(String fileName) throws IOException {
        return Files.newInputStream(tnrReportDirectory.resolve(fileName));
    }

    int synchronizedTestDirectory(Path featurePath) throws IOException, InterruptedException {
        LOGGER.debug("git pull rebase on " + featurePath);

        ProcessBuilder pb = new ProcessBuilder("git", "pull", "--rebase");
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        LOGGER.debug(PROCESS_EXIT_STATUS + p.exitValue());

        return p.exitValue();
    }

    /**
     * @param featurePath
     * @param branch
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    int checkout(Path featurePath, String branch) throws IOException, InterruptedException {
        LOGGER.debug("git checkout" + branch);

        ProcessBuilder pb = new ProcessBuilder("git", "checkout", branch);
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        LOGGER.debug(PROCESS_EXIT_STATUS + p.exitValue());

        return p.exitValue();
    }

    List<String> getBranches(Path featurePath) throws IOException, InterruptedException {
        LOGGER.debug("git get branches");

        ProcessBuilder pb = new ProcessBuilder(
            "git",
            "for-each-ref",
            "--sort=-committerdate",
            "refs/remotes/",
            "--format='%(refname:short)'"
        );
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        String stdout = stdToString(p.getInputStream());
        LOGGER.debug("process output " + stdout);

        return Arrays.asList(stdout.replaceAll("'", "").replaceAll("[[a-zA-Z0-9]_]+/", "").split(" \\| "));
    }

    private static String stdToString(InputStream std) {
        CircularFifoQueue<String> lastLines = new CircularFifoQueue<>(250);
        List<String> firstLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(std, UTF_8))) {
            String c;
            while ((c = reader.readLine()) != null) {
                if (firstLines.size() < 250) {
                    firstLines.add(c);
                } else {
                    lastLines.add(c);
                }
            }
        } catch (IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            return (
                Stream.concat(firstLines.stream(), lastLines.stream()).collect(Collectors.joining(" | ")) +
                "|" +
                e.getMessage()
            );
        }
        return Stream.concat(firstLines.stream(), lastLines.stream()).collect(Collectors.joining(" | "));
    }

    int reset(Path featurePath, String branch) throws InterruptedException, IOException {
        LOGGER.debug("git reset origin/" + branch);

        ProcessBuilder processBuilder = new ProcessBuilder("git", "reset", "--hard", "origin/" + branch);
        processBuilder.directory(featurePath.toFile());
        Process process = processBuilder.start();
        process.waitFor();
        LOGGER.debug(PROCESS_EXIT_STATUS + process.exitValue());
        return process.exitValue();
    }

    void fetch(Path featurePath) throws InterruptedException, IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "fetch");
        processBuilder.directory(featurePath.toFile());
        Process process = processBuilder.start();
        process.waitFor();
        LOGGER.debug(PROCESS_EXIT_STATUS + process.exitValue());
        process.exitValue();
    }

    void setIsTnrMasterActived(AtomicBoolean isTnrMasterActived) {
        this.isTnrMasterActived = isTnrMasterActived;
    }

    AtomicBoolean getIsTnrMasterActived() {
        return isTnrMasterActived;
    }

    String getTnrBranch() {
        return tnrBranch;
    }

    void setTnrBranch(String tnrBranch) {
        this.tnrBranch = tnrBranch;
    }
}
