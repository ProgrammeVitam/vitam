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
package fr.gouv.vitam.common.database.translators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.AbstractRequest;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;

import java.util.List;

/**
 * Request To X Abstract class. All translators should be based on this one.
 */
public class RequestToAbstract {

    protected AbstractParser<?> requestParser;

    /**
     * @param requestParser AbstractParser in unknown type
     */
    public RequestToAbstract(AbstractParser<?> requestParser) {
        this.requestParser = requestParser;
    }

    /**
     * @return the associated RequestParser
     */
    public AbstractParser<?> getRequestParser() {
        return requestParser;
    }

    /**
     * @return true if at least one Query is needing Elasticsearch
     */
    public boolean hasFullTextQuery() {
        return requestParser.hasFullTextQuery();
    }

    /**
     * @return True if the hint contains cache
     */
    public boolean hintCache() {
        return requestParser.hintCache();
    }

    /**
     * @return True if the hint contains notimeout
     */
    public boolean hintNoTimeout() {
        return requestParser.hintNoTimeout();
    }

    /**
     * @return the model between Units/ObjectGroups/Objects (in that order)
     */
    public FILTERARGS model() {
        return requestParser.model();
    }

    /**
     * @return The possible maximum depth
     */
    public int getLastDepth() {
        return requestParser.getLastDepth();
    }

    /**
     * @return the Request
     */
    public AbstractRequest getRequest() {
        return requestParser.getRequest();
    }

    /**
     * @return the number of queries
     */
    public int getNbQueries() {
        return requestParser.getRequest().getNbQueries();
    }

    /**
     * @return True if this request is a "multiple" result request
     */
    public boolean isMultiple() {
        final ObjectNode filter = requestParser.getRequest().getFilter();
        if (filter != null) {
            final JsonNode mult = filter.get(MULTIFILTER.MULT.exactToken());
            if (mult != null) {
                return mult.asBoolean();
            }
        }
        return false;
    }

    /**
     * Used by the Data Engine (cache, nocache, notimeout (noCursorTimeout(noCursorTimeout)))
     *
     * @return the array of hints (if any)
     */
    public ArrayNode getHints() {
        return (ArrayNode) requestParser.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken());
    }

    /**
     * FindIterable.limit(limit)
     *
     * @return the limit
     */
    public int getFinalLimit() {
        final JsonNode node = requestParser.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return GlobalDatas.LIMIT_LOAD;
    }

    /**
     * FindIterable.skip(offset)
     *
     * @return the offset
     */
    public int getFinalOffset() {
        final JsonNode node = requestParser.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return 0;
    }

    /**
     * @return the associated usage if any
     */
    public String getUsage() {
        final JsonNode node =
            ((SelectParserMultiple) requestParser).getRequest().getProjection().get(PROJECTION.USAGE.exactToken());
        if (node != null) {
            return node.asText();
        }
        return null;
    }

    /**
     * @param nth int
     * @return the nth Query
     */
    public final Query getNthQuery(int nth) {
        final List<Query> list = requestParser.getRequest().getQueries();
        if (nth >= list.size()) {
            throw new IllegalAccessError("This Query has not enough item to get the position: " + nth);
        }
        return list.get(nth);
    }
}
