package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class DatabaseConflictExceptionTest {
    @Test
    public final void testFileFormatException() {
        assertEquals("", new DatabaseConflictException("").getMessage());
        assertEquals("test", new DatabaseConflictException("test").getMessage());
        assertNotNull(new DatabaseConflictException(new Exception()).getCause());
        assertNotNull(new DatabaseConflictException("test", new Exception()).getCause());
        assertNotNull(new DatabaseConflictException("test", new Exception(), true, true).getCause());
    }
}
