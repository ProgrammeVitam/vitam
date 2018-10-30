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

package fr.gouv.vitam.griffins.imagemagick.pojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RawOutput {
    private final ProcessBuilder processBuilder;
    private final Input input;

    public final Exception exception;
    public final Action action;
    public final String stdout;
    public final String stderr;
    public final int exitCode;
    public final String outputName;

    public RawOutput(Exception exception, ProcessBuilder processBuilder, Input input, String outputName, Action action) {
        this.outputName = outputName;
        this.exception = exception;
        this.processBuilder = processBuilder;
        this.input = input;
        this.action = action;
        this.exitCode = 1;
        this.stdout = "";
        this.stderr = exception.getMessage();
    }

    public RawOutput(Process process, ProcessBuilder processBuilder, Input input, String outputName, Action action) throws IOException {
        this.processBuilder = processBuilder;
        this.input = input;
        this.outputName = outputName;
        this.action = action;
        this.exception = null;
        this.exitCode = process.exitValue();
        this.stdout = stdToString(process.getInputStream());
        this.stderr = stdToString(process.getErrorStream());
    }

    private static String stdToString(InputStream std) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(std, UTF_8))) {
            String c;
            while ((c = reader.readLine()) != null) {
                textBuilder.append(c);
            }
        }
        return textBuilder.toString();
    }

    public Output toError(boolean debug) {
        return debug
            ? Output.error(input, action.getType(), stderr, String.join(" ", processBuilder.command()))
            : Output.error(input, action.getType());
    }

    public Output toError(boolean debug, String newError) {
        return debug
            ? Output.error(input, action.getType(), newError, String.join(" ", processBuilder.command()))
            : Output.error(input, action.getType());
    }

    public Output toWarning(boolean debug) {
        return debug
            ? Output.warning(input, outputName, action.getType(), stderr, stdout, String.join(" ", processBuilder.command()))
            : Output.warning(input, outputName, action.getType());
    }

    public Output toOk(boolean debug) {
        return debug
            ? Output.ok(input, outputName, action.getType(), stderr, stdout, String.join(" ", processBuilder.command()))
            : Output.ok(input, outputName, action.getType());
    }
}
