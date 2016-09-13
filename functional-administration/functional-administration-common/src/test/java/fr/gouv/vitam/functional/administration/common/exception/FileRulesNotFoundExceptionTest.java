package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FileRulesNotFoundExceptionTest {

    @Test
    public final void testFileFormatException() {
        assertEquals("", new FileRulesNotFoundException("").getMessage());
        assertEquals("test", new FileRulesNotFoundException("test").getMessage());
        assertNotNull(new FileRulesNotFoundException(new Exception()).getCause());
        assertNotNull(new FileRulesNotFoundException("test", new Exception()).getCause());
        assertNotNull(new FileRulesNotFoundException("test", new Exception(), true, true).getCause());
    }
}


