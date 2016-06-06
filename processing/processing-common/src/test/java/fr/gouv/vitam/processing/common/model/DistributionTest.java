package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DistributionTest {

    private static final String Test = "test";
    @Test
    public void testConstructor() {
        assertEquals("", new Distribution().getElement());
        assertEquals(DistributionKind.REF, new Distribution().getKind());
        assertEquals(DistributionKind.LIST.value(), new Distribution().setKind(DistributionKind.LIST).getKind().value());
        assertEquals(Test, new Distribution().setElement(Test).getElement());
    }

}
