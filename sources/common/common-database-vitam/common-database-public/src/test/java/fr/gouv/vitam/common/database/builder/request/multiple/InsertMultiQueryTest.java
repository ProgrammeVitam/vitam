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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class InsertMultiQueryTest {

    @Test
    public void testSetMult() {
        final InsertMultiQuery insert = new InsertMultiQuery();
        assertTrue(insert.getFilter().size() == 0);
        insert.setMult(true);
        assertTrue(insert.getFilter().size() == 1);
        insert.setMult(false);
        assertTrue(insert.getFilter().size() == 1);
        assertTrue(insert.getFilter().has(MULTIFILTER.MULT.exactToken()));
        insert.resetFilter();
        assertTrue(insert.getFilter().size() == 0);
    }

    @Test
    public void testAddData() {
        final InsertMultiQuery insert = new InsertMultiQuery();
        assertNull(insert.data);
        insert.addData(JsonHandler.createObjectNode().put("var1", 1));
        insert.addData(JsonHandler.createObjectNode().put("var2", "val"));
        assertEquals(2, insert.data.size());
        insert.resetData();
        assertEquals(0, insert.data.size());
    }

    @Test
    public void testAddRequests() {
        final InsertMultiQuery insert = new InsertMultiQuery();
        assertTrue(insert.queries.isEmpty());
        try {
            insert.addQueries(
                new BooleanQuery(QUERY.AND).add(new ExistsQuery(QUERY.EXISTS, "varA")).setRelativeDepthLimit(5)
            );
            insert.addQueries(
                new PathQuery("path1", "path2"),
                new ExistsQuery(QUERY.EXISTS, "varB").setExactDepthLimit(10)
            );
            insert.addQueries(new PathQuery("path3"));
            assertEquals(4, insert.queries.size());
            insert.resetQueries();
            assertEquals(0, insert.queries.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetFinalInsert() {
        final InsertMultiQuery insert = new InsertMultiQuery();
        assertTrue(insert.queries.isEmpty());
        try {
            insert.addQueries(new PathQuery("path3"));
            assertEquals(1, insert.queries.size());
            insert.setMult(true);
            insert.addData(JsonHandler.createObjectNode().put("var1", 1));
            final ObjectNode node = insert.getFinalInsert();
            assertEquals(4, node.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSetData() throws InvalidParseOperationException {
        final InsertMultiQuery insert = new InsertMultiQuery();
        assertNull(insert.data);
        assertEquals(JsonHandler.createObjectNode(), insert.getData());
        insert.resetData();
        assertNull(insert.data);
        insert.reset();
        assertNull(insert.data);
        final JsonNode data1 = JsonHandler.createObjectNode().put("var1", 1);
        final JsonNode data2 = JsonHandler.createObjectNode().put("var2", 2);
        insert.setData(data1);
        assertEquals(1, insert.data.size());
        assertEquals(JsonHandler.createObjectNode().put("var1", 1), insert.getData());
        insert.setData(data2);
        assertEquals(2, insert.data.size());
        insert.reset();
        assertEquals(0, insert.data.size());
    }

    @Test
    public void testParseData() throws InvalidParseOperationException {
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.setMult(true);
        insert.resetFilter();

        final String data = "{'var1':1}";
        insert.parseData(data);
        final String res = "INSERT: Requests: \n\tFilter: {}\n\tRoots: []\n\tData: {\"var1\":1}";
        assertEquals(res, insert.toString());
    }

    @Test
    public void testSetMult1() throws InvalidParseOperationException {
        final InsertMultiQuery insert = new InsertMultiQuery();
        final JsonNode filt1 = JsonHandler.createObjectNode().put("$mult", "true");
        insert.setFilter(filt1);
        assertEquals("{\"$mult\":\"true\"}", insert.getFilter().toString());
    }
}
