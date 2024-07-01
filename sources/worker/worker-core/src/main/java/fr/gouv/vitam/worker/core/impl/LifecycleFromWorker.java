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
package fr.gouv.vitam.worker.core.impl;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.common.model.processing.LifecycleState;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.LocalDateUtil.now;

/**
 * classe permettant de générer des LFC à partir des ItemStatus renvoyé par le résultat d'une action.
 */
class LifecycleFromWorker {

    private List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersTemporaryBulks = new ArrayList<>();
    private List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulks = new ArrayList<>();
    private final LogbookLifeCyclesClient logbookLfcClient;

    LifecycleFromWorker(LogbookLifeCyclesClient logbookLfcClient) {
        this.logbookLfcClient = logbookLfcClient;
    }

    void generateLifeCycle(
        List<ItemStatus> pluginResponse,
        WorkerParameters workParams,
        Action action,
        DistributionType distributionType
    ) throws InvalidGuidOperationException {
        int i = 0;
        String handlerName = action.getActionDefinition().getActionKey();

        for (ItemStatus itemStatus : pluginResponse) {
            String objectName = workParams.getObjectNameList().get(i);
            i++;

            if (!StatusCode.ALREADY_EXECUTED.equals(itemStatus.getGlobalStatus()) && itemStatus.isLifecycleEnable()) {
                workParams.setObjectName(objectName);
                LogbookLifeCycleParameters lfcParam = createStartLogbookLfc(distributionType, handlerName, workParams);
                List<LogbookLifeCycleParameters> logbookParamList = createLogbookLifeCycleParameters(
                    handlerName,
                    itemStatus,
                    lfcParam
                );
                String objectId = objectName.replace(".json", "");
                if (action.getActionDefinition().lifecycleState() == LifecycleState.TEMPORARY) {
                    logbookLifeCycleParametersTemporaryBulks.add(
                        new LogbookLifeCycleParametersBulk(objectId, logbookParamList)
                    );
                } else {
                    logbookLifeCycleParametersBulks.add(new LogbookLifeCycleParametersBulk(objectId, logbookParamList));
                }
            } else {
                // FIXME (US 5769)
            }
        }
    }

    /**
     * sauvegarde des LFC
     *
     * @param distributionType unit or GOT
     * @throws VitamClientInternalException
     */
    void saveLifeCycles(DistributionType distributionType) throws VitamClientInternalException {
        if (!logbookLifeCycleParametersTemporaryBulks.isEmpty()) {
            logbookLfcClient.bulkLifeCycleTemporary(
                VitamThreadUtils.getVitamSession().getRequestId(),
                distributionType,
                logbookLifeCycleParametersTemporaryBulks
            );
            logbookLifeCycleParametersTemporaryBulks.clear();
        }
        if (!logbookLifeCycleParametersBulks.isEmpty()) {
            logbookLfcClient.bulkLifeCycle(
                VitamThreadUtils.getVitamSession().getRequestId(),
                distributionType,
                logbookLifeCycleParametersBulks
            );
            logbookLifeCycleParametersBulks.clear();
        }
    }

    private LogbookLifeCycleParameters createStartLogbookLfc(
        DistributionType distributionType,
        String handlerName,
        WorkerParameters workParams
    ) throws InvalidGuidOperationException {
        LogbookLifeCycleParameters lfcParam = null;
        switch (distributionType) {
            case Units:
                lfcParam = LogbookParameterHelper.newLogbookLifeCycleUnitParameters(
                    GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                    VitamLogbookMessages.getEventTypeLfc(handlerName),
                    GUIDReader.getGUID(workParams.getContainerName()),
                    // TODO Le type de process devrait venir du message recu (paramètre du workflow)
                    workParams.getLogbookTypeProcess(),
                    StatusCode.OK,
                    VitamLogbookMessages.getOutcomeDetailLfc(handlerName, StatusCode.OK),
                    VitamLogbookMessages.getCodeLfc(handlerName, StatusCode.OK),
                    GUIDReader.getGUID(LogbookLifecycleWorkerHelper.getObjectID(workParams))
                );

                lfcParam.putParameterValue(LogbookParameterName.eventDateTime, now().toString());

                break;
            case ObjectGroup:
                lfcParam = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
                    GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                    VitamLogbookMessages.getEventTypeLfc(handlerName),
                    GUIDReader.getGUID(workParams.getContainerName()),
                    // TODO Le type de process devrait venir du message recu (paramètre du workflow)
                    workParams.getLogbookTypeProcess(),
                    StatusCode.OK,
                    VitamLogbookMessages.getOutcomeDetailLfc(handlerName, StatusCode.OK),
                    VitamLogbookMessages.getCodeLfc(handlerName, StatusCode.OK),
                    GUIDReader.getGUID(LogbookLifecycleWorkerHelper.getObjectID(workParams))
                );

                lfcParam.putParameterValue(LogbookParameterName.eventDateTime, now().toString());

                break;
        }
        lfcParam.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );

        return lfcParam;
    }

    private List<LogbookLifeCycleParameters> createLogbookLifeCycleParameters(
        String handlerName,
        ItemStatus actionResponse,
        LogbookLifeCycleParameters logbookParam
    ) {
        logbookParam.putParameterValue(LogbookParameterName.eventDateTime, null);
        List<LogbookLifeCycleParameters> logbookParamList = new ArrayList<>();
        LogbookLifeCycleParameters finalLogbookLfcParam = LogbookLifeCyclesClientHelper.copy(logbookParam);
        if (!actionResponse.getItemId().contains(".")) {
            finalLogbookLfcParam.setFinalStatus(
                handlerName,
                null,
                actionResponse.getGlobalStatus(),
                actionResponse.getMessage()
            );
        } else {
            finalLogbookLfcParam.setFinalStatus(
                actionResponse.getItemId(),
                null,
                actionResponse.getGlobalStatus(),
                actionResponse.getMessage()
            );
        }
        if (!actionResponse.getEvDetailData().isEmpty()) {
            finalLogbookLfcParam.putParameterValue(
                LogbookParameterName.eventDetailData,
                actionResponse.getEvDetailData()
            );
        }
        if (actionResponse.getData("Detail") != null) {
            String outcomeDetailMessage =
                finalLogbookLfcParam.getParameterValue(LogbookParameterName.outcomeDetailMessage) +
                " " +
                actionResponse.getData("Detail");
            finalLogbookLfcParam.putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
        }
        for (final Map.Entry<String, ItemStatus> entry : actionResponse.getItemsStatus().entrySet()) {
            for (final Map.Entry<String, ItemStatus> subTaskEntry : entry.getValue().getSubTaskStatus().entrySet()) {
                LogbookLifeCycleParameters subLogbookLfcParam = LogbookLifeCyclesClientHelper.copy(logbookParam);
                // set a new eventId for every subTask
                subLogbookLfcParam.putParameterValue(
                    LogbookParameterName.eventIdentifier,
                    GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).getId()
                );
                // set parent eventId
                subLogbookLfcParam.putParameterValue(
                    LogbookParameterName.parentEventIdentifier,
                    logbookParam.getParameterValue(LogbookParameterName.eventIdentifier)
                );
                // set obId and gotId (used to determine the LFC to update)
                subLogbookLfcParam.putParameterValue(
                    LogbookParameterName.lifeCycleIdentifier,
                    subLogbookLfcParam.getParameterValue(LogbookParameterName.objectIdentifier)
                );
                subLogbookLfcParam.putParameterValue(LogbookParameterName.objectIdentifier, subTaskEntry.getKey());

                // set status
                ItemStatus subItemStatus = subTaskEntry.getValue();
                subLogbookLfcParam.setFinalStatus(
                    handlerName,
                    entry.getKey(),
                    subItemStatus.getGlobalStatus(),
                    subItemStatus.getMessage()
                );
                // set evDetailData
                if (!subItemStatus.getEvDetailData().isEmpty()) {
                    subLogbookLfcParam.putParameterValue(
                        LogbookParameterName.eventDetailData,
                        subItemStatus.getEvDetailData()
                    );
                }
                // set detailed message
                if (subItemStatus.getGlobalOutcomeDetailSubcode() != null) {
                    subLogbookLfcParam.putParameterValue(
                        LogbookParameterName.outcomeDetail,
                        VitamLogbookMessages.getOutcomeDetailLfc(
                            handlerName,
                            entry.getKey(),
                            subItemStatus.getGlobalOutcomeDetailSubcode(),
                            subItemStatus.getGlobalStatus()
                        )
                    );
                    subLogbookLfcParam.putParameterValue(
                        LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeLfc(
                            handlerName,
                            entry.getKey(),
                            subItemStatus.getGlobalOutcomeDetailSubcode(),
                            subItemStatus.getGlobalStatus()
                        )
                    );
                }
                // add to list
                logbookParamList.add(subLogbookLfcParam);
            }
            entry.getValue().getSubTaskStatus().clear();
        }
        logbookParamList.add(finalLogbookLfcParam);
        return logbookParamList;
    }
}
