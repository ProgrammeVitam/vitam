/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.EV_ID;

public class CheckAttachementActionHandler extends ActionHandler {

    public static final String MAPS_EXISTING_GOTS_GUID_FOR_ATTACHMENT_FILE =
        "Maps/EXISTING_GOTS_GUID_FOR_ATTACHMENT_MAP.json";
    public static final String MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE =
        "Maps/EXISTING_UNITS_GUID_FOR_ATTACHMENT_MAP.json";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckAttachementActionHandler.class);
    private static final String HANDLER_ID = "CHECK_ATTACHEMENT";
    private static final String OPI = "#opi";
    private static final String ARRAY_PROJECTION_FORMAT = "%s.%s";

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final ProcessingManagementClientFactory processingManagementClientFactory;

    @SuppressWarnings("unused")
    CheckAttachementActionHandler() {
        this(
            MetaDataClientFactory.getInstance(),
            ProcessingManagementClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    CheckAttachementActionHandler(
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID).increment(StatusCode.OK);

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            // Check existing Gots
            JsonNode existingGotsJsonNode = handlerIO.getJsonFromWorkspace(MAPS_EXISTING_GOTS_GUID_FOR_ATTACHMENT_FILE);
            Set<String> existingGotsMap = JsonHandler.getFromJsonNode(existingGotsJsonNode, Set.class);
            if (!existingGotsMap.isEmpty()) {
                Iterator<List<String>> bulksExistingGotsIds = Iterators.partition(
                    existingGotsMap.iterator(),
                    VitamConfiguration.getBatchSize()
                );
                while (bulksExistingGotsIds.hasNext()) {
                    final JsonNode queryDsl = getInitialOperationQuery(bulksExistingGotsIds.next());
                    final JsonNode gotsOpi = metaDataClient.selectObjectGroups(queryDsl);

                    if (isFailedOrIncompletedOperation(gotsOpi)) return new ItemStatus(HANDLER_ID).setItemsStatus(
                        HANDLER_ID,
                        new ItemStatus(HANDLER_ID).increment(StatusCode.KO)
                    );
                }
            }

            // Check existing Units
            JsonNode UnitsOpiJsonNode = handlerIO.getJsonFromWorkspace(MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE);
            Set<String> unitsIds = JsonHandler.getFromJsonNode(UnitsOpiJsonNode, Set.class);
            if (!unitsIds.isEmpty()) {
                Iterator<List<String>> bulksExistingUnitsIds = Iterators.partition(
                    unitsIds.iterator(),
                    VitamConfiguration.getBatchSize()
                );
                while (bulksExistingUnitsIds.hasNext()) {
                    final JsonNode queryDsl = getInitialOperationQuery(bulksExistingUnitsIds.next());
                    final JsonNode unitsOpi = metaDataClient.selectUnits(queryDsl);

                    if (isFailedOrIncompletedOperation(unitsOpi)) return new ItemStatus(HANDLER_ID).setItemsStatus(
                        HANDLER_ID,
                        new ItemStatus(HANDLER_ID).increment(StatusCode.KO)
                    );
                }
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Could not convert json to Object", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new ProcessingException("Could not retrive operation Id", e);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus).increment(StatusCode.OK);
    }

    private JsonNode getInitialOperationQuery(List<String> metadataIds) throws ProcessingException {
        String[] metadataIdsArray = metadataIds.toArray(String[]::new);
        final Select select = new Select();
        try {
            select.setQuery(
                QueryHelper.in(BuilderToken.PROJECTIONARGS.ID.exactToken(), metadataIdsArray).setDepthLimit(0)
            );
            select.setProjection(
                JsonHandler.createObjectNode()
                    .set(
                        BuilderToken.PROJECTION.FIELDS.exactToken(),
                        JsonHandler.createObjectNode()
                            .put(BuilderToken.PROJECTIONARGS.INITIAL_OPERATION.exactToken(), 1)
                    )
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Cannot create opi query", e);
            throw new ProcessingException("Cannot create opi query", e);
        }
        return select.getFinalSelect();
    }

    private JsonNode getLastEventsQuery(Set<String> operationsIds) throws ProcessingException {
        String[] operationsIdsArray = operationsIds.toArray(String[]::new);
        Select select = new Select();
        try {
            select.setQuery(in(EV_ID, operationsIdsArray));
            // FIXME : #9847 Fix logbook projections - Add back projection once projection handling is fixed
            // ObjectNode objectNode = createObjectNode().put(String.format(ARRAY_PROJECTION_FORMAT, EVENTS, OUTCOME), 1)
            //   .put(String.format(ARRAY_PROJECTION_FORMAT, EVENTS, EV_TYPE), 1)
            //   .put(String.format(ARRAY_PROJECTION_FORMAT, EVENTS, EV_ID_PROC), 1);
            // JsonNode projection = createObjectNode().set(BuilderToken.PROJECTION.FIELDS.exactToken(), objectNode);
            // select.setProjection(projection);
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("Cannot create last event query", e);
            throw new ProcessingException("Cannot create last event query", e);
        }
        return select.getFinalSelect();
    }

    private boolean isFailedOrIncompletedOperation(JsonNode metadataJsonNode) throws ProcessingException {
        try {
            Set<Map<String, String>> metadataResponse = JsonHandler.getFromJsonNode(
                metadataJsonNode.get(TAG_RESULTS),
                Set.class,
                Map.class
            );
            Set<String> operationsIds = metadataResponse
                .stream()
                .map(metadata -> metadata.get(OPI))
                .collect(Collectors.toSet());

            Map<String, Boolean> operationsStatus = operationsIds
                .stream()
                .collect(Collectors.toMap(Function.identity(), e -> true));

            Set<String> operationsIdToCheckWithLogbook = new HashSet<>();

            for (String t : operationsStatus.keySet()) {
                Optional<ItemStatus> itemStatus = getProcessStatusIfExists(t);
                if (itemStatus.isEmpty()) {
                    operationsIdToCheckWithLogbook.add(t);
                } else {
                    // FIXME : concurrent update?
                    // FIXME : fail fast?
                    operationsStatus.put(
                        t,
                        itemStatus.get().getGlobalStatus().isGreaterOrEqualToKo() ||
                        !itemStatus.get().getGlobalState().equals(ProcessState.COMPLETED)
                    );
                }
            }

            if (!operationsIdToCheckWithLogbook.isEmpty()) {
                List<LogbookOperation> logbookOperations = checkLogbook(operationsIdToCheckWithLogbook);
                logbookOperations
                    .stream()
                    .map(LogbookOperation::getEvents)
                    .map(Iterables::getLast)
                    .forEach(lastEvent ->
                        operationsStatus.put(
                            lastEvent.getEvIdProc(),
                            // FIXME : fail fast?
                            isWorkflowUncompleted(lastEvent) ||
                            StatusCode.valueOf(lastEvent.getOutcome()).isGreaterOrEqualToKo()
                        ));
            }

            return operationsStatus.values().stream().anyMatch(e -> e.equals(true));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    private boolean isWorkflowUncompleted(LogbookEventOperation lastEvent) {
        return (
            !lastEvent.getEvType().equals(Contexts.DEFAULT_WORKFLOW.getEventType()) &&
            !lastEvent.getEvType().equals(Contexts.FILING_SCHEME.getEventType()) &&
            !lastEvent.getEvType().equals(Contexts.HOLDING_SCHEME.getEventType())
        );
    }

    private List<LogbookOperation> checkLogbook(Set<String> operationsId) throws ProcessingException {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            JsonNode logbookOperationsQuery = getLastEventsQuery(operationsId);
            JsonNode logbookOperationsResult = logbookOperationsClient
                .selectOperation(logbookOperationsQuery)
                .get(TAG_RESULTS);
            if (logbookOperationsResult.size() == operationsId.size()) return JsonHandler.getFromJsonNode(
                logbookOperationsResult,
                List.class,
                LogbookOperation.class
            );
            throw new LogbookNotFoundException("Cannot find logbooks");
        } catch (LogbookClientException | InvalidParseOperationException | LogbookNotFoundException e) {
            throw new ProcessingException("Cannot retrive operation status from logbook", e);
        }
    }

    private Optional<ItemStatus> getProcessStatusIfExists(String operationId) throws ProcessingException {
        try (ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient()) {
            return Optional.of(processingManagementClient.getOperationProcessStatus(operationId));
        } catch (WorkflowNotFoundException e) {
            return Optional.empty();
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            throw new ProcessingException("Cannot retrive operation status from Process Management", e);
        }
    }
}
