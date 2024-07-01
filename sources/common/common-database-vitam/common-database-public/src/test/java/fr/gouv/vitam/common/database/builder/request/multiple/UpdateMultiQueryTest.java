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
import fr.gouv.vitam.common.database.builder.query.action.AddAction;
import fr.gouv.vitam.common.database.builder.query.action.IncAction;
import fr.gouv.vitam.common.database.builder.query.action.PopAction;
import fr.gouv.vitam.common.database.builder.query.action.PullAction;
import fr.gouv.vitam.common.database.builder.query.action.PushAction;
import fr.gouv.vitam.common.database.builder.query.action.RenameAction;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
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
public class UpdateMultiQueryTest {

    @Test
    public void testSetMult() {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        assertTrue(update.getFilter().size() == 0);
        update.setMult(true);
        assertTrue(update.getFilter().size() == 1);
        update.setMult(false);
        assertTrue(update.getFilter().size() == 1);
        assertTrue(update.getFilter().has(MULTIFILTER.MULT.exactToken()));
        update.resetFilter();
        assertTrue(update.getFilter().size() == 0);
    }

    @Test
    public void testAddActions() {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        assertTrue(update.actions.isEmpty());
        try {
            update.addActions(new AddAction("varname", 1).add(true));
            update.addActions(new IncAction("varname2", 2));
            update.addActions(new PullAction("varname3", true).add("val"));
            update.addActions(new PopAction("varname4"));
            update.addActions(new PushAction("varname5", "val").add(1.0));
            update.addActions(new RenameAction("varname6", "varname7"));
            update.addActions(new SetAction("varname8", "val").add("varname9", 1));
            update.addActions(new UnsetAction("varname10", "varname11").add("varname12"));
            assertEquals(8, update.actions.size());
            update.resetActions();
            assertEquals(0, update.actions.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testAddRequests() {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        assertTrue(update.queries.isEmpty());
        try {
            update.addQueries(
                new BooleanQuery(QUERY.AND).add(new ExistsQuery(QUERY.EXISTS, "varA")).setRelativeDepthLimit(5)
            );
            update.addQueries(
                new PathQuery("path1", "path2"),
                new ExistsQuery(QUERY.EXISTS, "varB").setExactDepthLimit(10)
            );
            update.addQueries(new PathQuery("path3"));
            assertEquals(4, update.queries.size());
            update.resetQueries();
            assertEquals(0, update.queries.size());
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetFinalUpdate_updateById() throws Exception {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        assertTrue(update.queries.isEmpty());
        update.addQueries(new PathQuery("path3"));
        assertEquals(1, update.queries.size());
        update.setMult(true);
        update.addActions(new IncAction("mavar"));
        final ObjectNode node = update.getFinalUpdate();
        assertEquals(4, node.size());
    }

    @Test
    public void testGetFinalUpdate_batchUpdate() throws Exception {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addQueries(new PathQuery("path3"));
        update.setThreshold(1L);
        assertEquals(1, update.queries.size());
        update.setMult(true);
        update.addActions(new IncAction("mavar"));
        final ObjectNode node = update.getFinalUpdate();
        assertEquals(5, node.size());
    }

    @Test
    public void testGetFinalUpdateById() throws Exception {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        assertTrue(update.queries.isEmpty());
        update.addQueries(new PathQuery("path3"));
        assertEquals(1, update.queries.size());
        update.setMult(true);
        update.addActions(new IncAction("mavar"));
        final ObjectNode node = update.getFinalUpdateById();
        assertEquals(1, node.size());
    }

    @Test
    public void testAllReset() throws InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.addActions(new AddAction("varname", 1));
        assertEquals(1, update.actions.size());
        update.reset();
        assertEquals(0, update.actions.size());
    }

    @Test
    public void testAllSet() throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        update.setMult(JsonHandler.createObjectNode().put("$mult", "true"));
        assertTrue(update.getFilter().size() == 1);
        update.resetFilter();
        assertTrue(update.getFilter().size() == 0);
        update.setFilter(JsonHandler.createObjectNode().put("$mult", "true"));
        assertTrue(update.getFilter().size() == 1);

        final String s =
            "UPDATEACTION: Requests: \n\tFilter: {\"$mult\":\"true\"}\n\tRoots: []\n\tActions: \n{\"$inc\":{\"var2\":2}}";
        update.addActions(new IncAction("var2", 2));
        assertEquals(s, update.toString());
    }
}
