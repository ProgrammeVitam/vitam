/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server.simulator.filesystem;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages filesystem directories for INA
 * Responsible for:
 * - Creating and managing exchange directories
 * - Providing paths to write, read, and markers directories
 */
public class FileSystemManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileSystemManager.class);

    private final InaTapeProxyConfiguration configuration;

    private Path writeDir;
    private Path readDir;
    private Path markersDir;
    private Path inaStorageDir;

    public FileSystemManager(InaTapeProxyConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Initialize filesystem directories
     * Creates necessary directories automatically
     */
    public void initialize() throws IOException {
        String baseDir = configuration.getExchangeDirectory();
        Path exchangeDir = Paths.get(baseDir);
        String inaStorageBase = configuration.getInaStorageDirectory();
        this.inaStorageDir = Paths.get(inaStorageBase);

        this.writeDir = exchangeDir.resolve("write");
        this.readDir = exchangeDir.resolve("read");
        this.markersDir = exchangeDir.resolve("markers");

        LOGGER.info("FileSystemManager initializing: exchangeDir={}, inaStorageDir={}", exchangeDir, inaStorageDir);

        Files.createDirectories(writeDir);
        Files.createDirectories(readDir);
        Files.createDirectories(markersDir);
        Files.createDirectories(inaStorageDir);

        LOGGER.info(
            "FileSystemManager initialized successfully: write={}, read={}, markers={}, inaStorage={}",
            writeDir,
            readDir,
            markersDir,
            inaStorageDir
        );
    }

    /**
     * Get path to a file in the read directory
     */
    public Path getReadPath(String filename) {
        return readDir.resolve(filename);
    }

    /**
     * Get path to a file in the write directory
     */
    public Path getWritePath(String filename) {
        return writeDir.resolve(filename);
    }

    /**
     * Get path to a marker file in the markers directory
     */
    public Path getMarkersPath(String filename) {
        return markersDir.resolve(filename);
    }

    /**
     * Get path to a file in the INA storage directory
     */
    public Path getInaStoragePath(String filename) {
        return inaStorageDir.resolve(filename);
    }
}
