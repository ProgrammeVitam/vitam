package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;

public class FileFormatNotFoundExceptionTest {

    @Test
    public final void testFileFormatException() {
        assertEquals("", new FileFormatNotFoundException("").getMessage());
        assertEquals("test", new FileFormatNotFoundException("test").getMessage());
        assertNotNull(new FileFormatNotFoundException(new Exception()).getCause());
        assertNotNull(new FileFormatNotFoundException("test", new Exception()).getCause());
        assertNotNull(new FileFormatNotFoundException("test", new Exception(), true, true).getCause());
    }
}
