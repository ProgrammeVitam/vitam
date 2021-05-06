/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

public class DeleteGotVersionsRequest {

    @JsonProperty("UsageName")
    private String usageName;

    @JsonProperty("SpecificVersions")
    private List<Integer> specificVersions;

    @JsonProperty("DslQuery")
    private JsonNode dslQuery;

    public DeleteGotVersionsRequest() {
    }

    public DeleteGotVersionsRequest(@JsonProperty(required = true) JsonNode dslQuery,
        @JsonProperty(required = true) String usageName,
        @JsonProperty(required = true) List<Integer> specificVersions) {
        this.dslQuery = dslQuery;
        this.usageName = usageName;
        this.specificVersions = specificVersions;
    }

    public JsonNode getDslQuery() {
        return dslQuery;
    }

    public void setDslQuery(JsonNode dslQuery) {
        this.dslQuery = dslQuery;
    }

    public String getUsageName() {
        return usageName;
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public List<Integer> getSpecificVersions() {
        return specificVersions;
    }

    public void setSpecificVersions(List<Integer> specificVersions) {
        this.specificVersions = specificVersions;
    }
}
