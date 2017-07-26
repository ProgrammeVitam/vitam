/**
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
 */
package fr.gouv.vitam.worker.core.plugin;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

/**
 * Stores MetaData Unit plugin.
 */
public class StoreMetaDataUnitActionPlugin extends StoreObjectActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreMetaDataUnitActionPlugin.class);

    private static final String JSON = ".json";
    private static final String $RESULTS = "$results";
    private static final String ARCHIVE_UNIT_NOT_FOUND = "Archive unit not found";
    private static final String UNIT_METADATA_STORAGE = "UNIT_METADATA_STORAGE";
    private HandlerIO handlerIO;
    private boolean asyncIO = false;

    /**
     * Empty parameter Constructor
     */
    public StoreMetaDataUnitActionPlugin() {

    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO actionDefinition)
        throws ProcessingException, ContentAddressableStorageServerException {
        checkMandatoryParameters(params);
        handlerIO = actionDefinition;
        final ItemStatus itemStatus = new ItemStatus(UNIT_METADATA_STORAGE);
        final String guid = StringUtils.substringBeforeLast(params.getObjectName(), ".");
        final String fileName = guid + JSON;

        checkMandatoryIOParameter(actionDefinition);
        try {
            // create metadata file in workspace
            createMetadataFileInWorkspace(params, guid, fileName, itemStatus);
            // TODO update indexed MetaData

            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (VitamException e) {
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(UNIT_METADATA_STORAGE).setItemsStatus(UNIT_METADATA_STORAGE, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub

    }

    private void createMetadataFileInWorkspace(WorkerParameters params, String guid, String fileName,
        ItemStatus itemStatus) throws VitamException {
        // select ArchiveUnit
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode jsonResponse = null;
            try {
                SelectMultiQuery query = new SelectMultiQuery();
                ObjectNode constructQuery = query.getFinalSelect();
                jsonResponse = metaDataClient.selectUnitbyId(constructQuery, guid);
                if (jsonResponse == null) {
                    LOGGER.error(ARCHIVE_UNIT_NOT_FOUND);
                    itemStatus.increment(StatusCode.KO);
                    throw new ProcessingException(ARCHIVE_UNIT_NOT_FOUND);
                }
                JsonNode unit = jsonResponse.get($RESULTS);
                // if result = 0 then throw Exception
                if (unit.size() == 0) {
                    throw new ProcessingException(ARCHIVE_UNIT_NOT_FOUND);
                }
                unit = unit.get(0);
                // transfer json to workspace
                try {
                    handlerIO.transferJsonToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER, fileName,
                        unit, true, asyncIO);
                } catch (ProcessingException e) {
                    LOGGER.error(params.getObjectName(), e);
                    throw new WorkspaceClientServerException(e);
                }
                // object Description
                final ObjectDescription description =
                    new ObjectDescription(StorageCollectionType.UNITS, params.getContainerName(), fileName,
                        IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName);
                // store metadata object from workspace
                StoredInfoResult result = storeObject(description, itemStatus);
                // Update unit with store information
                if (result != null) {
                    try {
                        UpdateMultiQuery queryUpdate = storeStorageInfo((ObjectNode) unit, result, true);
                        query.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
                        metaDataClient.updateUnitbyId(queryUpdate.getFinalUpdate(), guid);
                        try {
                            handlerIO.transferJsonToWorkspace(StorageCollectionType.UNITS.getCollectionName(), fileName,
                                unit, true, asyncIO);
                        } catch (ProcessingException e) {
                            LOGGER.error(params.getObjectName(), e);
                            throw new WorkspaceClientServerException(e);
                        }
                    } catch (InvalidCreateOperationException e) {
                        LOGGER.error(e);
                    }
                }
                
            } catch (MetaDataExecutionException | MetaDataDocumentSizeException |
                InvalidParseOperationException | MetaDataClientServerException e) {
                LOGGER.error(e);
                throw e;
            }
        }
    }
}
