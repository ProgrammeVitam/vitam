/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.client;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * This class expose wait methods that implements pooling logic
 */
public class VitamPoolingClient {

    private OperationStatusClient operationStatusClient;

    /**
     * @param operationStatusClient interface that expose getOperationProcessStatus
     */
    public VitamPoolingClient(OperationStatusClient operationStatusClient) {
        ParametersChecker.checkParameter("OperationStatusClient is required.", operationStatusClient);
        this.operationStatusClient = operationStatusClient;
    }

    /**
     * This is a helper method for checking the status of an operation
     * Loop until :
     * - nbTry is reached
     * - state is completed
     * - state is pause and status ordinal is higher than started
     *
     * @param tenantId
     * @param processId operationId du processWorkflow
     * @param state The state wanted
     * @param nbTry Number of retry
     * @param timeWait time to sleep
     * @param timeUnit timeUnit to apply to timeWait
     * @return true if completed false else
     */
    public boolean wait(
        int tenantId,
        String processId,
        ProcessState state,
        int nbTry,
        long timeWait,
        TimeUnit timeUnit
    ) throws VitamException {
        StopWatch stopWatch = StopWatch.createStarted();
        do {
            final RequestResponse<ItemStatus> requestResponse =
                this.operationStatusClient.getOperationProcessStatus(new VitamContext(tenantId), processId);
            if (requestResponse.isOk()) {
                ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) requestResponse).getResults().get(0);
                final ProcessState processState = itemStatus.getGlobalState();
                final StatusCode statusCode = itemStatus.getGlobalStatus();

                switch (processState) {
                    case COMPLETED:
                        return true;
                    case PAUSE:
                        /**
                         * FIXME(djh)  should we return true for any statusCode ???
                         * With statusCode == UNKNOWN and State = PAUSE means that processWorkflow is not started
                         * In this case logically we should also return true
                         * But checking status in parallel with ingest we may wait until processWorkflow starts
                         * The problem is calling wait on not started process return false after nbTry
                         * Should we add a params to say if not yet started return true?
                         */
                        if (StatusCode.STARTED.compareTo(statusCode) <= 0) {
                            return true;
                        }
                        // If StatusCode UNKNOWN
                        // If elapsed time is higher than 1 minute then
                        if (stopWatch.getTime(TimeUnit.MINUTES) >= 1) {
                            return false;
                        }
                        break;
                    case RUNNING:
                        break;
                }

                if (null != timeUnit) {
                    timeWait = timeUnit.toMillis(timeWait);
                }
                nbTry--;

                if (nbTry > 0) {
                    try {
                        Thread.sleep(timeWait);
                    } catch (InterruptedException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                }
            } else {
                throw new VitamException((VitamError) requestResponse);
            }
        } while (nbTry > 0);
        return false;
    }

    /**
     * @param tenantId
     * @param processId
     * @param nbTry
     * @param timeout
     * @param timeUnit
     * @return
     * @throws VitamException
     */
    public boolean wait(int tenantId, String processId, int nbTry, long timeout, TimeUnit timeUnit)
        throws VitamException {
        return wait(tenantId, processId, ProcessState.COMPLETED, nbTry, timeout, timeUnit);
    }

    /**
     * @param tenantId
     * @param processId
     * @param state
     * @return
     * @throws VitamException
     */
    public boolean wait(int tenantId, String processId, ProcessState state) throws VitamException {
        return wait(tenantId, processId, state, Integer.MAX_VALUE, 1000l, TimeUnit.MILLISECONDS);
    }

    /**
     * @param tenantId
     * @param processId
     * @return
     * @throws VitamException
     */
    public boolean wait(int tenantId, String processId) throws VitamException {
        return wait(tenantId, processId, ProcessState.COMPLETED);
    }
}
