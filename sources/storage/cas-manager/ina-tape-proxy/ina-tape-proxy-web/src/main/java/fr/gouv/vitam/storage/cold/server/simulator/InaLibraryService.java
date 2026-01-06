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
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.engine.common.api.exception.TapeCommandException;

/**
 * Library Service for INA
 * Handles library commands (move, rewind, load, eject, etc.)
 * All commands are sleep-only (no real effect)
 */
public class InaLibraryService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaLibraryService.class);

    private final InaTapeProxyConfiguration configuration;

    public InaLibraryService(InaTapeProxyConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Move tape position (simulated)
     */
    public void move(int position, boolean backward) throws TapeCommandException {
        LOGGER.debug("move(position={}, backward={}) - simulating...", position, backward);
        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("move() completed");
    }

    /**
     * Rewind tape (simulated)
     */
    public void rewind() throws TapeCommandException {
        LOGGER.debug("rewind() - simulating...");
        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("rewind() completed");
    }

    /**
     * Go to end of data (simulated)
     */
    public void goToEnd() throws TapeCommandException {
        LOGGER.debug("goToEnd() - simulating...");
        simulateLatency(configuration.getRoboticLatencyMs());
        LOGGER.debug("goToEnd() completed");
    }

    /**
     * Eject tape (simulated)
     */
    public void eject() throws TapeCommandException {
        LOGGER.debug("eject() - simulating...");
        simulateLatency(configuration.getEjectLatencyMs());
        LOGGER.debug("eject() completed");
    }

    /**
     * Load tape into drive (simulated)
     */
    public void loadTape(int slot, int drive) throws TapeCommandException {
        LOGGER.info("loadTape(slot={}, drive={}) - simulating...", slot, drive);
        simulateLatency(configuration.getLoadLatencyMs());
        LOGGER.info("loadTape() completed");
    }

    /**
     * Unload tape from drive (simulated)
     */
    public void unloadTape(int slot, int drive) throws TapeCommandException {
        LOGGER.info("unloadTape(slot={}, drive={}) - simulating...", slot, drive);
        simulateLatency(configuration.getUnloadLatencyMs());
        LOGGER.info("unloadTape() completed");
    }

    /**
     * Simulate latency with Thread.sleep
     */
    private void simulateLatency(long latencyMs) throws TapeCommandException {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapeCommandException("Operation interrupted", e);
        }
    }
}
