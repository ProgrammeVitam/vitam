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
package fr.gouv.vitam.storage.cold.server.simulator.service;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyBadRequestException;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyException;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyServerException;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeCatalogModel;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeDriveModel;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeCatalogRepository;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeDriveRepository;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.FileSystemManager;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.MarkerManager;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.MarkerType;
import fr.gouv.vitam.storage.cold.server.simulator.util.InaTapeHelper;
import org.apache.commons.lang3.time.StopWatch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * I/O Service for INA
 * Handles write and read operations with simple file copy
 */
public class InaIoService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaIoService.class);

    private final InaTapeProxyConfiguration configuration;
    private final FileSystemManager fsManager;
    private final MarkerManager markerManager;
    private final TapeCatalogRepository tapeCatalogRepository;
    private final TapeDriveRepository tapeDriveRepository;
    private final InaTapeHelper inaTapeHelper;
    private final Path vitamRoot;

    public InaIoService(
        InaTapeProxyConfiguration configuration,
        FileSystemManager fsManager,
        MarkerManager markerManager,
        TapeCatalogRepository tapeCatalogRepository,
        TapeDriveRepository tapeDriveRepository,
        InaTapeHelper inaTapeHelper
    ) {
        this.configuration = configuration;
        this.fsManager = fsManager;
        this.markerManager = markerManager;
        this.tapeDriveRepository = tapeDriveRepository;
        this.tapeCatalogRepository = tapeCatalogRepository;
        this.inaTapeHelper = inaTapeHelper;
        this.vitamRoot = Paths.get(configuration.getOfferStorageDirectory()).toAbsolutePath().normalize();
    }

    /**
     * Writes a file to tape (exchange directory).
     * If the file already exists in destination, checksums are compared to avoid unnecessary copy.
     * Checks tape capacity before writing and increments size after successful write.
     */
    public void writeToTape(int driveIndex, String inputPath) throws InaTapeProxyException {
        Path source = sanityCheckPath(inputPath);
        String filename = source.getFileName().toString();
        LOGGER.info("WRITE START: {}", filename);

        try {
            long fileSize = Files.size(source);
            LOGGER.debug("File size to write: {} bytes", fileSize);

            Path destination = fsManager.getWritePath(filename);

            // Atomic sequence: capacity check -> copy -> marker -> increment tape size
            executeAtomicWrite(fileSize, driveIndex, () -> {
                if (Files.exists(destination) && isSameContent(source, destination)) {
                    LOGGER.info("Checksums match for {}. Skipping copy.", filename);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    Thread.sleep(configuration.getWriteLatencyMs());
                }
                markerManager.createMarker(filename, MarkerType.WRITE_OK);

                // For simulation purposes only : simulate INA reception tar
                Path inaStorage = Paths.get(configuration.getInaStorageDirectory());
                new Thread(
                    () -> {
                        try {
                            Files.move(source, inaStorage.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            LOGGER.error("Error while copying to INA storage for {}", filename, e);
                        }
                    },
                    "thread-ina-tape-write-copy-" + filename
                ).start();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InaTapeProxyServerException("Write operation interrupted: " + filename, e);
        } catch (IOException | IllegalPathException e) {
            throw new InaTapeProxyServerException("I/O error during write: " + filename, e);
        } catch (InaTapeProxyServerException e) {
            throw e;
        } catch (Exception e) {
            throw new InaTapeProxyServerException("Write operation failed: " + filename, e);
        }
    }

    /**
     * Read from tape: Check if file is already in read directory or prepare it asynchronously
     */
    public void readFromTape(int driveIndex, String outputPath) throws InaTapeProxyException {
        Path destination = sanityCheckPath(outputPath);
        String filename = destination.getFileName().toString();
        TapeDriveModel driveModel = inaTapeHelper.getTapeDriveModel(driveIndex);

        try {
            Path readPath = fsManager.getReadPath(filename);
            Path source = fsManager.getInaStoragePath(filename);
            if (Files.exists(readPath) && Files.exists(source) && isSameContent(readPath, source)) {
                copyToVitamStorage(readPath, destination);
                LOGGER.info("File already exists in read directory with same content: {}", filename);
                markerManager.createMarker(filename, MarkerType.READ_OK);
                return;
            }

            markerManager.createMarker(filename, MarkerType.READ_REQUEST);

            // For simulation purposes only : simulate INA tar preparation
            new Thread(() -> prepareDataByINA(filename, readPath), "thread-ina-tape-read-" + filename).start();

            waitForReadCompletion(filename);
            copyToVitamStorage(readPath, destination);
            // Move drive position forward
            driveModel.setPosition(driveModel.getPosition() + 1);
            tapeDriveRepository.save(driveModel);
        } catch (IOException e) {
            throw new InaTapeProxyServerException("IO error while reading from tape: " + filename, e);
        }
    }

    /**
     * Functional interface for actions that may throw checked exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    // ========== PRIVATE ==========

    /**
     * Asynchronously prepares data from INA tape storage
     */
    private void prepareDataByINA(String filename, Path destination) {
        try {
            LOGGER.info("READ START: {}", filename);

            Path source = fsManager.getInaStoragePath(filename);
            if (Files.notExists(source)) {
                markerManager.createMarker(filename, MarkerType.READ_KO_NOT_FOUND);
                throw new FileNotFoundException("File not found in INA storage: " + filename);
            }

            Thread.sleep(configuration.getReadLatencyMs());

            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            markerManager.createMarker(filename, MarkerType.READ_OK);
            markerManager.deleteMarker(filename, MarkerType.READ_REQUEST);
        } catch (Exception e) {
            LOGGER.error("Read failed: {}", filename, e);
        }
    }

    private void waitForReadCompletion(String filename) throws InaTapeProxyException {
        StopWatch stopWatch = StopWatch.createStarted();

        Path ok = fsManager.getMarkersPath(filename, MarkerType.READ_OK);
        Path ko = fsManager.getMarkersPath(filename, MarkerType.READ_KO_NOT_FOUND);

        do {
            if (Files.exists(ok)) {
                LOGGER.info("Read completed successfully for file '{}'.", filename);
                return;
            }
            if (Files.exists(ko)) throw new InaTapeProxyBadRequestException("File not found in tape: " + filename);

            LOGGER.debug("Waiting for read completion of file '{}'.", filename);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InaTapeProxyServerException("Thread interrupted. Read operation failed: " + filename, e);
            }
        } while (stopWatch.getTime(TimeUnit.MILLISECONDS) < configuration.getReadTimeout());

        throw new InaTapeProxyServerException("Timeout waiting for: " + filename);
    }

    private void copyToVitamStorage(Path readPath, Path destination) throws IOException {
        Files.copy(readPath, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isSameContent(Path source, Path destination) throws IOException {
        return com.google.common.io.Files.equal(source.toFile(), destination.toFile());
    }

    /**
     * Executes a write atomically: validates capacity, runs the provided write action, then increments tape size.
     * The entire sequence is synchronized to prevent race conditions between concurrent writes and load/unload.
     */
    private synchronized void executeAtomicWrite(long fileSize, int driveIndex, ThrowingRunnable writeAction)
        throws Exception {
        TapeCatalogModel currentTape = getCurrentTapeFromDrive(driveIndex);

        if (currentTape.isFull()) {
            // Should never occur
            throw new InaTapeProxyBadRequestException(
                "Illegal state - Write order on an already full Tape:" + currentTape.getId()
            );
        }

        long currentSize = currentTape.getTotalSizeUsed();
        long maxCapacity = configuration.getTapeMaxCapacityMB() * 1_000_000L;
        long availableSpace = maxCapacity - currentSize;

        if (fileSize > availableSpace) {
            // Just mark tape as full. No need to increment its size, position in drive...
            currentTape.setFull(true);
            this.tapeCatalogRepository.save(currentTape);

            // Report IO Error
            throw new InaTapeProxyBadRequestException(
                String.format(
                    "Tape %s is full: %d / %d bytes used. Cannot write %d new bytes.",
                    currentTape.getId(),
                    currentSize,
                    maxCapacity,
                    fileSize
                )
            );
        }

        writeAction.run();
        incrementTapeSize(currentTape, fileSize);
    }

    /**
     * Retrieves the currently loaded tape from the default drive (drive_0).
     * Fetches the full TapeCatalogModel from MongoDB using the volumeTag.
     * @return the loaded tape
     * @throws InaTapeProxyException if no tape is loaded
     */
    private TapeCatalogModel getCurrentTapeFromDrive(int driveIndex) throws InaTapeProxyException {
        TapeDriveModel driveModel = inaTapeHelper.getTapeDriveModel(driveIndex);
        String tapeCode = driveModel.getTapeCode();
        if (tapeCode == null) {
            throw new InaTapeProxyBadRequestException("No tape is currently loaded in the drive");
        }
        return inaTapeHelper.getTapeCatalogModel(tapeCode);
    }

    /**
     * Increment the tape's size and update the drive position.
     * Thread-safety is ensured by:
     * - Called within synchronized methods (executeAtomicWrite)
     * - Optimistic locking in repositories (version field)
     */
    private void incrementTapeSize(TapeCatalogModel tape, long sizeBytes) throws InaTapeProxyServerException {
        tape.setTotalSizeUsed(tape.getTotalSizeUsed() + sizeBytes);
        tape.setTotalStoredFiles(tape.getTotalStoredFiles() + 1);

        TapeDriveModel drive = inaTapeHelper.getTapeDriveModel(tape.getDriveIndex());
        drive.setPosition(tape.getTotalStoredFiles());

        tapeCatalogRepository.save(tape);
        tapeDriveRepository.save(drive);
    }

    /* ===================== SECURITY ===================== */

    private Path sanityCheckPath(String inputPath) throws InaTapeProxyBadRequestException {
        try {
            // Normalize inputPath to match vitamRoot normalization
            Path normalizedInputPath = Paths.get(inputPath).toAbsolutePath().normalize();

            Path securePath = SafeFileChecker.checkSafeFilePath(
                vitamRoot.toAbsolutePath().toString(),
                normalizedInputPath.toString()
            ).toPath();
            if (Files.exists(securePath) && !Files.isRegularFile(securePath)) {
                throw new InaTapeProxyBadRequestException("Expected regular file: " + securePath);
            }
            return securePath;
        } catch (IllegalPathException e) {
            throw new InaTapeProxyBadRequestException("Illegal file path '" + inputPath + "'", e);
        }
    }
}
