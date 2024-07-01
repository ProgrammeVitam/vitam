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

package fr.gouv.vitam.metadata.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.FacetResult;

import java.util.List;

public class MetadataResult {

    private final JsonNode query;

    private final List<JsonNode> results;

    private final List<FacetResult> facetResults;

    private final long total;

    private final String scrollId;

    private final DatabaseCursor hits;

    public MetadataResult(
        JsonNode query,
        List<JsonNode> results,
        List<FacetResult> facetResults,
        long total,
        String scrollId,
        DatabaseCursor hits
    ) {
        this.query = query;
        this.results = results;
        this.facetResults = facetResults;
        this.total = total;
        this.scrollId = scrollId;
        this.hits = hits;
    }

    public JsonNode getQuery() {
        return query;
    }

    public List<JsonNode> getResults() {
        return results;
    }

    public List<FacetResult> getFacetResults() {
        return facetResults;
    }

    public long getTotal() {
        return total;
    }

    public String getScrollId() {
        return scrollId;
    }

    public DatabaseCursor getHits() {
        return hits;
    }
}
