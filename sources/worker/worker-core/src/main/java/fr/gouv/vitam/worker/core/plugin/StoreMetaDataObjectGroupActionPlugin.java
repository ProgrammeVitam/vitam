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
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;

import java.io.File;
import java.io.InputStream;

/**
 * Stores MetaData object group plugin.
 */
public class StoreMetaDataObjectGroupActionPlugin extends StoreMetadataObjectActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StoreMetaDataObjectGroupActionPlugin.class);

    private static final String JSON = ".json";
    private static final String OG_METADATA_STORAGE = "OG_METADATA_STORAGE";

    private boolean asyncIO = false;

    private final MetaDataClientFactory metaDataClientFactory;

    public StoreMetaDataObjectGroupActionPlugin() {
        this(MetaDataClientFactory.getInstance(), StorageClientFactory.getInstance());
    }


    public StoreMetaDataObjectGroupActionPlugin(MetaDataClientFactory metaDataClientFactory,
        StorageClientFactory storageClientFactory) {
        super(storageClientFactory);
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO actionDefinition)
        throws ProcessingException {

        final ItemStatus itemStatus = new ItemStatus(OG_METADATA_STORAGE);
        final String guid = StringUtils.substringBeforeLast(params.getObjectName(), ".");
        try {
            // create metadata-lfc file in workspace
            saveDocumentWithLfcInStorage(guid, actionDefinition, params.getContainerName(), itemStatus);
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

    /**
     * saveDocumentWithLfcInStorage
     *
     * @param guid
     * @param itemStatus
     * @throws VitamException
     */
    public void saveDocumentWithLfcInStorage(String guid, HandlerIO handlerIO, String containerName,
        ItemStatus itemStatus)
        throws VitamException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookClient = handlerIO.getLifecyclesClient()) {

            //// get metadata
            JsonNode got = selectMetadataDocumentRawById(guid, DataCategory.OBJECTGROUP, metaDataClient);
            MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(got);

            //// get lfc
            JsonNode lfc = getRawLogbookLifeCycleById(guid, DataCategory.OBJECTGROUP, logbookClient);

            //// create file for storage (in workspace or temp or memory)
            JsonNode docWithLfc = MetadataStorageHelper.getGotWithLFC(got, lfc);
            // transfer json to workspace
            final String fileName = guid + JSON;

            try {
                InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
                handlerIO
                    .transferInputStreamToWorkspace(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + fileName, is,
                        null, asyncIO);
            } catch (ProcessingException e) {
                LOGGER.error("Could not backup file for " + guid, e);
                throw new WorkspaceClientServerException(e);
            }

            //// call storage (save in offers)
            // object Description
            final ObjectDescription description =
                new ObjectDescription(DataCategory.OBJECTGROUP, containerName,
                    fileName, IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + fileName);
            // store metadata object from workspace
            storeObject(description, itemStatus);

        } catch (MetaDataExecutionException | InvalidParseOperationException | MetaDataClientServerException e) {
            LOGGER.error(e);
            throw e;
        }
    }

}
