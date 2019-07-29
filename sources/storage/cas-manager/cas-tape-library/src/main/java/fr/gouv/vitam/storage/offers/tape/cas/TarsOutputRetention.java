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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TarsOutputRetention implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarsOutputRetention.class);

    private final Path tarsOutputPath;

    // Max retention duration = 7 days
    private final static long MAX_RETENTION_DURATION_IN_MILISECONDS = 604800000;

    public TarsOutputRetention(String tarsOutputPath) {
        this.tarsOutputPath = Paths.get(tarsOutputPath);
    }


    @Override
    public void run() {
        while (true) {
            try (Stream<Path> list = Files.list(tarsOutputPath)) {
                List<Path> filesToDelete = list
                    .filter(path -> fileEligibleToDelete(path)).collect(Collectors.toList());

                filesToDelete.forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        LOGGER.error("Error when deleting file " + file);
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Error when deleting Tars from output folder", e);
            }

            // wait 5 minutes before the next check
            try {
                TimeUnit.MINUTES.sleep(5);
            } catch (InterruptedException e) {

            }
        }
    }

    private boolean fileEligibleToDelete(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis() <=
                MAX_RETENTION_DURATION_IN_MILISECONDS;
        } catch (IOException e) {
            LOGGER.error("Error when reading attributes of file " + path);
            return false;
        }
    }
}
