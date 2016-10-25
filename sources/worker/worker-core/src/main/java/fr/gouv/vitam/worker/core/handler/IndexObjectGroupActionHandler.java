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

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.multiple.Insert;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingInternalServerException;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * IndexObjectGroup Handler
 */
public class IndexObjectGroupActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexObjectGroupActionHandler.class);
    private static final String HANDLER_ID = "OG_METADATA_INDEXATION";

    public static final String JSON_EXTENSION = ".json";
    private static final String OBJECT_GROUP = "ObjectGroup";
    public static final String LIFE_CYCLE_EVENT_TYPE_PROCESS = "INGEST";
    public static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    public static final String TXT_EXTENSION = ".txt";
    private final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters = LogbookParametersFactory
        .newLogbookLifeCycleObjectGroupParameters();

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
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO actionDefinition) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            checkMandatoryIOParameter(actionDefinition);
            SedaUtils.updateLifeCycleByStep(logbookLifecycleObjectGroupParameters, params);
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
            if (StatusCode.FATAL.equals(itemStatus.getGlobalStatus()) ||
                StatusCode.WARNING.equals(itemStatus.getGlobalStatus())) {
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.INDEX_OBJECT_GROUP_KO.value());
            } else {
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    OutcomeMessage.INDEX_OBJECT_GROUP_OK.value());
            }
            SedaUtils.setLifeCycleFinalEventStatusByStep(logbookLifecycleObjectGroupParameters,
                itemStatus.getGlobalStatus());
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        if (StatusCode.UNKNOWN.equals(itemStatus.getGlobalStatus())) {
            itemStatus.increment(StatusCode.WARNING);
        }

        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
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

        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        // TODO once implementing autocloseable should be in the try (resource) too
        final MetaDataClient metadataClient = MetaDataClientFactory
            .create(params.getUrlMetadata());
        try (// TODO : whould use worker configuration instead of the processing configuration
            final WorkspaceClient workspaceClient = WorkspaceClientFactory
                .create(params.getUrlWorkspace());
            final InputStream input = workspaceClient.getObject(containerId, OBJECT_GROUP + "/" + objectName)) {

            if (input != null) {
                final JsonNode json = JsonHandler.getFromInputStream(input);
                final Insert insertRequest = new Insert().addData((ObjectNode) json);
                metadataClient.insertObjectGroup(insertRequest.getFinalInsert().toString());
                itemStatus.increment(StatusCode.OK);
            } else {
                LOGGER.error("Object group not found");
                throw new ProcessingException("Object group not found");
            }

        } catch (final MetaDataException e) {
            throw new ProcessingInternalServerException("Metadata Server Error", e);
        } catch (InvalidParseOperationException | IOException e) {
            throw new ProcessingException("Json wrong format", e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            throw new ProcessingException("Workspace Server Error", e);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Add objectGroup.json add input and check it

    }

}
