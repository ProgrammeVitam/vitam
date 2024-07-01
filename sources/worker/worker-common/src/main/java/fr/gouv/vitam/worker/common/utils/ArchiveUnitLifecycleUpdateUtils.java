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
package fr.gouv.vitam.worker.common.utils;

import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

/**
 * ArchiveUnitUpdateUtils in order to deal with common update operations for units
 */
public class ArchiveUnitLifecycleUpdateUtils {

    private static final String UNIT_METADATA_UPDATE = "UPDATE_UNIT_RULES";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitLifecycleUpdateUtils.class);

    /**
     * Method used to log lifecycles unit
     *
     * @param params
     * @param auGuid
     * @param code
     * @param evDetData
     * @param logbookLifeCycleClient
     */
    public void logLifecycle(
        WorkerParameters params,
        String auGuid,
        StatusCode code,
        String evDetData,
        LogbookLifeCyclesClient logbookLifeCycleClient
    ) {
        try {
            LogbookLifeCycleUnitParameters logbookLCParam = getLogbookLifeCycleUpdateUnitParameters(
                GUIDReader.getGUID(params.getContainerName()),
                code,
                GUIDReader.getGUID(auGuid),
                UNIT_METADATA_UPDATE
            );
            if (evDetData != null) {
                logbookLCParam.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            }
            logbookLifeCycleClient.update(logbookLCParam);
        } catch (
            LogbookClientNotFoundException
            | LogbookClientBadRequestException
            | LogbookClientServerException
            | InvalidGuidOperationException e
        ) {
            // Ignore since could be during first insert step
            LOGGER.error("Should be in First Insert step so not alreday commited", e);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(
        GUID eventIdentifierProcess,
        StatusCode logbookOutcome,
        GUID objectIdentifier,
        String action
    ) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        return LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
            updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            eventTypeProcess,
            logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome),
            objectIdentifier
        );
    }

    /**
     * Method used to commit lifecycle
     *
     * @param processId
     * @param archiveUnitId
     * @param logbookLifeCycleClient
     */
    public void commitLifecycle(
        String processId,
        String archiveUnitId,
        LogbookLifeCyclesClient logbookLifeCycleClient
    ) {
        try {
            logbookLifeCycleClient.commitUnit(processId, archiveUnitId);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error("Couldn't commit lifecycles", e);
        }
    }
}
