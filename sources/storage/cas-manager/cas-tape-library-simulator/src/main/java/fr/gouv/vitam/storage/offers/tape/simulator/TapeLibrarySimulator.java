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
package fr.gouv.vitam.storage.offers.tape.simulator;

import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.offers.tape.dto.TapeCartridge;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlotType;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helper class for emulating a Tape Library behaviour for integration testing purposes.
 *
 * Handles an in-memory tape library (1 changer, x drives, y slots and z tapes), and provides {@link TapeLoadUnloadService},
 * {@link TapeReadWriteService} and {@link TapeDriveCommandService} instances that simulates operations on tape library.
 * Concurrent operations on the same changer, drive, slot or tape is prohibited (e.g. trying to load a tape A into a slot that is currently being unloaded...)
 * Any unexpected error (i.e. reading past last file of a tape, loading from an empty slot...) is reported through {@code getFailures()} for post test checks
 */
public class TapeLibrarySimulator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibrarySimulator.class);

    private final Object syncRoot = new Object();

    private final VirtualChanger changer;
    private final List<VirtualDrive> drives;
    private final List<VirtualSlot> slots;
    private final TestTapeLoadUnloadService tapeLoadUnloadService;
    private final List<TapeReadWriteService> tapeReadWriteServices;
    private final List<TapeDriveCommandService> tapeDriveCommandServices;
    private final List<Exception> failures;
    private final int maxTapeCapacityInBytes;
    private final String cartridgeType;

    private volatile int sleepDelayMillis;

    public TapeLibrarySimulator(
        Path inputDirectory,
        Path tempOutputStorageDirectory,
        int nbDrives,
        int nbSlots,
        int nbTapes,
        int maxTapeCapacityInBytes,
        String cartridgeType,
        int sleepDelayMillis
    ) {
        ParametersChecker.checkParameter("Missing inputDirectory", inputDirectory);
        ParametersChecker.checkParameter("Missing tempOutputStorageDirectory", tempOutputStorageDirectory);
        ParametersChecker.checkValue("Invalid nbDrives", nbDrives, 1);
        ParametersChecker.checkValue("Invalid nbSlots", nbSlots, 1);
        ParametersChecker.checkValue("Invalid nbTapes", nbTapes, 1);
        ParametersChecker.checkValue("Invalid maxTapeCapacityInBytes", maxTapeCapacityInBytes, 1);
        ParametersChecker.checkValue("nbTapes must be <= nbSlots", nbSlots, nbTapes);
        ParametersChecker.checkParameter("Missing cartridgeType", cartridgeType);

        failures = Collections.synchronizedList(new ArrayList<>());

        this.changer = new VirtualChanger().setChangerStatus(VirtualChangerState.READY);

        this.drives = IntStream.range(0, nbDrives)
            .mapToObj(driveIndex -> new VirtualDrive(driveIndex).setCurrentTape(null).setState(VirtualDriveState.EMPTY))
            .collect(Collectors.toList());

        this.slots = IntStream.rangeClosed(1, nbSlots)
            .mapToObj(slotNumber -> new VirtualSlot(slotNumber).setCurrentTape(null).setState(VirtualSlotState.EMPTY))
            .collect(Collectors.toList());

        // Pre-fill tapes to available slots
        for (int i = 0; i < nbTapes; i++) {
            VirtualSlot virtualSlot = this.slots.get(i);
            virtualSlot.setCurrentTape(new VirtualTape("TAPE-" + i, "ALT-TAPE-TAG-" + i, maxTapeCapacityInBytes));
            virtualSlot.setState(VirtualSlotState.LOADED);
        }

        this.tapeLoadUnloadService = new TestTapeLoadUnloadService();
        this.tapeReadWriteServices = new ArrayList<>();
        this.tapeDriveCommandServices = new ArrayList<>();
        for (int driveIndex = 0; driveIndex < nbDrives; driveIndex++) {
            tapeReadWriteServices.add(
                new TestTapeReadWriteService(driveIndex, inputDirectory, tempOutputStorageDirectory)
            );
            tapeDriveCommandServices.add(new TestTapeDriveCommandService(driveIndex));
        }
        this.maxTapeCapacityInBytes = maxTapeCapacityInBytes;
        this.cartridgeType = cartridgeType;
        this.sleepDelayMillis = sleepDelayMillis;
    }

    public TapeLoadUnloadService getTapeLoadUnloadService() {
        return tapeLoadUnloadService;
    }

    public List<TapeReadWriteService> getTapeReadWriteServices() {
        return Collections.unmodifiableList(tapeReadWriteServices);
    }

    public List<TapeDriveCommandService> getTapeDriveCommandServices() {
        return Collections.unmodifiableList(tapeDriveCommandServices);
    }

    public List<Exception> getFailures() {
        return Collections.unmodifiableList(this.failures);
    }

    private void ensureTapeLoaded(VirtualDrive drive) throws TapeCommandException {
        switch (drive.getState()) {
            case EMPTY:
                throw createAndReportSevereTapeCommandException("No loaded tape in drive " + drive.getDriveIndex());
            case LOADED:
                LOGGER.info(
                    "OK, tape " +
                    drive.getCurrentTape().getVolumeTag() +
                    " is loaded into drive " +
                    drive.getDriveIndex()
                );
                break;
            case EJECTED:
                throw createAndReportSevereTapeCommandException(
                    "Tape " +
                    drive.getCurrentTape().getVolumeTag() +
                    " has already been ejected from drive " +
                    drive.getDriveIndex()
                );
            case BUSY:
                throw createAndReportSevereTapeCommandException("Drive " + drive.getDriveIndex() + " is busy !");
            default:
                throw createAndReportIllegalStateException("Unexpected value: " + drive.getState());
        }
    }

    private void ensureChargerIsReady(VirtualChanger changer) throws TapeCommandException {
        switch (changer.getChangerStatus()) {
            case READY:
                LOGGER.info("OK, charger is ready");
                break;
            case BUSY:
                throw createAndReportSevereTapeCommandException("Changer is busy !");
            default:
                throw createAndReportIllegalStateException("Unexpected value: " + changer.getChangerStatus());
        }
    }

    private void checkSlotNumber(int slotNumber) {
        // Warning : slotNumber is base-1, while driveIndex is base-0
        if (slotNumber < 1 || slotNumber > slots.size()) {
            throw createAndReportIllegalStateException("Invalid slotNumber: " + slotNumber);
        }
    }

    private void checkDriveIndex(int driveIndex) {
        // Warning : slotNumber is base-1, while driveIndex is base-0
        if (driveIndex < 0 || driveIndex >= drives.size()) {
            throw createAndReportIllegalStateException("Invalid driveIndex: " + driveIndex);
        }
    }

    private TapeCommandException createAndReportSevereTapeCommandException(String msg) {
        TapeCommandException tapeCommandException = new TapeCommandException(msg);
        this.failures.add(tapeCommandException);
        return tapeCommandException;
    }

    private TapeCommandException createAndReportSevereTapeCommandException(String msg, Throwable cause) {
        TapeCommandException tapeCommandException = new TapeCommandException(msg, cause);
        this.failures.add(tapeCommandException);
        return tapeCommandException;
    }

    private IllegalStateException createAndReportIllegalStateException(String msg) {
        IllegalStateException illegalStateException = new IllegalStateException(msg);
        this.failures.add(illegalStateException);
        return illegalStateException;
    }

    private class TestTapeReadWriteService implements TapeReadWriteService {

        private final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TestTapeReadWriteService.class);

        private final int driveIndex;
        private final VirtualDrive drive;
        private final Path inputDirectory;
        private final Path tmpOutputStorageFolder;

        private TestTapeReadWriteService(int driveIndex, Path inputDirectory, Path tmpOutputStorageFolder) {
            this.driveIndex = driveIndex;
            this.drive = drives.get(driveIndex);
            this.inputDirectory = inputDirectory;
            this.tmpOutputStorageFolder = tmpOutputStorageFolder;
        }

        @Override
        public void writeToTape(String inputPath) throws TapeCommandException {
            if (inputPath == null) {
                throw createAndReportSevereTapeCommandException("Missing inputPath");
            }

            VirtualTape currentTape;
            Integer filePosition;
            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                currentTape = drive.getCurrentTape();
                filePosition = drive.getFilePosition();

                // Override existing files
                if (filePosition < currentTape.getPersistedFiles().size()) {
                    LOGGER.warn("WARNING : TAPE " + currentTape.getVolumeTag() + " is being overridden");
                    ListIterator<Path> filesToOverrideIterator = currentTape
                        .getPersistedFiles()
                        .listIterator(filePosition);
                    while (filesToOverrideIterator.hasNext()) {
                        Path fileToOverride = filesToOverrideIterator.next();
                        try {
                            Files.delete(fileToOverride);
                        } catch (IOException e) {
                            throw createAndReportSevereTapeCommandException(
                                "IOError while overriding file " +
                                fileToOverride +
                                " of tape " +
                                currentTape.getVolumeTag(),
                                e
                            );
                        }
                        filesToOverrideIterator.remove();
                    }
                }
                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            Path destinationPath = null;
            int sizeToWrite = 0;
            try {
                destinationPath = tmpOutputStorageFolder
                    .resolve(currentTape.getVolumeTag())
                    .resolve("file" + currentTape.getPersistedFiles().size())
                    .toAbsolutePath();

                Files.createDirectories(destinationPath.getParent());

                Path sourceFile = inputDirectory.resolve(inputPath);

                int fileSize = (int) Files.size(sourceFile);
                if (fileSize == 0) {
                    throw createAndReportIllegalStateException("Empty source file " + sourceFile);
                }

                sizeToWrite = Math.min(maxTapeCapacityInBytes - currentTape.getUsedCapacity(), fileSize);

                LOGGER.info(
                    "Writing file " +
                    sourceFile +
                    " (" +
                    FileUtils.byteCountToDisplaySize(fileSize) +
                    ") to tape " +
                    currentTape.getVolumeTag() +
                    " at position " +
                    currentTape.getPersistedFiles().size()
                );

                try (InputStream sourceFileInputStream = Files.newInputStream(sourceFile)) {
                    Files.copy(new BoundedInputStream(sourceFileInputStream, sizeToWrite), destinationPath);
                }

                if (sizeToWrite < fileSize) {
                    // Ordinal exception. Do not report it as a failure
                    throw new TapeCommandException(
                        "IOError : No space left on device while writing " +
                        inputPath +
                        " to tape " +
                        currentTape.getVolumeTag()
                    );
                }
            } catch (IOException e) {
                throw createAndReportSevereTapeCommandException(
                    "IOError while writing " + inputPath + " to tape " + currentTape.getVolumeTag(),
                    e
                );
            } finally {
                synchronized (syncRoot) {
                    drive.setState(VirtualDriveState.LOADED);

                    drive.setFilePosition(filePosition + 1);

                    drive.setBeginningOfTape(false);
                    drive.setEndOfFile(true);
                    drive.setEndOfData(false);

                    if (destinationPath != null) {
                        currentTape.getPersistedFiles().add(destinationPath);
                    }
                    currentTape.setUsedCapacity(currentTape.getUsedCapacity() + sizeToWrite);
                }
            }
        }

        @Override
        public void readFromTape(String outputPath) throws TapeCommandException {
            if (outputPath == null) {
                throw createAndReportSevereTapeCommandException("Missing outputPath");
            }

            VirtualTape currentTape;
            Integer filePosition;
            Path srcFilePath;
            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                currentTape = drive.getCurrentTape();
                filePosition = drive.getFilePosition();

                // Ensure there is a file to read
                if (filePosition >= currentTape.getPersistedFiles().size()) {
                    drive.setBeginningOfTape(false);
                    drive.setEndOfFile(false);
                    drive.setEndOfData(true);
                    throw createAndReportSevereTapeCommandException(
                        "IOError. No more file to read from tape " +
                        currentTape.getVolumeTag() +
                        " in drive " +
                        driveIndex
                    );
                }

                srcFilePath = currentTape.getPersistedFiles().get(filePosition);
                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            try {
                Path destinationPath = this.tmpOutputStorageFolder.resolve(outputPath);
                if (Files.exists(destinationPath)) {
                    throw createAndReportSevereTapeCommandException(
                        "IOError while reading file from tape " +
                        currentTape.getVolumeTag() +
                        ". OutputPath '" +
                        outputPath +
                        "'already exists"
                    );
                }

                LOGGER.info(
                    "Reading file at position " +
                    filePosition +
                    " of tape " +
                    currentTape.getVolumeTag() +
                    " into file " +
                    destinationPath +
                    " (" +
                    FileUtils.byteCountToDisplaySize(Files.size(srcFilePath)) +
                    ")"
                );

                Files.copy(srcFilePath, destinationPath);
            } catch (IOException e) {
                throw createAndReportSevereTapeCommandException(
                    "IOError while copying tape " + currentTape.getVolumeTag() + " content into " + outputPath + "file",
                    e
                );
            } finally {
                synchronized (syncRoot) {
                    drive.setState(VirtualDriveState.LOADED);

                    drive.setFilePosition(filePosition + 1);

                    drive.setBeginningOfTape(false);
                    drive.setEndOfFile(true);
                    drive.setEndOfData(false);
                }
            }
        }

        @Override
        public String getTmpOutputStorageFolder() {
            return tmpOutputStorageFolder.toString();
        }
    }

    private class TestTapeDriveCommandService implements TapeDriveCommandService {

        private final int driveIndex;
        private final VirtualDrive drive;

        private TestTapeDriveCommandService(int driveIndex) {
            this.driveIndex = driveIndex;
            this.drive = drives.get(driveIndex);
        }

        @Override
        public TapeDriveSpec status() throws TapeCommandException {
            VirtualDriveState driveState;
            synchronized (syncRoot) {
                driveState = drive.getState();
                switch (driveState) {
                    case EMPTY:
                    case LOADED:
                    case EJECTED:
                        LOGGER.info("OK. Drive state is " + driveState + ".");
                        break;
                    case BUSY:
                        throw createAndReportSevereTapeCommandException("Drive " + driveIndex + " is busy !");
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + driveState);
                }

                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                drive.setState(driveState);
                TapeDriveState result = new TapeDriveState();

                switch (driveState) {
                    case EMPTY:
                    case EJECTED:
                        result.setCartridge(null);
                        result.setDriveStatuses(List.of(TapeDriveStatus.DR_OPEN, TapeDriveStatus.IM_REP_EN));
                        result.setTapeBlockSize(null);
                        result.setFileNumber(null);

                        break;
                    case LOADED:
                        result.setCartridge(cartridgeType);
                        List<TapeDriveStatus> driveStatuses = new ArrayList<>();
                        driveStatuses.add(TapeDriveStatus.ONLINE);
                        driveStatuses.add(TapeDriveStatus.IM_REP_EN);

                        if (drive.getEndOfData()) {
                            driveStatuses.add(TapeDriveStatus.EOD);
                        }
                        if (drive.getEndOfFile()) {
                            driveStatuses.add(TapeDriveStatus.EOF);
                        }
                        if (drive.getBeginningOfTape()) {
                            driveStatuses.add(TapeDriveStatus.BOT);
                        }

                        result.setDriveStatuses(driveStatuses);

                        result.setBlockNumber(0);
                        result.setTapeBlockSize(0L);
                        result.setFileNumber(drive.getFilePosition());
                        break;
                    case BUSY:
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + driveState);
                }

                result.setDescription("DRIVE-" + drive.getDriveIndex());
                result.setErrorCountSinceLastStatus(0);
                return result;
            }
        }

        @Override
        public void move(int position, boolean isBackward) throws TapeCommandException {
            if (position < 1) {
                throw createAndReportSevereTapeCommandException("Position " + position + " cannot be negative or zero");
            }

            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                drive.setState(VirtualDriveState.LOADED);

                int currentPosition = drive.getFilePosition();

                if (isBackward) {
                    drive.setEndOfFile(false);
                    drive.setEndOfData(false);

                    if (currentPosition == position) {
                        drive.setFilePosition(0);
                        drive.setBeginningOfTape(true);
                    } else if (currentPosition > position) {
                        drive.setFilePosition(currentPosition - position);
                        drive.setBeginningOfTape(false);
                    } else {
                        drive.setFilePosition(0);
                        drive.setBeginningOfTape(true);
                        throw createAndReportSevereTapeCommandException(
                            "Cannot move drive " +
                            position +
                            " files backward. Previous tape " +
                            drive.getCurrentTape().getVolumeTag() +
                            " position: " +
                            currentPosition +
                            ". Beginning Of Tape reached."
                        );
                    }
                } else {
                    int fileCount = drive.getCurrentTape().getPersistedFiles().size();
                    drive.setEndOfFile(false);
                    drive.setBeginningOfTape(false);

                    if (currentPosition + position > fileCount) {
                        drive.setFilePosition(fileCount);
                        drive.setEndOfData(true);

                        if (
                            currentPosition == 0 &&
                            drive.getCurrentTape().getPersistedFiles().isEmpty() &&
                            position == 1
                        ) {
                            // Ordinal exception that occurs when checking drive emptiness.
                            // Do not report it as a failure
                            throw new TapeCommandException(
                                "Cannot move drive " +
                                position +
                                " files backward. Tape " +
                                drive.getCurrentTape().getVolumeTag() +
                                " is empty"
                            );
                        }

                        throw createAndReportSevereTapeCommandException(
                            "Cannot move drive " +
                            position +
                            " files forward. Current tape " +
                            drive.getCurrentTape().getVolumeTag() +
                            " position: " +
                            currentPosition +
                            ". End Of Data reached."
                        );
                    } else {
                        drive.setFilePosition(currentPosition + position);
                        drive.setEndOfData(false);
                    }
                }
            }
        }

        @Override
        public void rewind() throws TapeCommandException {
            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                drive.setState(VirtualDriveState.LOADED);
                drive.setFilePosition(0);
                drive.setBeginningOfTape(true);
                drive.setEndOfFile(false);
                drive.setEndOfData(false);
            }
        }

        @Override
        public void goToEnd() throws TapeCommandException {
            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                drive.setState(VirtualDriveState.LOADED);
                drive.setFilePosition(drive.getCurrentTape().getPersistedFiles().size());
                drive.setBeginningOfTape(false);
                drive.setEndOfFile(false);
                drive.setEndOfData(true);
            }
        }

        @Override
        public void eject() throws TapeCommandException {
            synchronized (syncRoot) {
                ensureTapeLoaded(drive);

                drive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                drive.setState(VirtualDriveState.EJECTED);
                drive.setFilePosition(null);
                drive.setBeginningOfTape(null);
                drive.setEndOfFile(null);
                drive.setEndOfData(null);
            }
        }
    }

    private class TestTapeLoadUnloadService implements TapeLoadUnloadService {

        private TestTapeLoadUnloadService() {}

        @Override
        public TapeLibrarySpec status() throws TapeCommandException {
            synchronized (syncRoot) {
                ensureChargerIsReady(changer);
                changer.setChangerStatus(VirtualChangerState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                TapeLibraryState result = new TapeLibraryState();
                result.setDevice("Changer");

                result.setDriveCount(drives.size());
                result.setSlotsCount(slots.size());
                result.setMailBoxCount(0);

                List<TapeDrive> tapeDrives = new ArrayList<>();
                for (int driveIndex = 0; driveIndex < drives.size(); driveIndex++) {
                    VirtualDrive virtualDrive = drives.get(driveIndex);
                    TapeDrive tapeDrive = new TapeDrive();
                    tapeDrive.setIndex(driveIndex);
                    tapeDrive.setTape(toTapeCartridge(virtualDrive.getCurrentTape(), null));
                    tapeDrives.add(tapeDrive);
                }
                result.setDrives(tapeDrives);

                List<TapeSlot> tapeSlots = new ArrayList<>();
                for (int slotNumber = 1; slotNumber <= slots.size(); slotNumber++) {
                    VirtualSlot virtualSlot = slots.get(slotNumber - 1);
                    TapeSlot tapeSlot = new TapeSlot();
                    tapeSlot.setIndex(slotNumber);
                    tapeSlot.setStorageElementType(TapeSlotType.SLOT);
                    tapeSlot.setTape(toTapeCartridge(virtualSlot.getCurrentTape(), slotNumber));
                    tapeSlots.add(tapeSlot);
                }
                result.setSlots(tapeSlots);

                changer.setChangerStatus(VirtualChangerState.READY);

                return result;
            }
        }

        private TapeCartridge toTapeCartridge(VirtualTape virtualTape, Integer slotNumber) {
            if (virtualTape == null) {
                return null;
            }
            TapeCartridge tapeCartridge = new TapeCartridge();
            tapeCartridge.setVolumeTag(virtualTape.getVolumeTag());
            tapeCartridge.setAlternateVolumeTag(virtualTape.getAlternateVolumeTag());
            tapeCartridge.setSlotIndex(slotNumber);
            return tapeCartridge;
        }

        @Override
        public void loadTape(int slotNumber, int driveIndex) throws TapeCommandException {
            synchronized (syncRoot) {
                checkSlotNumber(slotNumber);
                checkDriveIndex(driveIndex);
                ensureChargerIsReady(changer);

                VirtualSlot virtualSlot = slots.get(slotNumber - 1);
                switch (virtualSlot.getState()) {
                    case EMPTY:
                        throw createAndReportSevereTapeCommandException(
                            "Cannot load tape from slot " + slotNumber + " into drive " + driveIndex + ". Slot is empty"
                        );
                    case LOADED:
                        LOGGER.info(
                            "OK. Slot " + slotNumber + " contains tape " + virtualSlot.getCurrentTape().getVolumeTag()
                        );
                        break;
                    case BUSY:
                        throw createAndReportSevereTapeCommandException("Slot " + slotNumber + " is busy !");
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + virtualSlot.getState());
                }

                VirtualDrive virtualDrive = drives.get(driveIndex);
                switch (virtualDrive.getState()) {
                    case EMPTY:
                        LOGGER.info("OK. Drive " + driveIndex + " is empty");
                        break;
                    case LOADED:
                        throw createAndReportSevereTapeCommandException(
                            "Cannot load tape from slot " + slotNumber + " into drive " + driveIndex + ". Drive is full"
                        );
                    case EJECTED:
                        throw createAndReportSevereTapeCommandException(
                            "Drive " +
                            driveIndex +
                            " has ejected the tape " +
                            virtualDrive.getCurrentTape().getVolumeTag() +
                            " but has not yet unloaded it into a slot!"
                        );
                    case BUSY:
                        throw createAndReportSevereTapeCommandException("Drive " + driveIndex + " is busy !");
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + virtualDrive.getState());
                }

                changer.setChangerStatus(VirtualChangerState.BUSY);
                virtualSlot.setState(VirtualSlotState.BUSY);
                virtualDrive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                VirtualDrive virtualDrive = drives.get(driveIndex);
                VirtualSlot virtualSlot = slots.get(slotNumber - 1);
                VirtualTape virtualTape = virtualSlot.getCurrentTape();

                virtualSlot.setState(VirtualSlotState.EMPTY);
                virtualSlot.setCurrentTape(null);

                virtualDrive.setState(VirtualDriveState.LOADED);
                virtualDrive.setCurrentTape(virtualTape);
                virtualDrive.setFilePosition(0);
                virtualDrive.setBeginningOfTape(true);
                virtualDrive.setEndOfFile(false);
                virtualDrive.setEndOfData(false);
                virtualDrive.setPreviousTapeSlot(slotNumber);

                changer.setChangerStatus(VirtualChangerState.READY);
            }
        }

        @Override
        public void unloadTape(int slotNumber, int driveIndex) throws TapeCommandException {
            synchronized (syncRoot) {
                // Warning : slotNumber is base-1, while driveIndex is base-0
                checkSlotNumber(slotNumber);
                checkDriveIndex(driveIndex);

                ensureChargerIsReady(changer);

                VirtualSlot virtualSlot = slots.get(slotNumber - 1);
                switch (virtualSlot.getState()) {
                    case EMPTY:
                        LOGGER.info("OK. Slot " + slotNumber + " is empty");
                        break;
                    case LOADED:
                        throw createAndReportSevereTapeCommandException(
                            "Cannot load tape from drive " + driveIndex + " into slot " + slotNumber + ". Slot is full"
                        );
                    case BUSY:
                        throw createAndReportSevereTapeCommandException("Slot " + slotNumber + " is busy !");
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + virtualSlot.getState());
                }

                VirtualDrive virtualDrive = drives.get(driveIndex);
                switch (virtualDrive.getState()) {
                    case EMPTY:
                        throw createAndReportSevereTapeCommandException(
                            "Cannot unload tape from drive " +
                            driveIndex +
                            " into slot " +
                            slotNumber +
                            ". Drive is empty"
                        );
                    case LOADED:
                        throw createAndReportSevereTapeCommandException(
                            "Cannot unload tape from drive " +
                            driveIndex +
                            " into slot " +
                            slotNumber +
                            ". Tape " +
                            virtualDrive.getCurrentTape().getVolumeTag() +
                            " has not yet been ejected"
                        );
                    case EJECTED:
                        LOGGER.info(
                            "OK. Drive " +
                            driveIndex +
                            " has an ejected tape " +
                            virtualDrive.getCurrentTape().getVolumeTag()
                        );
                        break;
                    case BUSY:
                        throw createAndReportSevereTapeCommandException("Drive " + driveIndex + " is busy !");
                    default:
                        throw createAndReportIllegalStateException("Unexpected value: " + virtualDrive.getState());
                }

                changer.setChangerStatus(VirtualChangerState.BUSY);
                virtualSlot.setState(VirtualSlotState.BUSY);
                virtualDrive.setState(VirtualDriveState.BUSY);
            }

            Uninterruptibles.sleepUninterruptibly(sleepDelayMillis, TimeUnit.MILLISECONDS);

            synchronized (syncRoot) {
                VirtualDrive virtualDrive = drives.get(driveIndex);
                VirtualSlot virtualSlot = slots.get(slotNumber - 1);
                VirtualTape virtualTape = virtualDrive.getCurrentTape();

                virtualSlot.setState(VirtualSlotState.LOADED);
                virtualSlot.setCurrentTape(virtualTape);

                virtualDrive.setState(VirtualDriveState.EMPTY);
                virtualDrive.setCurrentTape(null);
                virtualDrive.setFilePosition(null);
                virtualDrive.setBeginningOfTape(null);
                virtualDrive.setEndOfFile(null);
                virtualDrive.setEndOfData(null);
                virtualDrive.setPreviousTapeSlot(null);

                changer.setChangerStatus(VirtualChangerState.READY);
            }
        }
    }

    public TapeLibrarySimulator setSleepDelayMillis(int sleepDelayMillis) {
        this.sleepDelayMillis = sleepDelayMillis;
        return this;
    }
}
