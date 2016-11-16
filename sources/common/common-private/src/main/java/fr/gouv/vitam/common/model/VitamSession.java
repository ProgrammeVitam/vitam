/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.common.model;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.slf4j.MDC;

import javax.validation.constraints.NotNull;

/**
 * <p>Generic VitamSession object ; used to store thread context information.</p>
 * <p>Only one instance of this class must be used per thread. In fact, only
 * {@link fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread} should be allowed to create one.</p>
 * <p>Finally, this class is NOT threadsafe ; only the thread "owning" the instance should be used to access it. Any other thread will cause an {@link IllegalStateException} when mutating its state.</p>
 */
public class VitamSession {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamSession.class);


    // Thread owner management

    private final VitamThreadFactory.VitamThread owningThread;


    /**
     * @param owningThread
     */
    public VitamSession(VitamThreadFactory.VitamThread owningThread) {
        this.owningThread = owningThread;
    }

    private void checkCallingThread() {
        if (Thread.currentThread() != owningThread) {
            throw new IllegalStateException("VitamSession should only be called by the thread that owns it ; here, caller was "+Thread.currentThread()+", and owner was ");
        }
    }

    /**
     * @return the current X-Request-Id
     */
    public String getRequestId() {
        return requestId;
    }

    // Internal state management

    private String requestId = null;

    /**
     * Set the request id and saves it to the MDC
     *
     * @param newRequestId
     */
    public void setRequestId(String newRequestId) {
        checkCallingThread();
        Object oldRequestId = MDC.get(GlobalDataRest.X_REQUEST_ID);
        if (oldRequestId != requestId) {
            // KWA TODO: replace the check by thing like StringUtils.checkNullOrEmpty(toto)
            LOGGER.warn("Caution : inconsistency detected between content of the VitamSession (requestId:{}) and the Logging MDC (requestId:{})", oldRequestId, requestId);
        }
        this.requestId = newRequestId;
        MDC.put(GlobalDataRest.X_REQUEST_ID, newRequestId);
    }

    /**
     * Get the content of a given VitamSession and copy its internal values to the current instance
     * @param newSession Source session
     */
    public void mutateFrom(@NotNull VitamSession newSession) {
        // Parameters guard
        if (newSession == null) {
            throw new IllegalArgumentException("VitamSession should not be null");
        }
        setRequestId(newSession.getRequestId());
    }

    /**
     * Erase the content of the VitamSession
     */
    public void erase() {
        mutateFrom(new VitamSession(owningThread));
    }


    @Override
    public String toString() {
        return Integer.toHexString(hashCode()) + "{requestId='" + requestId + '\'' + '}';
    }
}
