package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class InvalidFileRulesParseExceptionTest {
    @Test
    public final void testFileRulesException() {
        assertEquals("", new InvalidFileRulesParseException("").getMessage());
        assertEquals("test", new InvalidFileRulesParseException("test").getMessage());
        assertNotNull(new InvalidFileRulesParseException(new Exception()).getCause());
        assertNotNull(new InvalidFileRulesParseException("test", new Exception()).getCause());
        assertNotNull(new InvalidFileRulesParseException("test", new Exception(), true, true).getCause());
    }
}
