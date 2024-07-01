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

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

/**
 * This implemented by the state machine and passed to the ProcessEngine
 * ProcessEngine can with this callback the state machine and update it with the information about the execution of step with her status code
 */
public interface IEventsProcessEngine {
    /**
     * Update the current step status code
     *
     * @param statusCode
     */
    void onUpdate(StatusCode statusCode);

    StatusCode getCurrentProcessWorkflowStatus();

    /**
     * @param messageIdentifier
     * @param originatingAgency
     */
    void onUpdate(String messageIdentifier, String originatingAgency);

    /**
     * The ProcessEngine callback on complete step (for any status code)
     *
     * @param itemStatus
     * @param workerParameters
     */
    void onProcessEngineCompleteStep(ItemStatus itemStatus, WorkerParameters workerParameters);

    /**
     * The ProcessEngine callback when the step is cancelled
     *
     * @param workerParameters
     */
    void onProcessEngineCancel(WorkerParameters workerParameters);

    /**
     * The ProcessEngine callback on system error occurred
     *
     * @param throwable
     */
    void onError(Throwable throwable);
}
