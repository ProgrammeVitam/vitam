/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;

public class DirectedCycleTest {

    @Test
    public void given_CyclycGraph() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_cyc.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final DirectedGraph g = new DirectedGraph(json);
        final DirectedCycle dc = new DirectedCycle(g);
        assertTrue(dc.isCyclic());
        assertTrue("ID029".equals(g.getId(dc.getCycle().get(0))));
        assertTrue("ID029".equals(g.getId(dc.getCycle().get(1))));
    }

    @Test
    public void given_CyclycGraph_2() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_cyc_2.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final DirectedGraph g = new DirectedGraph(json);
        final DirectedCycle dc = new DirectedCycle(g);
        assertTrue(dc.isCyclic());
        assertTrue("ID036".equals(g.getId(dc.getCycle().get(0))));
        assertTrue("ID035".equals(g.getId(dc.getCycle().get(1))));
        assertTrue("ID030".equals(g.getId(dc.getCycle().get(2))));
        assertTrue("ID036".equals(g.getId(dc.getCycle().get(3))));
    }

    @Test
    public void given_aCyclycGraph() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_acyc.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final DirectedGraph g = new DirectedGraph(json);
        final DirectedCycle dc = new DirectedCycle(g);
        assertFalse(dc.isCyclic());
    }

    @Test
    public void given_acyclicGraph_when_create_DirectedCycle_then_not_thrown_cycleFoundException() throws Exception {
        final File file = PropertiesUtils.getResourceFile("ingest_acyc.json");
        final JsonNode json = JsonHandler.getFromFile(file);
        final DirectedGraph g = new DirectedGraph(json);
        assertNotNull(new DirectedCycle(g));
    }
}
