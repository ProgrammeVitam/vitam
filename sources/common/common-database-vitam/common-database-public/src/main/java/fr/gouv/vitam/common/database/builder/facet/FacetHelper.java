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

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;

import java.util.List;
import java.util.Map;

/**
 * Facet helper
 */
public class FacetHelper {

    /**
     * Constructor
     */
    protected FacetHelper() {
        // empty
    }

    /**
     * Create a facet
     *
     * @param name name of the facet
     * @param field field of facet data
     * @param nestdPath nested path of field of facet data
     * @param size size of the facet
     * @param order order of the facet
     * @return a Facet
     * @throws InvalidCreateOperationException when creating facet errors
     */
    public static final Facet terms(String name, String field, String nestdPath, Integer size, FacetOrder order)
        throws InvalidCreateOperationException {
        return new TermsFacet(name, field, nestdPath, size, order);
    }

    /**
     * Create a facet
     *
     * @param name name of the facet
     * @param field field of facet data
     * @param size size of the facet
     * @param order order of the facet
     * @return a Facet
     * @throws InvalidCreateOperationException when creating facet errors
     */
    public static final Facet terms(String name, String field, Integer size, FacetOrder order)
        throws InvalidCreateOperationException {
        return new TermsFacet(name, field, size, order);
    }

    /**
     * Create a date range facet
     *
     * @param name name of the facet
     * @param field field of facet data
     * @param nestedPath nested path of field of facet data
     * @param dateFormat the date format for the ranges of the facet
     * @param ranges
     * @return
     * @throws InvalidCreateOperationException
     */
    public static final Facet dateRange(
        String name,
        String field,
        String nestedPath,
        String dateFormat,
        List<RangeFacetValue> ranges
    ) throws InvalidCreateOperationException {
        return new DateRangeFacet(name, field, nestedPath, dateFormat, ranges);
    }

    /**
     * Create a date range facet
     *
     * @param name name of the facet
     * @param field field of facet data
     * @param dateFormat the date format for the ranges of the facet
     * @param ranges
     * @return
     * @throws InvalidCreateOperationException
     */
    public static final Facet dateRange(String name, String field, String dateFormat, List<RangeFacetValue> ranges)
        throws InvalidCreateOperationException {
        return new DateRangeFacet(name, field, dateFormat, ranges);
    }

    /**
     * Create a filters facet
     *
     * @param name name of the facet
     * @param filters map of named filer queries
     * @return a Facet
     * @throws InvalidCreateOperationException when creating facet errors
     */
    public static final Facet filters(String name, Map<String, Query> filters) throws InvalidCreateOperationException {
        return new FiltersFacet(name, filters);
    }
}
