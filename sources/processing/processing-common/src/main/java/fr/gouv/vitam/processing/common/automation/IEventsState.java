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
package fr.gouv.vitam.processing.common.automation;

import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

import java.util.Map;

/**
 * This interface expose the action to be executed by the ProcessManager
 */
public interface IEventsState {
    /**
     * Do an evaluation of the State RUNNING If the state is not permitted a StateNotAllowedException is thrown Else
     * call doRunning method
     *
     * @param workerParameters
     * @throws StateNotAllowedException
     */
    void resume(WorkerParameters workerParameters) throws StateNotAllowedException, ProcessingException;

    /**
     * Like a resume but pause at the next step
     *
     * @param workerParameters
     * @throws StateNotAllowedException
     * @throws ProcessingException
     */
    void next(WorkerParameters workerParameters) throws StateNotAllowedException, ProcessingException;

    /**
     * Pause the processWorkflow, If the last step the just wait the final step Else pause the processWorkflow as soon
     * as possible Do not wait all elements of the current step to be executed The step pauseCancelAction will be
     * updated to PauseOrCancelAction.ACTION_PAUSE If all elements of the current step are executed then stop correctly
     * and step pauseCancelAction will be updated to PauseOrCancelAction.ACTION_COMPLETE
     *
     * After next or resume occurs on paused processWorkflow, It will starts from the step pauseCancelAction equals to
     * PauseOrCancelAction.ACTION_PAUSE if exists and update pauseCancelAction to be PauseOrCancelAction.ACTION_RECOVER
     * Else starts normally from the next step
     *
     * @throws StateNotAllowedException
     */
    void pause() throws StateNotAllowedException;

    /**
     * Replay the last executed step, or if it s stated, the step passed as a parameter
     *
     * @param workerParameters
     * @throws StateNotAllowedException
     * @throws ProcessingException
     */
    void replay(WorkerParameters workerParameters) throws StateNotAllowedException, ProcessingException;

    /**
     * Should used only when server is shutting down
     *
     * To prevent deadlock, this method is not synchronized, Because onComplete and onPauseOrCancel are synchronized and
     * called from ProcessEngine
     */
    void shutdown();

    /**
     * Cancel as soon as possible the processWorkflow, To do that, the step pauseCancelAction is updated to be
     * PauseOrCancelAction.ACTION_CANCEL Unlike pause, - The final step should be executed, -
     * PauseOrCancelAction.ACTION_CANCEL have no impact on the final step - The final step cannot be cancelled
     *
     * @throws StateNotAllowedException
     */
    void cancel() throws StateNotAllowedException;

    /**
     * @return true is processWorkflow is completed or Pause
     */
    boolean isDone();

    /**
     * @return true is processWorkflow is Pause
     */
    boolean isRecover();

    /**
     * @return true if processWorkflow is running in stepByStep (next) mode or in continue mode (resume)
     */
    boolean isStepByStep();

    /**
     * @return The LogbookTypeProcess
     */
    LogbookTypeProcess getLogbookTypeProcess();

    /**
     * @return The tenantId of the processWorkflow
     */
    int getTenant();

    /**
     * @return The workflow Id
     */
    String getWorkflowId();

    String getContextId();

    Map<String, String> getWorkflowParameters();
}
