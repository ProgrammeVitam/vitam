package fr.gouv.vitam.worker.core.handler;

import java.net.URISyntaxException;
import java.util.List;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

/**
 * Check conformity handler
 */
public class CheckConformityActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);
    private static final String HANDLER_ID = "CheckConformity";
    private final SedaUtilsFactory sedaUtilsFactory;
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
    public EngineResponse execute(WorkerParameters params) throws ProcessingException {
        checkMandatoryParameters(params);
        LOGGER.info("CheckConformityActionHandler running ...");

        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID,
            OutcomeMessage.CHECK_CONFORMITY_OK);

        final SedaUtils sedaUtils = sedaUtilsFactory.create();

        try {
            List<String> digestMessageInvalidList = sedaUtils.checkConformityBinaryObject(params);
            if (!digestMessageInvalidList.isEmpty()) {
                response.setErrorNumber(digestMessageInvalidList.size());
                response.setStatus(StatusCode.KO);
                response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
            }
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO);
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        } catch (URISyntaxException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.FATAL);
            response.setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        }

        LOGGER.info("CheckConformityActionHandler response: ", response.getStatus().name());
        return response;
    }

}
