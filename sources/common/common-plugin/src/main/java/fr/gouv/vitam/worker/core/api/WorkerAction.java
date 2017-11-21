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
package fr.gouv.vitam.worker.core.api;


import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 *
 *
 * Action interface: is a contract for different action Handler event
 *
 * action handler class must be implement this interface
 *
 */
public interface WorkerAction {

    /**
     * Execute an action 
     *
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     * @throws ContentAddressableStorageServerException
     */
    ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException;

    /**
     * Check mandatory parameter
     * 
     * @param handler input output list
     * @throws ProcessingException when handler io is not complete
     */
    void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException;

    /**
     * Should be used to check if LFC log is handled by the WorkerAction itself
     * 
     * @return true if LFC in handled internally
     */
    default boolean lfcHandledInternally(){
        return false;
    }
}
