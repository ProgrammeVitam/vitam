package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.functional.administration.common.exception.InvalidFileFormatParseException;

public class InvalidFileFormatParseExceptionTest {
    @Test
    public final void testFileFormatException() {
        assertEquals("", new InvalidFileFormatParseException("").getMessage());
        assertEquals("test", new InvalidFileFormatParseException("test").getMessage());
        assertNotNull(new InvalidFileFormatParseException(new Exception()).getCause());
        assertNotNull(new InvalidFileFormatParseException("test", new Exception()).getCause());
        assertNotNull(new InvalidFileFormatParseException("test", new Exception(), true, true).getCause());
    }
}
