package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * FileFormatException error
 */
public class FileRulesExceptionTest {

    @Test
    public final void testFileRulesException() {
        assertEquals("", new FileRulesException("").getMessage());
        assertEquals("test", new FileFormatException("test").getMessage());
        assertNotNull(new FileRulesException(new Exception()).getCause());
        assertNotNull(new FileRulesException("test", new Exception()).getCause());
        assertNotNull(new FileRulesException("test", new Exception(), true, true).getCause());
    }
}
