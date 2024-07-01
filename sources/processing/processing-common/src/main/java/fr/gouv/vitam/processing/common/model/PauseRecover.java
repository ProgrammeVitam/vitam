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
package fr.gouv.vitam.processing.common.model;

/**
 *
 */
public enum PauseRecover {
    /**
     * The default processWorkflow pauseRecover
     */
    NO_RECOVER,

    /**
     * The processWorkflow will be marked RECOVER_FROM_API_PAUSE when pause action origin is API
     * The processWorkflow will be paused as soon as possible without waiting the end of the step.
     * If the current step ends correctly (pauseCancelAction of the current step is PauseOrCancelAction.ACTION_COMPLETE)
     * then the processWorkflow will be in pause state and the next step will be executed normally
     *
     * If the current step ends with pauseCancelAction equals to PauseOrCancelAction.ACTION_PAUSE
     * this means that all elements of the current steps are not finished and state of the step should be saved in distributorIndex
     * When next or resume action occurs on the processWorkflow :
     * The processWorkflow will starts from the step marked PauseOrCancelAction.ACTION_PAUSE
     * After the execution of doRunning method in th state machine,
     * the pauseRecover of the processWorkflow must be updated to be NO_RECOVER
     *
     * And the distributorIndex will be used to initialize the last offset and ItemStatus before pause
     * Then the processWorkflow continue to be executed normally
     */
    RECOVER_FROM_API_PAUSE,

    /**
     * The processWorkflow will be marked RECOVER_FROM_SERVER_PAUSE when pause action origin is server stop
     * The scenario is the same like RECOVER_FROM_API_PAUSE
     * The only difference is that when the server restarts,
     * only processWorkflow marked RECOVER_FROM_SERVER_PAUSE will be started automatically
     */
    RECOVER_FROM_SERVER_PAUSE,
}
