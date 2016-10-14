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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.core.WorkerIOManagementHelper;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.worker.core.api.Worker;
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
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;



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
    private static final String HANDLER_INPUT_NOT_FOUND = "Handler input not found exception";
    private static final String HANDLER_NOT_FOUND = ": handler not found exception: ";
    private final Map<String, ActionHandler> actions = new HashMap<>();
    private final Map<String, Object> memoryMap = new HashMap<>();
    private final String workerId;

    /**
     * Empty Object constructor
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
        actions.put(CheckObjectsNumberActionHandler.getId(),new CheckObjectsNumberActionHandler());
        actions.put(CheckVersionActionHandler.getId(), new CheckVersionActionHandler());
        actions.put(CheckConformityActionHandler.getId(), new CheckConformityActionHandler());
        actions.put(StoreObjectGroupActionHandler.getId(), new StoreObjectGroupActionHandler());
        actions.put(CheckStorageAvailabilityActionHandler.getId(),
            new CheckStorageAvailabilityActionHandler());
        actions.put(CheckObjectUnitConsistencyActionHandler.getId(),
            new CheckObjectUnitConsistencyActionHandler());
        actions.put(FormatIdentificationActionHandler.getId(),
            new FormatIdentificationActionHandler());
        actions.put(TransferNotificationActionHandler.getId(),
            new TransferNotificationActionHandler());
        actions.put(DummyHandler.getId(), new DummyHandler());
    }

    @Override
    public List<EngineResponse> run(WorkerParameters workParams, Step step)
        throws IllegalArgumentException, ProcessingException {
        // mandatory check
        ParameterHelper.checkNullOrEmptyParameters(workParams);

        final WorkspaceClient client = WorkspaceClientFactory.create(workParams.getUrlWorkspace());
        if (step == null) {
            throw new IllegalArgumentException(STEP_NULL);
        }

        if (step.getActions() == null || step.getActions().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST);
        }

        final List<EngineResponse> responses = new ArrayList<>();
        final List<HandlerIO> handlerIOParams = new ArrayList<>();

        for (final Action action : step.getActions()) {
            final ActionHandler actionHandler = getActionHandler(action.getActionDefinition().getActionKey());
            LOGGER.debug("START handler {} in step {}",action.getActionDefinition().getActionKey(),step.getStepName());
            final HandlerIO handlerIO = getHandlerIOParam(action, client, workParams);            
            if (actionHandler == null) {
                throw new HandlerNotFoundException(action.getActionDefinition().getActionKey() + HANDLER_NOT_FOUND);
            }

            handlerIOParams.add(handlerIO);
            final EngineResponse actionResponse = actionHandler.execute(workParams, handlerIO);
            responses.add(actionResponse);
            LOGGER.debug("STOP handler {} in step {}",action.getActionDefinition().getActionKey(),step.getStepName());
            // if the action has been defined as Blocking and the action status is KO or FATAL
            // then break the process
            if (ProcessBehavior.BLOCKING.equals(action.getActionDefinition().getBehavior()) &&
                actionResponse.getStatus().isGreaterOrEqualToKo()) {
                break;
            }
        }
        // Clear all worker input and output
        try {
            clearWorkerIOParam(workParams.getContainerName() + "_" + workerId);
        } catch (final IOException e) {
            LOGGER.error("Can not clean temporary folder", e);
            throw new ProcessingException(e);
        }
        memoryMap.clear();

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

    private HandlerIO getHandlerIOParam(Action action, WorkspaceClient client, WorkerParameters workParams)
        throws HandlerNotFoundException {
        final HandlerIO handlerIO = new HandlerIO(workParams.getContainerName() + "_" + workerId);
        if (action.getActionDefinition().getIn() != null) {
            for (final IOParameter input : action.getActionDefinition().getIn()) {
                switch (input.getUri().getPrefix()) {
                    case WORKSPACE: {
                        try {
                            final File file = WorkerIOManagementHelper.findFileFromWorkspace(
                                client,
                                workParams.getContainerName(),
                                input.getUri().getPath(), workerId);
                            handlerIO.addInput(file);
                            break;
                        } catch (final FileNotFoundException e) {
                            LOGGER.error(HANDLER_INPUT_NOT_FOUND, e);
                            throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + input.getUri().getPath());
                        }
                    }
                    case MEMORY: {
                        handlerIO.addInput(memoryMap.get(input.getValue()));
                        break;
                    }
                    case VALUE: {
                        handlerIO.addInput(input.getUri().getPath());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + input.getUri().getPath());
                }
            }
        }
        if (action.getActionDefinition().getOut() != null) {
            for (final IOParameter output : action.getActionDefinition().getOut()) {
                switch (output.getUri().getPrefix()) {
                    case WORKSPACE:
                        handlerIO.addOutput(output.getUri().getPath());
                        break;
                    default:
                        throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + output.getUri().getPath());
                }
            }
        }

        return handlerIO;
    }

    private void clearWorkerIOParam(String containerName) throws IOException {
        FileUtils.deleteDirectory(new File(VitamConfiguration.getVitamTmpFolder() + "/" + containerName));
    }

}
