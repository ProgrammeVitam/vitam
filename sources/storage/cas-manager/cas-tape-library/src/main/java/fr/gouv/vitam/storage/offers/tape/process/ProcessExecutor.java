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
package fr.gouv.vitam.storage.offers.tape.process;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessExecutor {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessExecutor.class);

    private static final ProcessExecutor instance = new ProcessExecutor();

    public static ProcessExecutor getInstance() {
        return instance;
    }

    public Output execute(String commandPath, long timeoutInMilliseconds, List<String> args) {
        return execute(commandPath, false, timeoutInMilliseconds, args);
    }

    public Output execute(
        String commandPath,
        boolean redirectStreamToFile,
        long timeoutInMilliseconds,
        List<String> args
    ) {
        List<String> command = Lists.newArrayList(commandPath);

        if (!CollectionUtils.isEmpty(args)) {
            command.addAll(args);
        }

        String operationId = GUIDFactory.newGUID().toString();
        LOGGER.info(
            "[" +
            operationId +
            "] < Running '" +
            Strings.join(command, "' '") +
            "' with timeout " +
            timeoutInMilliseconds
        );
        Stopwatch started = Stopwatch.createStarted();

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = null;
        Output result;

        File cmd_stdout = null;
        File cmd_stderr = null;

        try {
            if (redirectStreamToFile) {
                cmd_stderr = File.createTempFile(
                    "cmd_stderr_",
                    GUIDFactory.newGUID().getId(),
                    new File(VitamConfiguration.getVitamTmpFolder())
                );
                cmd_stdout = File.createTempFile(
                    "cmd_stdout_",
                    GUIDFactory.newGUID().getId(),
                    new File(VitamConfiguration.getVitamTmpFolder())
                );

                processBuilder.redirectError(cmd_stderr);
                processBuilder.redirectOutput(cmd_stdout);
            }

            process = processBuilder.start();

            boolean processExit = process.waitFor(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
            if (processExit) {
                result = new Output(process, process.exitValue(), processBuilder, cmd_stdout, cmd_stderr);
            } else {
                result = new Output(process, Output.EXIT_CODE_WAIT_FOR_TIMEOUT, processBuilder, cmd_stdout, cmd_stderr);
            }
        } catch (Exception e) {
            result = new Output(e, process, processBuilder, cmd_stdout, cmd_stderr);
        } finally {
            FileUtils.deleteQuietly(cmd_stdout);
            FileUtils.deleteQuietly(cmd_stderr);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + operationId + "] Response details : " + JsonHandler.unprettyPrint(result));
        }

        if (0 == result.getExitCode()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "[" +
                    operationId +
                    "] > Success (" +
                    started.elapsed(TimeUnit.MILLISECONDS) +
                    " ms): " +
                    JsonHandler.unprettyPrint(result)
                );
            } else {
                LOGGER.info("[" + operationId + "] > Success (" + started.elapsed(TimeUnit.MILLISECONDS) + " ms)");
            }
        } else {
            LOGGER.error(
                "[" +
                operationId +
                "] > KO " +
                JsonHandler.unprettyPrint(result) +
                " (" +
                started.elapsed(TimeUnit.MILLISECONDS) +
                " ms)"
            );
        }
        return result;
    }
}
