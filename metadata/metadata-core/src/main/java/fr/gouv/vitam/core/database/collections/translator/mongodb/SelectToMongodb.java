/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections.translator.mongodb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.parser.request.parser.SelectParser;

/**
 * Select to MongoDb
 *
 */
public class SelectToMongodb extends RequestToMongodb {

    /**
     * @param selectParser
     */
    public SelectToMongodb(SelectParser selectParser) {
        super(selectParser);
    }

    /**
     * FindIterable.sort(orderby)
     * 
     * @return the orderBy MongoDB command
     */
    public Bson getFinalOrderBy() {
        JsonNode orderby = requestParser.getRequest().getFilter()
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
            Bson sort = Sorts.descending(desc);
            desc.clear();
            return sort;
        } else if (desc.isEmpty()) {
            Bson sort = Sorts.ascending(asc);
            asc.clear();
            return sort;
        } else {
            Bson sort = Sorts.orderBy(Sorts.ascending(asc), Sorts.descending(desc));
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
    	if (((SelectParser) requestParser).getRequest().getAllProjection()) {
    		return null;
    	}
        JsonNode node = ((SelectParser) requestParser).getRequest().getProjection()
                .get(PROJECTION.FIELDS.exactToken());
        final List<String> incl = new ArrayList<String>();
        final List<String> excl = new ArrayList<String>();
        final Iterator<Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().asInt() > 0) {
                incl.add(entry.getKey());
            } else {
                excl.add(entry.getKey());
            }
        }
        if (incl.isEmpty()) {
            if (excl.isEmpty()) {
                return null;
            }
            Bson projection = Projections.exclude(excl);
            excl.clear();
            return projection;
        } else if (excl.isEmpty()) {
            Bson projection = Projections.include(incl);
            incl.clear();
            return projection;
        } else {
            Bson projection = Projections.fields(Projections.include(incl),
                    Projections.exclude(excl));
            excl.clear();
            incl.clear();
            return projection;
        }
    }
}
