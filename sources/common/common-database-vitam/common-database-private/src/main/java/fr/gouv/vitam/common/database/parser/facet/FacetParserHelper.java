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
package fr.gouv.vitam.common.database.parser.facet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.facet.FacetHelper;
import fr.gouv.vitam.common.database.builder.facet.RangeFacetValue;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.database.parser.query.QueryParserHelper;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Facet from Parser Helper
 */
public class FacetParserHelper extends FacetHelper {

    /**
     * Construction
     */
    protected FacetParserHelper() {}

    /**
     * Transform facet jsonNode in terms Facet object
     *
     * @param facet facet node
     * @param adapter adapter
     * @return terms Facet object
     * @throws InvalidCreateOperationException error while creating terms Facet
     * @throws InvalidParseOperationException error in adapater
     */
    public static final Facet terms(final JsonNode facet, VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final String name = facet.get(FACETARGS.NAME.exactToken()).asText();
        JsonNode terms = facet.get(FACET.TERMS.exactToken());

        if (!terms.has(FACETARGS.ORDER.exactToken())) {
            throw new InvalidCreateOperationException(
                String.format("facet must contain a %s parameter", FACETARGS.ORDER.exactToken())
            );
        }
        if (!terms.has(FACETARGS.SIZE.exactToken())) {
            throw new InvalidCreateOperationException(
                String.format("facet must contain a %s parameter", FACETARGS.SIZE.exactToken())
            );
        }

        String translatedNestedPath = null;
        if (terms.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            String nestedPath = terms.get(FACETARGS.SUBOBJECT.exactToken()).asText();
            translatedNestedPath = adapter.getVariableName(nestedPath);
            if (translatedNestedPath == null) {
                translatedNestedPath = nestedPath;
            }
        }

        String fieldName = terms.get(FACETARGS.FIELD.exactToken()).asText();
        String translatedFieldName = adapter.getVariableName(fieldName);
        if (translatedFieldName == null) {
            translatedFieldName = fieldName;
        }

        Integer size = terms.get(FACETARGS.SIZE.exactToken()).asInt();
        FacetOrder order = FacetOrder.valueOf(terms.get(FACETARGS.ORDER.exactToken()).asText());

        return FacetHelper.terms(name, translatedFieldName, translatedNestedPath, size, order);
    }

    /**
     * Transform facet jsonNode into a dateRange Facet object
     *
     * @param facet
     * @param adapter
     * @return
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    public static final Facet dateRange(final JsonNode facet, VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final String name = facet.get(FACETARGS.NAME.exactToken()).asText();
        JsonNode dateRange = facet.get(FACET.DATE_RANGE.exactToken());
        ArrayNode ranges = (ArrayNode) dateRange.get(FACETARGS.RANGES.exactToken());

        String dateFormat = dateRange.get(FACETARGS.FORMAT.exactToken()).asText();

        List<RangeFacetValue> rangesList = new ArrayList<>();
        ranges.forEach(item -> {
            JsonNode from = item.get(FACETARGS.FROM.exactToken());
            JsonNode to = item.get(FACETARGS.TO.exactToken());
            rangesList.add(
                new RangeFacetValue(
                    from != null && !from.isNull() ? from.asText() : null,
                    to != null && !to.isNull() ? to.asText() : null
                )
            );
        });

        String translatedNestedPath = null;
        if (dateRange.get(FACETARGS.SUBOBJECT.exactToken()) != null) {
            String nestedPath = dateRange.get(FACETARGS.SUBOBJECT.exactToken()).asText();
            translatedNestedPath = adapter.getVariableName(nestedPath);
            if (translatedNestedPath == null) {
                translatedNestedPath = nestedPath;
            }
        }

        String fieldName = dateRange.get(FACETARGS.FIELD.exactToken()).asText();
        String translatedFieldName = adapter.getVariableName(fieldName);
        if (translatedFieldName == null) {
            translatedFieldName = fieldName;
        }

        return FacetHelper.dateRange(name, translatedFieldName, translatedNestedPath, dateFormat, rangesList);
    }

    /**
     * Transform facet jsonNode in filters Facet object
     *
     * @param facet facet node
     * @param adapter adapter
     * @return filters Facet object
     * @throws InvalidCreateOperationException error while creating terms Facet
     * @throws InvalidParseOperationException error in adapater
     */
    public static final Facet filters(final JsonNode facet, VarNameAdapter adapter)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        final String name = facet.get(FACETARGS.NAME.exactToken()).asText();
        JsonNode filtersFacetnode = facet.get(FACET.FILTERS.exactToken());

        Map<String, Query> filters = new HashMap<>();
        ArrayNode filtersNode = (ArrayNode) filtersFacetnode.get(FACETARGS.QUERY_FILTERS.exactToken());
        for (JsonNode node : filtersNode) {
            String key = node.get(FACETARGS.NAME.exactToken()).asText();
            JsonNode queryNode = node.get(FACETARGS.QUERY.exactToken());
            final Entry<String, JsonNode> queryItem = JsonHandler.checkUnicity("RootRequest", queryNode);
            Query query = QueryParserHelper.query(queryItem.getKey(), queryItem.getValue(), adapter);
            filters.put(key, query);
        }
        return FacetHelper.filters(name, filters);
    }
}
