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
package fr.gouv.vitam.ingest.external.common.util;

import fr.gouv.vitam.common.logging.SysErrLogger;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ExecutionOutput {

    private final List<String> command;
    private final Exception exception;
    private final String stdout;
    private final String stderr;
    private final int exitCode;

    public ExecutionOutput(Exception exception, Process process, ProcessBuilder processBuilder) {
        this.exception = exception;
        this.command = processBuilder.command();
        this.exitCode = -1;
        if (null != process) {
            this.stdout = stdToString(process.getInputStream());
            this.stderr = stdToString(process.getErrorStream()) + "|" + exception.getMessage();
        } else {
            this.stdout = "";
            this.stderr = "";
        }
    }

    public ExecutionOutput(Process process, ProcessBuilder processBuilder) {
        this.command = processBuilder.command();
        this.exception = null;
        this.exitCode = process.exitValue();
        this.stdout = stdToString(process.getInputStream());
        this.stderr = stdToString(process.getErrorStream());
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

    public Exception getException() {
        return exception;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public List<String> getCommand() {
        return command;
    }
}
