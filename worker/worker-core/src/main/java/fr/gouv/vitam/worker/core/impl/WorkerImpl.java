/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.worker.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionType;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.utils.ContainerExtractionUtilsFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckConformityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectsNumberActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckStorageAvailabilityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckVersionActionHandler;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.IndexObjectGroupActionHandler;
import fr.gouv.vitam.worker.core.handler.IndexUnitActionHandler;
import fr.gouv.vitam.worker.core.handler.StoreObjectGroupActionHandler;


/**
 * WorkerImpl class implements Worker interface
 *
 * manages and executes actions by step
 */
// TODO REVIEW since Factory => class and constructors package protected (many tests broken)
public class WorkerImpl implements Worker {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerImpl.class);

    private static final String EMPTY_LIST = "null or Empty Action list";
    private static final String STEP_NULL = "step paramaters is null";
    private static final String HANDLER_NOT_FOUND = ": handler not found exception";
    private static final String ELAPSED_TIME_MESSAGE = "Total elapsed time in execution of WorkerImpl method run is :";

    Map<String, ActionHandler> actions = new HashMap<>();
    String workerId;

    /**
     * Empty Object constructor
     **/
    public WorkerImpl() {
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
    public WorkerImpl addActionHandler(String actionName, ActionHandler actionHandler) {
        ParametersChecker.checkParameter("actionName is a mandatory parameter", actionName);
        ParametersChecker.checkParameter("actionHandler is a mandatory parameter", actionHandler);
        actions.put(actionName, actionHandler);
        return this;
    }

    private void init() {
        workerId = GUIDFactory.newGUID().toString();

        /**
         * Pool of action 's object
         */
        actions.put(ExtractSedaActionHandler.getId(), new ExtractSedaActionHandler(new SedaUtilsFactory()));
        actions.put(IndexUnitActionHandler.getId(), new IndexUnitActionHandler(new SedaUtilsFactory()));
        actions.put(IndexObjectGroupActionHandler.getId(), new IndexObjectGroupActionHandler(new SedaUtilsFactory()));
        actions.put(CheckSedaActionHandler.getId(), new CheckSedaActionHandler(new SedaUtilsFactory()));
        actions.put(CheckObjectsNumberActionHandler.getId(),
            new CheckObjectsNumberActionHandler(new SedaUtilsFactory(), new ContainerExtractionUtilsFactory()));
        actions.put(CheckVersionActionHandler.getId(), new CheckVersionActionHandler(new SedaUtilsFactory()));
        actions.put(CheckConformityActionHandler.getId(), new CheckConformityActionHandler(new SedaUtilsFactory()));
        actions.put(StoreObjectGroupActionHandler.getId(), new StoreObjectGroupActionHandler(new SedaUtilsFactory()));
        actions.put(CheckStorageAvailabilityActionHandler.getId(),
            new CheckStorageAvailabilityActionHandler(new SedaUtilsFactory()));
    }

    @Override
    public List<EngineResponse> run(WorkerParameters workParams, Step step)
        throws IllegalArgumentException, ProcessingException {


        final long time = System.currentTimeMillis();        

        // mandatory check
        ParameterHelper.checkNullOrEmptyParameters(workParams);

        if (step == null) {
            throw new IllegalArgumentException(STEP_NULL);
        }

        if (step.getActions() == null || step.getActions().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST);
        }

        final List<EngineResponse> responses = new ArrayList<>();

        for (final Action action : step.getActions()) {

            final ActionHandler actionHandler = getActionHandler(action.getActionDefinition().getActionKey());
            if (actionHandler == null) {
                throw new HandlerNotFoundException(action.getActionDefinition().getActionKey() + HANDLER_NOT_FOUND);
            }
            EngineResponse actionResponse = actionHandler.execute(workParams);
            responses.add(actionResponse);
            // if the action has been defined as Blocking, then, we check the action Status then break the process
            // (actions) if
            // the status is KO            
            if (ActionType.BLOCK.equals(action.getActionDefinition().getActionType()) &&
                (StatusCode.KO.equals(actionResponse.getStatus()) ||
                    StatusCode.FATAL.equals(actionResponse.getStatus()))) {
                break;
            }
        }

        LOGGER.info(ELAPSED_TIME_MESSAGE + (System.currentTimeMillis() - time) / 1000 + "s / for step name :" +
            step.getStepName());
        return responses;
    }

    private ActionHandler getActionHandler(String actionId) {
        return actions.get(actionId);
    }

    @Override
    public String getWorkerId() {
        return workerId;
    }

}
