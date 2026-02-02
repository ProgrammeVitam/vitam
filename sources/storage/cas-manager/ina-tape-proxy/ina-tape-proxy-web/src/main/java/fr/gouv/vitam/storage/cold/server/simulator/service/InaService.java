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
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyException;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveState;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeLibraryState;

/**
 * Main service orchestrator
 * Delegates to specialized services:
 * - InaIoService: write/read operations
 * - InaLibraryService: robotic commands (sleep-only) and status queries (static responses)
 */
public class InaService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaService.class);

    private final InaIoService ioService;
    private final InaLibraryService inaLibraryService;

    public InaService(InaIoService ioService, InaLibraryService inaLibraryService) {
        this.ioService = ioService;
        this.inaLibraryService = inaLibraryService;

        LOGGER.info("InaService initialized");
    }

    // ========== I/O OPERATIONS ==========

    public void writeToTape(int driveIndex, String inputPath) throws InaTapeProxyException {
        ioService.writeToTape(driveIndex, inputPath);
    }

    public void readFromTape(int driveIndex, String outputPath) throws InaTapeProxyException {
        ioService.readFromTape(driveIndex, outputPath);
    }

    // ========== ROBOTIC OPERATIONS ==========

    public void move(int driveIndex, int position, boolean backward) throws InaTapeProxyException {
        inaLibraryService.move(driveIndex, position, backward);
    }

    public void rewind(int driveIndex) throws InaTapeProxyException {
        inaLibraryService.rewind(driveIndex);
    }

    public void goToEnd(int driveIndex) throws InaTapeProxyException {
        inaLibraryService.goToEnd(driveIndex);
    }

    public void eject(int driveIndex) throws InaTapeProxyException {
        inaLibraryService.eject(driveIndex);
    }

    public void loadTape(int slotNumber, int driveIndex) throws InaTapeProxyException {
        inaLibraryService.loadTape(slotNumber, driveIndex);
    }

    public void unloadTape(int slotNumber, int driveIndex) throws InaTapeProxyException {
        inaLibraryService.unloadTape(slotNumber, driveIndex);
    }

    // ========== STATUS OPERATIONS ==========

    public TapeDriveState getDriveStatus(int driveIndex) throws InaTapeProxyException {
        return inaLibraryService.getDriveStatus(driveIndex);
    }

    public TapeLibraryState getLibraryStatus() throws InaTapeProxyException {
        return inaLibraryService.getLibraryStatus();
    }
}
