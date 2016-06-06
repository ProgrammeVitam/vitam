package fr.gouv.vitam.processing.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessingEntryTest {

    private static final String Test = "test";
    @Test
    public void testConstructor() {
        assertEquals(Test, new ProcessingEntry(Test, Test).getContainer());
        assertEquals(Test, new ProcessingEntry(Test, Test).getWorkflow());
    }


}
