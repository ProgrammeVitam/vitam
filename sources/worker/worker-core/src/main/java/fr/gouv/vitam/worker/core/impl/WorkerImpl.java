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
package fr.gouv.vitam.worker.core.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.StatusAggregationBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckConcurrentWorkflowLockHandler;
import fr.gouv.vitam.worker.core.handler.CheckDataObjectPackageActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckHeaderActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckIngestContractActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckNoObjectsActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectUnitConsistencyActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectsNumberActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckStorageAvailabilityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckVersionActionHandler;
import fr.gouv.vitam.worker.core.handler.CommitLifeCycleObjectGroupActionHandler;
import fr.gouv.vitam.worker.core.handler.CommitLifeCycleUnitActionHandler;
import fr.gouv.vitam.worker.core.handler.DummyHandler;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.IngestAccessionRegisterActionHandler;
import fr.gouv.vitam.worker.core.handler.IngestPrepareActionHandler;
import fr.gouv.vitam.worker.core.handler.ListArchiveUnitsActionHandler;
import fr.gouv.vitam.worker.core.handler.ListRunningIngestsActionHandler;
import fr.gouv.vitam.worker.core.handler.PrepareStorageInfoActionHandler;
import fr.gouv.vitam.worker.core.handler.RollBackActionHandler;
import fr.gouv.vitam.worker.core.handler.TransferNotificationActionHandler;
import fr.gouv.vitam.worker.core.handler.UploadSIPActionHandler;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import fr.gouv.vitam.worker.core.plugin.computeinheritedrules.ComputedInheritedRulesCheckDistributionThreshold;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionAccessionRegisterPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionCheckDistributionThresholdHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionFinalizationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionObjectGroupPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionReportGenerationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionUnitPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationAnalysisCheckDistributionThresholdHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationAnalysisFinalizationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationAnalysisPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.PreservationAccessionRegisterActionHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationFinalizationHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationCheckGraphHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationLoadRequestHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationUpdateDistributionHandler;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyAccessionRegisterPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyObjectGroupPreparationHandler;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyReportGenerationHandler;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.TransferReplyUnitPreparationHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_ANTIVIRUS;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_DIGEST_MANIFEST;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_FILENAME_MANIFEST;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_FORMAT;
import static fr.gouv.vitam.common.model.processing.LifecycleState.FLUSH_LFC;

/**
 * WorkerImpl class implements Worker interface
 * <p>
 * manages and executes actions by step
 */
public class WorkerImpl implements Worker {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerImpl.class);
    private static final String EMPTY_EV_DET_DATA = "{}";

    private static PerformanceLogger PERFORMANCE_LOGGER = PerformanceLogger.getInstance();

    private static final String EMPTY_LIST = "null or Empty Action list";
    private static final String STEP_NULL = "step paramaters is null";
    private static final String HANDLER_NOT_FOUND = ": handler not found exception: ";

    private final Map<String, Supplier<ActionHandler>> actions = new HashMap<>();
    private String workerId;
    private final PluginLoader pluginLoader;

    /**
     * Constructor
     *
     * @param pluginLoader the plugin loader
     */
    public WorkerImpl(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
        /*
         * Default workerId but changed in case of bulk
         */
        workerId = GUIDFactory.newGUID().toString();
        /*
         * temporary init: will be managed by spring annotation
         */
        init();
    }

    /**
     * Add an actionhandler in the pool of action
     *
     * @param actionName action name
     * @param actionHandler action handler
     * @return WorkerImpl
     */
    @Override
    @VisibleForTesting
    public WorkerImpl addActionHandler(String actionName, ActionHandler actionHandler) {
        ParametersChecker.checkParameter("actionName is a mandatory parameter", actionName);
        ParametersChecker.checkParameter("actionHandler is a mandatory parameter", actionHandler);
        actions.put(actionName, () -> actionHandler);
        return this;
    }

    private void init() {
        /*
         * Pool of action 's object
         */
        actions.put(UploadSIPActionHandler.getId(), UploadSIPActionHandler::new);
        actions.put(CHECK_ANTIVIRUS.getItemValue(), IngestPrepareActionHandler::new);
        actions.put(CHECK_FORMAT.getItemValue(), IngestPrepareActionHandler::new);
        actions.put(CHECK_FILENAME_MANIFEST.getItemValue(), IngestPrepareActionHandler::new);
        actions.put(CHECK_DIGEST_MANIFEST.getItemValue(), IngestPrepareActionHandler::new);
        actions.put(CheckSedaActionHandler.getId(), CheckSedaActionHandler::new);
        actions.put(CheckIngestContractActionHandler.getId(), CheckIngestContractActionHandler::new);
        actions.put(CheckObjectsNumberActionHandler.getId(), CheckObjectsNumberActionHandler::new);
        actions.put(CheckNoObjectsActionHandler.getId(), CheckNoObjectsActionHandler::new);
        actions.put(CheckVersionActionHandler.getId(), CheckVersionActionHandler::new);
        actions.put(CheckStorageAvailabilityActionHandler.getId(), CheckStorageAvailabilityActionHandler::new);
        actions.put(CheckObjectUnitConsistencyActionHandler.getId(), CheckObjectUnitConsistencyActionHandler::new);
        actions.put(PrepareStorageInfoActionHandler.getId(), PrepareStorageInfoActionHandler::new);
        actions.put(IngestAccessionRegisterActionHandler.getId(), IngestAccessionRegisterActionHandler::new);
        actions.put(
            PreservationAccessionRegisterActionHandler.getId(),
            PreservationAccessionRegisterActionHandler::new
        );
        actions.put(TransferNotificationActionHandler.getId(), TransferNotificationActionHandler::new);
        actions.put(DummyHandler.getId(), DummyHandler::new);

        actions.put(CommitLifeCycleUnitActionHandler.getId(), CommitLifeCycleUnitActionHandler::new);
        actions.put(CommitLifeCycleObjectGroupActionHandler.getId(), CommitLifeCycleObjectGroupActionHandler::new);

        actions.put(RollBackActionHandler.getId(), RollBackActionHandler::new);

        actions.put(CheckHeaderActionHandler.getId(), CheckHeaderActionHandler::new);

        CheckNoObjectsActionHandler checkNoObjectsActionHandler = new CheckNoObjectsActionHandler();
        CheckObjectsNumberActionHandler checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler();
        ExtractSedaActionHandler extractSedaActionHandler = new ExtractSedaActionHandler();
        CheckObjectUnitConsistencyActionHandler checkObjectUnitConsistencyActionHandler =
            new CheckObjectUnitConsistencyActionHandler();
        CheckDataObjectPackageActionHandler checkDataObjectPackageActionHandler =
            new CheckDataObjectPackageActionHandler(
                checkNoObjectsActionHandler,
                checkObjectsNumberActionHandler,
                extractSedaActionHandler,
                checkObjectUnitConsistencyActionHandler
            );
        actions.put(CheckDataObjectPackageActionHandler.getId(), () -> checkDataObjectPackageActionHandler);

        actions.put(ListRunningIngestsActionHandler.getId(), ListRunningIngestsActionHandler::new);
        actions.put(ListArchiveUnitsActionHandler.getId(), ListArchiveUnitsActionHandler::new);

        actions.put(CheckConcurrentWorkflowLockHandler.getId(), CheckConcurrentWorkflowLockHandler::new);
        actions.put(
            ReclassificationPreparationLoadRequestHandler.getId(),
            ReclassificationPreparationLoadRequestHandler::new
        );
        actions.put(
            ReclassificationPreparationCheckGraphHandler.getId(),
            ReclassificationPreparationCheckGraphHandler::new
        );
        actions.put(
            ReclassificationPreparationUpdateDistributionHandler.getId(),
            ReclassificationPreparationUpdateDistributionHandler::new
        );
        actions.put(ReclassificationFinalizationHandler.getId(), ReclassificationFinalizationHandler::new);

        actions.put(
            EliminationAnalysisCheckDistributionThresholdHandler.getId(),
            EliminationAnalysisCheckDistributionThresholdHandler::new
        );
        actions.put(EliminationAnalysisPreparationHandler.getId(), EliminationAnalysisPreparationHandler::new);
        actions.put(EliminationAnalysisFinalizationHandler.getId(), EliminationAnalysisFinalizationHandler::new);

        actions.put(
            EliminationActionCheckDistributionThresholdHandler.getId(),
            EliminationActionCheckDistributionThresholdHandler::new
        );
        actions.put(EliminationActionUnitPreparationHandler.getId(), EliminationActionUnitPreparationHandler::new);
        actions.put(
            EliminationActionObjectGroupPreparationHandler.getId(),
            EliminationActionObjectGroupPreparationHandler::new
        );
        actions.put(
            EliminationActionAccessionRegisterPreparationHandler.getId(),
            EliminationActionAccessionRegisterPreparationHandler::new
        );
        actions.put(EliminationActionReportGenerationHandler.getId(), EliminationActionReportGenerationHandler::new);
        actions.put(EliminationActionFinalizationHandler.getId(), EliminationActionFinalizationHandler::new);

        actions.put(TransferReplyUnitPreparationHandler.getId(), TransferReplyUnitPreparationHandler::new);
        actions.put(
            TransferReplyObjectGroupPreparationHandler.getId(),
            TransferReplyObjectGroupPreparationHandler::new
        );
        actions.put(
            TransferReplyAccessionRegisterPreparationHandler.getId(),
            TransferReplyAccessionRegisterPreparationHandler::new
        );
        actions.put(TransferReplyReportGenerationHandler.getId(), TransferReplyReportGenerationHandler::new);

        actions.put(
            ComputedInheritedRulesCheckDistributionThreshold.getId(),
            ComputedInheritedRulesCheckDistributionThreshold::new
        );
    }

    @Override
    public ItemStatus run(WorkerParameters workParams, Step step) throws IllegalArgumentException, ProcessingException {
        // mandatory check
        ParametersChecker.checkNullOrEmptyParameters(workParams);

        if (step == null) {
            throw new IllegalArgumentException(STEP_NULL);
        }

        if (step.getActions() == null || step.getActions().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST);
        }

        final ItemStatus responses = new ItemStatus(step.getStepName());

        // loop on objectList
        // Each task should have its own workerId
        workerId = GUIDFactory.newGUID().toString();
        try (
            final HandlerIO handlerIO = new HandlerIOImpl(
                workParams.getContainerName(),
                workerId,
                workParams.getObjectNameList()
            );
            LogbookLifeCyclesClient logbookLfcClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            LifecycleFromWorker lifecycleFromWorker = new LifecycleFromWorker(logbookLfcClient);

            // loop on actions
            for (final Action action : step.getActions()) {
                Stopwatch stopwatch = Stopwatch.createStarted();

                // Reset handlerIO for next execution
                handlerIO.reset();
                ActionDefinition actionDefinition = action.getActionDefinition();
                if (actionDefinition.getIn() != null) {
                    handlerIO.addInIOParameters(actionDefinition.getIn());
                }
                if (actionDefinition.getOut() != null) {
                    handlerIO.addOutIOParameters(actionDefinition.getOut());
                }
                String handlerName = actionDefinition.getActionKey();
                // If this is a plugin
                List<ItemStatus> pluginResponse;
                ItemStatus aggregateItemStatus = new ItemStatus();

                if (pluginLoader.contains(handlerName)) {
                    try (ActionHandler actionPlugin = pluginLoader.newInstance(handlerName)) {
                        LOGGER.debug("START plugin ", actionDefinition.getActionKey(), step.getStepName());

                        pluginResponse = actionPlugin.executeList(workParams, handlerIO);
                        List<ItemStatus> itemStatuses = pluginResponse
                            .stream()
                            .flatMap(itemStatus -> itemStatus.getItemsStatus().values().stream())
                            .collect(Collectors.toList());

                        aggregateItemStatus = getActionResponse(
                            handlerName,
                            itemStatuses,
                            action.getActionDefinition().getStatusAggregationBehavior()
                        );
                        aggregateItemStatus.setItemId(handlerName);
                        if (action.getActionDefinition().lifecycleEnabled()) {
                            lifecycleFromWorker.generateLifeCycle(
                                pluginResponse,
                                workParams,
                                action,
                                step.getDistribution().getType()
                            );
                        }

                        responses.setItemsStatus(aggregateItemStatus);
                    }
                } else {
                    final ActionHandler actionHandler = getActionHandler(handlerName);
                    if (actionHandler == null) {
                        throw new HandlerNotFoundException(actionDefinition.getActionKey() + HANDLER_NOT_FOUND);
                    }

                    LOGGER.debug("START handler {} in step {}", actionDefinition.getActionKey(), step.getStepName());

                    pluginResponse = actionHandler.executeList(workParams, handlerIO);

                    for (ItemStatus itemStatus : pluginResponse) {
                        aggregateItemStatus.setItemId(itemStatus.getItemId());
                        aggregateItemStatus.setItemsStatus(itemStatus);
                    }

                    responses.setItemsStatus(aggregateItemStatus);
                }

                if (FLUSH_LFC.equals(action.getActionDefinition().getLifecycleState())) {
                    lifecycleFromWorker.saveLifeCycles(step.getDistribution().getType());
                }

                LOGGER.debug("STOP handler {} in step {}", actionDefinition.getActionKey(), step.getStepName());
                // if the action has been defined as Blocking and the action status is KO or FATAL
                // then break the process

                long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

                PERFORMANCE_LOGGER.log(step.getStepName(), actionDefinition.getActionKey(), elapsed);

                if (responses.shallStop(ProcessBehavior.BLOCKING.equals(actionDefinition.getBehavior()))) {
                    break;
                }
            }

            lifecycleFromWorker.saveLifeCycles(step.getDistribution().getType());

            clearEvDetDataForDistributedSteps(step, responses);
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
        LOGGER.debug("step name :" + step.getStepName());
        return responses;
    }

    private void clearEvDetDataForDistributedSteps(Step step, ItemStatus responses) {
        // EvDetData for distributed steps should not be returned to process engine, and should not be set for
        // logbook operation.
        if (step.getDistribution().getKind().isDistributed()) {
            for (ItemStatus item : responses.getItemsStatus().values()) {
                item.setEvDetailData(EMPTY_EV_DET_DATA);
            }
            responses.setEvDetailData(EMPTY_EV_DET_DATA);
        }
    }

    private static ItemStatus getActionResponse(
        String handlerName,
        List<ItemStatus> pluginResponse,
        StatusAggregationBehavior statusBehavior
    ) {
        ItemStatus status = new ItemStatus(handlerName);

        for (ItemStatus subItemStatus : pluginResponse) {
            subItemStatus.setItemId(handlerName);
            status.setItemsStatus(handlerName, subItemStatus, statusBehavior);
        }

        return status;
    }

    private ActionHandler getActionHandler(String actionId) {
        return actions.get(actionId).get();
    }

    @Override
    public String getWorkerId() {
        return workerId;
    }

    @Override
    public void close() {
        actions.clear();
    }
}
