/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.processing.management.api;



import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

import java.util.List;

/**
 * ProcessManagement interface
 *
 * This service will be invoked by Ingest Module
 *
 */
public interface ProcessManagement extends VitamAutoCloseable {


    /**
     * Init a new process workflow
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param workflowId
     * @param logbookTypeProcess
     * @param tenantId
     * @return
     * @throws ProcessingException
     */
    ProcessWorkflow init(WorkerParameters workerParameters, String workflowId, LogbookTypeProcess logbookTypeProcess, Integer tenantId) throws ProcessingException;

    /**
     * Handle a next action for the corresponding process workflow
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param tenantId
     * @throws ProcessingException
     * @throws StateNotAllowedException
     */
    ItemStatus next(WorkerParameters workerParameters, Integer tenantId) throws ProcessingException,
        StateNotAllowedException;

    /**
     * Handle a resume action for the corresponding process workflow
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param tenantId
     * @throws ProcessingException
     * @throws StateNotAllowedException
     */
    ItemStatus resume(WorkerParameters workerParameters, Integer tenantId) throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a pause action for the corresponding process workflow
     * @param operationId
     * @param tenantId
     * @throws ProcessingException
     * @throws StateNotAllowedException
     */
    ItemStatus pause(String operationId, Integer tenantId) throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a cancel action for the corresponding process workflow
     * @param operationId
     * @param tenantId
     * @throws ProcessingException
     * @throws StateNotAllowedException
     */
    ItemStatus cancel(String operationId, Integer tenantId) throws ProcessingException, StateNotAllowedException;
    /**
     * Retrieve All the workflow process for monitoring purpose The final business scope of this feature is likely to be
     * redefined, to match the future need
     * 
     * @param tenantId
     * @return All the workflow process details
     */
    List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId);

    /**
     * find the workflow process according to the operation_id and the tenant_id
     * 
     * @param operationId
     * @param tenantId
     * @return the workFlow process
     */
    ProcessWorkflow findOneProcessWorkflow(String operationId, Integer tenantId);
}
