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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Check SIP - Object and Archiveunit Consistency handler
 */
public class CheckObjectUnitConsistencyActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        CheckObjectUnitConsistencyActionHandler.class
    );

    private static final int OBJECTGROUP_TO_GUID_MAP_RANK = 1;
    private static final int OBJECTGROUP_TO_UNIT_MAP_RANK = 0;
    private static final String HANDLER_ID = "CHECK_CONSISTENCY";
    private static final String SUBTASK_ORPHAN = "CHECK_CONSISTENCY_ORPHAN_OBJECT";

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();

    /**
     * Empty constructor
     */
    public CheckObjectUnitConsistencyActionHandler() {
        handlerInitialIOList.add(Map.class);
        handlerInitialIOList.add(Map.class);
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(params);
        checkMandatoryIOParameter(handler);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            final List<String> notConformOGs = findObjectGroupsNonReferencedByArchiveUnit(handler, params, itemStatus);
            if (!notConformOGs.isEmpty()) {
                itemStatus.setData("errorNumber", notConformOGs.size());
            }
        } catch (Exception e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * find the object groups non referenced by at least one archive unit
     *
     * @param params worker parameter
     * @return list of non conform OG
     */
    private List<String> findObjectGroupsNonReferencedByArchiveUnit(
        HandlerIO handlerIO,
        WorkerParameters params,
        ItemStatus itemStatus
    ) {
        final List<String> ogList = new ArrayList<>();

        @SuppressWarnings("unchecked")
        final Map<String, Object> objectGroupToUnitStoredMap = (Map<String, Object>) handlerIO.getInput(
            OBJECTGROUP_TO_UNIT_MAP_RANK
        );
        @SuppressWarnings("unchecked")
        final Map<String, Object> objectGroupToGuidStoredMap = (Map<String, Object>) handlerIO.getInput(
            OBJECTGROUP_TO_GUID_MAP_RANK
        );

        if (objectGroupToGuidStoredMap.size() == 0) {
            itemStatus.increment(StatusCode.OK);
        } else {
            final Iterator<Entry<String, Object>> it = objectGroupToGuidStoredMap.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Object> objectGroup = it.next();
                if (!objectGroupToUnitStoredMap.containsKey(objectGroup.getKey())) {
                    itemStatus.increment(StatusCode.KO);
                    itemStatus.setGlobalOutcomeDetailSubcode(SUBTASK_ORPHAN);
                    try {
                        // Update logbook OG lifecycle
                        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
                            LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
                        logbookLifecycleObjectGroupParameters.setFinalStatus(SUBTASK_ORPHAN, null, StatusCode.KO, null);
                        LogbookLifecycleWorkerHelper.updateLifeCycleStep(
                            handlerIO.getHelper(),
                            logbookLifecycleObjectGroupParameters,
                            params,
                            HANDLER_ID,
                            params.getLogbookTypeProcess(),
                            StatusCode.KO,
                            objectGroupToGuidStoredMap.get(objectGroup.getKey()).toString()
                        );

                        final String objectID = logbookLifecycleObjectGroupParameters.getParameterValue(
                            LogbookParameterName.objectIdentifier
                        );
                        handlerIO
                            .getLifecyclesClient()
                            .bulkUpdateObjectGroup(
                                params.getContainerName(),
                                handlerIO.getHelper().removeUpdateDelegate(objectID)
                            );
                    } catch (
                        LogbookClientBadRequestException
                        | LogbookClientNotFoundException
                        | LogbookClientServerException
                        | ProcessingException e
                    ) {
                        LOGGER.error("Can not update logbook lifcycle", e);
                    }
                    ogList.add(objectGroup.getKey());
                } else {
                    itemStatus.increment(StatusCode.OK);
                    try {
                        // Update logbook OG lifecycle
                        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
                            LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
                        logbookLifecycleObjectGroupParameters.setFinalStatus(HANDLER_ID, null, StatusCode.OK, null);
                        LogbookLifecycleWorkerHelper.updateLifeCycleStep(
                            handlerIO.getHelper(),
                            logbookLifecycleObjectGroupParameters,
                            params,
                            HANDLER_ID,
                            params.getLogbookTypeProcess(),
                            StatusCode.OK,
                            objectGroupToGuidStoredMap.get(objectGroup.getKey()).toString()
                        );

                        final String objectID = logbookLifecycleObjectGroupParameters.getParameterValue(
                            LogbookParameterName.objectIdentifier
                        );
                        handlerIO
                            .getLifecyclesClient()
                            .bulkUpdateObjectGroup(
                                params.getContainerName(),
                                handlerIO.getHelper().removeUpdateDelegate(objectID)
                            );
                    } catch (
                        LogbookClientBadRequestException
                        | LogbookClientNotFoundException
                        | LogbookClientServerException
                        | ProcessingException e
                    ) {
                        LOGGER.error("Can not update logbook lifcycle", e);
                    }
                }
            }
        }
        return ogList;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(0, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }
}
