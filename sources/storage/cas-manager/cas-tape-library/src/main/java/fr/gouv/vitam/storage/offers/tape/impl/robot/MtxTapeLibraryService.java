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
package fr.gouv.vitam.storage.offers.tape.impl.robot;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.parser.TapeLibraryStatusParser;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;

import java.util.List;

public class MtxTapeLibraryService implements TapeLoadUnloadService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MtxTapeLibraryService.class);
    public static final String F = "-f";
    public static final String UNLOAD = "unload";
    public static final String LOAD = "load";
    public static final String STATUS = "status";

    private final TapeRobotConf tapeRobotConf;
    private final ProcessExecutor processExecutor;

    public MtxTapeLibraryService(TapeRobotConf tapeRobotConf, ProcessExecutor processExecutor) {
        ParametersChecker.checkParameter("All params are required", tapeRobotConf, processExecutor);
        this.tapeRobotConf = tapeRobotConf;
        this.processExecutor = processExecutor;
    }

    @Override
    public TapeLibrarySpec status() throws TapeCommandException {
        List<String> args = Lists.newArrayList(F, tapeRobotConf.getDevice(), STATUS);
        LOGGER.debug(
            "Execute script : {},timeout: {}, args : {}",
            tapeRobotConf.getMtxPath(),
            tapeRobotConf.getTimeoutInMilliseconds(),
            args
        );
        Output output =
            this.processExecutor.execute(
                    tapeRobotConf.getMtxPath(),
                    true,
                    tapeRobotConf.getTimeoutInMilliseconds(),
                    args
                );
        return parseTapeLibraryState(output);
    }

    @Override
    public void loadTape(int slotNumber, int driveIndex) throws TapeCommandException {
        Output output = executeCommand(slotNumber, driveIndex, LOAD);

        if (output.getExitCode() != 0) {
            throw new TapeCommandException(
                "Could not load tape from slot " + slotNumber + " into drive " + driveIndex,
                output
            );
        }
    }

    @Override
    public void unloadTape(int slotNumber, int driveIndex) throws TapeCommandException {
        Output output = executeCommand(slotNumber, driveIndex, UNLOAD);

        if (output.getExitCode() != 0) {
            throw new TapeCommandException(
                "Could not unload tape from drive " + driveIndex + " into slot " + slotNumber,
                output
            );
        }
    }

    private TapeLibrarySpec parseTapeLibraryState(Output output) throws TapeCommandException {
        if (output.getExitCode() != 0) {
            throw new TapeCommandException("Could not retrieve tape library status", output);
        }

        final TapeLibraryStatusParser tapeLibraryStatusParser = new TapeLibraryStatusParser();
        return tapeLibraryStatusParser.parse(output.getStdout());
    }

    private Output executeCommand(int tapeIndex, int driveIndex, String command) {
        List<String> args = Lists.newArrayList(
            F,
            tapeRobotConf.getDevice(),
            command,
            Integer.toString(tapeIndex),
            Integer.toString(driveIndex)
        );
        LOGGER.debug(
            "Execute script : {},timeout: {}, args : {}",
            tapeRobotConf.getMtxPath(),
            tapeRobotConf.getTimeoutInMilliseconds(),
            args
        );

        return this.processExecutor.execute(tapeRobotConf.getMtxPath(), tapeRobotConf.getTimeoutInMilliseconds(), args);
    }
}
