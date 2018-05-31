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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.RegisterValueDetail}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RegisterValueDetailModel {

    @JsonProperty("ingested")
    private long ingested;
    @JsonProperty("deleted")
    private long deleted;
    @JsonProperty("remained")
    private long remained;

    @JsonProperty("attached")
    private long attached;
    @JsonProperty("detached")
    private long detached;
    @JsonProperty("symbolicRemained")
    private long symbolicRemained;

    /**
     * Constructor without fields
     *
     * use for jackson
     */
    public RegisterValueDetailModel() {
    }


    /**
     * @param ingested
     * @param attached
     */
    public RegisterValueDetailModel(long ingested, long attached) {
        this.ingested = ingested;
        this.remained = ingested - getDeleted();
        this.attached = attached;
        this.symbolicRemained = attached - getDetached();
    }

    /**
     * Constructor using fields
     *
     * @param ingested number of objects
     * @param deleted  number of deleted object
     * @param remained number of remaining object
     */
    public RegisterValueDetailModel(long ingested, long deleted, long remained) {
        this.ingested = ingested;
        this.deleted = deleted;
        this.remained = remained;
    }

    /**
     * Constructor using fields
     *
     * @param total         number of objects
     * @param deleted       number of deleted object
     * @param remained      number of remaining object
     * @param totalSymbolic number of symbolic object
     * @param attached      number of attached object
     * @param detached      number of detached object
     * @param symbolic      if the register is symbolic
     */
    public RegisterValueDetailModel(long totalSymbolic, long attached, long detached, boolean symbolic) {
        if (symbolic) {
            this.attached = totalSymbolic;
            this.detached = detached;
            this.symbolicRemained = attached;
        }
    }

    /**
     * @return ingested
     */
    public long getIngested() {
        return ingested;
    }

    /**
     * @param ingested value to set field
     * @return this
     */
    public RegisterValueDetailModel setIngested(long ingested) {
        this.ingested = ingested;
        return this;
    }

    /**
     * @return deleted value to set field
     */
    public long getDeleted() {
        return deleted;
    }

    /**
     * @param deleted value to set field
     * @return this
     */
    public RegisterValueDetailModel setDeleted(long deleted) {
        this.deleted = deleted;
        return this;
    }

    /**
     * @return remained
     */
    public long getRemained() {
        return remained;
    }

    /**
     * @param remained value to set field
     * @return this
     */
    public RegisterValueDetailModel setRemained(long remained) {
        this.remained = remained;
        return this;
    }

    /**
     * @return attached
     */
    public long getAttached() {
        return attached;
    }

    /**
     * @param attached value to set field
     * @return this
     */
    public RegisterValueDetailModel setAttached(long attached) {
        this.attached = attached;
        return this;
    }

    /**
     * @return detached
     */
    public long getDetached() {
        return detached;
    }

    /**
     * @param detached value to set field
     * @return this
     */
    public RegisterValueDetailModel setDetached(long detached) {
        this.detached = detached;
        return this;
    }

    /**
     * @return symbolicRemained
     */
    public long getSymbolicRemained() {
        return symbolicRemained;
    }

    /**
     * @param symbolicRemained value to set field
     * @return this
     */
    public RegisterValueDetailModel setSymbolicRemained(long symbolicRemained) {
        this.symbolicRemained = symbolicRemained;
        return this;
    }
}
