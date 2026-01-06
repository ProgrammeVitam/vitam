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
package fr.gouv.vitam.storage.cold.server.simulator;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.manifest.naming.FileNameCleaner;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.server.simulator.filesystem.FileSystemManager;
import fr.gouv.vitam.storage.cold.server.simulator.filesystem.MarkerManager;
import fr.gouv.vitam.storage.engine.common.api.exception.TapeCommandException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * I/O Service for INA
 * Handles write and read operations with simple file copy
 */
public class InaIoService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaIoService.class);

    private final InaTapeProxyConfiguration configuration;
    private final FileSystemManager fsManager;
    private final MarkerManager markerManager;
    private final Path vitamRoot;

    public InaIoService(
        InaTapeProxyConfiguration configuration,
        FileSystemManager fsManager,
        MarkerManager markerManager
    ) {
        this.configuration = configuration;
        this.fsManager = fsManager;
        this.markerManager = markerManager;
        this.vitamRoot = Paths.get(configuration.getVitamStorageDirectory()).toAbsolutePath().normalize();
    }

    /**
     * Writes a file to tape (exchange directory).
     * If the file already exists in destination, checksums are compared to avoid unnecessary copy.
     */
    public void writeToTape(String inputPath) throws TapeCommandException {
        Path source = securePath(inputPath);

        String filename = secureFilename(source.getFileName().toString());
        LOGGER.info("WRITE START: {}", filename);

        try {
            Path destination = fsManager.getWritePath(filename);

            if (Files.exists(destination) && isSameContent(source, destination)) {
                LOGGER.info("Checksums match for {}. Skipping copy.", filename);
            } else {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                Thread.sleep(configuration.getWriteLatencyMs());
            }

            markerManager.createMarker(filename, MarkerManager.MarkerType.WRITTEN);
            LOGGER.info("WRITE END: {} ({}ms)", filename, configuration.getWriteLatencyMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapeCommandException("Write operation interrupted: " + filename, e);
        } catch (IOException e) {
            throw new TapeCommandException("I/O error during write: " + filename, e);
        }
    }

    /**
     * Read from tape: Check if file is already in read directory or prepare it asynchronously
     */
    public void readFromTape(String outputPath) throws TapeCommandException {
        Path destination = securePath(outputPath);
        String filename = secureFilename(destination.getFileName().toString());

        try {
            Path readPath = fsManager.getReadPath(filename);
            Path source = fsManager.getInaStoragePath(filename);
            if (Files.exists(readPath) && Files.exists(source) && isSameContent(readPath, source)) {
                LOGGER.info("File already exists in read directory with same content: {}", filename);
                markerManager.createMarker(filename, MarkerManager.MarkerType.AVAILABLE_FOR_READ);
                copyToVitamStorage(readPath);
                return;
            }

            markerManager.createMarker(filename, MarkerManager.MarkerType.READ_REQUEST);
            new Thread(() -> prepareDataByINA(filename, readPath), "tape-read-" + filename).start();

            waitForReadCompletion(filename);
            copyToVitamStorage(readPath);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapeCommandException("Read operation failed: " + filename, e);
        }
    }

    /**
     * Asynchronously prepares data from INA tape storage
     */
    private void prepareDataByINA(String filename, Path destination) {
        try {
            LOGGER.info("READ START: {}", filename);

            Path source = fsManager.getInaStoragePath(filename);
            if (Files.notExists(source)) {
                markerManager.createMarker(filename, MarkerManager.MarkerType.NOT_FOUND);
                return;
            }

            Thread.sleep(configuration.getReadLatencyMs());
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            markerManager.createMarker(filename, MarkerManager.MarkerType.AVAILABLE_FOR_READ);
            markerManager.deleteMarker(filename, MarkerManager.MarkerType.READ_REQUEST);

            LOGGER.info("READ END: {} ({}ms)", filename, configuration.getReadLatencyMs());
        } catch (Exception e) {
            LOGGER.error("Read failed: {}", filename, e);
        }
    }

    private void waitForReadCompletion(String filename) throws InterruptedException, TapeCommandException {
        long deadline = System.currentTimeMillis() + configuration.getReadTimeout();

        Path ok = fsManager.getMarkersPath(filename + MarkerManager.MarkerType.AVAILABLE_FOR_READ.getSuffix());
        Path ko = fsManager.getMarkersPath(filename + MarkerManager.MarkerType.NOT_FOUND.getSuffix());

        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(ok)) return;
            if (Files.exists(ko)) throw new TapeCommandException("File not found in tape: " + filename);
            Thread.sleep(300);
        }
        throw new TapeCommandException("Timeout waiting for: " + filename);
    }

    private void copyToVitamStorage(Path readPath) throws IOException {
        Files.copy(readPath, vitamRoot.resolve(readPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isSameContent(Path source, Path destination) throws IOException {
        return com.google.common.io.Files.equal(source.toFile(), destination.toFile());
    }

    /* ===================== SECURITY ===================== */

    protected Path securePath(String path) throws TapeCommandException {
        Path p = Paths.get(path).toAbsolutePath().normalize();
        if (!p.startsWith(vitamRoot)) {
            throw new TapeCommandException("Unauthorized path (outside Vitam): " + p);
        }
        if (Files.notExists(p)) {
            throw new TapeCommandException("File not found: " + p);
        }
        return p;
    }

    private String secureFilename(String filename) throws TapeCommandException {
        try {
            return FileNameCleaner.cleanFileName(filename);
        } catch (IllegalArgumentException e) {
            throw new TapeCommandException("Unsafe filename: " + filename, e);
        }
    }
}
