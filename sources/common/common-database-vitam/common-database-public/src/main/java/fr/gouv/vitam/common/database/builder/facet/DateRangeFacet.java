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
package fr.gouv.vitam.common.database.builder.facet;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.List;

/**
 * Date Range facet
 */
public class DateRangeFacet extends Facet {

    /**
     * Date Range facet constructor
     *
     * @param name
     * @param field
     * @param nestdPath
     * @param dateFormat
     * @param ranges
     * @throws InvalidCreateOperationException
     */
    public DateRangeFacet(String name, String field, String nestdPath, String dateFormat, List<RangeFacetValue> ranges)
        throws InvalidCreateOperationException {
        super(name);
        populateFacet(name, field, nestdPath, dateFormat, ranges);
    }

    /**
     * Date Range facet constructor
     *
     * @param name
     * @param field
     * @param dateFormat
     * @param ranges
     * @throws InvalidCreateOperationException
     */
    public DateRangeFacet(String name, String field, String dateFormat, List<RangeFacetValue> ranges)
        throws InvalidCreateOperationException {
        super(name);
        populateFacet(name, field, null, dateFormat, ranges);
    }

    private void populateFacet(
        String name,
        String field,
        String nestdPath,
        String dateFormat,
        List<RangeFacetValue> ranges
    ) throws InvalidCreateOperationException {
        setName(name);
        currentTokenFACET = FACET.DATE_RANGE;
        if (name == null || name.isEmpty()) {
            throw new InvalidCreateOperationException("name value is requested");
        }
        if (field == null || field.isEmpty()) {
            throw new InvalidCreateOperationException("field value is requested");
        }
        if (dateFormat == null || dateFormat.isEmpty()) {
            throw new InvalidCreateOperationException("dateFormat value is requested");
        }
        if (ranges == null || ranges.size() <= 0) {
            throw new InvalidCreateOperationException("Ranges must be > 0 ");
        }

        ObjectNode facetNode = JsonHandler.createObjectNode();
        facetNode.put(FACETARGS.FIELD.exactToken(), field);
        if (nestdPath != null) {
            facetNode.put(FACETARGS.SUBOBJECT.exactToken(), nestdPath);
        }
        facetNode.put(FACETARGS.FORMAT.exactToken(), dateFormat);
        ArrayNode rangesNode = JsonHandler.createArrayNode();
        for (RangeFacetValue item : ranges) {
            ObjectNode rangeNode = JsonHandler.createObjectNode();
            if (
                (item.getFrom() == null || item.getFrom().isEmpty()) && (item.getTo() == null || item.getTo().isEmpty())
            ) throw new InvalidCreateOperationException("Either a 'from' or a 'to' value are requested");
            rangeNode.put(FACETARGS.FROM.exactToken(), item.getFrom());
            rangeNode.put(FACETARGS.TO.exactToken(), item.getTo());
            rangesNode.add(rangeNode);
        }
        facetNode.set(FACETARGS.RANGES.exactToken(), rangesNode);

        currentFacet = facetNode;
    }
}
