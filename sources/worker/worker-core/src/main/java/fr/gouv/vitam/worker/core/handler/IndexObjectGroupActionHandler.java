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

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;

/**
 * IndexObjectGroup Handler
 */
public class IndexObjectGroupActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionHandler.class);
    private static final String HANDLER_ID = "OG_METADATA_INDEXATION";

    private static final String OBJECT_GROUP = "ObjectGroup";
    public static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";

    private HandlerIO handlerIO;
    
    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public IndexObjectGroupActionHandler() {
        // empty constructor
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO actionDefinition) {
        checkMandatoryParameters(params);
        handlerIO = actionDefinition;
        LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try (LogbookLifeCyclesClient logbookClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try {
                checkMandatoryIOParameter(actionDefinition);

                LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(logbookClient,
                    logbookLifecycleObjectGroupParameters,
                    params, HANDLER_ID, LogbookTypeProcess.INGEST);

                indexObjectGroup(params, itemStatus);

            } catch (final ProcessingInternalServerException exc) {
                LOGGER.error(exc);
                itemStatus.increment(StatusCode.FATAL);
            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.WARNING);
            }

            // Update lifeCycle
            try {
                logbookLifecycleObjectGroupParameters.setFinalStatus(HANDLER_ID, null, itemStatus.getGlobalStatus(),
                    null);
                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(logbookClient,
                    logbookLifecycleObjectGroupParameters,
                    itemStatus);

            } catch (final ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }
        if (StatusCode.UNKNOWN.equals(itemStatus.getGlobalStatus())) {
            itemStatus.increment(StatusCode.WARNING);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }


    /**
     * The function is used for retrieving ObjectGroup in workspace and use metadata client to index ObjectGroup
     *
     * @param params work parameters
     * @param itemStatus item status
     * @throws ProcessingException when error in execution
     */
    private void indexObjectGroup(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String objectName = params.getObjectName();

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
                final ObjectNode json = (ObjectNode) handlerIO.getJsonFromWorkspace(OBJECT_GROUP + "/" + objectName);
                json.remove(SedaConstants.PREFIX_WORK);
                final Insert insertRequest = new Insert().addData(json);
                metadataClient.insertObjectGroup(insertRequest.getFinalInsert());
                itemStatus.increment(StatusCode.OK);
        } catch (final MetaDataException e) {
            throw new ProcessingInternalServerException("Metadata Server Error", e);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException("Json wrong format", e);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add objectGroup.json add input and check it

    }

}
