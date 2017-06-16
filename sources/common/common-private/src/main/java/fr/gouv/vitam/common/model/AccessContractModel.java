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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Data Transfer Object Model of access contract (DTO).
 */

public class AccessContractModel extends AbstractContractModel {

    /**
     * Work for all data object version
     */
    public static final String EVERY_DATA_OBJECT_VERSION = "EveryDataObjectVersion";

    /**
     * Work for all originating agencies 
     */
    public static final String EVERY_ORIGINATINGAGENCY = "EveryOriginatingAgency";

    @JsonProperty("DataObjectVersion")
    private Set<String> dataObjectVersion;

    @JsonProperty("OriginatingAgencies")
    private Set<String> originatingAgencies;
    
    @JsonProperty("WritingPermission")
    private boolean writingPermission;

    @JsonProperty(EVERY_ORIGINATINGAGENCY)
    private boolean everyOriginatingAgency;
    
    @JsonProperty(EVERY_DATA_OBJECT_VERSION)
    private boolean everyDataObjectVersion;

    /**
     * Constructor without fields
     * use for jackson
     */
    public AccessContractModel() {
        super();
    }


    /**
     *  Get the collection of originating agency
     * @return originatingAgencies collection
     */
    public Set<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    /**
     * Set the collection of originating agency
     * @param originatingAgencies
     */
    public AccessContractModel setOriginatingAgencies(Set<String> originatingAgencies) {
        this.originatingAgencies = originatingAgencies;
        return this;
    }
    
    /**
     * @return dataObjectVersion
     */
    public Set<String> getDataObjectVersion() {
        return dataObjectVersion;
    }


    /**
     * @param dataObjectVersion
     * @return AccessContractModel
     */
    public AccessContractModel setDataObjectVersion(Set<String> dataObjectVersion) {
        this.dataObjectVersion = dataObjectVersion;
        return this;
    }
    
    /**
     * @return writingPermission
     */
    public boolean getWritingPermission() {
        return writingPermission;
    }

    /**
     * @param writingPermission
     * @return AccessContractModel
     */
    public AccessContractModel setWritingPermission(boolean writingPermission) {
        this.writingPermission = writingPermission;
        return this;
    }


	/**
	 * @return true if all originatingAgencies are enabled for this contract
	 */
	public boolean getEveryOriginatingAgency() {
		return everyOriginatingAgency;
	}


	/**
	 * Set the 'everyOriginatingAgency' flag on the contract.
	 *
	 * @param everyOriginatingAgency If true, all originatingAgencies are enabled for this contract
	 * @return the contract
	 */
	public AccessContractModel setEveryOriginatingAgency(boolean everyOriginatingAgency) {
		this.everyOriginatingAgency = everyOriginatingAgency;
		return this;
	}


    /**
     * @return true if all data object version are enabled for this contract
     */
    public boolean isEveryDataObjectVersion() {
        return everyDataObjectVersion;
    }


    /**
     * Set the 'everyDataObjectVersion' flag on the contract.
     * 
     * @param everyDataObjectVersion if true, all data object version are enabled for this contract
     * @return
     */
    public AccessContractModel setEveryDataObjectVersion(boolean everyDataObjectVersion) {
        this.everyDataObjectVersion = everyDataObjectVersion;
        return this;
    }

}
