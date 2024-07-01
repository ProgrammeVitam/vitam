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
package fr.gouv.vitam.common.graph;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GraphTest {

    @Test
    public void test_graph() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_tree.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final Graph g = new Graph(json);
        assertNotNull(g.getGraphWithLongestPaths());
        assertEquals(
            g.getGraphWithLongestPaths().toString(),
            "{0=[ID027], 1=[ID030, ID031, ID028], 2=[ID029], 3=[ID032]}"
        );
    }

    @Test
    public void test_graph_DAG() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_acyc_2.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final Graph g = new Graph(json);
        assertNotNull(g.getGraphWithLongestPaths());

        assertTrue(g.getGraphWithLongestPaths().size() == 3);

        Set<String> level_0 = g.getGraphWithLongestPaths().get(0);
        assertNotNull(level_0);
        assertTrue(level_0.size() == 2);
        assertTrue(level_0.contains("ID030"));
        assertTrue(level_0.contains("ID027"));

        Set<String> level_1 = g.getGraphWithLongestPaths().get(1);
        assertNotNull(level_1);
        assertTrue(level_1.size() == 2);
        assertTrue(level_1.contains("ID031"));
        assertTrue(level_1.contains("ID028"));

        Set<String> level_2 = g.getGraphWithLongestPaths().get(2);
        assertNotNull(level_2);
        assertTrue(level_2.size() == 3);
        assertTrue(level_2.contains("ID032"));
        assertTrue(level_2.contains("ID033"));
        assertTrue(level_2.contains("ID029"));
    }

    @Test
    public void test_graph_Acyc_multi_roots() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_tree_multi_roots.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final Graph g = new Graph(json);
        assertNotNull(g.getGraphWithLongestPaths());
        assertTrue(g.getGraphWithLongestPaths().size() == 2);
        Set<String> level_0 = g.getGraphWithLongestPaths().get(0);
        assertTrue(level_0.contains("ID10"));
        assertTrue(level_0.contains("ID4"));
        Set<String> level_1 = g.getGraphWithLongestPaths().get(1);
        assertTrue(level_1.contains("ID14"));
        assertTrue(level_1.contains("ID8"));
    }
}
