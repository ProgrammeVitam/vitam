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
package fr.gouv.vitam.common.client;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 * Basic client api for vitam client either in Mock or Rest mode
 */
public interface PoolingStatusClient extends VitamAutoCloseable {

    /**
     * This is a helper method for checking the status of an operation
     * @param tenantId
     * @param processId operationId du processWorkflow
     * @param state The state wanted
     * @param nbTry Number of retry
     * @param timeout
     * @param timeUnit
     * @return true if completed false else
     */
    public boolean wait(int tenantId, String processId, ProcessState state, int nbTry, long timeout, TimeUnit timeUnit) throws VitamException;

    public boolean wait(int tenantId, String processId, int nbTry, long timeout, TimeUnit timeUnit) throws VitamException;

    public boolean wait(int tenantId, String processId, ProcessState state) throws VitamException;

    public boolean wait(int tenantId, String processId) throws VitamException;
}
