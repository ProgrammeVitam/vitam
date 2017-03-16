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
package fr.gouv.vitam.functional.administration.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.RegisterValueDetail}
 */
public class RegisterValueDetailModel {

    @JsonProperty("total")
    private long total;
    @JsonProperty("deleted")
    private long deleted;
    @JsonProperty("remained")
    private long remained;

    /**
     * Constructor without fields
     * 
     * use for jackson
     */
    public RegisterValueDetailModel() {
    }
    
    /**
     * Constructor using fields
     * 
     * @param total             number of objects
     * @param deleted           number of deleted object
     * @param remained          number of remaining object
     * @param originatingAgency originating agency
     */
    public RegisterValueDetailModel(long total, long deleted, long remained) {
        this.total = total;
        this.deleted = deleted;
        this.remained = remained;
    }
    
    /**
     * 
     * @return total
     */
    public long getTotal() {
        return total;
    }
    
    /**
     * 
     * @param total
     * @return this
     */
    public RegisterValueDetailModel setTotal(long total) {
        this.total = total;
        return this;
    }
    
    /**
     * 
     * @return deleted
     */
    public long getDeleted() {
        return deleted;
    }
    
    /**
     * 
     * @param deleted
     * @return this
     */
    public RegisterValueDetailModel setDeleted(long deleted) {
        this.deleted = deleted;
        return this;
    }
    
    /**
     * 
     * @return remained
     */
    public long getRemained() {
        return remained;
    }
    
    /**
     * 
     * @param remained
     * @return this
     */
    public RegisterValueDetailModel setRemained(long remained) {
        this.remained = remained;
        return this;
    }

}
