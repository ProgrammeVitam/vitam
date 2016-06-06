package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ActionTest {
    private static final String TEST = "test";

    @Test
    public void testConstructor() {
        assertEquals("", new Action().getActionKey());
        assertEquals(TEST, new Action().setActionKey(TEST).getActionKey());
    }

}
