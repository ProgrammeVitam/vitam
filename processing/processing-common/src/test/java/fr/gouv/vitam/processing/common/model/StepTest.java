package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class StepTest {

    private static final String TEST = "test";

    @Test
    public void testConstructor() {
        assertEquals("", new Step().getStepName());
        assertEquals("", new Step().getWorkerGroupId());
        assertEquals("", new Step().getWorkerGroupId());
        assertEquals(true, new Step().getActions().isEmpty());
        assertEquals(DistributionKind.REF, new Step().getDistribution().getKind());
        assertEquals(DistributionKind.LIST,
            new Step().setDistribution(new Distribution().setKind(DistributionKind.LIST))
                .getDistribution().getKind());

        final List<Action> actions = new ArrayList<>();
        actions.add(new Action());
        assertEquals(TEST, new Step().setStepName(TEST).getStepName());
        assertEquals(TEST, new Step().setWorkerGroupId(TEST).getWorkerGroupId());
        assertEquals(false, new Step().setActions(actions).getActions().isEmpty());
    }

}
