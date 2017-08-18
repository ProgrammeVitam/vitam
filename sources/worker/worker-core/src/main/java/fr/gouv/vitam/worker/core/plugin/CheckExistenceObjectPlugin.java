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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * CheckExistenceObject Plugin.<br>
 */

public class CheckExistenceObjectPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckExistenceObjectPlugin.class);

    private static final String CHECK_EXISTENCE_ID = "AUDIT_FILE_EXISTING";
    private static final String DEFAULT_STRATEGY = "default";
    private HandlerIO handlerIO;

    /**
     * Empty constructor UnitsRulesComputePlugin
     */
    public CheckExistenceObjectPlugin() {
        // Empty
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug(CHECK_EXISTENCE_ID + " in execute");
        handlerIO = handler;

        final ItemStatus itemStatus = new ItemStatus(CHECK_EXISTENCE_ID);
        itemStatus.increment(StatusCode.OK);
        try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode searchResult =
                metadataClient.selectObjectGrouptbyId(new SelectMultiQuery().getFinalSelect(), params.getObjectName());
            JsonNode qualifiersList = searchResult.get("$results").get(0).get("#qualifiers");
            for (JsonNode qualifier : qualifiersList) {
                JsonNode versions = qualifier.get("versions");
                for (JsonNode version : versions) {
                    if (!storageClient.exists(DEFAULT_STRATEGY, StorageCollectionType.OBJECTS, version.get("_id").asText())) {
                        itemStatus.increment(StatusCode.KO);
                    }
                }
            }
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage server errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException | MetaDataException e) {
            LOGGER.error("Metadta server errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (NullPointerException e) {
            LOGGER.error("Object does not exist : ", e);
            itemStatus.increment(StatusCode.WARNING);
        }

        return new ItemStatus(CHECK_EXISTENCE_ID).setItemsStatus(CHECK_EXISTENCE_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }


}
