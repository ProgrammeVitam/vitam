package fr.gouv.vitam.functional.administration.common.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.functional.administration.common.exception.JsonNodeFormatCreationException;

public class JsonNodeFormatCreationExceptionTest {
    @Test
    public final void testFileFormatException() {    
    assertEquals("", new JsonNodeFormatCreationException("").getMessage());
    assertEquals("test", new JsonNodeFormatCreationException("test").getMessage());
    assertNotNull(new JsonNodeFormatCreationException(new Exception()).getCause());
    assertNotNull(new JsonNodeFormatCreationException("test", new Exception()).getCause());
    assertNotNull(new JsonNodeFormatCreationException("test", new Exception(), true, true).getCause());
    }

}
