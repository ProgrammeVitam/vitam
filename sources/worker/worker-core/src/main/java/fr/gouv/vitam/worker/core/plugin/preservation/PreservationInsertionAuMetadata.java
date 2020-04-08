/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.model.UpdateUnitKey;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.operations;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.DIFF;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.KEY;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.MESSAGE;
import static fr.gouv.vitam.metadata.core.model.UpdateUnit.STATUS;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class PreservationInsertionAuMetadata extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationInsertionAuMetadata.class);

    private static final String ITEM_ID = "PRESERVATION_INSERTION_AU_METADATA";
    private static final TypeReference<RequestResponseOK<JsonNode>> REQUEST_RESPONSE_TYPE_REFERENCE = new TypeReference<>() {};

    private final MetaDataClientFactory metaDataClientFactory;

    public PreservationInsertionAuMetadata() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    public PreservationInsertionAuMetadata(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) throws ProcessingException {
        LOGGER.debug("Starting {}.'", ITEM_ID);

        List<String> units = workerParameters.getObjectNameList();
        List<JsonNode> extractedMetadatasToUpdate = workerParameters.getObjectMetadataList();

        List<ItemStatus> itemStatuses = new ArrayList<>();
        try (MetaDataClient mdClient = metaDataClientFactory.getClient()) {
            for (int i = 0; i < units.size(); i++) {
                UpdateMultiQuery update = new UpdateMultiQuery();
                update.addActions(
                    set((ObjectNode) extractedMetadatasToUpdate.get(i)),
                    push(operations(), workerParameters.getRequestId())
                );

                String unitId = units.get(i);
                RequestResponseOK<JsonNode> requestResponse = JsonHandler.getFromJsonNode(mdClient.updateUnitById(update.getFinalUpdate(), unitId), REQUEST_RESPONSE_TYPE_REFERENCE);
                JsonNode unitAsNode = requestResponse.getFirstResult();

                UpdateUnitKey key = UpdateUnitKey.valueOf(unitAsNode.get(KEY).asText());
                StatusCode status = StatusCode.valueOf(unitAsNode.get(STATUS).asText());
                String message = unitAsNode.get(MESSAGE).asText();
                String diff = unitAsNode.get(DIFF).asText();

                if (!KO.equals(status) && !FATAL.equals(status) && !OK.equals(status)) {
                    throw new VitamRuntimeException(String.format("Status must be of type KO, FATAL or OK here '%s'.", status));
                }

                if (KO.equals(status) || FATAL.equals(status)) {
                    itemStatuses.add(buildItemStatusWithMessage(ITEM_ID, status, String.format("Failed to add extracted metadata for unit: '%s', in database with message '%s' and key '%s'.", unitId, message, key)));
                } else {
                    itemStatuses.add(buildItemStatusWithMessage(ITEM_ID, OK, diff));
                }
            }
            return itemStatuses;
        } catch (InvalidCreateOperationException | InvalidParseOperationException | MetaDataException e) {
            throw new ProcessingException(e);
        } finally {
            LOGGER.debug("Ending {}.'", ITEM_ID);
        }
    }
}
