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

package fr.gouv.vitam.antivirus.rest;

import fr.gouv.vitam.antivirus.util.ExecutionOutput;
import fr.gouv.vitam.antivirus.util.JavaExecuteScript;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.io.File;
import java.util.Arrays;

@Path("/antivirus/v1")
@Tag(name = "Antivirus")
public class AntivirusResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AntivirusResource.class);

    private static final String MESSAGE_INVALID_PATH_PARAM = "Invalid path parameter";
    private static final String MESSAGE_FILE_NOT_FOUND = "File {} not found";
    private static final String MESSAGE_FILE_CANNOT_BE_READ = "File {} cannot be read";
    private static final String MESSAGE_OK_VIRUS = "No virus detected";
    private static final String MESSAGE_KO_VIRUS = "Virus detected on file {}";
    private static final String MESSAGE_FATAL_VIRUS = "Fatal error during virus scan";

    private static final int STATUS_ANTIVIRUS_OK = 0;
    private static final int STATUS_ANTIVIRUS_WARNING = 1;
    private static final int STATUS_ANTIVIRUS_KO = 2;

    private final AntivirusConfiguration antivirusConfiguration;

    /**
     * Constructor AntivirusResource
     *
     * @param antivirusConfiguration the configuration of server resource
     */
    public AntivirusResource(AntivirusConfiguration antivirusConfiguration) {
        this.antivirusConfiguration = antivirusConfiguration;
        LOGGER.info("init Antivirus Resource server");
    }

    /**
     * Scan file on local filesystem
     *
     * @param path the path of the file that we want to scan, relative to the base path
     */
    @Path("scanByPath")
    @GET
    public Response scanByPath(@QueryParam("path") String path) {
        final String[] basePaths = antivirusConfiguration.getBasePaths();
        final String antiVirusScriptName = antivirusConfiguration.getAntiVirusScriptName();
        final long timeoutScanDelay = antivirusConfiguration.getTimeoutScanDelay();
        final File file;
        try {
            SanityChecker.checkParameter(path);
            String basePath = Arrays.stream(basePaths).filter(path::startsWith).findFirst().orElse(null);
            if (basePath == null) {
                throw new IllegalPathException("File " + path + " is not within valid base paths");
            }
            String relativePath = path.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            file = SafeFileChecker.checkSafeFilePath(basePath, relativePath.split("/"));
        } catch (InvalidParseOperationException e) { // Should not occur as the regex forces a non-empty path
            LOGGER.error(MESSAGE_INVALID_PATH_PARAM, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (IllegalPathException e) {
            LOGGER.error(MESSAGE_FILE_NOT_FOUND, path, e);
            return Response.status(Status.NOT_FOUND).build();
        }
        if (!file.exists()) {
            LOGGER.error(MESSAGE_FILE_NOT_FOUND, path);
            return Response.status(Status.NOT_FOUND).build();
        }
        if (!file.canRead()) {
            LOGGER.error(MESSAGE_FILE_CANNOT_BE_READ, path);
            return Response.status(Status.FORBIDDEN).build();
        }
        JavaExecuteScript javaExecuteScript = new JavaExecuteScript();
        ExecutionOutput executionOutput;
        try {
            executionOutput = javaExecuteScript.executeCommand(
                antiVirusScriptName,
                file.getAbsolutePath(),
                timeoutScanDelay
            );
        } catch (final Exception e) {
            LOGGER.error("Cannot scan virus", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return switch (executionOutput.getExitCode()) {
            case STATUS_ANTIVIRUS_OK -> {
                LOGGER.info(MESSAGE_OK_VIRUS);
                yield Response.status(Status.OK).build();
            }
            case STATUS_ANTIVIRUS_WARNING, STATUS_ANTIVIRUS_KO -> {
                LOGGER.error(MESSAGE_KO_VIRUS, path);
                yield Response.status(Status.BAD_REQUEST).build();
            }
            default -> { // 3 (not performed) or -1 (exception occurred) or any other return code
                LOGGER.error("{},{},{}", MESSAGE_FATAL_VIRUS, executionOutput.getStdout(), executionOutput.getStderr());
                yield Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        };
    }
}
