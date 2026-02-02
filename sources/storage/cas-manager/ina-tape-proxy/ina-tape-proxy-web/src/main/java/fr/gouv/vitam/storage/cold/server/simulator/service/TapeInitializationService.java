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
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyRepositoryException;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeCatalogModel;
import fr.gouv.vitam.storage.cold.server.simulator.model.TapeDriveModel;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeCatalogRepository;
import fr.gouv.vitam.storage.cold.server.simulator.repository.TapeDriveRepository;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.FileSystemManager;
import fr.gouv.vitam.storage.cold.server.simulator.service.filesystem.MarkerManager;
import fr.gouv.vitam.storage.cold.server.simulator.util.InaTapeHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service responsible for initializing tape catalog and drives at application startup.
 */
public class TapeInitializationService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeInitializationService.class);

    private final InaTapeProxyConfiguration configuration;
    private final TapeCatalogRepository tapeCatalogRepository;
    private final TapeDriveRepository tapeDriveRepository;

    public TapeInitializationService(
        InaTapeProxyConfiguration configuration,
        TapeCatalogRepository tapeCatalogRepository,
        TapeDriveRepository tapeDriveRepository
    ) {
        this.configuration = configuration;
        this.tapeCatalogRepository = tapeCatalogRepository;
        this.tapeDriveRepository = tapeDriveRepository;
    }

    private void validateConfig(InaTapeProxyConfiguration configuration) {
        if (CollectionUtils.isEmpty(configuration.getTapeCodes())) {
            throw new IllegalArgumentException("Empty tape codes list");
        }

        if (configuration.getTapeMaxCapacityMB() <= 0) {
            throw new IllegalArgumentException("Invalid tape max capacity");
        }

        if (configuration.getNbDrives() <= 0) {
            throw new IllegalArgumentException("Invalid number of drives");
        }

        if (configuration.getNbSlots() <= 0) {
            throw new IllegalArgumentException("Invalid number of slots");
        }

        if (configuration.getTapeCodes().size() > configuration.getNbSlots()) {
            throw new IllegalArgumentException("Not enough slots to store all tapes");
        }
        if (StringUtils.isEmpty(configuration.getExchangeDirectory())) {
            throw new IllegalArgumentException("Invalid exchange directory");
        }
        if (StringUtils.isEmpty(configuration.getInaStorageDirectory())) {
            throw new IllegalArgumentException("Invalid ina storage directory");
        }
        if (StringUtils.isEmpty(configuration.getOfferStorageDirectory())) {
            throw new IllegalArgumentException("Invalid offer storage directory");
        }
    }

    /**
     * Ensures that tape catalog entries exist for the provided tape codes.
     * Idempotent: skips tape codes that already have an entry in the collection.
     * Each new tape is initially assigned to the slot matching its position in the list.
     *
     * @param configuration ordered list of tape codes from configuration
     * @throws IllegalStateException if tape codes exist in database but not in configuration
     */
    public void ensureTapeCatalogExists(InaTapeProxyConfiguration configuration)
        throws InaTapeProxyRepositoryException {
        List<String> tapeCodes = configuration.getTapeCodes();

        List<TapeDriveModel> drives = tapeDriveRepository.findAll();
        List<TapeCatalogModel> existingTapesInDb = tapeCatalogRepository.findAll();
        Set<String> existingTapeCodesInDb = existingTapesInDb
            .stream()
            .map(TapeCatalogModel::getId)
            .collect(Collectors.toSet());

        validateTapeCodesConsistency(existingTapeCodesInDb, tapeCodes);

        Set<Integer> usedSlotIndexes = existingTapesInDb
            .stream()
            .map(TapeCatalogModel::getSlotNumber)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<Integer> reservedSlotInDrives = drives
            .stream()
            .map(TapeDriveModel::getInitialTapeSlotNumber)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<Integer> availableSlotNumbers = IntStream.rangeClosed(1, configuration.getNbSlots())
            .boxed()
            .filter(slotIndex -> !usedSlotIndexes.contains(slotIndex) && !reservedSlotInDrives.contains(slotIndex))
            .collect(Collectors.toCollection(LinkedList::new));

        for (String tapeCode : tapeCodes) {
            if (existingTapeCodesInDb.contains(tapeCode)) {
                LOGGER.info("Tape catalog entry already exists for: {}, skipping", tapeCode);
                continue;
            }

            TapeCatalogModel tape = new TapeCatalogModel();
            tape.setId(tapeCode);
            tape.setTotalStoredFiles(0);
            tape.setFull(false);
            tape.setDriveIndex(null);
            tape.setTotalSizeUsed(0L);

            if (availableSlotNumbers.isEmpty()) {
                throw new IllegalStateException("No available slot found. Please update configuration.");
            }
            Integer slotNumber = availableSlotNumbers.removeFirst();
            tape.setSlotNumber(slotNumber);

            tapeCatalogRepository.save(tape);
            LOGGER.info("Created tape catalog entry: {} in slot {}", tapeCode, slotNumber);
        }
    }

    /**
     * Validates that all tape codes in database exist in configuration.
     *
     * @param tapeCodes list of tape codes from configuration
     * @throws IllegalStateException if tape codes exist in database but not in configuration
     */
    private void validateTapeCodesConsistency(Set<String> existingTapesInDb, List<String> tapeCodes) {
        List<String> orphanTapeCodes = existingTapesInDb.stream().filter(id -> !tapeCodes.contains(id)).toList();

        if (!orphanTapeCodes.isEmpty()) {
            String errorMessage = String.format(
                "Found tape codes in database that are not present in configuration: %s. " +
                "Please update configuration or remove these entries from database.",
                orphanTapeCodes
            );
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Ensures that drive entries exist for the configured number of drives.
     * Idempotent: skips drives that already exist in the collection.
     *
     * @param nbDrives Number of drives to initialize
     */
    public void ensureDrivesExist(int nbDrives) throws InaTapeProxyRepositoryException {
        List<TapeDriveModel> existingDrivesInDb = tapeDriveRepository.findAll();

        if (!existingDrivesInDb.isEmpty()) {
            // For now, no need to handle drive count update.
            if (nbDrives != existingDrivesInDb.size()) {
                throw new IllegalStateException(
                    "Inconsistent number of drives in the database. Please update configuration or cleanup unused drives in database."
                );
            }
            return;
        }

        for (int i = 0; i < nbDrives; i++) {
            String id = InaTapeHelper.driveIdByIndex(i);

            TapeDriveModel drive = new TapeDriveModel();
            drive.setId(id);
            drive.setIndex(i);
            drive.setTapeCode(null);
            drive.setPosition(null);

            tapeDriveRepository.save(drive);
            LOGGER.info("Initialized drive: {}", id);
        }
    }

    public InaService initializeService() throws IOException, InaTapeProxyRepositoryException {
        // Validate config
        validateConfig(configuration);

        // Seed tape catalog from config (idempotent) and initialize drives
        ensureTapeCatalogExists(configuration);
        ensureDrivesExist(configuration.getNbDrives());

        // 1. Initialize filesystem manager
        LOGGER.info("Initializing FileSystemManager...");
        FileSystemManager fileSystemManager = new FileSystemManager(configuration);
        fileSystemManager.initialize();
        LOGGER.info("FileSystemManager initialized");

        // 2. Initialize marker manager
        LOGGER.info("Initializing MarkerManager...");
        MarkerManager markerManager = new MarkerManager(fileSystemManager);
        LOGGER.info("MarkerManager initialized");

        // 3. Initialize tape helper
        LOGGER.info("Initializing InaTapeHelper...");
        InaTapeHelper inaTapeHelper = new InaTapeHelper(tapeCatalogRepository, tapeDriveRepository);
        LOGGER.info("InaTapeHelper initialized");

        // 4. Initialize library service (tape lifecycle + robotic simulation)
        LOGGER.info("Initializing InaLibraryService...");
        InaLibraryService libraryService = new InaLibraryService(
            configuration,
            tapeCatalogRepository,
            tapeDriveRepository,
            inaTapeHelper
        );
        LOGGER.info("  - InaLibraryService initialized");

        // 5. Initialize I/O service
        LOGGER.info("Initializing InaIoService...");
        InaIoService ioService = new InaIoService(
            configuration,
            fileSystemManager,
            markerManager,
            tapeCatalogRepository,
            tapeDriveRepository,
            inaTapeHelper
        );
        LOGGER.info("  - InaIoService initialized");

        // 6. Create main service
        LOGGER.info("Initializing main InaService...");
        InaService service = new InaService(ioService, libraryService);
        LOGGER.info("InaService initialized");

        return service;
    }
}
