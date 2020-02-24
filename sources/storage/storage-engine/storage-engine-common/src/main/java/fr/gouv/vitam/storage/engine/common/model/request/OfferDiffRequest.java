/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.engine.common.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

import java.util.List;

/**
 * Description of offerSync request item model. <br/>
 */
public class OfferDiffRequest {

    /**
     * the identifier of the first offer to compare.
     */
    @JsonProperty("offer1")
    private String offer1;

    /**
     * the identifier of the second offer to compare.
     */
    @JsonProperty("offer2")
    private String offer2;

    /**
     * containerToSync.
     */
    @JsonProperty("container")
    private String container;

    /**
     * tenantId
     */
    @JsonProperty("tenantId")
    private Integer tenantId;

    public OfferDiffRequest() {
        // Empty constructor for deserialization
    }

    public String getOffer1() {
        return offer1;
    }

    public OfferDiffRequest setOffer1(String offer1) {
        this.offer1 = offer1;
        return this;
    }

    public String getOffer2() {
        return offer2;
    }

    public OfferDiffRequest setOffer2(String offer2) {
        this.offer2 = offer2;
        return this;
    }

    public String getContainer() {
        return container;
    }

    public OfferDiffRequest setContainer(String container) {
        this.container = container;
        return this;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public OfferDiffRequest setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
        return this;
    }
}
