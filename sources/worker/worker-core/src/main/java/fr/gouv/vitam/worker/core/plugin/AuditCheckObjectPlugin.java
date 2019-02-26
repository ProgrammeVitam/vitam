package fr.gouv.vitam.worker.core.plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Audit - Check Object class
 */
public class AuditCheckObjectPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuditCheckObjectPlugin.class);

    private static final String HANDLER_ID = "AUDIT_CHECK_OBJECT";
    private static final int SHOULD_WRITE_RANK = 0;

    private HandlerIO handlerIO;

    /**
     * empty Constructor
     *
     */
    public AuditCheckObjectPlugin() {
        // empty constructor
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        LOGGER.debug(HANDLER_ID + " in execute");
        handlerIO = handler;
        
        String actionType = null;

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
        String actions = mapParameters.get(WorkerParameterName.auditActions);

        List<String> auditActions = Arrays.asList(actions.split("\\s*,\\s*"));

        try (final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            // param.getObjectName() get id of the object group
            JsonNode searchResult =
                metadataClient.selectObjectGrouptbyId(new SelectMultiQuery().getFinalSelect(), param.getObjectName());
            JsonNode ogNode = searchResult.get(RequestResponseOK.TAG_RESULTS);
            if (ogNode != null && ogNode.size() > 0) {
                handler.getInput().add(ogNode.get(0));
            } else {
                itemStatus.increment(StatusCode.FATAL);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        } catch (InvalidParseOperationException | MetaDataException e) {
            LOGGER.error("Metadta server errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        if (auditActions.contains(CheckExistenceObjectPlugin.getId())) {
            try (CheckExistenceObjectPlugin checkExistenceObjectPlugin = new CheckExistenceObjectPlugin()) {
                final ItemStatus checkExistenceActionStatus = checkExistenceObjectPlugin.execute(param, handler);
                actionType = CheckExistenceObjectPlugin.getId();
                itemStatus.setItemsStatus(CheckExistenceObjectPlugin.getId(), checkExistenceActionStatus);
                if (checkExistenceActionStatus.getGlobalStatus().equals(StatusCode.KO)) {
                    handlerIO.addOutputResult(SHOULD_WRITE_RANK, true, true, false);
                } else {
                    handlerIO.addOutputResult(SHOULD_WRITE_RANK, false, true, false);
                }
            }
        } else if (auditActions.contains(CheckIntegrityObjectPlugin.getId())) {
            try (CheckIntegrityObjectPlugin checkIntegrityObjectPlugin = new CheckIntegrityObjectPlugin()) {
                final ItemStatus checkIntegreityActionStatus = checkIntegrityObjectPlugin.execute(param, handler);
                actionType = CheckIntegrityObjectPlugin.getId();
                itemStatus.setItemsStatus(CheckIntegrityObjectPlugin.getId(), checkIntegreityActionStatus);
                if (checkIntegreityActionStatus.getGlobalStatus().equals(StatusCode.KO)) {
                    handlerIO.addOutputResult(SHOULD_WRITE_RANK, true, true, false);
                } else {
                    handlerIO.addOutputResult(SHOULD_WRITE_RANK, false, true, false);
                }
            }
        }

        writeLfcFromItemStatus(param, itemStatus);

        if (actionType != null && itemStatus.getGlobalStatus().isGreaterOrEqualToKo()) {
            itemStatus.setGlobalOutcomeDetailSubcode(actionType);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }



    /**
     * write LFC for KO tasks
     * 
     * @param param
     * @param itemStatus
     */
    private void writeLfcFromItemStatus(WorkerParameters param, ItemStatus itemStatus){
        if(itemStatus.getGlobalStatus().isGreaterOrEqualToKo()){
            try (LogbookLifeCyclesClient lfcClient = LogbookLifeCyclesClientFactory.getInstance().getClient()){
                for (ItemStatus subtask: itemStatus.getItemsStatus().values()) {
                    if(subtask.getGlobalStatus().isGreaterOrEqualToKo()){
                        LogbookLifeCycleParameters logbookLfcParam = 
                                LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
                            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                            VitamLogbookMessages.getEventTypeLfc(HANDLER_ID),
                            GUIDReader.getGUID(param.getContainerName()),
                            param.getLogbookTypeProcess(),
                            StatusCode.KO,
                            VitamLogbookMessages.getOutcomeDetailLfc(HANDLER_ID, subtask.getItemId(), StatusCode.KO),
                            VitamLogbookMessages.getCodeLfc(HANDLER_ID, subtask.getItemId(), StatusCode.KO),
                            GUIDReader.getGUID(LogbookLifecycleWorkerHelper.getObjectID(param)));
                        if (!subtask.getEvDetailData().isEmpty()) {
                            logbookLfcParam.putParameterValue(LogbookParameterName.eventDetailData,
                                    subtask.getEvDetailData());
                        }
                        if (subtask.getData("Detail") != null) {
                            String outcomeDetailMessage = logbookLfcParam.getParameterValue(
                                    LogbookParameterName.outcomeDetailMessage) + " " + subtask.getData("Detail");
                            logbookLfcParam.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                                    outcomeDetailMessage);
                        }
                        lfcClient.update(logbookLfcParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while updating Data Object LFC ", e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }
    }

}
