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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckAttachementActionHandler extends ActionHandler {

    public static final String MAPS_EXISITING_GOT_TO_NEW_GOT_FOR_ATTACHMENT_FILE = "Maps/EXISTING_GOT_TO_NEW_GOT_GUID_FOR_ATTACHMENT_MAP.json";
    public static final String MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE = "Maps/EXISTING_UNITS_GUID_FOR_ATTACHMENT_MAP.json";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckAttachementActionHandler.class);
    private static final String HANDLER_ID = "CHECK_ATTACHEMENT";

    private final MetaDataClientFactory metaDataClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;

    @SuppressWarnings("unused")
    CheckAttachementActionHandler() {
        this(MetaDataClientFactory.getInstance(), ProcessingManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    CheckAttachementActionHandler(MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
    }



    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID).increment(StatusCode.OK);

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient()) {
            // Check existing Gots
            JsonNode existingGotsJsonNode =
                handlerIO.getJsonFromWorkspace(MAPS_EXISITING_GOT_TO_NEW_GOT_FOR_ATTACHMENT_FILE);
            Map<String, String> existingGotsMap = JsonHandler.getFromJsonNode(existingGotsJsonNode, Map.class);
            if (!existingGotsMap.isEmpty()) {
                RequestResponse<JsonNode> gotsResponse =
                    metaDataClient.getObjectGroupsByIdsRaw(existingGotsMap.keySet());

                if (!gotsResponse.isOk()) {
                    throw new ProcessingException("Could not retrieve raw metadata "
                        + gotsResponse.getStatus() + " " + ((VitamError) gotsResponse).getDescription());
                }

                if (isFailedOperation(processingManagementClient, (RequestResponseOK<JsonNode>) gotsResponse))
                    return new ItemStatus(HANDLER_ID)
                        .setItemsStatus(HANDLER_ID, new ItemStatus(HANDLER_ID).increment(StatusCode.KO));

            }

            // Check existing Units
            JsonNode existingUnitsJsonNode =
                handlerIO.getJsonFromWorkspace(MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE);
            Map<String, List<String>> unitsIds = JsonHandler.getFromJsonNode(existingUnitsJsonNode, Map.class);
            if (!unitsIds.isEmpty()) {
                RequestResponse<JsonNode> unitsResponse = metaDataClient.getUnitsByIdsRaw(new HashSet<>(unitsIds.get(
                    IngestWorkflowConstants.EXISTING_UNITS)));

                if (!unitsResponse.isOk()) {
                    throw new ProcessingException("Could not retrieve raw metadata "
                        + unitsResponse.getStatus() + " " + ((VitamError) unitsResponse).getDescription());
                }

                if (isFailedOperation(processingManagementClient, (RequestResponseOK<JsonNode>) unitsResponse))
                    return new ItemStatus(HANDLER_ID)
                        .setItemsStatus(HANDLER_ID, new ItemStatus(HANDLER_ID).increment(StatusCode.KO));

            }

        } catch (InvalidParseOperationException e) {
            LOGGER.error("Could not convert json to Object", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (VitamClientException e) {
            throw new ProcessingException("Could not retrive operation Id", e);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus).increment(StatusCode.OK);
    }

    private boolean isFailedOperation(ProcessingManagementClient processingManagementClient,
        RequestResponseOK<JsonNode> metadataResponse) throws ProcessingException {
        Set<String> operationsId =
            metadataResponse.getResults().stream().map(t -> t.get(MetadataDocument.OPI)).map(
                JsonNode::asText)
                .collect(Collectors.toSet());
        try {
            for (String opId : operationsId) {
                ItemStatus operationProcessStatus = processingManagementClient.getOperationProcessStatus(opId);
                if (operationProcessStatus.getGlobalStatus().isGreaterOrEqualToKo())
                    return true;
            }
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            throw new ProcessingException("Could not retrieve processStatus");
        }
        return false;
    }
}
