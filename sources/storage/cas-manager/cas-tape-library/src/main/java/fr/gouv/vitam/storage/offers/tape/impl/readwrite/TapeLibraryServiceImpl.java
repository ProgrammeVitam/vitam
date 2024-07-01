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
package fr.gouv.vitam.storage.offers.tape.impl.readwrite;

import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TapeLibraryServiceImpl implements TapeLibraryService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibraryServiceImpl.class);
    private static final long SLEEP_TIME = 20L;
    private static final long MB_TO_BYTES = 1_000_000;

    private final TapeDriveService tapeDriveService;
    private final TapeRobotPool tapeRobotPool;
    private final int fullCartridgeDetectionThresholdInMB;

    public final String MSG_PREFIX;

    public TapeLibraryServiceImpl(
        TapeDriveService tapeDriveService,
        TapeRobotPool tapeRobotPool,
        int fullCartridgeDetectionThresholdInMB
    ) {
        this.tapeDriveService = tapeDriveService;
        this.tapeRobotPool = tapeRobotPool;
        this.fullCartridgeDetectionThresholdInMB = fullCartridgeDetectionThresholdInMB;
        this.MSG_PREFIX = String.format(
            "[Library] : %s, [Drive] : %s, ",
            tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex()
        );
    }

    @Override
    public void goToPosition(TapeCatalog tape, Integer position, ReadWriteErrorCode readWriteErrorCode)
        throws ReadWriteException {
        if (position == 0) {
            rewindTape(tape);
            return;
        }

        int offset = position - tape.getCurrentPosition();
        if (offset == 0) {
            LOGGER.debug(
                "No need to move (current position=" + tape.getCurrentPosition() + ", target position=" + position + ")"
            );
            return;
        }

        try {
            tapeDriveService.getDriveCommandService().move(Math.abs(offset), offset < 0);
        } catch (TapeCommandException e) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                tape.getCode() +
                " Action : FSF goto position Error , Entity: " +
                JsonHandler.unprettyPrint(e.getDetails()),
                readWriteErrorCode,
                e
            );
        }

        // Update current position only if fsf/bsf command success
        tape.setCurrentPosition(position);
    }

    @Override
    public void rewindTape(TapeCatalog tape) throws ReadWriteException {
        try {
            tapeDriveService.getDriveCommandService().rewind();
        } catch (TapeCommandException e) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                tape.getCode() +
                " Action : Rewind Tape, Entity: " +
                JsonHandler.unprettyPrint(e.getDetails()),
                ReadWriteErrorCode.KO_ON_REWIND_TAPE,
                e
            );
        }

        tape.setCurrentPosition(0);
    }

    @Override
    public void write(String filePath, long writtenBytes, TapeCatalog tape) throws ReadWriteException {
        if (tape == null) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: can't write, current tape is null.",
                ReadWriteErrorCode.NULL_CURRENT_TAPE
            );
        }

        if (tape.getFileCount() < tape.getCurrentPosition()) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: current position must be <= to fileCount.",
                ReadWriteErrorCode.KO_TAPE_CURRENT_POSITION_GREATER_THAN_FILE_COUNT
            );
        }

        try {
            goToPosition(tape, tape.getFileCount(), ReadWriteErrorCode.KO_ON_GOTO_FILE_COUNT);

            try {
                tapeDriveService.getReadWriteService().writeToTape(filePath);
            } catch (TapeCommandException e) {
                LOGGER.error(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    " Action : Write, Entity: " +
                    JsonHandler.unprettyPrint(e.getDetails()),
                    e
                );

                // Check tape capacity :
                // EndOfTape status is not properly handled by MTX utility.
                // According to the man page of mtx utility : "In addition,  MTX  does  not handle the end of tape properly."
                // We use the following heuristic for EndOfTape detection :
                //  - If written data > a cartridge type dependent threshold (by default, 90% of tape capacity),
                //      ==> We assume the tape to be FULL
                //  - Otherwise ==> tape is considered as corrupted

                // Exec status to ensure drive is up, and has no errors reported
                TapeDriveSpec status = getDriveStatus(ReadWriteErrorCode.KO_DRIVE_STATUS_KO_AFTER_WRITE_ERROR);

                String cartridgeType = status.getCartridge();
                long tapeOccupation = tape.getWrittenBytes() + writtenBytes;

                if (tapeOccupation >= fullCartridgeDetectionThresholdInMB * MB_TO_BYTES) {
                    throw new ReadWriteException(
                        MSG_PREFIX +
                        TAPE_MSG +
                        tape.getCode() +
                        " Action : Write, Drive Status: " +
                        JsonHandler.unprettyPrint(status) +
                        ", Error: End Of Tape, Tape Occupation: " +
                        tapeOccupation +
                        " (bytes)" +
                        ", Cartridge type: " +
                        cartridgeType +
                        "]",
                        ReadWriteErrorCode.KO_ON_END_OF_TAPE,
                        e
                    );
                }

                tape.setTapeState(TapeState.CONFLICT);

                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    " Action : Write, Drive Status: " +
                    JsonHandler.unprettyPrint(status) +
                    ", Error: Tape is CORRUPTED" +
                    ", Cartridge type: '" +
                    cartridgeType +
                    "'" +
                    ", Tape space usage: " +
                    tapeOccupation +
                    " (bytes)" +
                    ", Full tape threshold: " +
                    (fullCartridgeDetectionThresholdInMB * MB_TO_BYTES) +
                    " (bytes)",
                    ReadWriteErrorCode.KO_ON_WRITE_TO_TAPE,
                    e
                );
            }

            tape.setFileCount(tape.getFileCount() + 1);
            tape.setCurrentPosition(tape.getFileCount());
            tape.setWrittenBytes(tape.getWrittenBytes() + writtenBytes);
            tape.setTapeState(TapeState.OPEN);
        } catch (ReadWriteException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
        }
    }

    @Override
    public void read(TapeCatalog tape, Integer position, String outputPath) throws ReadWriteException {
        // Seek position
        goToPosition(tape, position, ReadWriteErrorCode.KO_ON_GO_TO_POSITION);

        // read file from tape
        try {
            tapeDriveService.getReadWriteService().readFromTape(outputPath);

            // Advance tape position
            tape.setCurrentPosition(tape.getCurrentPosition() + 1);
        } catch (TapeCommandException e) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                tape.getCode() +
                " Action : Write, Entity: " +
                JsonHandler.unprettyPrint(e.getDetails()),
                ReadWriteErrorCode.KO_ON_READ_FROM_TAPE,
                e
            );
        }
    }

    @Override
    public void loadTape(TapeCatalog tape) throws ReadWriteException {
        ParametersChecker.checkParameter(
            MSG_PREFIX + ", Error: tape to load is null. please get markReady tape from catalog",
            tape
        );

        if (null == tape.getCurrentLocation()) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                tape.getCode() +
                ", Error: tape current location is null. please update catalog",
                ReadWriteErrorCode.TAPE_LOCATION_CONFLICT_ON_LOAD
            );
        }

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex = tape.getCurrentLocation().getIndex();

        loadTapeFromSlotIntoDrive(tape, driveIndex, slotIndex);

        // Rewind the tape
        rewindTape(tape);
    }

    private void loadTapeFromSlotIntoDrive(TapeCatalog tape, Integer driveIndex, Integer slotIndex)
        throws ReadWriteException {
        try {
            TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                loadUnloadService.loadTape(slotIndex, driveIndex);

                // Update tape location
                tape.setPreviousLocation(tape.getCurrentLocation());
                tape.setCurrentLocation(new TapeLocation(getDriveIndex(), TapeLocationType.DRIVE));
            } catch (TapeCommandException e) {
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    ", Action : load, Entity: " +
                    JsonHandler.unprettyPrint(e.getDetails()),
                    ReadWriteErrorCode.KO_ON_LOAD_TAPE,
                    e
                );
            } finally {
                tapeRobotPool.pushRobotService(tapeRobotService);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReadWriteException(MSG_PREFIX + ", Error: ", e);
        }
    }

    @Override
    public void unloadTape(TapeCatalog tape) throws ReadWriteException {
        ParametersChecker.checkParameter(MSG_PREFIX + ", Error: tape to unload is null.", tape);

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex = null;

        if (null != tape.getPreviousLocation()) {
            switch (tape.getPreviousLocation().getLocationType()) {
                case SLOT:
                case IMPORTEXPORT:
                    slotIndex = tape.getPreviousLocation().getIndex();
                    break;
                case DRIVE:
                case OUTSIDE:
                    throw new ReadWriteException(
                        MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Error: previous location should no be in drive",
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT_ON_UNLOAD
                    );
                default:
                    throw new IllegalArgumentException(
                        MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Error: location type not implemented"
                    );
            }
        }

        if (null == slotIndex) {
            // FIXME : slotIndex = findEmptySlot() ?
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Error : no empty slot found => cannot unload tape",
                ReadWriteErrorCode.NO_EMPTY_SLOT_FOUND
            );
        }

        // Eject tape
        ejectTapeFromDrive(tape);

        unloadTapeFromDriveToSlot(tape, driveIndex, slotIndex);
    }

    private void ejectTapeFromDrive(TapeCatalog tape) throws ReadWriteException {
        try {
            tapeDriveService.getDriveCommandService().eject();
        } catch (TapeCommandException e) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                tape.getCode() +
                " Action : Eject tape with forced rewind, Error: Could not rewind or unload tape, Entity: " +
                JsonHandler.unprettyPrint(e.getDetails()),
                ReadWriteErrorCode.KO_REWIND_BEFORE_UNLOAD_TAPE,
                e
            );
        }

        tape.setCurrentPosition(0);
    }

    private void unloadTapeFromDriveToSlot(TapeCatalog tape, Integer driveIndex, Integer slotIndex)
        throws ReadWriteException {
        try {
            final TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                try {
                    loadUnloadService.unloadTape(slotIndex, driveIndex);
                } catch (TapeCommandException e) {
                    throw new ReadWriteException(
                        MSG_PREFIX +
                        TAPE_MSG +
                        tape.getCode() +
                        ", Action : unload, Entity: " +
                        JsonHandler.unprettyPrint(e.getDetails()),
                        ReadWriteErrorCode.KO_ON_UNLOAD_TAPE,
                        e
                    );
                }

                tape.setCurrentLocation(tape.getPreviousLocation());
            } finally {
                tapeRobotPool.pushRobotService(tapeRobotService);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
        }
    }

    @Override
    public TapeDriveSpec getDriveStatus(ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        int retry = 3;

        // FIXME : Is retry on failure really a wanted feature or a possible bug cause?
        while (true) {
            try {
                return tapeDriveService.getDriveCommandService().status();
            } catch (TapeCommandException e) {
                retry--;
                if (retry == 0) {
                    throw new ReadWriteException(
                        MSG_PREFIX +
                        TAPE_MSG +
                        " Action : drive status, Entity: " +
                        JsonHandler.unprettyPrint(e.getDetails()),
                        readWriteErrorCode
                    );
                }

                LOGGER.error(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : drive status, Entity: " +
                    JsonHandler.unprettyPrint(e.getDetails()),
                    ReadWriteErrorCode.KO_ON_STATUS,
                    e
                );

                Uninterruptibles.sleepUninterruptibly(SLEEP_TIME, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public Integer getDriveIndex() {
        return tapeDriveService.getTapeDriveConf().getIndex();
    }

    @Override
    public String getLibraryIdentifier() {
        return tapeRobotPool.getLibraryIdentifier();
    }

    @Override
    public String getTmpOutputDirectory() {
        return tapeDriveService.getReadWriteService().getTmpOutputStorageFolder();
    }

    @Override
    public void ensureTapeIsEmpty(TapeCatalog tape, boolean forceOverrideNonEmptyCartridges) throws ReadWriteException {
        // Check empty tape
        try {
            LOGGER.debug(
                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Action: try read from empty tape (expected to fail)"
            );
            tapeDriveService.getDriveCommandService().move(1, false);

            // Moving tape to next position was expected to fail as tape was supposed to be empty, but was not.

            if (forceOverrideNonEmptyCartridges) {
                LOGGER.warn("OVERRIDING NON EMPTY CARTRIDGE " + tape.getCode());
                try {
                    tapeDriveService.getDriveCommandService().rewind();
                } catch (TapeCommandException e) {
                    throw new ReadWriteException(
                        MSG_PREFIX +
                        TAPE_MSG +
                        tape.getCode() +
                        " Action : Force override non empty tape, " +
                        "Error: Could not rewind for force empty cartridge overriding, Entity: " +
                        JsonHandler.unprettyPrint(e.getDetails()),
                        ReadWriteErrorCode.KO_REWIND_BEFORE_FORCE_OVERRIDE_NON_EMPTY_TAPE,
                        e
                    );
                }
            } else {
                tape.setCurrentPosition(tape.getCurrentPosition() + 1);
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    " Action : Is Tape Empty, Error: Tape not empty but tape catalog is empty",
                    ReadWriteErrorCode.KO_LABEL_DISCORDING_NOT_EMPTY_TAPE
                );
            }
        } catch (TapeCommandException e) {
            // Nominal case (expected to fail)
            LOGGER.debug(
                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Action: trying read from empty tape failed successfully",
                e
            );
        }

        // Do status to get tape TYPE and some other information (update catalog)
        TapeDriveSpec driveStatus = getDriveStatus(ReadWriteErrorCode.KO_ON_STATUS);

        tape.setType(driveStatus.getCartridge());
        tape.setWorm(driveStatus.getDriveStatuses().contains(TapeDriveStatus.WR_PROT));
    }

    @Override
    public void checkNonEmptyTapeLabel(TapeCatalog tape) throws ReadWriteException {
        // Read Label from tape
        File labelFile = null;
        try {
            labelFile = new File(this.getTmpOutputDirectory(), GUIDFactory.newGUID().getId());

            try {
                tapeDriveService.getReadWriteService().readFromTape(labelFile.getAbsolutePath());
            } catch (TapeCommandException e) {
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    " Action : Read from tape, Entity: " +
                    JsonHandler.unprettyPrint(e.getDetails()),
                    ReadWriteErrorCode.KO_ON_READ_LABEL,
                    e
                );
            }

            TapeCatalogLabel tapeLabel = JsonHandler.getFromFile(labelFile, TapeCatalogLabel.class);

            final TapeCatalogLabel tapeCatalogLabel = tape.getLabel();

            if (!Objects.equals(tapeLabel.getId(), tapeCatalogLabel.getId())) {
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    tape.getCode() +
                    " Action : Check tape label, Expected label: " +
                    tapeCatalogLabel.getId() +
                    ", read label: " +
                    tapeLabel.getId(),
                    ReadWriteErrorCode.KO_LABEL_DISCORDING
                );
            }

            tape.setCurrentPosition(1);
        } catch (ReadWriteException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
        } finally {
            FileUtils.deleteQuietly(labelFile);
        }
    }
}
