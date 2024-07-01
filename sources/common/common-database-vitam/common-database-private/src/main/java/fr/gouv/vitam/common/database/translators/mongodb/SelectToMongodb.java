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
package fr.gouv.vitam.common.database.translators.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Select to MongoDb
 */
public class SelectToMongodb extends RequestToMongodb {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SelectToMongodb.class);
    private static final String UNSUPPORTED_PROJECTION = "Unsupported DSL projection node: '%s'";
    private static final String SLICE_KEYWORD = "$slice";
    private static final int REQUIRED_SLICE_ARRAY_SIZE = 2;

    /**
     * @param selectParser AbstractParser of unknown type
     */
    public SelectToMongodb(AbstractParser<?> selectParser) {
        super(selectParser);
    }

    /**
     * FindIterable.sort(orderby) for MongoDb
     *
     * @return the orderBy MongoDB command
     */
    public Bson getFinalOrderBy() {
        final JsonNode orderby = requestParser.getRequest().getFilter().get(SELECTFILTER.ORDERBY.exactToken());
        if (orderby == null || orderby.size() == 0 || orderby.fields().hasNext() == false) {
            return null;
        }
        final List<Bson> sorts = new ArrayList<>(orderby.size());
        final Iterator<Entry<String, JsonNode>> iterator = orderby.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            if (entry.getValue().asInt() > 0) {
                sorts.add(Sorts.ascending(entry.getKey()));
            } else {
                sorts.add(Sorts.descending(entry.getKey()));
            }
        }
        return Sorts.orderBy(sorts);
    }

    /**
     * @return the Select Single
     */
    public Select getSingleSelect() {
        return (Select) requestParser.getRequest();
    }

    /**
     * @return True if #id was in the primary projection or empty projection
     */
    public boolean idWasInProjection() {
        if (requestParser.getRequest().getAllProjection()) {
            return true;
        }
        final JsonNode node = requestParser.getRequest().getProjection().get(PROJECTION.FIELDS.exactToken());
        if (!node.fieldNames().hasNext()) {
            return true;
        }
        JsonNode value = node.get(VitamDocument.ID);
        if (value != null && value instanceof NumericNode) {
            if (value.asInt() > 0) {
                return true;
            } else {
                return false;
            }
        }
        if (requestParser.getRequest().getProjection().elements().hasNext()) {
            return false;
        }
        return true;
    }

    /**
     * @return True if the score is included, or false if explicitly excluded
     */
    public boolean isScoreIncluded() {
        if (requestParser.getRequest().getAllProjection()) {
            return true;
        }
        final JsonNode node = requestParser.getRequest().getProjection().get(PROJECTION.FIELDS.exactToken());
        JsonNode score = node.get(VitamDocument.SCORE);
        return (score == null || score.asInt() > 0);
    }

    /**
     * FindIterable.projection(projection)
     *
     * @return the projection
     * @throws InvalidParseOperationException when getting invalid projection request
     */
    public Bson getFinalProjection() throws InvalidParseOperationException {
        if (requestParser.getRequest().getAllProjection()) {
            return null;
        }
        final JsonNode node = requestParser.getRequest().getProjection().get(PROJECTION.FIELDS.exactToken());
        final List<String> incl = new ArrayList<>();
        final List<String> excl = new ArrayList<>();
        final Map<String, ObjectNode> sliceProjections = new HashMap<>();
        final Iterator<Entry<String, JsonNode>> iterator = node.fields();
        boolean idFound = false;
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            final JsonNode value = entry.getValue();
            if (value instanceof NumericNode) {
                if (value.asInt() > 0) {
                    if (entry.getKey().equals(VitamDocument.ID)) {
                        idFound = true;
                    }
                    incl.add(entry.getKey());
                } else {
                    if (entry.getKey().equals(VitamDocument.ID)) {
                        continue;
                    }
                    excl.add(entry.getKey());
                }
            } else if (value instanceof ObjectNode && value.has(SLICE_KEYWORD)) {
                sliceProjections.put(entry.getKey(), (ObjectNode) value);
            } else {
                throw new InvalidParseOperationException(
                    String.format(UNSUPPORTED_PROJECTION, JsonHandler.writeAsString(value))
                );
            }
        }

        if (incl.isEmpty() && excl.isEmpty() && sliceProjections.isEmpty()) {
            return null;
        }

        // TODO : Mongo do not accept inclusions and exclusions in query projections,
        //  so please add : !excl.isEmpty() in condition !!
        if (!idFound) {
            incl.add(VitamDocument.ID);
        }

        return computeBsonProjection(incl, excl, sliceProjections);
    }

    private Bson computeBsonProjection(List<String> incl, List<String> excl, Map<String, ObjectNode> sliceProjections)
        throws InvalidParseOperationException {
        tempFixProjectionPathCollision(incl);
        tempFixProjectionPathCollision(excl);

        final List<Bson> projections = new ArrayList<>();
        if (!incl.isEmpty()) {
            projections.add(Projections.include(incl));
            incl.clear();
        }
        if (!excl.isEmpty()) {
            projections.add(Projections.exclude(excl));
            excl.clear();
        }
        if (!sliceProjections.isEmpty()) {
            for (final Entry<String, ObjectNode> sliceEntry : sliceProjections.entrySet()) {
                final String fieldName = sliceEntry.getKey();
                final ObjectNode sliceNode = sliceEntry.getValue();
                checkSliceValue(sliceNode);
                final JsonNode sliceValueNode = sliceNode.get(SLICE_KEYWORD);
                if (sliceValueNode instanceof ArrayNode) {
                    projections.add(
                        Projections.slice(fieldName, sliceValueNode.get(0).asInt(), sliceValueNode.get(1).asInt())
                    );
                } else {
                    projections.add(Projections.slice(fieldName, sliceValueNode.asInt()));
                }
            }
            sliceProjections.clear();
        }
        return Projections.fields(projections);
    }

    /**
     * @deprecated Temporary path collision detection / prevention
     * FIXME : #9847 Fix logbook projections
     */
    private void tempFixProjectionPathCollision(List<String> incl) {
        List<String> allKeys = new ArrayList<>(incl);
        allKeys.sort(Comparator.naturalOrder());
        for (int i = 0; i < allKeys.size(); i++) {
            String key = allKeys.get(i);
            for (int j = i + 1; j < allKeys.size(); j++) {
                String nextKey = allKeys.get(j);
                if (key.length() > nextKey.length() || !nextKey.startsWith(key)) {
                    continue;
                }
                if (nextKey.length() == key.length()) {
                    LOGGER.warn(
                        "INVALID PROJECTION. IGNORING DUPLICATE KEY '" +
                        key +
                        "'. THIS WILL BE NO MORE SUPPORTED IS FUTURE RELEASES."
                    );
                    incl.remove(nextKey);
                } else if (nextKey.charAt(key.length()) == '.') {
                    LOGGER.warn(
                        "INVALID PROJECTION. IGNORING KEY PATH COLLISION '" +
                        key +
                        "' VS '" +
                        nextKey +
                        "'. THIS WILL BE NO MORE SUPPORTED IS FUTURE RELEASES."
                    );
                    incl.remove(nextKey);
                }
            }
        }
    }

    private void checkSliceValue(ObjectNode sliceNode) throws InvalidParseOperationException {
        final JsonNode sliceValueNode = sliceNode.get(SLICE_KEYWORD);
        if (sliceValueNode instanceof ArrayNode) {
            final ArrayNode sliceValueArray = (ArrayNode) sliceValueNode;
            if (!isLegalSliceArray(sliceValueArray)) {
                throw new InvalidParseOperationException(
                    String.format(UNSUPPORTED_PROJECTION, JsonHandler.writeAsString(sliceNode))
                );
            }
        } else if (!(sliceValueNode instanceof NumericNode)) {
            throw new InvalidParseOperationException(
                String.format(UNSUPPORTED_PROJECTION, JsonHandler.writeAsString(sliceNode))
            );
        }
    }

    private boolean isLegalSliceArray(ArrayNode sliceValueArray) {
        if (sliceValueArray == null || sliceValueArray.size() != REQUIRED_SLICE_ARRAY_SIZE) {
            return false;
        }
        for (final JsonNode jsonNode : sliceValueArray) {
            if (!(jsonNode instanceof NumericNode)) {
                return false;
            }
        }
        return true;
    }
}
