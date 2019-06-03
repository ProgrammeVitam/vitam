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
package fr.gouv.vitam.storage.offers.tape.impl.readwrite;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;

import java.io.File;
import java.util.Objects;

public class TapeLibraryServiceImpl implements TapeLibraryService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibraryServiceImpl.class);

    private static final int MAX_ATTEMPS = 3;
    public static final long SLEEP_TIME = 20l;

    private TapeDriveService tapeDriveService;
    private TapeRobotPool tapeRobotPool;

    public final String MSG_PREFIX;


    public TapeLibraryServiceImpl(TapeDriveService tapeDriveService, TapeRobotPool tapeRobotPool) {
        this.tapeDriveService = tapeDriveService;
        this.tapeRobotPool = tapeRobotPool;
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex());
    }

    @Override
    public void goToPosition(TapeCatalog tape, Integer position, ReadWriteErrorCode readWriteErrorCode)
        throws ReadWriteException {
        Integer offset = position - tape.getCurrentPosition();

        if (offset != 0) {
            TapeResponse moveResponse = tapeDriveService.getDriveCommandService().move(Math.abs(offset), offset < 0);
            if (!moveResponse.isOK()) {
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                    " Action : FSF goto position Error " + moveResponse.getErrorCode() + ", Entity: " +
                    JsonHandler.unprettyPrint(moveResponse.getEntity()),
                    readWriteErrorCode,
                    moveResponse);
            }

            // Update current position only if fsf/bsf command success
            tape.setCurrentPosition(position);

        }
    }

    private void rewindTape(TapeCatalog tape) throws ReadWriteException {
        TapeResponse response = tapeDriveService.getDriveCommandService().rewind();
        if (!response.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                " Action : Rewind Tape, Entity: " +
                JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_REWIND_TAPE,
                response);
        }

        tape.setCurrentPosition(0);
    }

    @Override
    public void write(String filePath, long writtenBytes, TapeCatalog tape)
        throws ReadWriteException {
        if (tape == null) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: can't write, current tape is null.", ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }

        if (tape.getFileCount() < tape.getCurrentPosition()) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: current position must be <= to fileCount.",
                ReadWriteErrorCode.KO_TAPE_CURRENT_POSITION_GREATER_THAN_FILE_COUNT);
        }

        try {
            goToPosition(tape, tape.getFileCount(), ReadWriteErrorCode.KO_ON_GOTO_FILE_COUNT);


            TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(filePath);

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                    " Action : Write, Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));

                // Do status and check if end of tape
                TapeDriveSpec status = getDriveStatus(ReadWriteErrorCode.KO_UNKNOWN_CURRENT_POSITION);

                if (status.isEndOfTape()) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                        " Action : Write, Drive Status: " +
                        JsonHandler.unprettyPrint(status.getEntity()) + ", Error: End of tape",
                        ReadWriteErrorCode.KO_ON_END_OF_TAPE, response);
                }


                tape.setTapeState(TapeState.CONFLICT);

                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                    " Action : Write, Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_WRITE_TO_TAPE,
                    response);
            }

            tape.setFileCount(tape.getFileCount() + 1);
            tape.setCurrentPosition(tape.getFileCount());
            tape.setWrittenBytes(tape.getWrittenBytes() + writtenBytes);
            tape.setTapeState(TapeState.OPEN);

        } catch (Exception e) {
            if (e instanceof ReadWriteException) {
                throw (ReadWriteException) e;
            }
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
        }
    }

    @Override
    public void read(TapeCatalog tape, Integer position, String outputPath) throws ReadWriteException {
        ReadWriteException throwedException = null;

        // retry
        int nbRetry = MAX_ATTEMPS;
        while (nbRetry != 0) {
            try {
                throwedException = null;
                // move drive to the given position
                if (nbRetry != MAX_ATTEMPS) {
                    // Rewind the tape
                    rewindTape(tape);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                }
                nbRetry--;
                goToPosition(tape, position, ReadWriteErrorCode.KO_ON_GO_TO_POSITION);

                // read file from tape
                TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                    .readFromTape(outputPath);

                if (!response.isOK()) {
                    throwedException = new ReadWriteException("Error when reading file from tape",
                        ReadWriteErrorCode.KO_ON_READ_FROM_TAPE, response);
                    continue;
                }

                tape.setCurrentPosition(tape.getCurrentPosition() + 1);

            } catch (ReadWriteException e) {
                throwedException = e;
                continue;
            }

            break;
        }

        if (throwedException != null) {
            throw throwedException;
        }
    }

    @Override
    public void loadTape(TapeCatalog tape) throws ReadWriteException {
        ParametersChecker
            .checkParameter(
                MSG_PREFIX + ", Error: tape to load is null. please get markReady tape from catalog",
                tape);

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex;
        if (null != tape.getCurrentLocation()) {
            slotIndex = tape.getCurrentLocation().getIndex();
        } else {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                ", Error: tape current location is null. please update catalog",
                ReadWriteErrorCode.TAPE_LOCATION_CONFLICT_ON_LOAD);
        }

        try {
            TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                TapeResponse response = loadUnloadService.loadTape(slotIndex, driveIndex);

                if (!response.isOK()) {

                    TapeDriveSpec status = getDriveStatus(ReadWriteErrorCode.KO_ON_LOAD_THEN_STATUS);

                    if (!status.driveHasTape()) {

                        // Retry once
                        response = loadUnloadService.loadTape(slotIndex, driveIndex);

                        if (!response.isOK()) {
                            throw new ReadWriteException(
                                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Action : load, Entity: " +
                                    JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_LOAD_TAPE,
                                response);
                        }
                    }
                }

                tape.setPreviousLocation(tape.getCurrentLocation());
                tape.setCurrentLocation(new TapeLocation(getDriveIndex(), TapeLocationType.DRIVE));

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
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                        ", Error: previous location should no be in drive",
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT_ON_UNLOAD);
                default:
                    throw new IllegalArgumentException(
                        MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Error: location type not implemented");
            }
        }

        if (null == slotIndex) {
            // slotIndex = findEmptySlot() ?
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                ", Error : no empty slot found => cannot unload tape", ReadWriteErrorCode.NO_EMPTY_SLOT_FOUND);
        }

        try {
            TapeResponse ejectResponse = tapeDriveService.getDriveCommandService().eject();
            if (!ejectResponse.isOK()) {
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                    " Action : Eject tape with forced rewind, Error: Could not rewind or unload tape",
                    ReadWriteErrorCode.KO_REWIND_BEFORE_UNLOAD_TAPE, ejectResponse);
            }

            tape.setCurrentPosition(0);

            final TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                TapeResponse response = loadUnloadService.unloadTape(slotIndex, driveIndex);

                if (!response.isOK()) {

                    TapeDriveSpec status = getDriveStatus(ReadWriteErrorCode.KO_ON_UNLOAD_THEN_STATUS);

                    if (status.driveHasTape()) {
                        // Retry once
                        response = loadUnloadService.unloadTape(slotIndex, driveIndex);

                        if (!response.isOK()) {
                            throw new ReadWriteException(
                                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Action : unload, Entity: " +
                                    JsonHandler.unprettyPrint(response.getEntity()),
                                ReadWriteErrorCode.KO_ON_UNLOAD_TAPE,
                                response);
                        }
                    }
                }

                tape.setCurrentLocation(tape.getPreviousLocation());

            } finally {
                tapeRobotPool.pushRobotService(tapeRobotService);
            }
        } catch (InterruptedException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
        }
    }

    @Override
    public TapeDriveSpec getDriveStatus(ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        int retry = 3;

        TapeDriveSpec driveStatus = tapeDriveService.getDriveCommandService().status();

        retry--;

        while (retry != 0 && !driveStatus.isOK()) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG +
                    " Action : drive status, Entity: " + JsonHandler.unprettyPrint(driveStatus.getEntity()),
                ReadWriteErrorCode.KO_ON_STATUS, driveStatus);

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }

            retry--;

            driveStatus = tapeDriveService.getDriveCommandService().status();
        }

        if (!driveStatus.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG +
                " Action : drive status, Entity: " + JsonHandler.unprettyPrint(driveStatus.getEntity()),
                readWriteErrorCode, (TapeResponse) driveStatus);
        }

        return driveStatus;
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
    public String getOutputDirectory() {
        return tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD).
            getOutputDirectory();
    }

    @Override
    /**
     * Check if label of tape catalog match label of loaded tape
     *
     * @throws ReadWriteException
     */
    public boolean checkTapeLabel(TapeCatalog tape, boolean forceOverrideNonEmptyCartridges) throws ReadWriteException {

        // If no label then cartridge is unknown
        if (null == tape.getLabel()) {

            // Check empty tape
            TapeResponse moveResponse = tapeDriveService.getDriveCommandService().move(1, false);
            if (moveResponse.isOK()) {

                if (forceOverrideNonEmptyCartridges) {

                    LOGGER.warn("OVERRIDING NON EMPTY CARTRIDGE " + tape.getCode());
                    TapeResponse rewindResponse = tapeDriveService.getDriveCommandService().rewind();

                    if (!rewindResponse.isOK()) {
                        throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                            " Action : Force override non empty tape, Error: Could not rewind for force empty cartridge overriding",
                            ReadWriteErrorCode.KO_REWIND_BEFORE_FORCE_OVERRIDE_NON_EMPTY_TAPE, rewindResponse);
                    }

                } else {

                    tape.setCurrentPosition(tape.getCurrentPosition() + 1);
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                        " Action : Is Tape Empty, Error: Tape not empty but tape catalog is empty",
                        ReadWriteErrorCode.KO_LABEL_DISCORDING_NOT_EMPTY_TAPE, moveResponse);
                }
            }

            // Do status to get tape TYPE and some other information (update catalog)
            LOGGER.debug(
                MSG_PREFIX + TAPE_MSG + tape.getCode() + ", Action: drive status");
            TapeDriveSpec driveStatus = getDriveStatus(ReadWriteErrorCode.KO_ON_STATUS);

            tape.setType(driveStatus.getCartridge());
            tape.setWorm(driveStatus.getDriveStatuses().contains(TapeDriveStatus.WR_PROT));

            return true;
        } else {

            // Read Label from tape
            File labelFile = null;
            try {
                labelFile = File.createTempFile(TAPE_LABEL, GUIDFactory.newGUID().getId());

                TapeResponse readStatus =
                    tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                        .readFromTape(labelFile.getAbsolutePath());

                if (!readStatus.isOK()) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                        " Action : Read from tape, Entity: " + JsonHandler.unprettyPrint(readStatus.getEntity()),
                        ReadWriteErrorCode.KO_ON_READ_LABEL, readStatus);
                }

                final TapeCatalogLabel tapeLabel = JsonHandler.getFromFile(labelFile, TapeCatalogLabel.class);
                final TapeCatalogLabel tapeCatalogLabel = tape.getLabel();

                if (tapeLabel == null || !Objects.equals(tapeLabel.getId(), tapeCatalogLabel.getId())) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode() +
                        " Action : Check tape label, Entity: " + JsonHandler.unprettyPrint(readStatus.getEntity()),
                        ReadWriteErrorCode.KO_LABEL_DISCORDING, readStatus);
                }

                tape.setCurrentPosition(1);

                return false;

            } catch (Exception e) {
                if (e instanceof ReadWriteException) {
                    throw (ReadWriteException) e;
                }
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + tape.getCode(), e);
            } finally {
                labelFile.delete();
            }

        }
    }
}
