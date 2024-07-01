/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;

/**
 * Description of reconstruction response item model. <br/>
 */
public class ReconstructionResponseItem {

    /**
     * collection name.
     */
    @JsonProperty("collection")
    private String collection;

    /**
     * Tenant.
     */
    @JsonProperty("tenant")
    private Integer tenant;

    /**
     * Status.
     */
    @JsonProperty("status")
    private StatusCode status;

    /**
     * Constructor.
     */
    public ReconstructionResponseItem() {
        super();
    }

    /**
     * Constructor
     *
     * @param reconstructionRequestItem request item
     * @param status status
     */
    public ReconstructionResponseItem(ReconstructionRequestItem reconstructionRequestItem, StatusCode status) {
        super();
        this.collection = reconstructionRequestItem.getCollection();
        this.tenant = reconstructionRequestItem.getTenant();
        this.status = status;
    }

    /**
     * Get the collection of the profile
     *
     * @return collection as String
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Set the collection of the profile This value must be unique by tenant
     *
     * @param collection as String
     * @return this
     */
    public ReconstructionResponseItem setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * Get tenant
     *
     * @return tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * Set or change tenant
     *
     * @param tenant
     * @return this
     */
    public ReconstructionResponseItem setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * @return the status
     */
    public StatusCode getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     * @return this
     */
    public ReconstructionResponseItem setStatus(StatusCode status) {
        this.status = status;
        return this;
    }
}
