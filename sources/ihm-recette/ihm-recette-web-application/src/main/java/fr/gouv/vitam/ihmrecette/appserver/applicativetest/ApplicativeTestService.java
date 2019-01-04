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
package fr.gouv.vitam.ihmrecette.appserver.applicativetest;

import com.google.common.base.Throwables;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    public String launchCucumberTest(Path featurePath) {
        if (!Files.exists(featurePath)) {
            throw new RuntimeException("path does not exist: " + featurePath);
        }
        String fileName = String.format("report_%s.json", LocalDateTime.now().format(DATE_TIME_FORMATTER));

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


    public String launchPiecesCucumberTest(String pieces) throws IOException {


        File reportFile = File.createTempFile("tmp", ".json", new File(VitamConfiguration.getVitamTmpFolder()));
        File featureFile = File.createTempFile("tmp", ".feature", new File(VitamConfiguration.getVitamTmpFolder()));

        try {
            Files.write(featureFile.toPath(), pieces.getBytes());
            List<String> arguments = cucumberLauncher
                .buildCucumberArgument(GLUE_CODE_PACKAGE, featureFile.toPath(), reportFile.getAbsolutePath());
            cucumberLauncher.launchCucumberTest(arguments);
            final String result = String.join(System.lineSeparator(), Files.readAllLines(reportFile.toPath()));

            return result;
        } finally {
            Files.deleteIfExists(featureFile.toPath());
            Files.deleteIfExists(reportFile.toPath());
        }
    }

    /**
     * @return if a tnr is in progress.
     */
    public boolean inProgress() {
        return inProgress.get();
    }

    /**
     * @return the list of reports.
     */
    public List<Path> reports() throws IOException {
        return Files.list(tnrReportDirectory).collect(Collectors.toList());
    }

    /**
     * @param fileName name of the report.
     * @return stream on the report.
     * @throws IOException if the report is not found.
     */
    public InputStream readReport(String fileName) throws IOException {
        return Files.newInputStream(tnrReportDirectory.resolve(fileName));
    }

    public int synchronizedTestDirectory(Path featurePath) throws IOException, InterruptedException {
        LOGGER.debug("git pull rebase on " + featurePath);

        ProcessBuilder pb = new ProcessBuilder("git", "pull", "--rebase");
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        LOGGER.debug("process exit status " + p.exitValue());

        return p.exitValue();
    }



    /**
     * @param featurePath
     * @param branche
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    void checkout(Path featurePath, String branche) throws IOException, InterruptedException {
        LOGGER.debug("git checkout" + branche);

        ProcessBuilder pb = new ProcessBuilder("git", "checkout", branche);
        pb.directory(featurePath.toFile());
        Process p = pb.start();
        p.waitFor();
        LOGGER.debug("process exit status " + p.exitValue());
    }

    int resetTnrMaster(Path featurePath) throws InterruptedException, IOException {
        LOGGER.debug("git reset ");

        ProcessBuilder processBuilder = new ProcessBuilder("git", "reset", "--hard", "origin/tnr_master");
        processBuilder.directory(featurePath.toFile());
        Process process = processBuilder.start();
        process.waitFor();
        LOGGER.debug("process exit status " + process.exitValue());
        return process.exitValue();
    }

    void fetch(Path featurePath) throws InterruptedException, IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "fetch");
        processBuilder.directory(featurePath.toFile());
        Process process = processBuilder.start();
        process.waitFor();
        LOGGER.debug("process exit status " + process.exitValue());
        process.exitValue();
    }

    public void setIsTnrMasterActived(AtomicBoolean isTnrMasterActived) {
        this.isTnrMasterActived = isTnrMasterActived;
    }

    public AtomicBoolean getIsTnrMasterActived() {
        return isTnrMasterActived;
    }

    public String getTnrBranch() {
        return tnrBranch;
    }

    public void setTnrBranch(String tnrBranch) {
        this.tnrBranch = tnrBranch;
    }
}
