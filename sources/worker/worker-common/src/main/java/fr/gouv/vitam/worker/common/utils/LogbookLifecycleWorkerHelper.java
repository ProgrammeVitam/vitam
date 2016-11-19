/**
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
 */
package fr.gouv.vitam.worker.common.utils;

import org.apache.commons.io.FilenameUtils;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

/**
 * Helper for Worker handlers to handle Logbook Lifecycle at startup/at end
 */
public class LogbookLifecycleWorkerHelper {
    private static final String LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG = "LogbookClient Unsupported request";
    private static final String LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG = "Logbook LifeCycle resource not found";
    private static final String LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG = "Logbook Server internal error";

    /**
     * Private
     */
    private LogbookLifecycleWorkerHelper() {
        // Empty
    }

    /**
     * @param logbooklifeCyclesClient
     * @param logbookLifecycleParameters
     * @param params the parameters
     * @param lfcEventType 
     * @param logbookTypeProcess 
     * @throws ProcessingException
     */
    public static void updateLifeCycleStartStep(LogbookLifeCyclesClient logbooklifeCyclesClient,
        LogbookLifeCycleParameters logbookLifecycleParameters, WorkerParameters params, String lfcEventType,
        LogbookTypeProcess logbookTypeProcess)
        throws ProcessingException {

        try {
            final String extension = FilenameUtils.getExtension(params.getObjectName());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.objectIdentifier,
                params.getObjectName().replace("." + extension, ""));
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                params.getContainerName());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventIdentifier,
                GUIDFactory.newEventGUID(0).toString());
            // TODO P2 to be passed within the parameters since multiple workflow types could exist
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
                logbookTypeProcess.name());

            if (lfcEventType == null) {
                logbookLifecycleParameters.setFinalStatus(params.getCurrentStep(), null, StatusCode.STARTED, null);
            } else {
                logbookLifecycleParameters.setFinalStatus(lfcEventType, null, StatusCode.STARTED, null);
            }


            logbooklifeCyclesClient.update(logbookLifecycleParameters);
        } catch (final LogbookClientBadRequestException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            SedaUtils.LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * @param logbooklifeCyclesClient
     * @param logbookLifecycleParameters
     * @param params the parameters
     * @throws ProcessingException
     */
    public static void updateLifeCycleForBegining(LogbookLifeCyclesClient logbooklifeCyclesClient,
        LogbookLifeCycleParameters logbookLifecycleParameters, WorkerParameters params)
        throws ProcessingException {

        try {
            String extension = FilenameUtils.getExtension(params.getObjectName());
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.objectIdentifier,
                params.getObjectName().replace("." + extension, ""));
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                params.getContainerName());
            // TODO P2 to be passed within the parameters since multiple workflow types could exist
            logbookLifecycleParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
                LogbookTypeProcess.INGEST.name());
            logbooklifeCyclesClient.update(logbookLifecycleParameters);
        } catch (final LogbookClientBadRequestException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            SedaUtils.LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * 
     * 
     * @param logbooklifeCyclesClient
     * @param logbookLifecycleParameters logbook LC parameters
     * @param itemStatus the Item Status
     * @throws ProcessingException
     */
    public static void setLifeCycleFinalEventStatusByStep(LogbookLifeCyclesClient logbooklifeCyclesClient,
        LogbookLifeCycleParameters logbookLifecycleParameters,
        ItemStatus itemStatus)
        throws ProcessingException {

        try {
            logbookLifecycleParameters.setFinalStatus(itemStatus.getItemId(), null, itemStatus.getGlobalStatus(), null);
            logbooklifeCyclesClient.update(logbookLifecycleParameters);
        } catch (final LogbookClientBadRequestException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_BAD_REQUEST_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientServerException e) {
            SedaUtils.LOGGER.error(LOGBOOK_SERVER_INTERNAL_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        } catch (final LogbookClientNotFoundException e) {
            SedaUtils.LOGGER.error(LOGBOOK_LF_RESOURCE_NOT_FOUND_EXCEPTION_MSG, e);
            throw new ProcessingException(e);
        }
    }

}
