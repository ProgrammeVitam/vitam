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
        File file = PropertiesUtils.getResourcesFile("ingest_cyc.json");
        JsonNode json = JsonHandler.getFromFile(file);
        DirectedGraph g = new DirectedGraph(json);

        DirectedCycle dc = new DirectedCycle(g);
        assertTrue(dc.isCyclic());
    }


    @Test
    public void given_aCyclycGraph() throws Exception {
        File file = PropertiesUtils.getResourcesFile("ingest_acyc.json");
        JsonNode json = JsonHandler.getFromFile(file);
        DirectedGraph g = new DirectedGraph(json);
        DirectedCycle dc = new DirectedCycle(g);
        assertFalse(dc.isCyclic());
    }

    @Test
    public void given_acyclicGraph_when_create_DirectedCycle_then_not_thrown_cycleFoundException() throws Exception {
        File file = PropertiesUtils.getResourcesFile("ingest_acyc.json");
        JsonNode json = JsonHandler.getFromFile(file);
        DirectedGraph g = new DirectedGraph(json);
        assertNotNull(new DirectedCycle(g));
    }
}
