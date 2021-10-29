/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.offers.tape.impl.readwrite;

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

import java.nio.file.Paths;
import java.util.List;

public class DdTapeLibraryService implements TapeReadWriteService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DdTapeLibraryService.class);
    public static final String IF = "if=";
    public static final String OF = "of=";
    private final TapeDriveConf tapeDriveConf;
    private final ProcessExecutor processExecutor;
    private final String inputDirectory;
    private final String tmpOutputStorageFolder;

    public DdTapeLibraryService(TapeDriveConf tapeDriveConf, ProcessExecutor processExecutor,
        String inputDirectory, String tmpOutputStorageFolder) {
        ParametersChecker
            .checkParameter("All params are required", tapeDriveConf, processExecutor, inputDirectory,
                tmpOutputStorageFolder);


        this.tapeDriveConf = tapeDriveConf;
        this.processExecutor = processExecutor;
        this.inputDirectory = inputDirectory;
        this.tmpOutputStorageFolder = tmpOutputStorageFolder;
    }

    @Override
    public TapeResponse writeToTape(String inputPath) {
        ParametersChecker.checkParameter("Arguments inputPath is required", inputPath);

        List<String> args =
            Lists.newArrayList(IF + Paths.get(this.getInputDirectory()).resolve(inputPath).toAbsolutePath(),
                OF + tapeDriveConf.getDevice());

        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getDdPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor()
                .execute(tapeDriveConf.getDdPath(), tapeDriveConf.isUseSudo(), tapeDriveConf.getTimeoutInMilliseconds(),
                    args);

        return parseCommonResponse(output);
    }

    @Override
    public TapeResponse readFromTape(String outputPath) {
        ParametersChecker.checkParameter("Arguments outputPath is required", outputPath);

        List<String> args = Lists.newArrayList(IF + tapeDriveConf.getDevice(),
            OF + Paths.get(this.getTmpOutputStorageFolder()).resolve(outputPath).toAbsolutePath());

        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getDdPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor()
                .execute(tapeDriveConf.getDdPath(), tapeDriveConf.isUseSudo(), tapeDriveConf.getTimeoutInMilliseconds(),
                    args);

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
    public ProcessExecutor getExecutor() {
        return processExecutor;
    }

    @Override
    public String getTmpOutputStorageFolder() {
        return tmpOutputStorageFolder;
    }

    @Override
    public String getInputDirectory() {
        return inputDirectory;
    }
}
