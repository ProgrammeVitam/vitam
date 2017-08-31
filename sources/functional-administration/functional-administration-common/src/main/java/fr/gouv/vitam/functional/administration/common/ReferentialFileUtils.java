package fr.gouv.vitam.functional.administration.common;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;

/**
 * Common utils for masterdata referential sevices.
 */
public final class ReferentialFileUtils {

    /**
     * Add into a logbookOperation's evDetdata the filename information. If evDetData does not exists create the
     * evDetData.
     * 
     * @param filename the filename information
     * @param logbookParameters logbookoperation parameters
     * @throws InvalidParseOperationException if the existing evDetData is not valid
     */
    public static void addFilenameInLogbookOperation(String filename,
        final LogbookOperationParameters logbookParameters)
        throws InvalidParseOperationException {
        if (StringUtils.isNotBlank(filename)) {
            ObjectNode evDetData = null;
            if (logbookParameters.getParameterValue(LogbookParameterName.eventDetailData) != null &&
                !logbookParameters.getParameterValue(LogbookParameterName.eventDetailData).isEmpty()) {
                evDetData = (ObjectNode) JsonHandler
                    .getFromString(logbookParameters.getParameterValue(LogbookParameterName.eventDetailData));
            } else {
                evDetData = JsonHandler.createObjectNode();
            }
            evDetData.put("FileName", filename);
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                JsonHandler.unprettyPrint(evDetData));
            logbookParameters.putParameterValue(LogbookParameterName.masterData,
                JsonHandler.unprettyPrint(evDetData));
        }
    }
}
