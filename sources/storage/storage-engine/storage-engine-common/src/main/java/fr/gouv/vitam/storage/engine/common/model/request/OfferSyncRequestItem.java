/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.storage.engine.common.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Description of offerSync request item model. <br/>
 */
public class OfferSyncRequestItem {

    /**
     * offerSource identifier.
     */
    @JsonProperty("offerSource")
    private String offerSource;

    /**
     * offerDestination identifier.
     */
    @JsonProperty("offerDestination")
    private String offerDestination;

    /**
     * offset.
     */
    @JsonProperty("offset")
    private Long offset;

    /**
     * containerToSync.
     */
    @JsonProperty("containerToSync")
    private String containerToSync;

    /**
     * tenantIdToSync
     */
    @JsonProperty("tenantIdToSync")
    private Integer tenantIdToSync;

    /**
     * Constructor.
     */
    public OfferSyncRequestItem() {
        super();
    }

    public String getOfferSource() {
        return offerSource;
    }

    public OfferSyncRequestItem setOfferSource(String offerSource) {
        this.offerSource = offerSource;
        return this;
    }

    public String getOfferDestination() {
        return offerDestination;
    }

    public OfferSyncRequestItem setOfferDestination(String offerDestination) {
        this.offerDestination = offerDestination;
        return this;
    }

    public Long getOffset() {
        return offset;
    }

    public OfferSyncRequestItem setOffset(Long offset) {
        this.offset = offset;
        return this;
    }

    public String getContainerToSync() {
        return containerToSync;
    }

    public OfferSyncRequestItem setContainerToSync(String containerToSync) {
        this.containerToSync = containerToSync;
        return this;
    }

    public Integer getTenantIdToSync() {
        return tenantIdToSync;
    }

    public OfferSyncRequestItem setTenantIdToSync(Integer tenantIdToSync) {
        this.tenantIdToSync = tenantIdToSync;
        return this;
    }
}
