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

package fr.gouv.vitam.worker.core.distribution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JsonLine Model.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonLineModel {

    /**
     * identifier.
     */
    @JsonProperty("id")
    private String id;

    /**
     * distribGroup.
     */
    @JsonProperty("distribGroup")
    private Integer distribGroup;

    /**
     * params
     */
    @JsonProperty("params")
    private JsonNode params;

    public JsonLineModel() {
        // Empty constructor for deserialization
    }

    public JsonLineModel(String id) {
        this(id, null, null);
    }

    public JsonLineModel(String id, Integer distribGroup, JsonNode params) {
        this.id = id;
        this.distribGroup = distribGroup;
        this.params = params;
    }

    /**
     * getId
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * setId
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * getDistribGroup
     *
     * @return
     */
    public Integer getDistribGroup() {
        return distribGroup;
    }

    /**
     * setDistribGroup
     *
     * @param distribGroup
     */
    public void setDistribGroup(Integer distribGroup) {
        this.distribGroup = distribGroup;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }
}
