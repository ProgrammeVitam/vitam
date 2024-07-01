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

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import org.apache.commons.io.FilenameUtils;

/**
 * Helper for Worker handlers to handle Logbook Lifecycle at startup/at end
 */
public class LogbookLifecycleWorkerHelper {

    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";

    /**
     * Private
     */
    private LogbookLifecycleWorkerHelper() {
        // Empty
    }

    /**
     * @param params the worker parameter
     * @return the ObjectID as String
     */
    public static final String getObjectID(WorkerParameters params) {
        final String extension = FilenameUtils.getExtension(params.getObjectName());
        return params.getObjectName().replace("." + extension, "");
    }

    /**
     * @param helper the LogbookLifeCyclesClientHelper
     * @param logbookLifecycleParameters the parameter of logbook lifecycle
     * @param params the worker parameters
     * @param lfcEventType the event type of lfc
     * @param logbookTypeProcess the logbook type process
     * @param statusCode the global status code
     * @param additionalParams the additional params
     * @throws ProcessingException if logbook lfc ressouce not found
     */
    public static void updateLifeCycleStep(
        LogbookLifeCyclesClientHelper helper,
        LogbookLifeCycleParameters logbookLifecycleParameters,
        WorkerParameters params,
        String lfcEventType,
        LogbookTypeProcess logbookTypeProcess,
        StatusCode statusCode,
        String... additionalParams
    ) throws ProcessingException {
        try {
            if (additionalParams != null && additionalParams.length >= 1) {
                logbookLifecycleParameters.putParameterValue(
                    LogbookParameterName.objectIdentifier,
                    additionalParams[0]
                );
            } else {
                logbookLifecycleParameters.putParameterValue(
                    LogbookParameterName.objectIdentifier,
                    getObjectID(params)
                );
            }

            logbookLifecycleParameters.putParameterValue(
                LogbookParameterName.eventIdentifierProcess,
                params.getContainerName()
            );
            logbookLifecycleParameters.putParameterValue(
                LogbookParameterName.eventIdentifier,
                GUIDFactory.newEventGUID(0).toString()
            );
            logbookLifecycleParameters.putParameterValue(
                LogbookParameterName.eventTypeProcess,
                logbookTypeProcess.name()
            );

            if (lfcEventType == null) {
                logbookLifecycleParameters.setFinalStatus(params.getCurrentStep(), null, statusCode, null);
            } else {
                logbookLifecycleParameters.setFinalStatus(lfcEventType, null, statusCode, null);
            }

            helper.updateDelegate(logbookLifecycleParameters);
        } catch (final LogbookClientNotFoundException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * @param helper the LogbookLifeCyclesClientHelper
     * @param logbookLifecycleParameters the parameter of logbook lifecycle
     * @param params the parameters the worker parameters
     * @throws ProcessingException if logbook lfc ressouce not found
     */
    public static void updateLifeCycleForBegining(
        LogbookLifeCyclesClientHelper helper,
        LogbookLifeCycleParameters logbookLifecycleParameters,
        WorkerParameters params,
        LogbookTypeProcess logbookTypeProcess
    ) throws ProcessingException {
        try {
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, getObjectID(params));
            logbookLifecycleParameters.putParameterValue(
                LogbookParameterName.eventIdentifierProcess,
                params.getContainerName()
            );
            logbookLifecycleParameters.putParameterValue(
                LogbookParameterName.eventTypeProcess,
                logbookTypeProcess.name()
            );
            helper.updateDelegate(logbookLifecycleParameters);
        } catch (final LogbookClientNotFoundException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }
}
