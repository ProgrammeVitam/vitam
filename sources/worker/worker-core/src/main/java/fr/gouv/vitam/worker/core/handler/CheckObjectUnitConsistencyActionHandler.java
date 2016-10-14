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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.core.api.HandlerIO;

/**
 * Check SIP - Object and Archiveunit Consistency handler
 */
public class CheckObjectUnitConsistencyActionHandler extends ActionHandler {

    private static final String ERROR_MESSAGE =
        "Ce Groupe d'objet ou un de ses Objets n'est référencé par aucunes Unités Archivistiques : ";
    private static final String EVENT_TYPE =
        "Contrôle de cohérence entre entre Objets, Groupes d'Objets et Unités Archivistiques";
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(CheckObjectUnitConsistencyActionHandler.class);
    private static final LogbookLifeCyclesClient LOGBOOK_LIFECYCLE_CLIENT = LogbookLifeCyclesClientFactory.getInstance()
        .getLogbookLifeCyclesClient();
    private static final String HANDLER_ID = "CheckObjectUnitConsistency";
    private HandlerIO handlerIO;
    private final HandlerIO handlerInitialIOList;

    /**
     * Empty constructor
     */
    public CheckObjectUnitConsistencyActionHandler() {
        handlerInitialIOList = new HandlerIO("");
        handlerInitialIOList.addInput(File.class);
        handlerInitialIOList.addInput(File.class);
    }
    
    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }



    @Override
    public EngineResponse execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(params);
        checkMandatoryIOParameter(handler);
        handlerIO = handler;
        final EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID,
            OutcomeMessage.CHECK_CONFORMITY_OK);

        try {
            final List<String> notConformOGs = findObjectGroupsNonReferencedByArchiveUnit(params);
            if (notConformOGs.isEmpty()) {
                response.setStatus(StatusCode.OK);
            } else {
                response.setStatus(StatusCode.KO);
            }
        } catch (InvalidParseOperationException | InvalidGuidOperationException | IOException e) {
            LOGGER.error(e);
            response.setStatus(StatusCode.KO);
        }

        return response;
    }

    /**
     * find the object groups non referenced by at least one archive unit
     *
     * @param params worker parameter
     * @return list of non conform OG
     * @throws IOException if can not read file
     * @throws InvalidParseOperationException when maps loaded is not conform
     * @throws InvalidGuidOperationException when og guid is not correct
     */
    private List<String> findObjectGroupsNonReferencedByArchiveUnit(WorkerParameters params)
        throws IOException, InvalidParseOperationException, InvalidGuidOperationException {
        final List<String> ogList = new ArrayList<>();

        //TODO: Use MEMORY to stock this map after extract seda
        final InputStream objectGroupToUnitMapFile = new FileInputStream((File) handlerIO.getInput().get(0));        
        final Map<String, Object> objectGroupToUnitStoredMap = JsonHandler.getMapFromInputStream(objectGroupToUnitMapFile);

        //TODO: Use MEMORY to stock this map after extract seda
        final InputStream objectGroupToGuidMapFile = new FileInputStream((File) handlerIO.getInput().get(1));               
        final Map<String, Object> objectGroupToGuidStoredMap = JsonHandler.getMapFromInputStream(objectGroupToGuidMapFile);      

        final Iterator<Entry<String, Object>> it = objectGroupToGuidStoredMap.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry<String, Object> objectGroup = it.next();
            if (!objectGroupToUnitStoredMap.containsKey(objectGroup.getKey())) {
                // Update logbook OG lifecycle
                final LogbookLifeCycleObjectGroupParameters logbookOGParameter =
                    LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
                        GUIDReader.getGUID(params.getContainerName()),
                        EVENT_TYPE,
                        GUIDFactory.newGUID(),
                        LogbookTypeProcess.CHECK,
                        StatusCode.WARNING,
                        StatusCode.WARNING.toString(),
                        ERROR_MESSAGE + objectGroup.getKey(),
                        GUIDReader.getGUID(objectGroup.getValue().toString()));
                try {
                    LOGBOOK_LIFECYCLE_CLIENT.update(logbookOGParameter);
                } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                    LogbookClientServerException e) {
                    LOGGER.error("Can not update logbook lifcycle", e);
                }
                ogList.add(objectGroup.getKey());
            }

        }
        return ogList;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (handler.getInput().size() != handlerInitialIOList.getInput().size()) {
            throw new ProcessingException(HandlerIO.NOT_ENOUGH_PARAM);
        } else if (!HandlerIO.checkHandlerIO(handler, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIO.NOT_CONFORM_PARAM);
        }
    }

}
