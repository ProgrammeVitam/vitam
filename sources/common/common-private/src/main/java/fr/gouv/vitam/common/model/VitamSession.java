/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.slf4j.MDC;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;

/**
 * <p>
 * Generic VitamSession object ; used to store thread context information.
 * </p>
 * <p>
 * Only one instance of this class must be used per thread. In fact, only
 * {@link fr.gouv.vitam.common.thread.VitamThreadFactory.VitamThread} should be allowed to create one.
 * </p>
 * <p>
 * Finally, this class is NOT threadsafe ; only the thread "owning" the instance should be used to access it. Any other
 * thread will cause an {@link IllegalStateException} when mutating its state.
 * </p>
 */
public class VitamSession {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamSession.class);
    private final VitamThreadFactory.VitamThread owningThread;
    private String requestId = null;
    private Integer tenantId = null;
    private String contractId = null;
    private AccessContractModel contract = null;

    /**
     * @param owningThread the owning thread
     */
    public VitamSession(VitamThreadFactory.VitamThread owningThread) {
        this.owningThread = owningThread;
    }

    /**
     * Build a clone of the original VitamSession, attached to the same thread.
     *
     * @param origin VitamSession to clone
     * @return A new session
     */
    public static VitamSession from(VitamSession origin) {
        final VitamSession newSession = new VitamSession(origin.owningThread);
        newSession.requestId = origin.getRequestId();
        newSession.tenantId = origin.getTenantId();
        newSession.contractId = origin.getContractId();
        newSession.contract = origin.getContract();
        return newSession;
    }

    // Thread owner management

    private void checkCallingThread() {
        if (Thread.currentThread() != owningThread) {
            throw new IllegalStateException(
                "VitamSession should only be called by the thread that owns it ; here, caller was " +
                    Thread.currentThread() + ", and owner was ");
        }
    }

    // Internal state management

    /**
     * @return the current X-Request-Id
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Set the request id and saves it to the MDC
     *
     * @param newRequestId the request id
     */
    public void setRequestId(String newRequestId) {
        checkCallingThread();
        final Object oldRequestId = MDC.get(GlobalDataRest.X_REQUEST_ID);
        if (oldRequestId != requestId) {
            // KWA TODO: replace the check by thing like StringUtils.checkNullOrEmpty(toto)
            LOGGER.warn(
                "Caution : inconsistency detected between content of the VitamSession (requestId:{}) and the Logging MDC (requestId:{})",
                oldRequestId, requestId);
        }
        requestId = newRequestId;
        MDC.put(GlobalDataRest.X_REQUEST_ID, newRequestId);
    }

    /**
     * @return the current X-Tenant-Id
     */
    public Integer getTenantId() {
        return tenantId;
    }


    /**
     * Sets the tenantId 
     * 
     * @param newTenantId
     */
    public void setTenantId(Integer newTenantId) {
        checkCallingThread();
        this.tenantId = newTenantId;
    }

    /**
     * Sets the request id from the guid
     *
     * @param guid the guid
     */
    public void setRequestId(GUID guid) {
        setRequestId(guid.getId());
    }
    
    /**
     * @return contract Id
     */
    public String getContractId() {
        return contractId;
    }

    /**
     * @param contractId
     */
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }
    
	/**
	 * 
	 * 
	 * @return
	 */
	public AccessContractModel getContract() {
		return contract;
	}

	/**
	 * @param contract
	 */
	public void setContract(AccessContractModel contract) {
		this.contract = contract;
	}

	/**
     * Get the content of a given VitamSession and copy its internal values to the current instance
     *
     * @param newSession Source session
     */
    public void mutateFrom(@NotNull VitamSession newSession) {
        // Parameters guard
        if (newSession == null) {
            throw new IllegalArgumentException("VitamSession should not be null");
        }
        setRequestId(newSession.getRequestId());
        setTenantId(newSession.getTenantId());
        setContractId(newSession.getContractId());
        setContract(newSession.getContract());
    }

    /**
     * Erase the content of the VitamSession
     */
    public void erase() {
        mutateFrom(new VitamSession(owningThread));
    }

    /**
     * Check if the session contains a valid request id
     *
     * @throws IllegalArgumentException
     */
    public void checkValidRequestId() {
        ParametersChecker.checkParameter("Request-Id should be defined !", requestId);
    }


    @Override
    public String toString() {
        return Integer.toHexString(hashCode()) + "{requestId='" + requestId + "', tenantId:'" + tenantId + "'}";
    }


}
