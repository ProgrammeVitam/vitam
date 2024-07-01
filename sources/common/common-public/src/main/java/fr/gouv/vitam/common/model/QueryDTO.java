/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

/**
 * Query DTO class
 */
public class QueryDTO {

    @JsonProperty("$roots")
    private List<String> roots;

    @JsonProperty("$query")
    private List<JsonNode> query;

    @JsonProperty("$filter")
    private QueryFilter filter;

    @JsonProperty("$projection")
    private QueryProjection projection;

    /**
     * Default Constructor
     */
    public QueryDTO() {
        // nothing
    }

    /**
     * @return roots value
     */
    public List<String> getRoots() {
        return roots;
    }

    /**
     * @param roots
     */
    public void setRoots(List<String> roots) {
        this.roots = roots;
    }

    /**
     * @return the query
     */
    public List<JsonNode> getQuery() {
        return query;
    }

    /**
     * @param query
     */
    public void setQuery(List<JsonNode> query) {
        this.query = query;
    }

    /**
     * @return the Query Filter
     */
    public QueryFilter getFilter() {
        return filter;
    }

    /**
     * @param filter
     */
    public void setFilter(QueryFilter filter) {
        this.filter = filter;
    }

    /**
     * @return the Projection
     */
    public QueryProjection getProjection() {
        return projection;
    }

    /**
     * @param projection
     */
    public void setProjection(QueryProjection projection) {
        this.projection = projection;
    }
}
