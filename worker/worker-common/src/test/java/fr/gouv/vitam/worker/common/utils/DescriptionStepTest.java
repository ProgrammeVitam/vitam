package fr.gouv.vitam.worker.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.StepType;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.DescriptionStep;

public class DescriptionStepTest {

    @Test
    public void testDescriptionStep() {
        Step step = new Step();
        step.setStepName("StepName");
        step.setStepType(StepType.NOBLOCK);
        DescriptionStep ds = new DescriptionStep(new Step(), WorkerParametersFactory.newWorkerParameters());
        ds.setStep(step);
        ds.setWorkParams(WorkerParametersFactory.newWorkerParameters());
        assertNotNull(ds.getWorkParams());
        assertNotNull(ds.getStep());
        assertEquals(StepType.NOBLOCK, ds.getStep().getStepType());
        assertEquals("StepName", ds.getStep().getStepName());
        try {
            ds = new DescriptionStep(null, WorkerParametersFactory.newWorkerParameters());
            fail("Should have raized an exception");
        } catch (Exception e) {
            // nothing to do
        }
        try {
            ds = new DescriptionStep(null, null);
            fail("Should have raized an exception");
        } catch (Exception e) {
            // nothing to do
        }
    }

    @Test
    public void testDescriptionStepJson() {
        try {
            File json = PropertiesUtils.findFile("descriptionStep.json");
            JsonNode node = JsonHandler.getFromFile(json);
            DescriptionStep step = JsonHandler.getFromFile(json, DescriptionStep.class);
        } catch (Exception e) {
            fail("Should not have raised an exception");
        }

    }
}
