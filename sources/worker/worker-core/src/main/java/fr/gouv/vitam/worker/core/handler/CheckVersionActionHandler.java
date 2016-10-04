/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.handler;

import java.util.List;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;

/**
 * CheckVersionActionHandler handler class used to check the versions of BinaryDataObject in manifest
 */
public class CheckVersionActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckVersionActionHandler.class);
    private static final String HANDLER_ID = "CheckVersion";

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     * @param factory SedaUtils factory
     */
    public CheckVersionActionHandler() {
    }
    
    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO actionDefinition){
        checkMandatoryParameters(params);
        LOGGER.debug("CheckVersionActionHandler running ...");
        
        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_VERSION_OK);
        final SedaUtils sedaUtils = SedaUtilsFactory.create();

        try {
            checkMandatoryIOParameter(actionDefinition);
            List<String> versionInvalidList = sedaUtils.checkSupportedBinaryObjectVersion(params);
            if (versionInvalidList.size() != 0){
                response.setErrorNumber(versionInvalidList.size());
                response.setStatus(StatusCode.KO).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_VERSION_KO);
            }
        } catch (ProcessingException e) {
            LOGGER.error(e.getMessage());
            response.setStatus(StatusCode.FATAL).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_VERSION_KO);
        }

        LOGGER.debug("CheckVersionActionHandler response: " + response.getStatus().name());
        return response;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
      //TODO Add Workspace:SIP/manifest.xml and check it         
    }

}
