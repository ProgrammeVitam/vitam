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

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.common.exception.JavaExecuteScriptException;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

/**
 * Class JavaExecuteScript used to execute the shell script in java
 */
public class JavaExecuteScript {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(JavaExecuteScript.class);

    /**
     * Return status when execute the shell script scan-clamav.sh for scanning the file
     *
     * @param cmd the command line that will be executed
     * @param arg the file to scan
     * @param timeoutScanDelay in ms
     * @return The return value of the cmd or 3 if the execution failed
     * @throws JavaExecuteScriptException if script could not be executed
     */
    public static ExecutionOutput executeCommand(String cmd, String arg, long timeoutScanDelay)
        throws JavaExecuteScriptException {
        String scriptPath;
        try {
            scriptPath = PropertiesUtils.findFile(cmd).getPath();
        } catch (final FileNotFoundException e) {
            LOGGER.error(cmd + " does not exit");
            throw new JavaExecuteScriptException(e);
        }
        return exec(scriptPath, arg, timeoutScanDelay);
    }

    private static ExecutionOutput exec(String scriptPath, String arg, long timeoutScanDelay) {
        ProcessBuilder processBuilder = new ProcessBuilder(Lists.newArrayList(scriptPath, arg));
        Process process = null;
        try {
            process = processBuilder.start();
            process.waitFor(timeoutScanDelay, TimeUnit.MILLISECONDS);
            return new ExecutionOutput(process, processBuilder);
        } catch (Exception e) {
            return new ExecutionOutput(e, process, processBuilder);
        }
    }
}
