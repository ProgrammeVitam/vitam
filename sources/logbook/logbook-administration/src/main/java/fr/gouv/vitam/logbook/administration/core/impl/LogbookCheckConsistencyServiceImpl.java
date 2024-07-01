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
package fr.gouv.vitam.logbook.administration.core.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.coherence.EventModel;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckError;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckEvent;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookEventName;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookEventType;
import fr.gouv.vitam.logbook.common.model.coherence.OutcomeStatus;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import org.bson.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Logbook consistency check service.<br>
 */
public class LogbookCheckConsistencyServiceImpl implements LogbookCheckConsistencyService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyServiceImpl.class);

    private static final String LFC_EVENT_TYPE_PREFIX = "LFC.";
    private static final String STEP_STARTED_SUFFIX = ".STARTED";

    /**
     * SAVED_LOGBOOK_MSG
     */
    private final String SAVED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG =
        "The saved logbook event %s value %s, is not present in the workflow";
    private final String SAVED_LOGBOOK_EVENTS_EMPTY_MSG = "The logbook operation's event list is empty";

    /**
     * EXPECTED_LOGBOOK_MSG
     */
    private final String EXPECTED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG =
        "The logbook event %s, must be present in the workflow";
    private final String EXPECTED_LOGBOOK_EVENTS_EMPTY_MSG = "The logbook operation's event list must not be empty";

    /**
     * Error/Exceptions messages.
     */
    private static final String CHECK_LOGBOOK_MONDATORY_PARAMETERS_MSG =
        "the tenant parameter is mondatory to the logbook check consistency.";

    /**
     * VitamRepository provider.
     */
    private VitamRepositoryProvider vitamRepository;

    /**
     * Logbook's properties check service.
     */
    private LogbookDetailsCheckService logbookDetailsCheckService;

    /**
     * List of the logbook check results
     */
    private final Set<LogbookCheckError> logbookKoCheckTotalResults = new HashSet<>();

    /**
     * List of the logbook checked events
     */
    private final Set<LogbookCheckEvent> logbookCheckedEvents = new HashSet<>();

    /**
     * List of workflow event types
     */
    private Map<String, Set<String>> workflowEventTypes;

    /**
     * List of events that are generated in a wf-operation but are not declared in the wf itself
     */
    private List<String> opEventsNotInWf;

    /**
     * List of logbook operation events to skip when check coherence between operation and lifecycles.
     */
    private List<String> opLfcEventsToSkip;

    /**
     * List of logbook operation with lifecycles.
     */
    private List<String> opWithLfc;

    /**
     * LogbookCheckConsistencyService constructor.
     *
     * @param configuration
     * @param vitamRepository
     */
    public LogbookCheckConsistencyServiceImpl(
        LogbookConfiguration configuration,
        VitamRepositoryProvider vitamRepository
    ) {
        this(configuration, vitamRepository, new LogbookDetailsCheckServiceImpl());
    }

    /**
     * LogbookCheckConsistencyService constructor.
     *
     * @param configuration
     * @param vitamRepository
     * @param checkLogbookPropertiesService
     */
    @VisibleForTesting
    public LogbookCheckConsistencyServiceImpl(
        LogbookConfiguration configuration,
        VitamRepositoryProvider vitamRepository,
        LogbookDetailsCheckService checkLogbookPropertiesService
    ) {
        this.vitamRepository = vitamRepository;
        this.logbookDetailsCheckService = checkLogbookPropertiesService;

        opLfcEventsToSkip = Collections.unmodifiableList(configuration.getOpLfcEventsToSkip());
        opWithLfc = Collections.unmodifiableList(configuration.getOpWithLFC());
        opEventsNotInWf = Collections.unmodifiableList(configuration.getOpEventsNotInWf());
    }

    /**
     * Logbook consistency check by tenant.
     *
     * @param tenant
     * @throws LogbookException
     */
    @Override
    public LogbookCheckResult logbookCoherenceCheckByTenant(Integer tenant) throws VitamException {
        ParametersChecker.checkParameter(CHECK_LOGBOOK_MONDATORY_PARAMETERS_MSG, tenant);
        LOGGER.debug(String.format("Logbook coherence check on the %s Vitam tenant", tenant));

        // check and init workflow eventTypes if not already done
        initWorkflowEventTypes();

        // get all documents of the logbook operationss
        MongoCursor<Document> cursor = vitamRepository
            .getVitamMongoRepository(LogbookCollections.OPERATION.getVitamCollection())
            .findDocuments(VitamConfiguration.getBatchSize(), tenant)
            .iterator();

        // converting the results to a list of LogbookOperation.
        Iterable<Document> documentIterable = () -> cursor;
        StreamSupport.stream(documentIterable.spliterator(), false).forEach(operation -> {
            // get operation identifer and eventType
            String operationId = operation.getString(LogbookEventName.ID.getValue());
            String operationEvType = operation.getString(LogbookEventName.EVTYPE.getValue());

            // get allowed event (if null no check is done against workflow)
            Set<String> allowedEvTypes = workflowEventTypes.containsKey(operationEvType)
                ? workflowEventTypes.get(operationEvType)
                : null;

            // check logbook operation root event details.
            EventModel eventModelOp = getEventModel(operation, operationId, null, LogbookEventType.OPERATION);
            logbookKoCheckTotalResults.addAll(logbookDetailsCheckService.checkEvent(eventModelOp));

            // get operation events and Check their coherence.
            Map<String, EventModel> mapOpEvents = new HashMap<>();
            final List<Document> operationEvents = (List<Document>) operation.get(LogbookDocument.EVENTS.toString());
            // Check operation's events
            if (operationEvents != null && !operationEvents.isEmpty()) {
                checkCoherenceEvents(
                    LogbookEventType.OPERATION,
                    operationId,
                    null,
                    operationEvents,
                    allowedEvTypes,
                    mapOpEvents
                );
            } else {
                logbookKoCheckTotalResults.add(
                    new LogbookCheckError(
                        operationId,
                        "",
                        eventModelOp.getEvType(),
                        SAVED_LOGBOOK_EVENTS_EMPTY_MSG,
                        EXPECTED_LOGBOOK_EVENTS_EMPTY_MSG
                    )
                );
            }

            // Logbook lifecycles.
            if (opWithLfc.contains(operationEvType)) {
                Map<String, EventModel> mapLfcEvents = new HashMap<>();
                // FIXME : no need to use client as always in logbook app
                try (final LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient();) {
                    // get the unit's lifeCycles for the current operation.
                    checkUnitLfcByOperation(
                        client,
                        LifeCycleStatusCode.LIFE_CYCLE_COMMITTED,
                        operationId,
                        allowedEvTypes,
                        mapLfcEvents
                    );

                    // tests only when purgeTempLfc is false
                    checkUnitLfcByOperation(
                        client,
                        LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS,
                        operationId,
                        allowedEvTypes,
                        mapLfcEvents
                    );

                    // get the objectGroup's lifeCycles for the current operation.
                    checkObjectGroupLfcByOperation(
                        client,
                        LifeCycleStatusCode.LIFE_CYCLE_COMMITTED,
                        operationId,
                        allowedEvTypes,
                        mapLfcEvents
                    );

                    // tests only when purgeTempLfc is false
                    checkObjectGroupLfcByOperation(
                        client,
                        LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS,
                        operationId,
                        allowedEvTypes,
                        mapLfcEvents
                    );
                }

                // Check coherence between logbook operation and lifecycles.
                mapOpEvents.remove(operationEvType); // skip operation result event (last event)
                mapOpEvents.keySet().removeAll(opLfcEventsToSkip); // clear events that are not concerned
                logbookKoCheckTotalResults.addAll(
                    logbookDetailsCheckService.checkLFCandOperation(mapOpEvents, mapLfcEvents)
                );
            }
        });

        // return the logbook coherence check results
        return new LogbookCheckResult(tenant, logbookCheckedEvents, logbookKoCheckTotalResults);
    }

    /**
     * load workflow and get a list of eventType for Steps and Actions
     */
    private void initWorkflowEventTypes() {
        if (workflowEventTypes == null) {
            workflowEventTypes = new HashMap<>();

            try (
                ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance()
                    .getClient();
            ) {
                RequestResponse<WorkFlow> requestResponse = processingClient.getWorkflowDefinitions();
                if (requestResponse.isOk()) {
                    List<WorkFlow> workflows = ((RequestResponseOK) requestResponse).getResults();
                    workflows.forEach(workflow -> {
                        Set<String> eventTypes = new HashSet<>();
                        eventTypes.add(workflow.getIdentifier());
                        workflow
                            .getSteps()
                            .forEach(step -> {
                                eventTypes.add(step.getStepName());
                                eventTypes.addAll(
                                    step
                                        .getActions()
                                        .stream()
                                        .map(action -> action.getActionDefinition().getActionKey())
                                        .collect(Collectors.toSet())
                                );
                            });

                        workflowEventTypes.put(workflow.getIdentifier(), eventTypes);
                    });
                }
            } catch (VitamClientException e) {
                LOGGER.warn("Unable to load workflows'definitions from ProcessEngineManagement.");
            }
        }
    }

    /**
     * checkUnitLfcByOperation
     *
     * @param client
     * @param cycleStatusCode
     * @param operationId
     * @param allowedEvTypes
     * @param mapLfcEvents
     */
    private void checkUnitLfcByOperation(
        LogbookLifeCyclesClient client,
        LifeCycleStatusCode cycleStatusCode,
        String operationId,
        Set<String> allowedEvTypes,
        Map<String, EventModel> mapLfcEvents
    ) {
        Select select = new Select();
        try (
            CloseableIterator<JsonNode> iterator = client.unitLifeCyclesByOperationIterator(
                operationId,
                cycleStatusCode,
                select.getFinalSelect()
            )
        ) {
            CloseableIteratorUtils.map(iterator, LogbookLifeCycleUnit::new).forEachRemaining(unitLFC -> {
                // check unit lifecycle root event details.
                logbookDetailsCheckService.checkEvent(
                    getEventModel(unitLFC, operationId, unitLFC.getId(), LogbookEventType.UNIT_LFC)
                );
                // Check unitlifecycle's events
                List<Document> unitLFCEvents = (List<Document>) unitLFC.get(LogbookDocument.EVENTS);
                if (unitLFCEvents != null && !unitLFCEvents.isEmpty()) {
                    checkCoherenceEvents(
                        LogbookEventType.UNIT_LFC,
                        operationId,
                        unitLFC.getId(),
                        unitLFCEvents,
                        allowedEvTypes,
                        mapLfcEvents
                    );
                }
            });
        } catch (IllegalStateException | LogbookClientException e) {
            LOGGER.error("ERROR: Exception has been thrown when loading unit's lifeCycles : ", e);
        }
    }

    /**
     * checkObjectGroupLfcByOperation
     *
     * @param client
     * @param cycleStatusCode
     * @param operationId
     * @param allowedEvTypes
     * @param mapLfcEvents
     */
    private void checkObjectGroupLfcByOperation(
        LogbookLifeCyclesClient client,
        LifeCycleStatusCode cycleStatusCode,
        String operationId,
        Set<String> allowedEvTypes,
        Map<String, EventModel> mapLfcEvents
    ) {
        Select select = new Select();
        try (
            CloseableIterator<JsonNode> iterator = client.objectGroupLifeCyclesByOperationIterator(
                operationId,
                cycleStatusCode,
                select.getFinalSelect()
            )
        ) {
            CloseableIteratorUtils.map(iterator, LogbookLifeCycleObjectGroup::new).forEachRemaining(objectGroupLFC -> {
                // Get all objectGroup LifeCycles's events and Check their coherence.
                logbookDetailsCheckService.checkEvent(
                    getEventModel(objectGroupLFC, operationId, objectGroupLFC.getId(), LogbookEventType.OBJECTGROUP_LFC)
                );
                // Check objectGrouplifecycle's events
                List<Document> objectGroupLFCEvents = (List<Document>) objectGroupLFC.get(LogbookDocument.EVENTS);
                if (objectGroupLFCEvents != null && !objectGroupLFCEvents.isEmpty()) {
                    checkCoherenceEvents(
                        LogbookEventType.OBJECTGROUP_LFC,
                        operationId,
                        objectGroupLFC.getId(),
                        objectGroupLFCEvents,
                        allowedEvTypes,
                        mapLfcEvents
                    );
                }
            });
        } catch (IllegalStateException | LogbookClientException e) {
            LOGGER.error("ERROR: Exception has been thrown when loading objectGroup's lifeCycles : ", e);
        }
    }

    /**
     * check events coherence.
     *
     * @param logbookEventType
     * @param operationId
     * @param lfcId
     * @param events
     * @param allowedEvTypes
     * @param mapLogbookEvents
     * @return
     */
    private void checkCoherenceEvents(
        LogbookEventType logbookEventType,
        String operationId,
        String lfcId,
        List<Document> events,
        Set<String> allowedEvTypes,
        Map<String, EventModel> mapLogbookEvents
    ) {
        EventModel lastEvent = new EventModel();
        EventModel eventModel;
        boolean doBreak = false;
        for (Document event : events) {
            // consider only the events corresponding on the current operation.
            if (event.getString(LogbookEventName.EVID_PROC.getValue()).equals(operationId)) {
                doBreak = true;
            } else if (doBreak) {
                // only stop if flag doBreak is true, this is useful for events appended to LFC (Unit and Got)
                break;
            }

            // construct eventModel with level/evType
            eventModel = getEventModel(event, operationId, lfcId, null);
            updateEventLogbookType(
                eventModel,
                logbookEventType,
                event.getString(LogbookEventName.EVPARENT_ID.getValue()),
                lastEvent
            );

            // save last event for coherence check.
            lastEvent = eventModel;

            // populate checked event
            logbookCheckedEvents.add(
                new LogbookCheckEvent(eventModel.getEvType(), eventModel.getOutcome(), eventModel.getOutDetail())
            );

            // check eventType against workflow for STEP and ACTION
            if (
                (LogbookEventType.STEP.equals(eventModel.getLogbookEventType()) ||
                    LogbookEventType.ACTION.equals(eventModel.getLogbookEventType())) &&
                allowedEvTypes != null &&
                !allowedEvTypes.contains(eventModel.getEvType()) &&
                !eventModel.getEvType().endsWith(STEP_STARTED_SUFFIX) &&
                !opEventsNotInWf.contains(eventModel.getEvType())
            ) {
                logbookKoCheckTotalResults.add(
                    new LogbookCheckError(
                        eventModel.getOperationId(),
                        eventModel.getLfcId(),
                        eventModel.getEvType(),
                        String.format(
                            SAVED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG,
                            LogbookEventName.EVTYPE.getValue(),
                            eventModel.getEvType()
                        ),
                        String.format(
                            EXPECTED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG,
                            LogbookEventName.EVTYPE.getValue(),
                            eventModel.getEvType()
                        )
                    )
                );
            }

            // Check all events : evType, outcome and outcomeDetails
            logbookKoCheckTotalResults.addAll(logbookDetailsCheckService.checkEvent(eventModel));

            // construct maps for logbook operation and the lifecycles to check coherence between each other.
            if (
                !(LogbookEventType.STEP.equals(eventModel.getLogbookEventType()) ||
                    eventModel.getEvType().endsWith(STEP_STARTED_SUFFIX))
            ) {
                if (!mapLogbookEvents.containsKey(eventModel.getEvType())) {
                    mapLogbookEvents.put(eventModel.getEvType(), eventModel);
                } else {
                    // aggregation of logbook events
                    EventModel value = mapLogbookEvents.get(eventModel.getEvType());
                    String eventStatus = eventModel.getOutcome();
                    if (
                        Stream.of(OutcomeStatus.values()).anyMatch(x -> x.name().equals(eventStatus)) &&
                        (OutcomeStatus.valueOf(eventStatus).getWeight() >
                            OutcomeStatus.valueOf(value.getOutcome()).getWeight())
                    ) {
                        mapLogbookEvents.replace(eventModel.getEvType(), eventModel);
                    }
                }
            }
        }
    }

    /**
     * get eventModel from the given document.
     *
     * @param document
     * @param operationId
     * @param lfcId
     * @param logbookEventType
     * @return
     */
    private EventModel getEventModel(
        Document document,
        String operationId,
        String lfcId,
        LogbookEventType logbookEventType
    ) {
        EventModel eventModel = new EventModel();

        // extract event detail
        eventModel.setEvId(document.getString(LogbookEventName.EVID.getValue()));
        eventModel.setEvParentId(document.getString(LogbookEventName.EVPARENT_ID.getValue()));
        eventModel.setOutcome(document.getString(LogbookEventName.OUTCOME.getValue()));
        eventModel.setEvType(clearLfcPrefix(document.getString(LogbookEventName.EVTYPE.getValue())));
        eventModel.setOutDetail(clearLfcPrefix(document.getString(LogbookEventName.OUTCOMEDETAILS.getValue())));

        // set extra detail
        eventModel.setLogbookEventType(logbookEventType);
        eventModel.setOperationId(operationId);
        eventModel.setLfcId(lfcId);

        return eventModel;
    }

    /**
     * clear logbook lifecycles "LFC." property.
     *
     * @param property
     * @return
     */
    private String clearLfcPrefix(String property) {
        if (property.startsWith(LFC_EVENT_TYPE_PREFIX)) {
            return property.replace(LFC_EVENT_TYPE_PREFIX, "");
        }

        return property;
    }

    /**
     * addLogbookEventType for the given event.
     *
     * @param evModel
     * @param logbookEventType
     * @param evParentId
     * @param lastEvent
     */
    private void updateEventLogbookType(
        EventModel evModel,
        LogbookEventType logbookEventType,
        String evParentId,
        EventModel lastEvent
    ) {
        switch (logbookEventType) {
            // case of logbook operation.
            case OPERATION:
                if (evParentId == null) { // event of type "STEP"
                    evModel.setLogbookEventType(LogbookEventType.STEP);
                } else if (lastEvent != null) {
                    // same level, event of type same as the last one
                    if (lastEvent.getEvParentId() != null && lastEvent.getEvParentId().equals(evParentId)) {
                        evModel.setLogbookEventType(lastEvent.getLogbookEventType());
                        evModel.setEvTypeParent(lastEvent.getEvTypeParent());
                    } else {
                        // change level, change type
                        switch (lastEvent.getLogbookEventType()) {
                            case STEP: // down level
                                evModel.setLogbookEventType(LogbookEventType.ACTION);
                                break;
                            case ACTION: // event of type "TASK" (TREATMENT)
                                evModel.setLogbookEventType(LogbookEventType.TASK);
                                evModel.setEvTypeParent(lastEvent.getEvType());
                                break;
                            case TASK:
                                evModel.setLogbookEventType(LogbookEventType.ACTION);
                                break;
                        }
                    }
                }
                break;
            // case of logbook lifecycles.
            case OBJECTGROUP_LFC:
            case UNIT_LFC:
                if (evParentId == null) {
                    // event of type "ACTION"
                    evModel.setLogbookEventType(LogbookEventType.ACTION);
                } else {
                    // event of type "TASK" (TREATMENT)
                    evModel.setLogbookEventType(LogbookEventType.TASK);
                    if (lastEvent != null) {
                        evModel.setEvTypeParent(lastEvent.getEvType());
                    }
                }
                break;
            default:
                evModel.setLogbookEventType(LogbookEventType.DEFAULT);
        }
    }
}
