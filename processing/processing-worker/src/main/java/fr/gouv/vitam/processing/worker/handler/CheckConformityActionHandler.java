package fr.gouv.vitam.processing.worker.handler;

import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

/**
 * Check conformity handler
 */
public class CheckConformityActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);
    private static final String HANDLER_ID = "CheckConformity";
    private final SedaUtilsFactory sedaUtilsFactory;
    
    /**
     * Constructor CheckConformityActionHandler with parameter SedaUtilsFactory
     * 
     * @param factory SedaUtils factory
     */
    public CheckConformityActionHandler(SedaUtilsFactory factory){
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
        
        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_OK);
        final SedaUtils sedaUtils = sedaUtilsFactory.create();
        
        try {
            List<String> digestMessageInvalidList = sedaUtils.checkConformityBinaryObject(params);
            if (digestMessageInvalidList.size() != 0){
                response.setStatus(StatusCode.KO)
                .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO)
                .setDetailMessages(digestMessageInvalidList);
            }
        } catch (ProcessingException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_CONFORMITY_KO);
        }

        LOGGER.info("CheckConformityActionHandler response: "+ response.getStatus().value());
        return response;
    }  
    
}
