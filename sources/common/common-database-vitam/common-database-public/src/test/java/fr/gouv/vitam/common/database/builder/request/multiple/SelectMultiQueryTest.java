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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.facet.TermsFacet;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SelectMultiQueryTest
 */
public class SelectMultiQueryTest {

    @Test
    public void testAddLimitFilter() {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.setLimitFilter(0, 0);
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(1, 0);
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertTrue(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(0, 1);
        assertTrue(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.setLimitFilter(1, 1);
        assertTrue(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertTrue(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
        select.resetLimitFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.LIMIT.exactToken()));
        assertFalse(select.getFilter().has(SELECTFILTER.OFFSET.exactToken()));
    }

    @Test
    public void testAddHintFilter() {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
        assertEquals(1, select.getFilter().get(SELECTFILTER.HINT.exactToken()).size());
        try {
            select.addHintFilter(FILTERARGS.NOCACHE.exactToken());
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
        assertEquals(2, select.getFilter().get(SELECTFILTER.HINT.exactToken()).size());
        select.resetHintFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.HINT.exactToken()));
    }

    @Test
    public void testAddOrderByAscFilter() {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addOrderByAscFilter("var1", "var2");
            assertEquals(2, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByAscFilter("var3").addOrderByAscFilter("var4");
            assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByDescFilter("var1", "var2");
            assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
            select.addOrderByDescFilter("var3").addOrderByDescFilter("var4");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(4, select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).size());
        select.resetOrderByFilter();
        assertFalse(select.getFilter().has(SELECTFILTER.ORDERBY.exactToken()));
    }

    @Test
    public void testAddUsedProjection() {
        final SelectMultiQuery select = new SelectMultiQuery();
        assertTrue(select.getProjection().size() == 0);
        try {
            select.addUsedProjection("var1", "var2");
            assertEquals(2, select.getProjection().get(PROJECTION.FIELDS.exactToken()).size());
            select.addUsedProjection("var3").addUsedProjection("var4");
            assertEquals(4, select.getProjection().get(PROJECTION.FIELDS.exactToken()).size());
            select.addUnusedProjection("var1", "var2");
            // used/unused identical so don't change the number
            assertEquals(4, select.getProjection().get(PROJECTION.FIELDS.exactToken()).size());
            select.addUnusedProjection("var3").addUnusedProjection("var4");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertEquals(4, select.getProjection().get(PROJECTION.FIELDS.exactToken()).size());
        select.resetUsedProjection();
        assertFalse(select.getProjection().has(PROJECTION.FIELDS.exactToken()));
    }

    @Test
    public void testAddUsageProjection() {
        final SelectMultiQuery select = new SelectMultiQuery();
        assertTrue(select.getProjection().size() == 0);
        try {
            select.setUsageProjection("usage");
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(select.getProjection().has(PROJECTION.USAGE.exactToken()));
        select.resetUsageProjection();
        assertFalse(select.getProjection().has(PROJECTION.USAGE.exactToken()));
    }

    @Test
    public void testAddRequests() {
        final SelectMultiQuery select = new SelectMultiQuery();
        assertTrue(select.queries.isEmpty());
        try {
            select.addQueries(
                new BooleanQuery(QUERY.AND).add(new ExistsQuery(QUERY.EXISTS, "varA")).setRelativeDepthLimit(5)
            );
            select.addQueries(
                new PathQuery("path1", "path2"),
                new ExistsQuery(QUERY.EXISTS, "varB").setExactDepthLimit(10)
            );
            select.addQueries(new PathQuery("path3"));
            assertEquals(4, select.getQueries().size());

            select.setLimitFilter(10, 10);
            try {
                select.addHintFilter(FILTERARGS.CACHE.exactToken());
                select.addOrderByAscFilter("var1").addOrderByDescFilter("var2");
                select.addUsedProjection("var3").addUnusedProjection("var4");
                select.setUsageProjection("usageId");
                ObjectNode node = select.getFinalSelect();
                assertEquals(5, node.size());
                assertEquals(0, select.getRoots().size());
                select.addRoots("root1", "root2");
                assertEquals(2, select.getRoots().size());
                node = select.getFinalSelect();
                assertEquals(5, node.size());
                select.resetQueries();
                assertEquals(0, select.getQueries().size());
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddFacets() {
        final SelectMultiQuery select = new SelectMultiQuery();
        assertTrue(select.facets.isEmpty());
        try {
            select.addFacets(
                new TermsFacet("myFacet1", "myField1", 5, FacetOrder.ASC),
                new TermsFacet("myFacet2", "myField2", 10, FacetOrder.ASC)
            );
            select.addFacets(new TermsFacet("myFacet3", "myField3", 5, FacetOrder.ASC));
            assertEquals(3, select.getFacets().size());
            select.setFacet(new TermsFacet("myFacet1", "myField1", 1, FacetOrder.DESC));
            assertEquals(1, select.getFacets().size());
            select.resetFacets();
            assertEquals(0, select.getFacets().size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // add null facet
        assertThatThrownBy(() -> {
            final SelectMultiQuery selectNull = new SelectMultiQuery();
            selectNull.addFacets((Facet) null);
        }).isInstanceOf(IllegalArgumentException.class);

        // add empty facet array
        assertThatCode(() -> {
            final SelectMultiQuery selectNull = new SelectMultiQuery();
            selectNull.addFacets(new Facet[0]);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testAllReset() throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addUsedProjection("var1");
        select.setUsageProjection("usageId1");
        select.addFacets(new TermsFacet("myFacet1", "myField1", 2, FacetOrder.ASC));
        assertEquals(2, select.getProjection().size());
        assertEquals(1, select.getFacets().size());
        select.reset();
        assertEquals(0, select.getProjection().size());
        assertEquals(0, select.getFacets().size());
    }

    @Test
    public void testParser() throws InvalidParseOperationException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.parseOrderByFilter("{$orderby : { maclef1 : 1 , maclef2 : -1 }}");
        select.parseLimitFilter("{$limit : 5}");
        assertEquals(
            "{\"maclef1\":1,\"maclef2\":-1}",
            select.getFilter().get(SELECTFILTER.ORDERBY.exactToken()).toString()
        );
        assertEquals("5", select.getFilter().get(SELECTFILTER.LIMIT.exactToken()).toString());
        select.resetFilter();
        select.parseFilter("{$orderby : { maclef1 : 1 , maclef2 : -1 }, $limit : 5}");
        select.parseProjection("{$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' }");
        assertTrue(select.getAllProjection());
        assertEquals(
            "{\"$fields\":{\"#dua\":1,\"#all\":1},\"$usage\":\"abcdef1234\"}",
            select.getProjection().toString()
        );
        final String s =
            "QUERY: Requests: \n\tFilter: {\"$limit\":5,\"$orderby\":{\"maclef1\":1,\"maclef2\":-1}}" +
            "\n\tRoots: []\n\tProjection: {\"$fields\":{\"#dua\":1,\"#all\":1},\"$usage\":\"abcdef1234\"}\n\tFacets: \n" +
            "\tThreshold: null";
        assertEquals(s, select.toString());
        select.parseRoots("[ 'id0' ]");
        assertEquals(1, select.roots.size());
    }

    @Test
    public void testTrackTotalHits() {
        // Given
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        // When
        selectMultiQuery.trackTotalHits(true);
        selectMultiQuery.setLimitFilter(10, 50);
        ObjectNode finalSelect = selectMultiQuery.getFinalSelect();

        // Then
        ObjectNode expectedFilters = JsonHandler.createObjectNode()
            .put("$offset", 10)
            .put("$limit", 50)
            .put("$track_total_hits", true);
        JsonAssert.assertJsonEquals(expectedFilters, finalSelect.get("$filter"));
    }
}
