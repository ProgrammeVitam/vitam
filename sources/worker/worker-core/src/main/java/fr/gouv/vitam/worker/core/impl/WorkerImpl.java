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
package fr.gouv.vitam.worker.core.impl;

import java.util.HashMap;
import java.util.Map;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.handler.AccessionRegisterActionHandler;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckConformityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectUnitConsistencyActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectsNumberActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckStorageAvailabilityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckVersionActionHandler;
import fr.gouv.vitam.worker.core.handler.DummyHandler;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.FormatIdentificationActionHandler;
import fr.gouv.vitam.worker.core.handler.IndexObjectGroupActionHandler;
import fr.gouv.vitam.worker.core.handler.IndexUnitActionHandler;
import fr.gouv.vitam.worker.core.handler.StoreObjectGroupActionHandler;
import fr.gouv.vitam.worker.core.handler.TransferNotificationActionHandler;
import fr.gouv.vitam.worker.core.handler.UnitsRulesComputeHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;



/**
 * WorkerImpl class implements Worker interface
 *
 * manages and executes actions by step
 */
public class WorkerImpl implements Worker {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerImpl.class);

    private static final String EMPTY_LIST = "null or Empty Action list";
    private static final String STEP_NULL = "step paramaters is null";
    private static final String HANDLER_NOT_FOUND = ": handler not found exception: ";
    private final Map<String, ActionHandler> actions = new HashMap<>();
    private final String workerId;

    /**
     * Constructor
     **/
    public WorkerImpl() {
        workerId = GUIDFactory.newGUID().toString();
        /**
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
    public WorkerImpl addActionHandler(String actionName, ActionHandler actionHandler) {
        ParametersChecker.checkParameter("actionName is a mandatory parameter", actionName);
        ParametersChecker.checkParameter("actionHandler is a mandatory parameter", actionHandler);
        actions.put(actionName, actionHandler);
        return this;
    }

    private void init() {
        /**
         * Pool of action 's object
         */
        actions.put(ExtractSedaActionHandler.getId(), new ExtractSedaActionHandler());
        actions.put(IndexUnitActionHandler.getId(), new IndexUnitActionHandler());
        actions.put(IndexObjectGroupActionHandler.getId(), new IndexObjectGroupActionHandler());
        actions.put(CheckSedaActionHandler.getId(), new CheckSedaActionHandler());
        actions.put(CheckObjectsNumberActionHandler.getId(), new CheckObjectsNumberActionHandler());
        actions.put(CheckVersionActionHandler.getId(), new CheckVersionActionHandler());
        actions.put(CheckConformityActionHandler.getId(), new CheckConformityActionHandler());
        actions.put(StoreObjectGroupActionHandler.getId(), new StoreObjectGroupActionHandler());
        actions.put(CheckStorageAvailabilityActionHandler.getId(),
            new CheckStorageAvailabilityActionHandler());
        actions.put(CheckObjectUnitConsistencyActionHandler.getId(),
            new CheckObjectUnitConsistencyActionHandler());
        actions.put(FormatIdentificationActionHandler.getId(),
            new FormatIdentificationActionHandler());
        actions.put(AccessionRegisterActionHandler.getId(),
            new AccessionRegisterActionHandler());
        actions.put(TransferNotificationActionHandler.getId(),
            new TransferNotificationActionHandler());
        actions.put(DummyHandler.getId(), new DummyHandler());
        actions.put(UnitsRulesComputeHandler.getId(), new UnitsRulesComputeHandler());
    }

    @Override
    public ItemStatus run(WorkerParameters workParams, Step step)
        throws IllegalArgumentException, ProcessingException, ContentAddressableStorageServerException {
        // mandatory check
        ParameterHelper.checkNullOrEmptyParameters(workParams);

        if (step == null) {
            throw new IllegalArgumentException(STEP_NULL);
        }

        if (step.getActions() == null || step.getActions().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST);
        }

        final ItemStatus responses = new ItemStatus(step.getStepName());

        try (final HandlerIO handlerIO = new HandlerIOImpl(workParams.getContainerName(), workerId)) {
            for (final Action action : step.getActions()) {
                // Reset handlerIO for next execution
                handlerIO.reset();
                final ActionHandler actionHandler = getActionHandler(action.getActionDefinition().getActionKey());
                LOGGER.debug("START handler {} in step {}", action.getActionDefinition().getActionKey(),
                    step.getStepName());
                if (actionHandler == null) {
                    throw new HandlerNotFoundException(action.getActionDefinition().getActionKey() + HANDLER_NOT_FOUND);
                }
                if (action.getActionDefinition().getIn() != null) {
                    handlerIO.addInIOParameters(action.getActionDefinition().getIn());
                }
                if (action.getActionDefinition().getOut() != null) {
                    handlerIO.addOutIOParameters(action.getActionDefinition().getOut());
                }
                final ItemStatus actionResponse = actionHandler.execute(workParams, handlerIO);
                responses.setItemsStatus(actionResponse);
                LOGGER.debug("STOP handler {} in step {}", action.getActionDefinition().getActionKey(),
                    step.getStepName());
                // if the action has been defined as Blocking and the action status is KO or FATAL
                // then break the process
                if (actionResponse
                    .shallStop(ProcessBehavior.BLOCKING.equals(action.getActionDefinition().getBehavior()))) {
                    break;
                }
            }
        }
        LOGGER.debug("step name :" + step.getStepName());
        return responses;
    }

    private ActionHandler getActionHandler(String actionId) {
        return actions.get(actionId);
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
