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
package fr.gouv.vitam.storage.offers.tape.parser;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import org.apache.commons.lang3.StringUtils;

public class TapeDriveStatusParser {

    private static final String TAPE_DRIVE = "tape drive:";
    private static final String FILE_NUMBER = "File number=";
    private static final String BLOCK_NUMBER = ", block number=";
    private static final String PARTITION = ", partition=";
    private static final String TAPE_BLOCK_SIZE = "Tape block size ";
    private static final String DENSITY = " bytes. Density code ";
    private static final String OPEN_PARENTHESIS = "(";
    private static final String CLOSE_PARENTHESIS = ")";
    private static final String SOFT_ERROR = "Soft error count since last status=";
    private static final String GENERAL_STATUS = "General status bits on ";
    private static final String POINT = ".";

    public TapeDriveState parse(String output) {
        ParametersChecker.checkParameter("All params is required", output);

        final TapeDriveState tapeDriveState = new TapeDriveState();
        for (String s : output.split("\n")) {
            if (s.contains(TAPE_DRIVE)) {
                tapeDriveState.setDescription(s.trim());
            } else if (s.contains(FILE_NUMBER)) {
                extractFileAndBlockNumberAndPartition(tapeDriveState, s);
            } else if (s.contains(TAPE_BLOCK_SIZE)) {
                extractBlockSizeAndDensity(tapeDriveState, s);
            } else if (s.contains(SOFT_ERROR)) {
                extractSoftErrorCount(tapeDriveState, s);
            } else if (s.contains(GENERAL_STATUS)) {
                extractGeneralStatus(tapeDriveState, s);
            } else {
                extractDriveStatus(tapeDriveState, s);
            }
        }

        return tapeDriveState;
    }

    private void extractBlockSizeAndDensity(TapeDriveState tapeDriveState, String s) {
        String blockSize = StringUtils.substringBetween(s, TAPE_BLOCK_SIZE, DENSITY);
        tapeDriveState.setTapeBlockSize(Long.valueOf(blockSize.trim()));

        String densityCode = StringUtils.substringBetween(s, DENSITY, OPEN_PARENTHESIS);
        tapeDriveState.setDensityCode(densityCode.trim());

        String lto = StringUtils.substringBetween(s, OPEN_PARENTHESIS, CLOSE_PARENTHESIS);
        tapeDriveState.setCartridge(lto);
    }

    private void extractFileAndBlockNumberAndPartition(TapeDriveState tapeDriveState, String s) {
        String fileNumber = StringUtils.substringBetween(s, FILE_NUMBER, BLOCK_NUMBER);
        tapeDriveState.setFileNumber(Integer.valueOf(fileNumber.trim()));

        String blockNumber = StringUtils.substringBetween(s, BLOCK_NUMBER, PARTITION);
        tapeDriveState.setBlockNumber(Integer.valueOf(blockNumber.trim()));

        String partition = StringUtils.substringBetween(s, PARTITION, POINT);
        tapeDriveState.setPartition(Integer.valueOf(partition.trim()));

        String lto = StringUtils.substringBetween(s, OPEN_PARENTHESIS, CLOSE_PARENTHESIS);
        tapeDriveState.setCartridge(lto);
    }

    private void extractSoftErrorCount(TapeDriveState tapeDriveState, String s) {
        String errorCount = StringUtils.substringAfterLast(s, SOFT_ERROR);
        tapeDriveState.setErrorCountSinceLastStatus(Integer.valueOf(errorCount.trim()));
    }

    private void extractGeneralStatus(TapeDriveState tapeDriveState, String s) {
        String statusBits = StringUtils.substringBetween(s, OPEN_PARENTHESIS, CLOSE_PARENTHESIS);
        tapeDriveState.setStatusBits(statusBits);
    }

    private void extractDriveStatus(TapeDriveState tapeDriveState, String s) {
        for (TapeDriveStatus tapeDriveStatus : TapeDriveStatus.values()) {
            if (s.contains(tapeDriveStatus.getStatus())) {
                tapeDriveState.addToDriveStatuses(tapeDriveStatus);
            }
        }
    }
}
