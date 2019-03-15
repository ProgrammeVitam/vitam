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

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;

public class TarTapeLibraryService implements TapeReadWriteService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarTapeLibraryService.class);
    public static final String CVF = "-cvf";
    public static final String XVF = "-xvf";
    public static final String TVF = "-tvf";
    public static final String C = "-C";
    private final TapeDriveConf tapeDriveConf;
    private final ProcessExecutor processExecutor;
    private final Lock canReadWrite;

    public TarTapeLibraryService(TapeDriveConf tapeDriveConf, ProcessExecutor processExecutor) {
        ParametersChecker.checkParameter("Params are required", tapeDriveConf, processExecutor);
        this.tapeDriveConf = tapeDriveConf;
        this.processExecutor = processExecutor;
        this.canReadWrite = tapeDriveConf.getLock();
    }

    @Override
    public TapeResponse writeToTape(String workingDir, String filePath) {
        ParametersChecker
            .checkParameter("Arguments device and inputPath is required", tapeDriveConf.getDevice(), filePath);

        List<String> args = Lists.newArrayList(CVF, tapeDriveConf.getDevice(), workingDir + filePath);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getTarPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getTarPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);

        return parseCommonResponse(output);
    }

    @Override
    public TapeResponse readFromTape(String workingDir, String filetoExtract) {

        List<String> args = Lists.newArrayList(C, workingDir, XVF, tapeDriveConf.getDevice());

        if (!Strings.isNullOrEmpty(filetoExtract)) {
            args.add(filetoExtract);
        }

        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getTarPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getTarPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        return parseCommonResponse(output);
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

    @Override
    public TapeResponse listFromTape() {
        List<String> args = Lists.newArrayList(TVF, tapeDriveConf.getDevice());
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getTarPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getTarPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);

        return parseCommonResponse(output);
    }

    @Override
    public ProcessExecutor getExecutor() {
        return processExecutor;
    }

    @Override
    public boolean begin() {
        return canReadWrite.tryLock();
    }

    @Override
    public void end() {
        canReadWrite.unlock();
    }
}
