package fr.gouv.vitam.processing.worker.handler;

import java.net.URISyntaxException;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Check conformity handler
 */
public class CheckConformityActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);
    private static final String HANDLER_ID = "CheckConformity";
    private final SedaUtilsFactory sedaUtilsFactory;
    private static LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
    LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();

    /**
     * Constructor CheckConformityActionHandler with parameter SedaUtilsFactory
     * 
     * @param factory SedaUtils factory
     */
    public CheckConformityActionHandler(SedaUtilsFactory factory) {
        sedaUtilsFactory = factory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkParams params) throws ProcessingException {
        ParametersChecker.checkParameter("params is a mandatory parameter", params);
        ParametersChecker.checkParameter("ServerConfiguration is a mandatory parameter",
            params.getServerConfiguration());
        LOGGER.info("CheckConformityActionHandler running ...");

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID,
            OutcomeMessage.CHECK_CONFORMITY_OK);

        final SedaUtils sedaUtils = sedaUtilsFactory.create();

        try {
            List<String> digestMessageInvalidList = sedaUtils.checkConformityBinaryObject(params);
            if (digestMessageInvalidList.size() != 0) {
                String error = digestMessageInvalidList.get(0);
                for (int i = 1; i < digestMessageInvalidList.size(); i++) {
                    error += ", " + digestMessageInvalidList.get(i);
                }
                // TODO : the handler should not call the logbook operation, it should be done in the Engine
                // This is a bug causing 3 occurences of the step "Contrôle global entrée"
                parameters.putParameterValue(LogbookParameterName.eventIdentifier, GUIDFactory.newGUID().toString());
                parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, params.getContainerName());
                parameters.putParameterValue(LogbookParameterName.eventIdentifierRequest, params.getCurrentStep());
                parameters.putParameterValue(LogbookParameterName.eventType, params.getCurrentStep());
                parameters.putParameterValue(LogbookParameterName.eventTypeProcess, LogbookOutcome.WARNING.name());
                parameters.putParameterValue(LogbookParameterName.outcome, LogbookOutcome.WARNING.name());
                parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, getId() + " Error: " + error);
                client.update(parameters);
                response.setStatus(StatusCode.WARNING);
            }
        } catch (

        ProcessingException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.KO);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.KO);
        } catch (ContentAddressableStorageServerException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.KO);
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.KO);
        } catch (URISyntaxException | LogbookClientBadRequestException | LogbookClientNotFoundException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.FATAL);
        }

        LOGGER.info("CheckConformityActionHandler response: ", response.getStatus().name());
        return response;
    }

}
