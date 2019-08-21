package fr.gouv.vitam.storage.offers.tape.parser;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.StatusCode;
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

        final TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
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
