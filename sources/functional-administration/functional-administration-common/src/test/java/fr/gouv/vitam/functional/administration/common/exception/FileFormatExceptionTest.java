package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;


public class FileFormatExceptionTest {

    @Test
    public final void testFileFormatException() {
        assertEquals("", new FileFormatException("").getMessage());
        assertEquals("test", new FileFormatException("test").getMessage());
        assertNotNull(new FileFormatException(new Exception()).getCause());
        assertNotNull(new FileFormatException("test", new Exception()).getCause());
        assertNotNull(new FileFormatException("test", new Exception(), true, true).getCause());
    }
}
