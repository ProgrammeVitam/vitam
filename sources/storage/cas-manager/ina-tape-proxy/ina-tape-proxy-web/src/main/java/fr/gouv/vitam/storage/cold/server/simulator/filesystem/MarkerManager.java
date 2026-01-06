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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Manages marker files for tracking operation states
 * Marker files indicate the state of write/read operations:
 * - .written: Write operation completed
 * - .read: Read operation completed
 */
public class MarkerManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MarkerManager.class);

    private final FileSystemManager fsManager;

    public MarkerManager(FileSystemManager fsManager) {
        this.fsManager = fsManager;
    }

    /**
     * Marker types for different operation states
     */
    public enum MarkerType {
        WRITTEN(".WRITTEN"),
        NOT_FOUND(".NOT_FOUND"),
        READ_REQUEST(".READ_REQUEST"),
        AVAILABLE_FOR_READ(".AVAILABLE_FOR_READ");

        private final String suffix;

        MarkerType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    /**
     * Create a marker file for the given filename and marker type
     * The marker file contains a timestamp
     */
    public void createMarker(String filename, MarkerType type) throws IOException {
        Path markerPath = fsManager.getMarkersPath(filename + type.getSuffix());
        String timestamp = Instant.now().toString();

        Files.writeString(markerPath, timestamp);

        LOGGER.debug("Created marker: {} (type={})", markerPath.getFileName(), type);
    }

    /**
     * Delete a marker file for the given filename and marker type
     */
    public void deleteMarker(String filename, MarkerType type) throws IOException {
        Path markerPath = fsManager.getMarkersPath(filename + type.getSuffix());
        boolean deleted = Files.deleteIfExists(markerPath);

        if (deleted) {
            LOGGER.debug("Deleted marker: {} (type={})", markerPath.getFileName(), type);
        } else {
            LOGGER.trace("Marker does not exist, nothing to delete: {} (type={})", markerPath.getFileName(), type);
        }
    }
}
