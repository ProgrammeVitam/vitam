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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.TapeDriveWorker;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Helper JUnit Rule for setup / clean of a {@link TapeLibrarySimulator} as test context.
 */
public class TapeLibrarySimulatorRule extends ExternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibrarySimulatorRule.class);

    private final TapeLibrarySimulator tapeLibrarySimulator;

    public TapeLibrarySimulatorRule(
        Path inputDirectory,
        Path tempOutputStorageDirectory,
        int nbDrives,
        int nbSlots,
        int nbTapes,
        int maxTapeCapacityInBytes,
        String cartridgeType,
        int sleepDelayMillis
    ) {
        this.tapeLibrarySimulator = new TapeLibrarySimulator(
            inputDirectory,
            tempOutputStorageDirectory,
            nbDrives,
            nbSlots,
            nbTapes,
            maxTapeCapacityInBytes,
            cartridgeType,
            sleepDelayMillis
        );

        TapeLibraryFactory.TapeServiceCreator tapeServiceCreator = new TapeLibraryFactory.TapeServiceCreator() {
            @Override
            public TapeRobotService createRobotService(TapeRobotConf tapeRobotConf) {
                return tapeLibrarySimulator::getTapeLoadUnloadService;
            }

            @Override
            public TapeDriveService createTapeDriveService(
                TapeLibraryConfiguration configuration,
                TapeDriveConf tapeDriveConf
            ) {
                return new TapeDriveService() {
                    @Override
                    public TapeReadWriteService getReadWriteService() {
                        return tapeLibrarySimulator.getTapeReadWriteServices().get(tapeDriveConf.getIndex());
                    }

                    @Override
                    public TapeDriveCommandService getDriveCommandService() {
                        return tapeLibrarySimulator.getTapeDriveCommandServices().get(tapeDriveConf.getIndex());
                    }

                    @Override
                    public TapeDriveConf getTapeDriveConf() {
                        return tapeDriveConf;
                    }
                };
            }
        };

        TapeLibraryFactory.getInstance().overrideTapeServiceCreatorForTesting(tapeServiceCreator);
        TapeDriveWorker.updateInactivitySleepDelayForTesting();
    }

    public TapeLibrarySimulator getTapeLibrarySimulator() {
        return tapeLibrarySimulator;
    }

    @Override
    public void after() {
        TapeLibraryFactory.getInstance().resetTapeLibraryFactoryAfterTests();

        List<Exception> reportedExceptions = tapeLibrarySimulator.getFailures();
        if (!reportedExceptions.isEmpty()) {
            for (Exception exception : reportedExceptions) {
                LOGGER.error("Reported exception : " + exception);
            }
            fail("Expected no error, got " + reportedExceptions);
        }
    }
}
