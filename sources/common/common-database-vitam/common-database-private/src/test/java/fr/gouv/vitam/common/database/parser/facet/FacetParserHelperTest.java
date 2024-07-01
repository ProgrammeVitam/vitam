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

import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.facet.RangeFacetValue;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.dateRange;
import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.filters;
import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.terms;
import static fr.gouv.vitam.common.database.parser.facet.FacetParserHelper.dateRange;
import static fr.gouv.vitam.common.database.parser.facet.FacetParserHelper.filters;
import static fr.gouv.vitam.common.database.parser.facet.FacetParserHelper.terms;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class FacetParserHelperTest {

    VarNameAdapter noAdapter = new VarNameAdapter();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testTerms() {
        try {
            // basic terms
            Facet facet1 = terms("facet1", "var1", 2, FacetOrder.ASC);
            Facet facet2 = terms(facet1.getCurrentFacet(), noAdapter);
            assertEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
            // terms with size
            facet1 = terms("facet1", "var1", 1, FacetOrder.ASC);
            facet2 = terms(facet1.getCurrentFacet(), noAdapter);
            assertEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
            // different facet by size
            facet1 = terms("facet1", "var1", 3, FacetOrder.ASC);
            assertNotEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testTermsWithNestedField() {
        try {
            // basic terms
            Facet facet1 = terms("facet1", "path.to.var1", "path.to", 2, FacetOrder.ASC);
            Facet facet2 = terms(facet1.getCurrentFacet(), noAdapter);
            assertEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilters() throws InvalidCreateOperationException, InvalidParseOperationException {
        // basic filters
        Map<String, Query> filters = new HashMap<>();
        filters.put("query_1", QueryHelper.eq("var1", 1));
        filters.put("query_2", QueryHelper.and().add(QueryHelper.match("var2", "value")));
        Facet facet1 = filters("facet1", filters);
        Facet facet2 = filters(facet1.getCurrentFacet(), noAdapter);
        assertEquals("String shall be equal", facet1.getCurrentFacet().toString(), facet2.getCurrentFacet().toString());
        // different facet by query number
        filters.remove("query_2");
        facet1 = filters("facet1", filters);
        assertNotEquals(
            "String shall be equal",
            facet1.getCurrentFacet().toString(),
            facet2.getCurrentFacet().toString()
        );
        // null filters
        assertThatThrownBy(() -> {
            filters("facet1", null);
        }).isInstanceOf(InvalidCreateOperationException.class);
        // empty filters
        assertThatThrownBy(() -> {
            filters("facet1", new HashMap<>());
        }).isInstanceOf(InvalidCreateOperationException.class);
    }

    @Test
    public void testDateRangeOk() {
        try {
            List<RangeFacetValue> ranges = new ArrayList<>();
            ranges.add(new RangeFacetValue("from", null));
            Facet facet1 = dateRange("facet1", "EndDate", "yyyy", ranges);
            Facet facet2 = dateRange(facet1.getCurrentFacet(), noAdapter);
            assertEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
            assertEquals("$date_range", facet1.getCurrentTokenFACET().exactToken());
            assertEquals("$date_range", facet2.getCurrentTokenFACET().exactToken());
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDateRangeOkWithNestedField() {
        try {
            List<RangeFacetValue> ranges = new ArrayList<>();
            ranges.add(new RangeFacetValue("from", null));
            Facet facet1 = dateRange("facet1", "path.to.EndDate", "path.to", "yyyy", ranges);
            Facet facet2 = dateRange(facet1.getCurrentFacet(), noAdapter);
            assertEquals(
                "String shall be equal",
                facet1.getCurrentFacet().toString(),
                facet2.getCurrentFacet().toString()
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void testDateRangeKoNoRanges() throws InvalidParseOperationException, InvalidCreateOperationException {
        List<RangeFacetValue> ranges = new ArrayList<>();
        Facet facet1 = dateRange("facet1", "EndDate", "yyyy", ranges);
        Facet facet2 = dateRange(facet1.getCurrentFacet(), noAdapter);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void testDateRangeKoNullRangeValue() throws InvalidParseOperationException, InvalidCreateOperationException {
        List<RangeFacetValue> ranges = new ArrayList<>();
        ranges.add(new RangeFacetValue(null, null));
        Facet facet1 = dateRange("facet1", "EndDate", "yyyy", ranges);
    }

    @Test(expected = InvalidCreateOperationException.class)
    public void testDateRangeKoNoField() throws InvalidParseOperationException, InvalidCreateOperationException {
        List<RangeFacetValue> ranges = new ArrayList<>();
        ranges.add(new RangeFacetValue("from", "to"));
        Facet facet1 = dateRange("facet1", "", "yyyy", ranges);
    }
}
