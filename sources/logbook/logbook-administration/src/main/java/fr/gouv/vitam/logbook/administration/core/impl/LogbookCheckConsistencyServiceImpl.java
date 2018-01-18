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
package fr.gouv.vitam.logbook.administration.core.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.LifecyclesSpliterator;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.EventModel;
import fr.gouv.vitam.logbook.common.model.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.model.LogbookEventName;
import fr.gouv.vitam.logbook.common.model.LogbookEventType;
import fr.gouv.vitam.logbook.common.model.OutcomeStatus;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

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

    private static final String STRATEGY_ID = "default";
    /**
     * Default offset for LifecycleSpliterator
     */
    private static final int OFFSET = 0;
    /**
     * Default limit for LifecycleSpliterator
     */
    private static final int LIMIT = 1000;


    /**
     * List of logbook operation with lifecycles.
     */
    private static final List<String> OP_WITH_LFC = Arrays.asList("PROCESS_SIP_UNITARY",
        "FILINGSCHEME", "HOLDINGSCHEME", "UPDATE_RULES_ARCHIVE_UNITS", "PROCESS_AUDIT", "STP_UPDATE_UNIT");

    /**
     * SAVED_LOGBOOK_MSG
     */
    private final String SAVED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG =
        "The saved logbook event %s value %s, is not present in the workflow";

    private final String SAVED_LOGBOOK_EVENTS_EMPTY_MSG =
        "The logbook operation's event list is empty";

    /**
     * EXPECTED_LOGBOOK_MSG
     */
    private final String EXPECTED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG =
        "The logbook event %s, must be present in the workflow";

    private final String EXPECTED_LOGBOOK_EVENTS_EMPTY_MSG =
        "The logbook operation's event list must not be empty";

    /**
     * Error/Exceptions messages.
     */
    private static final String CHECK_LOGBOOK_MONDATORY_PARAMETERS_MSG =
        "the tenant parameter is mondatory to the logbook check consistency.";

    private LogbookConfiguration configuration;

    /**
     * VitamRepository provider.
     */
    private VitamRepositoryProvider vitamRepository;

    /**
     * Logbook's properties check service.
     */
    private LogbookDetailsCheckService logbookDetailsCheckService;

    // list of the logbook check results
    List<LogbookCheckResult> logbookCheckTotalResults = new ArrayList<>();

    private Map<String, Set<String>> workflowEventTypes;

    /**
     * LogbookCheckConsistencyService constructor.
     *
     * @param configuration
     * @param vitamRepository
     */
    public LogbookCheckConsistencyServiceImpl(
        LogbookConfiguration configuration,
        VitamRepositoryProvider vitamRepository) {
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
        LogbookDetailsCheckService checkLogbookPropertiesService) {
        this.configuration = configuration;
        this.vitamRepository = vitamRepository;
        this.logbookDetailsCheckService = checkLogbookPropertiesService;
    }

    /**
     * Logbook consistency check by tenant.
     *
     * @param tenant
     * @throws LogbookException
     */
    @Override
    public void logbookCoherenceCheckByTenant(Integer tenant) throws VitamException {
        ParametersChecker.checkParameter(CHECK_LOGBOOK_MONDATORY_PARAMETERS_MSG, tenant);
        LOGGER.debug(String.format("Logbook coherence check on the %s Vitam tenant", tenant));

        // check and init workflow eventTypes if not already done
        initWorkflowEventTypes();

        // get all documents of the logbook operations
        MongoCursor<Document> cursor =
            vitamRepository.getVitamMongoRepository(LogbookCollections.OPERATION)
                .findDocuments(VitamConfiguration.getBatchSize(), tenant).iterator();

        final List<Document> documents = getDocuments(cursor);
        Map<String, EventModel> mapOpEvents;
        Map<String, EventModel> mapLfcEvents;
        Set<String> allowedEvTypes;
        String operationId;

        // Check coherence of the different logbook properties
        for (Document operation : documents) {
            // get operation identifer and eventType
            operationId = operation.get(LogbookEventName.ID.getValue()).toString();
            String operationEvType = operation.getString(LogbookEventName.EVTYPE.getValue());

            // get allowed event (if null no check is done against workflow)
            allowedEvTypes = workflowEventTypes.containsKey(operationEvType) ?
                workflowEventTypes.get(operation.getString(LogbookEventName.EVTYPE.getValue())) : null;

            // check logbook operation root event details.
            EventModel eventModelOp = getEventModel(operation, operationId, null, LogbookEventType.OPERATION);
            logbookCheckTotalResults.addAll(logbookDetailsCheckService
                .checkEvent(eventModelOp));

            // get operation events and Check their coherence.
            mapOpEvents = new HashMap<>();
            final List<Document> operationEvents = (List<Document>) operation.get(LogbookDocument.EVENTS.toString());
            // Check operation's events
            if (operationEvents != null && !operationEvents.isEmpty()) {
                checkCoherenceEvents(LogbookEventType.OPERATION, operationId, null, operationEvents, allowedEvTypes,
                    mapOpEvents);
            } else {
                logbookCheckTotalResults.add(new LogbookCheckResult(operationId,
                    "", eventModelOp.getEvType(),
                    SAVED_LOGBOOK_EVENTS_EMPTY_MSG,
                    EXPECTED_LOGBOOK_EVENTS_EMPTY_MSG));
            }

            // Logbook lifecycles.
            if (OP_WITH_LFC.contains(operationEvType)) {
                mapLfcEvents = new HashMap<>();
                try (final LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient();) {

                    // get the unit's lifeCycles for the current operation.
                    checkUnitLfcByOperation(client, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED, operationId,
                        allowedEvTypes, mapLfcEvents);

                    // tests only when purgeTempLfc is false
                    checkUnitLfcByOperation(client, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS, operationId,
                        allowedEvTypes, mapLfcEvents);

                    // get the objectGroup's lifeCycles for the current operation.
                    checkObjectGroupLfcByOperation(client, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED, operationId,
                        allowedEvTypes, mapLfcEvents);

                    // tests only when purgeTempLfc is false
                    checkObjectGroupLfcByOperation(client, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS, operationId,
                        allowedEvTypes, mapLfcEvents);
                }
                // Check coherence between logbook operation and lifecycles.
                mapOpEvents.remove(operationEvType); // skip operation result event (last event)
                logbookCheckTotalResults
                    .addAll(logbookDetailsCheckService.checkLFCandOperation(mapOpEvents, mapLfcEvents));
            }

        }

        // save the logbook coherence check results
        storeReportsInStorage(logbookCheckTotalResults);
    }

    /**
     * load workflow and get a list of eventType for Steps and Actions
     */
    private void initWorkflowEventTypes() {
        if (workflowEventTypes == null) {
            workflowEventTypes = new HashMap<>();

            try (ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance()
                .getClient()) {
                RequestResponse<WorkFlow> requestResponse = processingClient.getWorkflowDefinitions();
                if (requestResponse.isOk()) {
                    List<WorkFlow> workflows = ((RequestResponseOK) requestResponse).getResults();
                    workflows.forEach(workflow -> {
                        Set<String> eventTypes = new HashSet<>();

                        workflow.getSteps().forEach(step -> {
                            eventTypes.add(step.getStepName());
                            eventTypes.addAll(step.getActions().stream().map(
                                action -> action.getActionDefinition().getActionKey()).collect(Collectors.toSet()));
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
    private void checkUnitLfcByOperation(LogbookLifeCyclesClient client, LifeCycleStatusCode cycleStatusCode,
        String operationId, Set<String> allowedEvTypes, Map<String, EventModel> mapLfcEvents)
        throws LogbookClientException, InvalidParseOperationException {
        try {
            final Select select = new Select();
            LifecyclesSpliterator<JsonNode> scrollSplitator = new LifecyclesSpliterator<>(select,
                query -> {
                    RequestResponse response;
                    try {
                        response = client.unitLifeCyclesByOperationIterator(operationId,
                            cycleStatusCode, select.getFinalSelect());
                    } catch (LogbookClientException | InvalidParseOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    if (response.isOk()) {
                        return (RequestResponseOK) response;
                    } else {
                        throw new IllegalStateException(
                            String.format("Error while loading logbook lifecycle Unit RequestResponse %d",
                                response.getHttpCode()));
                    }
                }, OFFSET, LIMIT);
            StreamSupport.stream(scrollSplitator, false).map(LogbookLifeCycleUnit::new)
                .forEach(unitLFC -> {
                    try {
                        // check unit lifecycle root event details.
                        logbookDetailsCheckService.checkEvent(getEventModel(unitLFC, operationId, unitLFC.getId(),
                            LogbookEventType.UNIT_LFC));
                        // Check unitlifecycle's events
                        List<Document> unitLFCEvents =
                            (List<Document>) unitLFC.get(LogbookDocument.EVENTS.toString());
                        if (unitLFCEvents != null && !unitLFCEvents.isEmpty()) {
                            checkCoherenceEvents(LogbookEventType.UNIT_LFC, operationId, unitLFC.getId(),
                                unitLFCEvents, allowedEvTypes, mapLfcEvents);
                        }
                    } catch (IllegalStateException e) {
                        throw e;
                    }
                });
        } catch (IllegalStateException e) {
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
    private void checkObjectGroupLfcByOperation(LogbookLifeCyclesClient client, LifeCycleStatusCode cycleStatusCode,
        String operationId, Set<String> allowedEvTypes, Map<String, EventModel> mapLfcEvents) {
        try {
            Select select = new Select();
            LifecyclesSpliterator<JsonNode> scrollSplitator = new LifecyclesSpliterator<>(select,
                query -> {
                    RequestResponse response;
                    try {
                        response = client.objectGroupLifeCyclesByOperationIterator(operationId, cycleStatusCode,
                            select.getFinalSelect());
                    } catch (LogbookClientException | InvalidParseOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    if (response.isOk()) {
                        return (RequestResponseOK) response;
                    } else {
                        throw new IllegalStateException(
                            String.format("Error while loading logbook lifecycle Unit RequestResponse %d",
                                response.getHttpCode()));
                    }
                }, OFFSET, LIMIT);

            StreamSupport.stream(scrollSplitator, false).map(LogbookLifeCycleObjectGroup::new)
                .forEach(objectGroupLFC -> {
                    try {
                        // Get all objectGroup LifeCycles's events and Check their coherence.
                        logbookDetailsCheckService
                            .checkEvent(getEventModel(objectGroupLFC, operationId, objectGroupLFC.getId(),
                                LogbookEventType.OBJECTGROUP_LFC));
                        // Check objectGrouplifecycle's events
                        List<Document> objectGroupLFCEvents =
                            (List<Document>) objectGroupLFC.get(LogbookDocument.EVENTS.toString());
                        if (objectGroupLFCEvents != null && !objectGroupLFCEvents.isEmpty()) {
                            checkCoherenceEvents(LogbookEventType.OBJECTGROUP_LFC, operationId, objectGroupLFC.getId(),
                                objectGroupLFCEvents, allowedEvTypes, mapLfcEvents);
                        }
                    } catch (IllegalStateException e) {
                        throw e;
                    }
                });
        } catch (IllegalStateException e) {
            LOGGER.error("ERROR: Exception has been thrown when loading objectGroup's lifeCycles : ", e);
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
    private EventModel getEventModel(Document document, String operationId, String lfcId,
        LogbookEventType logbookEventType) {
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
     * Logbook coherence check on all Vitam tenants.
     *
     * @throws LogbookException
     */
    @Override
    public void logbookCoherenceCheck() throws VitamException {

        // get the list of vitam tenants from the configuration.
        List<Integer> tenants = configuration.getTenants();
        if (null != tenants && !tenants.isEmpty()) {
            LOGGER.debug(String.format("Logbook coherence check on the %s Vitam tenants", tenants.size()));

            // scan && check coherence of the logbook on all the Vitam tenants
            for (Integer tenant : tenants) {
                logbookCoherenceCheckByTenant(tenant);
            }
        } else {
            LOGGER.warn(String.format("Warning : there is no Vitam tenants"));
        }
    }

    /**
     * store check logbook reports in storage (offer).
     *
     * @param logbookCheckResults
     * @throws VitamException
     */
    @Override
    public void storeReportsInStorage(List<LogbookCheckResult> logbookCheckResults)
        throws VitamException {

        if (logbookCheckResults == null || logbookCheckResults.isEmpty()) {
            return;
        }
        // save the logbook coherence check results
        try (InputStream logbookCheckReportFile = new ByteArrayInputStream(logbookCheckResults.toString().getBytes(
            CharsetUtils.UTF_8))) {
            String fileName = GUIDFactory.newGUID().toString();
            // Save data to storage
            try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();) {
                //store in workSpace
                workspaceClient.createContainer(fileName);
                workspaceClient.putObject(fileName, fileName, logbookCheckReportFile);
                try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                    //store in offer
                    final ObjectDescription description = new ObjectDescription();
                    description.setWorkspaceContainerGUID(fileName);
                    description.setWorkspaceObjectURI(fileName);
                    storageClient
                        .storeFileFromWorkspace(STRATEGY_ID, DataCategory.CHECKLOGBOOKREPORTS, fileName,
                            description);
                } finally {
                    // delete the workspace container
                    workspaceClient.deleteContainer(fileName, true);
                }
            } catch (ContentAddressableStorageServerException
                | ContentAddressableStorageAlreadyExistException e) {
                //workspace Error
                throw new VitamException("Unable to store file in workSpace " + fileName, e);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
                //Offer storage Error
                throw new VitamException("Unable to store file from workSpace to storage " + fileName, e);
            } catch (ContentAddressableStorageNotFoundException e) {
                // clean container Error
                throw new VitamException("Unable to delete file", e);
            }

        } catch (Exception e) {
            LOGGER.error("Exception has been thrown when converting on inputStream ", e);
        }

    }

    /**
     * get the document list from the cursor.
     *
     * @param cursor
     * @return
     */
    private static List<Document> getDocuments(MongoCursor<Document> cursor) {
        int cpt = 0;
        List<Document> documents = new ArrayList<>();
        while (cpt < VitamConfiguration.getBatchSize() && cursor.hasNext()) {
            documents.add(cursor.next());
            cpt++;
        }
        return documents;
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
    private void checkCoherenceEvents(LogbookEventType logbookEventType, String operationId, String lfcId,
        List<Document> events, Set<String> allowedEvTypes, Map<String, EventModel> mapLogbookEvents) {

        EventModel parentEvent = new EventModel();
        String evTypeParent = null;
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
            eventModel = getEventModel(event, operationId, lfcId, getEventLogbookType(
                logbookEventType, event.getString(LogbookEventName.EVPARENT_ID.getValue()), parentEvent));

            // save parent evType for coherence check.
            eventModel.setEvTypeParent(evTypeParent);
            evTypeParent = eventModel.getEvType();
            parentEvent = eventModel;

            // check eventType against workflow for STEP and ACTION
            if ((LogbookEventType.STEP.equals(eventModel.getLogbookEventType()) ||
                LogbookEventType.ACTION.equals(eventModel.getLogbookEventType())) &&
                allowedEvTypes != null && !allowedEvTypes.contains(eventModel.getEvType())) {
                logbookCheckTotalResults.add(new LogbookCheckResult(eventModel.getOperationId(),
                    eventModel.getLfcId(), eventModel.getEvType(),
                    String.format(SAVED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG, LogbookEventName.EVTYPE.getValue(),
                        eventModel.getEvType()),
                    String.format(EXPECTED_LOGBOOK_WORKFLOW_NOT_EXISTS_MSG, LogbookEventName.EVTYPE.getValue(),
                        eventModel.getEvType())));
            }

            // Check all events : evType, outcome and outcomeDetails
            logbookCheckTotalResults.addAll(logbookDetailsCheckService.checkEvent(eventModel));

            // construct maps for logbook operation and the lifecycles to check coherence between each other.
            if (!(eventModel.getLogbookEventType().equals(LogbookEventType.STEP)
                && eventModel.getEvType().endsWith(STEP_STARTED_SUFFIX))) {

                if (!mapLogbookEvents.containsKey(eventModel.getEvType())) {
                    mapLogbookEvents.put(eventModel.getEvType(), eventModel);
                } else {
                    // aggregation of logbook events
                    EventModel value = mapLogbookEvents.get(eventModel.getEvType());
                    if (OutcomeStatus.valueOf(eventModel.getOutcome()).getWeight() >
                        OutcomeStatus.valueOf(value.getOutcome()).getWeight()) {
                        mapLogbookEvents.replace(eventModel.getEvType(), eventModel);
                    }
                }
            }
        }
    }


    /**
     * addLogbookEventType for the given event.
     *
     * @param logbookEventType
     * @param evParentId
     * @param parentEvent
     * @return
     */
    private LogbookEventType getEventLogbookType(LogbookEventType logbookEventType, String evParentId,
        EventModel parentEvent) {
        // compute the event type on the logbook operation.
        if (LogbookEventType.OPERATION.equals(logbookEventType)) {
            if (evParentId == null) {
                // event of type "STEP"
                return LogbookEventType.STEP;
            } else if (parentEvent.getEvParentId() == null) {
                // event of type "ACTION"
                return LogbookEventType.ACTION;
            } else if (LogbookEventType.ACTION.equals(parentEvent.getLogbookEventType())) {
                // event of type "TASK" (TREATMENT)
                return LogbookEventType.TASK;
            }
        }

        // compute the event type on the logbook lifecycles.
        if (LogbookEventType.UNIT_LFC.equals(logbookEventType) ||
            LogbookEventType.OBJECTGROUP_LFC.equals(logbookEventType)) {
            if (evParentId == null) {
                // event of type "ACTION"
                return LogbookEventType.ACTION;
            } else {
                // event of type "TASK" (TREATMENT)
                return LogbookEventType.TASK;
            }
        }

        return logbookEventType.DEFAULT;
    }

}
