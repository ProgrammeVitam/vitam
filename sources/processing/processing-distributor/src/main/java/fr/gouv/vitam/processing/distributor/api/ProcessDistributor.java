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
package fr.gouv.vitam.processing.distributor.api;

import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.exception.WorkerNotFoundException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

/**
 * interface ProcessDistributor
 *
 * improves a availability and scalability
 *
 * Various methods Distributor engine
 */
// TODO REVIEW improve comment form
public interface ProcessDistributor {

    /**
     * Distribute different steps (execute a workflow actions step by step)
     *
     * @param workParams {@link fr.gouv.vitam.processing.common.parameter.WorkerParameters} null not allowed
     * @param step {@link ProcessStep} null not allowed
     * @param workflowId workflow Id
     *
     * @return CompositeItemStatus : list of action response
     */
    CompositeItemStatus distribute(WorkerParameters workParams, Step step, String workflowId);

    /**
     * Register a new worker knowing its family
     *
     * @param familyId the id of the family
     * @param workerId the id of the worker
     * @param workerInformation information of the worker to be registered
     * @throws WorkerAlreadyExistsException if the worker already exists
     * @throws ProcessingBadRequestException if the worker description is not correct
     */
    void registerWorker(String familyId, String workerId, String workerInformation)
        throws WorkerAlreadyExistsException, ProcessingBadRequestException;

    /**
     * Delete a worker knowing its id
     *
     * @param familyId the id of the family
     * @param workerId the id of the worker
     * @throws WorkerFamilyNotFoundException if the family does not exist
     * @throws WorkerNotFoundException if the worker does not exist
     */
    void unregisterWorker(String familyId, String workerId)
        throws WorkerFamilyNotFoundException, WorkerNotFoundException;
}
