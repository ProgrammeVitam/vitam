package fr.gouv.vitam.common.graph;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.CycleFoundException;
import fr.gouv.vitam.common.json.JsonHandler;

public class DirectedCycleTest {

    @Test(expected = CycleFoundException.class)
    public void given_CyclycGraph_then_returnTrue() throws Exception {
        File file = PropertiesUtils.getResourcesFile("ingest_cyc.json");
        JsonNode json = JsonHandler.getFromFile(file);
        DirectedGraph g = new DirectedGraph(json);
        new DirectedCycle(g);

    }

    @Test
    public void given_acyclicGraph_when_create_DirectedCycle_then_not_thrown_cycleFoundException() throws Exception {
        File file = PropertiesUtils.getResourcesFile("ingest_acyc.json");
        JsonNode json = JsonHandler.getFromFile(file);
        DirectedGraph g = new DirectedGraph(json);
        assertNotNull(new DirectedCycle(g));
    }
}
