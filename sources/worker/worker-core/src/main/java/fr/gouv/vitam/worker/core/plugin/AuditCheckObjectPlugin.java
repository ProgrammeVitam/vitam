package fr.gouv.vitam.worker.core.plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

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
            CheckExistenceObjectPlugin checkExistenceObjectPlugin = new CheckExistenceObjectPlugin();
            final ItemStatus checkExistenceActionStatus = checkExistenceObjectPlugin.execute(param, handler);
            itemStatus.setItemsStatus(CheckExistenceObjectPlugin.getId(), checkExistenceActionStatus);
            checkExistenceObjectPlugin.close();
            
            if (checkExistenceActionStatus.getGlobalStatus().equals(StatusCode.KO)) {
                handlerIO.addOuputResult(SHOULD_WRITE_RANK, true, true, false);
            } else {
                handlerIO.addOuputResult(SHOULD_WRITE_RANK, false, true, false);
            }            
        } else if (auditActions.contains(CheckIntegrityObjectPlugin.getId())) {
            CheckIntegrityObjectPlugin checkIntegrityObjectPlugin = new CheckIntegrityObjectPlugin();
            final ItemStatus checkIntegreityActionStatus = checkIntegrityObjectPlugin.execute(param, handler);
            itemStatus.setItemsStatus(CheckIntegrityObjectPlugin.getId(), checkIntegreityActionStatus);
            checkIntegrityObjectPlugin.close();
            
            if (checkIntegreityActionStatus.getGlobalStatus().equals(StatusCode.KO)) {
                handlerIO.addOuputResult(SHOULD_WRITE_RANK, true, true, false);
            } else {
                handlerIO.addOuputResult(SHOULD_WRITE_RANK, false, true, false);
            }
        }
        
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
        
    }

}
