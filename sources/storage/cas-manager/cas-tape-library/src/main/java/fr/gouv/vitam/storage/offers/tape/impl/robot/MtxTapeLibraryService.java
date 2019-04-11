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
package fr.gouv.vitam.storage.offers.tape.impl.robot;

import java.util.List;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.parser.TapeLibraryStatusParser;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;

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
    public synchronized TapeLibrarySpec status() {
        List<String> args = Lists.newArrayList(F, tapeRobotConf.getDevice(), STATUS);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeRobotConf.getMtxPath(),
            tapeRobotConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeRobotConf.getMtxPath(), tapeRobotConf.isUseSudo(),
                tapeRobotConf.getTimeoutInMilliseconds(), args);
        return parseTapeLibraryState(output);
    }

    @Override
    public synchronized TapeResponse loadTape(String tapeIndex, String driveIndex) {
        ParametersChecker.checkParameter("Arguments tapeIndex and deriveIndex are required", tapeIndex, driveIndex);

        List<String> args = Lists.newArrayList(F, tapeRobotConf.getDevice(), LOAD, tapeIndex, driveIndex);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeRobotConf.getMtxPath(),
            tapeRobotConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeRobotConf.getMtxPath(), tapeRobotConf.isUseSudo(),
                tapeRobotConf.getTimeoutInMilliseconds(), args);

        return parseCommonResponse(output);
    }

    @Override
    public synchronized TapeResponse unloadTape(String tapeIndex, String driveIndex) {
        ParametersChecker.checkParameter("Arguments tapeIndex and deriveIndex are required", tapeIndex, driveIndex);

        List<String> args = Lists.newArrayList(F, tapeRobotConf.getDevice(), UNLOAD, tapeIndex, driveIndex);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeRobotConf.getMtxPath(),
            tapeRobotConf.getTimeoutInMilliseconds(),
            args);

        Output output =
            getExecutor().execute(tapeRobotConf.getMtxPath(), tapeRobotConf.isUseSudo(),
                tapeRobotConf.getTimeoutInMilliseconds(), args);

        return parseCommonResponse(output);
    }

    @Override
    public synchronized TapeResponse loadTape(Integer tapeIndex, Integer driveIndex) {
        return loadTape(tapeIndex.toString(), driveIndex.toString());
    }

    @Override
    public synchronized TapeResponse unloadTape(Integer tapeIndex, Integer driveIndex) {
        return unloadTape(tapeIndex.toString(), driveIndex.toString());
    }

    @Override
    public ProcessExecutor getExecutor() {
        return processExecutor;
    }

    private TapeResponse parseCommonResponse(Output output) {
        TapeResponse response;
        if (output.getExitCode() == 0) {
            response = new TapeResponse(output, StatusCode.OK);
        } else {
            response = new TapeResponse(output, output.getExitCode() == -1 ? StatusCode.WARNING : StatusCode.KO);
        }

        return response;
    }


    private TapeLibrarySpec parseTapeLibraryState(Output output) {
        if (output.getExitCode() == 0) {
            final TapeLibraryStatusParser tapeLibraryStatusParser = new TapeLibraryStatusParser();
            TapeLibraryState tapeLibraryState = tapeLibraryStatusParser.parse(output.getStdout());
            tapeLibraryState.setStatus(StatusCode.OK);
            tapeLibraryState.setEntity(output);
            return tapeLibraryState;
        } else {
            TapeLibraryState tapeLibraryState =
                new TapeLibraryState(output, output.getExitCode() == -1 ? StatusCode.WARNING : StatusCode.KO);
            tapeLibraryState.setStatus(output.getExitCode() == -1 ? StatusCode.WARNING : StatusCode.KO);
            return tapeLibraryState;
        }
    }
}
