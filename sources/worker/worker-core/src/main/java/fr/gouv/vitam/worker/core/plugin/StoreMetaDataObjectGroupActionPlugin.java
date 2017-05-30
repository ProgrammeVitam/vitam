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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

/**
 * Stores MetaData object group plugin.
 */
public class StoreMetaDataObjectGroupActionPlugin extends StoreObjectActionHandler {


    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StoreMetaDataObjectGroupActionPlugin.class);

    private static final String OG_METADATA_STORAGE = "OG_METADATA_STORAGE";
    private static final String OBJECT_GROUP_NOT_FOUND_EXCEPTION = "objectGroup not found Exception";
    private static final String $RESULTS = "$results";
    private HandlerIO handlerIO;

    /**
     * StoreMetaDataObjectGroupActionPlugin constructor
     */
    public StoreMetaDataObjectGroupActionPlugin() {}

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO actionDefinition)
        throws ProcessingException, ContentAddressableStorageServerException {
        checkMandatoryParameters(params);
        handlerIO = actionDefinition;
        final ItemStatus itemStatus = new ItemStatus(OG_METADATA_STORAGE);
        try {
            checkMandatoryIOParameter(actionDefinition);
            final String objectName = params.getObjectName();
            // create metadata file in workspace
            createMetadataFileInWorkspace(params);

            final ObjectDescription description =
                new ObjectDescription(StorageCollectionType.OBJECTGROUPS, params.getContainerName(), objectName);
            // transfer json to workspace
            storeObject(description, itemStatus);
            // TODO update indexed MetaData

            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (VitamException e) {
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(OG_METADATA_STORAGE).setItemsStatus(OG_METADATA_STORAGE, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handlerIO) throws ProcessingException {
        // TODO Auto-generated method stub

    }

    private void createMetadataFileInWorkspace(WorkerParameters params) throws VitamException {
        JsonNode jsonNode;
        final String objectName = StringUtils.substringBeforeLast(params.getObjectName(),
            ".");
        // select ObjectGroup
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery query = new SelectMultiQuery();
            ObjectNode constructQuery = query.getFinalSelect();
            jsonNode =
                metaDataClient.selectObjectGrouptbyId(constructQuery, objectName);
            if (jsonNode == null) {
                throw new ProcessingException(OBJECT_GROUP_NOT_FOUND_EXCEPTION);
            }
            jsonNode = jsonNode.get($RESULTS);
            // if result = 0 then throw Exception
            if (jsonNode.size() == 0) {
                throw new ProcessingException(OBJECT_GROUP_NOT_FOUND_EXCEPTION);
            }

        } catch (MetadataInvalidSelectException | MetaDataDocumentSizeException | MetaDataExecutionException |
            InvalidParseOperationException |
            MetaDataClientServerException e) {
            LOGGER.error(params.getObjectName(), e);
            throw e;
        }
        // transfer json to workspace
        try {
            handlerIO.transferJsonToWorkspace(StorageCollectionType.OBJECTGROUPS.getCollectionName(),
                params.getObjectName(),
                jsonNode, true);
        } catch (ProcessingException e) {
            LOGGER.error(params.getObjectName(), e);
            throw new WorkspaceClientServerException(e);
        }
    }



}
