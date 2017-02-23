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
package fr.gouv.vitam.processing.management.client;


import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.client.OperationManagementClient;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.WorkerBean;

/**
 * Processing Management Client
 */
public interface ProcessingManagementClient extends OperationManagementClient {

    /**
     * executeVitamProcess : processing operation of a workflow
     *
     * @param actionId : name of action
     * @param container : name of the container
     * @param workflow : id of the workflow
     * @return Engine response containing message and status
     * @throws BadRequestException
     * @throws IllegalArgumentException thrown in case of illegal argument in request server error
     * @throws WorkflowNotFoundException thrown if the defined workfow is not found by server
     * @throws ProcessingException
     */
    @Deprecated
    Response executeVitamProcess(String container, String workflow, String actionId)
        throws BadRequestException, WorkflowNotFoundException, ProcessingException;

    /**
     * Register a new worker knowing its family and with a WorkerBean. If a problem is encountered, an exception is
     * thrown.
     *
     * @param familyId the id of the family to which the worker has to be registered
     * @param workerId the id of the worker to be registered
     * @param workerDescription the description of the worker as a workerBean
     * @throws ProcessingBadRequestException if a bad request has been sent
     * @throws WorkerAlreadyExistsException if the worker family does not exist
     */
    void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
        throws ProcessingBadRequestException, WorkerAlreadyExistsException;

    /**
     * Unregister a worker knowing its family and its workerId. If the familyId or the workerId is unknown, an exception
     * is thrown.
     *
     * @param familyId the id of the family to which the worker has to be registered
     * @param workerId the id of the worker to be registered
     * @throws ProcessingBadRequestException if the worker or the family does not exist
     */
    void unregisterWorker(String familyId, String workerId)
        throws ProcessingBadRequestException;
}
