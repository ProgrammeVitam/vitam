/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common.database.parser.request.single;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.single.Select;

/**
 * Request (Select) to MongoDB for Logbook
 */
public class SelectToMongoDb {

    private final SelectParserSingle selectParser;

    /**
     * Constructor from SelectParser
     *
     * @param selectParser
     */
    public SelectToMongoDb(SelectParserSingle selectParser) {
        this.selectParser = selectParser;
    }

    /**
     * @return true if at least one Query is needing Elasticsearch
     */
    public boolean hasFullTextQuery() {
        return selectParser.hasFullTextQuery();
    }

    /**
     * @return True if the hint contains notimeout
     */
    public boolean hintNoTimeout() {
        return selectParser.hintNoTimeout();
    }

    /**
     * @return the Select
     */
    public Select getSelect() {
        return selectParser.getRequest();
    }

    /**
     * Used by the Data Engine (cache, nocache, notimeout (noCursorTimeout(noCursorTimeout)))
     *
     * @return the array of hints (if any)
     */
    public ArrayNode getHints() {
        return (ArrayNode) selectParser.getRequest().getFilter()
            .get(SELECTFILTER.HINT.exactToken());
    }

    /**
     * FindIterable.limit(limit)
     *
     * @return the limit
     */
    public int getFinalLimit() {
        final JsonNode node = selectParser.getRequest().getFilter()
            .get(SELECTFILTER.LIMIT.exactToken());
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
        final JsonNode node = selectParser.getRequest().getFilter()
            .get(SELECTFILTER.OFFSET.exactToken());
        if (node != null) {
            return node.asInt();
        }
        return 0;
    }

    /**
     * FindIterable.sort(orderby)
     *
     * @return the orderBy MongoDB command
     */
    public Bson getFinalOrderBy() {
        final JsonNode orderby = selectParser.getRequest().getFilter()
            .get(SELECTFILTER.ORDERBY.exactToken());
        if (orderby == null) {
            return null;
        }
        final List<String> asc = new ArrayList<String>();
        final List<String> desc = new ArrayList<String>();
        final Iterator<Entry<String, JsonNode>> iterator = orderby.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().asInt() > 0) {
                asc.add(entry.getKey());
            } else {
                desc.add(entry.getKey());
            }
        }
        if (asc.isEmpty()) {
            if (desc.isEmpty()) {
                return null;
            }
            final Bson sort = Sorts.descending(desc);
            desc.clear();
            return sort;
        } else if (desc.isEmpty()) {
            final Bson sort = Sorts.ascending(asc);
            asc.clear();
            return sort;
        } else {
            final Bson sort = Sorts.orderBy(Sorts.ascending(asc), Sorts.descending(desc));
            desc.clear();
            asc.clear();
            return sort;
        }
    }

    /**
     * FindIterable.projection(projection)
     *
     * @return the projection
     */
    public Bson getFinalProjection() {
        if (selectParser.getRequest().getAllProjection()) {
            return null;
        }
        final JsonNode node = selectParser.getRequest().getProjection()
            .get(PROJECTION.FIELDS.exactToken());
        final List<String> incl = new ArrayList<String>();
        final List<String> excl = new ArrayList<String>();
        final Iterator<Entry<String, JsonNode>> iterator = node.fields();
        int pos = 0;
        String key = null;
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().isObject()) {
                key = entry.getKey();
                pos = entry.getValue().get("$slice").asInt();
            } else if (entry.getValue().asInt() > 0) {
                incl.add(entry.getKey());
            } else {
                excl.add(entry.getKey());
            }
        }
        if (incl.isEmpty()) {
            if (excl.isEmpty()) {
                if (pos != 0) {
                    return Projections.slice(key, pos);
                }
                return null;
            }
            Bson projection = Projections.exclude(excl);
            if (pos != 0) {
                projection = Projections.fields(projection,
                    Projections.slice(key, pos));
            }
            excl.clear();
            return projection;
        } else if (excl.isEmpty()) {
            Bson projection = Projections.include(incl);
            if (pos != 0) {
                projection = Projections.fields(projection,
                    Projections.slice(key, pos));
            }
            incl.clear();
            return projection;
        } else {
            Bson projection = Projections.fields(Projections.include(incl),
                Projections.exclude(excl));
            if (pos != 0) {
                projection = Projections.fields(projection,
                    Projections.slice(key, pos));
            }
            excl.clear();
            incl.clear();
            return projection;
        }
    }

}
