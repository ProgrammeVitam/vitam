package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class WorkFlowTest {
    private static final String TEST = "test";

    @Test
    public void testConstructor() {
        assertEquals("", new WorkFlow().getComment());
        assertEquals("", new WorkFlow().getId());
        assertEquals(true, new WorkFlow().getSteps().isEmpty());
        
        List<Step> steps = new ArrayList<>();
        steps.add(new Step().setStepName(TEST));
        
        assertEquals(TEST, new WorkFlow().setComment(TEST).getComment());
        assertEquals(TEST, new WorkFlow().setId(TEST).getId());
        assertEquals(false, new WorkFlow().setSteps(steps).getSteps().isEmpty());
    }

}
