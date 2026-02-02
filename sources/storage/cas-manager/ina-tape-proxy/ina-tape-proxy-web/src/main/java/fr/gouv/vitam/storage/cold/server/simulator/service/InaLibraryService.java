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
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyBadRequestException;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyException;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyServerException;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeCatalogModel;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeDriveModel;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeCatalogRepository;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeDriveRepository;
import fr.gouv.vitam.storage.cold.server.simulator.util.InaTapeHelper;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeCartridge;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDrive;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveState;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeLibraryState;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeSlot;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeSlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Library Service for INA.
 * Handles robotic commands (move, rewind, load, eject, etc.), virtual tape lifecycle
 * (capacity, load/unload, slot allocation) and status queries.
 *
 * Source of truth:
 * - Slots: TapeCatalogModel collection (tapes with slotIndex != null occupy that slot)
 * - Drives: TapeDriveModel collection (persisted drive state)
 * - TapeLibrarySpec is built dynamically on each getLibraryStatus() call; it is never stored in the database.
 */
public class InaLibraryService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaLibraryService.class);
    private static final String DEV_INA_TAPE_PROXY_NST_PREFIX = "/dev/ina-tape-proxy-nst";

    private final InaTapeProxyConfiguration configuration;
    private final TapeCatalogRepository tapeCatalogRepository;
    private final TapeDriveRepository tapeDriveRepository;
    private final InaTapeHelper inaTapeHelper;

    public InaLibraryService(
        InaTapeProxyConfiguration configuration,
        TapeCatalogRepository tapeCatalogRepository,
        TapeDriveRepository tapeDriveRepository,
        InaTapeHelper inaTapeHelper
    ) {
        this.configuration = configuration;
        this.tapeCatalogRepository = tapeCatalogRepository;
        this.tapeDriveRepository = tapeDriveRepository;
        this.inaTapeHelper = inaTapeHelper;
        LOGGER.info("InaLibraryService initialized - waiting for tape to be loaded");
    }

    // ========== ROBOTIC SIMULATION ==========

    /**
     * Move tape position (simulated)
     */
    public void move(int driveIndex, int position, boolean backward) throws InaTapeProxyException {
        LOGGER.debug("move(driveIndex={}, position={}, backward={}) - simulating...", driveIndex, position, backward);

        if (position == 0) {
            throw new InaTapeProxyBadRequestException("Position is zero");
        }

        TapeDriveModel tapeDriveModel = inaTapeHelper.getTapeDriveModel(driveIndex);
        String tapeCode = tapeDriveModel.getTapeCode();

        if (tapeCode == null) {
            throw new InaTapeProxyBadRequestException(
                "Cannot move tape position in drive " + driveIndex + ". No tape is present in the drive"
            );
        }

        TapeCatalogModel tapeCatalogModel = inaTapeHelper.getTapeCatalogModel(tapeCode);

        int newPosition;
        if (backward) {
            newPosition = tapeDriveModel.getPosition() - position;
            if (newPosition < 0) {
                throw new InaTapeProxyBadRequestException(
                    "Cannot move drive " +
                    driveIndex +
                    " from position " +
                    tapeDriveModel.getPosition() +
                    " with " +
                    position +
                    " files backward"
                );
            }
        } else {
            newPosition = tapeDriveModel.getPosition() + position;
            if (newPosition > tapeCatalogModel.getTotalStoredFiles()) {
                throw new InaTapeProxyBadRequestException(
                    "Cannot move drive " +
                    driveIndex +
                    " from position " +
                    tapeDriveModel.getPosition() +
                    " to " +
                    newPosition +
                    " . Tape " +
                    tapeCode +
                    " only contains " +
                    tapeCatalogModel.getTotalStoredFiles() +
                    " entries"
                );
            }
        }
        tapeDriveModel.setPosition(newPosition);

        tapeDriveRepository.save(tapeDriveModel);

        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("move() completed");
    }

    /**
     * Rewind tape (simulated)
     */
    public void rewind(int driveIndex) throws InaTapeProxyException {
        LOGGER.debug("rewind() - simulating...");

        TapeDriveModel tapeDriveModel = inaTapeHelper.getTapeDriveModel(driveIndex);
        String tapeCode = tapeDriveModel.getTapeCode();

        if (tapeCode == null) {
            throw new InaTapeProxyBadRequestException(
                "Cannot rewind tape in drive " + driveIndex + ". No tape is present in the drive"
            );
        }

        tapeDriveModel.setPosition(0);
        tapeDriveRepository.save(tapeDriveModel);

        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("rewind() completed");
    }

    /**
     * Go to end of data (simulated)
     */
    public void goToEnd(int driveIndex) throws InaTapeProxyException {
        LOGGER.debug("goToEnd() - simulating...");

        TapeDriveModel tapeDriveModel = inaTapeHelper.getTapeDriveModel(driveIndex);
        String tapeCode = tapeDriveModel.getTapeCode();

        if (tapeCode == null) {
            throw new InaTapeProxyBadRequestException(
                "Cannot go to EndOfTape for drive " + driveIndex + ". No tape is present in the drive"
            );
        }

        TapeCatalogModel tapeCatalogModel = inaTapeHelper.getTapeCatalogModel(tapeCode);

        tapeDriveModel.setPosition(tapeCatalogModel.getTotalStoredFiles());
        tapeDriveRepository.save(tapeDriveModel);

        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("goToEnd() completed");
    }

    /**
     * Eject tape (simulated)
     */
    public void eject(int driveIndex) throws InaTapeProxyException {
        LOGGER.debug("eject() - simulating...");

        TapeDriveModel tapeDriveModel = inaTapeHelper.getTapeDriveModel(driveIndex);
        String tapeCode = tapeDriveModel.getTapeCode();

        if (tapeCode == null) {
            throw new InaTapeProxyBadRequestException(
                "Cannot go to EndOfTape for drive " + driveIndex + ". No tape is present in the drive"
            );
        }

        // For some reason, tape library eject can be done without previous rewind of the tape

        tapeDriveModel.setPosition(null);
        tapeDriveRepository.save(tapeDriveModel);

        simulateLatency(configuration.getEjectLatencyMs());
        LOGGER.debug("eject() completed");
    }

    // ========== LOAD / UNLOAD ==========

    /**
     * Load tape from slot into drive.
     * Looks up the tape currently in the given slot, validates its status,
     * then moves it into the drive (clears slotIndex on the tape, sets tape on drive).
     * Thread-safety is ensured by:
     * - synchronized method to prevent concurrent access
     * - optimistic locking in repositories (version field)
     */
    public synchronized void loadTape(int slotNumber, int driveIndex) throws InaTapeProxyException {
        LOGGER.info("LoadTape(slot={}, drive={}) - loading...", slotNumber, driveIndex);

        // Check if drive already has a tape loaded
        TapeDriveModel driveModel = inaTapeHelper.getTapeDriveModel(driveIndex);

        if (driveModel.getTapeCode() != null) {
            throw new InaTapeProxyBadRequestException("A tape is already loaded in the drive.");
        }

        TapeCatalogModel tape = tapeCatalogRepository
            .findBySlotNumber(slotNumber)
            .orElseThrow(() -> new InaTapeProxyBadRequestException("No tape found in slot " + slotNumber));

        // Update tape / drive in db
        tape.setSlotNumber(null);
        tape.setDriveIndex(driveIndex);

        driveModel.setTapeCode(tape.getId());
        driveModel.setInitialTapeSlotNumber(slotNumber);
        driveModel.setPosition(0);

        tapeCatalogRepository.save(tape);
        tapeDriveRepository.save(driveModel);

        simulateLatency(configuration.getLoadLatencyMs());
    }

    /**
     * Unload tape from drive back to a slot.
     * Assigns the next available slot to the tape, clears the drive.
     * Thread-safety is ensured by:
     * - synchronized method to prevent concurrent access
     * - optimistic locking in repositories (version field)
     */
    public synchronized void unloadTape(int slotNumber, int driveIndex) throws InaTapeProxyException {
        LOGGER.info("unloadTape(slot={}, drive={}) - unloading...", slotNumber, driveIndex);

        // Check if drive already has a tape loaded
        TapeDriveModel driveModel = inaTapeHelper.getTapeDriveModel(driveIndex);

        if (driveModel.getTapeCode() == null) {
            throw new InaTapeProxyBadRequestException("No tape is loaded in the drive.");
        }

        Optional<TapeCatalogModel> existingTapeInSlot = tapeCatalogRepository.findBySlotNumber(slotNumber);
        if (existingTapeInSlot.isPresent()) {
            throw new InaTapeProxyBadRequestException(
                "Another tape " + existingTapeInSlot.get().getId() + " is stored in slot " + slotNumber
            );
        }

        TapeCatalogModel tape = tapeCatalogRepository.findById(driveModel.getTapeCode()).orElseThrow();

        // Update tape / drive in db
        tape.setSlotNumber(slotNumber);
        tape.setDriveIndex(null);

        driveModel.setTapeCode(null);
        driveModel.setPosition(null);
        driveModel.setInitialTapeSlotNumber(null);

        tapeCatalogRepository.save(tape);
        tapeDriveRepository.save(driveModel);

        simulateLatency(configuration.getUnloadLatencyMs());
    }

    // ========== STATUS ==========

    /**
     * Retrieves the drive status from the persisted TapeDriveModel.
     */
    public TapeDriveState getDriveStatus(int driveIndex) throws InaTapeProxyException {
        TapeDriveModel tapeDriveModel = inaTapeHelper.getTapeDriveModel(driveIndex);

        TapeDriveState result = new TapeDriveState();
        result.setDescription(DEV_INA_TAPE_PROXY_NST_PREFIX + driveIndex);
        result.setErrorCountSinceLastStatus(0);

        List<TapeDriveStatus> statuses = new ArrayList<>();
        statuses.add(TapeDriveStatus.IM_REP_EN);

        if (tapeDriveModel.getTapeCode() != null) {
            TapeCatalogModel tapeCatalogModel = inaTapeHelper.getTapeCatalogModel(tapeDriveModel.getTapeCode());

            result.setCartridge(tapeDriveModel.getTapeCode());
            result.setFileNumber(tapeDriveModel.getPosition());
            statuses.add(TapeDriveStatus.ONLINE);
            if (tapeDriveModel.getPosition() == 0) {
                statuses.add(TapeDriveStatus.BOT);
            }
            if (tapeDriveModel.getPosition() == tapeCatalogModel.getTotalStoredFiles()) {
                statuses.add(TapeDriveStatus.EOD);
            }
        } else {
            statuses.add(TapeDriveStatus.DR_OPEN);
        }

        result.setDriveStatuses(statuses);
        return result;
    }

    /**
     * Builds the library status dynamically.
     */
    public TapeLibraryState getLibraryStatus() throws InaTapeProxyException {
        List<TapeCatalogModel> allTapes = tapeCatalogRepository.findAll();
        List<TapeDriveModel> allDrives = tapeDriveRepository.findAll();

        // Build slots: iterate over configured slot count, place tapes that have a matching slotIndex
        List<TapeSlot> slots = new ArrayList<>();
        for (int i = 1; i <= configuration.getNbSlots(); i++) {
            TapeSlot slot = new TapeSlot();
            slot.setIndex(i);
            slot.setStorageElementType(TapeSlotType.SLOT);

            final int slotIdx = i;
            allTapes
                .stream()
                .filter(t -> t.getSlotNumber() != null && t.getSlotNumber() == slotIdx)
                .findFirst()
                .ifPresent(tape -> {
                    slot.setTape(new TapeCartridge().setSlotIndex(slotIdx).setVolumeTag(tape.getId()));
                });

            slots.add(slot);
        }

        // Build drives list from persisted TapeDriveModel entries
        List<TapeDrive> drives = allDrives.stream().map(this::toDto).toList();

        // Assemble the library spec
        TapeLibraryState lib = new TapeLibraryState();
        lib.setDevice(configuration.getLibraryName());
        lib.setDriveCount(configuration.getNbDrives());
        lib.setSlotsCount(configuration.getNbSlots());
        lib.setDrives(drives);
        lib.setSlots(slots);

        return lib;
    }

    // ========== PRIVATE ==========

    private TapeDrive toDto(TapeDriveModel tapeDriveModel) {
        TapeDrive result = new TapeDrive();
        result.setIndex(tapeDriveModel.getIndex());
        if (tapeDriveModel.getTapeCode() != null) {
            result.setTape(
                new TapeCartridge()
                    .setVolumeTag(tapeDriveModel.getTapeCode())
                    .setSlotIndex(tapeDriveModel.getInitialTapeSlotNumber())
            );
        }
        return result;
    }

    /**
     * Simulate latency with Thread.sleep
     */
    private void simulateLatency(long latencyMs) throws InaTapeProxyException {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InaTapeProxyServerException("Operation interrupted", e);
        }
    }
}
