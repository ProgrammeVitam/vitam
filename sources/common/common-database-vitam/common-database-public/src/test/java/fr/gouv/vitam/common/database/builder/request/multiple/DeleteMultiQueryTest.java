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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class DeleteMultiQueryTest {

    @Test
    public void testSetMult() {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        assertTrue(delete.getFilter().size() == 0);
        delete.setMult(true);
        assertTrue(delete.getFilter().size() == 1);
        delete.setMult(false);
        assertTrue(delete.getFilter().size() == 1);
        assertTrue(delete.getFilter().has(MULTIFILTER.MULT.exactToken()));
        delete.resetFilter();
        assertTrue(delete.getFilter().size() == 0);
    }

    @Test
    public void testAddRequests() {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        assertTrue(delete.queries.isEmpty());
        try {
            delete.addQueries(
                new BooleanQuery(QUERY.AND).add(new ExistsQuery(QUERY.EXISTS, "varA")).setRelativeDepthLimit(5)
            );
            delete.addQueries(
                new PathQuery("path1", "path2"),
                new ExistsQuery(QUERY.EXISTS, "varB").setExactDepthLimit(10)
            );
            delete.addQueries(new PathQuery("path3"));
            assertEquals(4, delete.queries.size());
            delete.resetQueries();
            assertEquals(0, delete.queries.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetFinalDelete() {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        assertTrue(delete.queries.isEmpty());
        try {
            delete.addQueries(new PathQuery("path3"));
            assertEquals(1, delete.queries.size());
            delete.setMult(true);
            final ObjectNode node = delete.getFinalDelete();
            assertEquals(3, node.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAllSet() throws InvalidParseOperationException {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.setFilter(JsonHandler.createObjectNode().put("$mult", "true"));
        assertEquals("{\"$mult\":\"true\"}", delete.getFilter().toString());
    }

    @Test
    public void testToString() throws InvalidCreateOperationException {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.addQueries(new ExistsQuery(QUERY.EXISTS, "var1"));
        delete.setMult(true);
        delete.resetFilter();
        final String s = "DELETE: Requests: \n{\"$exists\":\"var1\"}\n\tFilter: {}\n\tRoots: []";
        assertEquals(s, delete.toString());
    }
}
