package fr.gouv.vitam.processing.common.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HandlerNotFoundExceptionTest {

    private static final String ERROR = "ERROR";
    private static final String EXCEPTION = "fr.gouv.vitam.processing.common.exception.HandlerNotFoundException";
    
    @Test
    public void testConstructor() {
        HandlerNotFoundException exception =
            new HandlerNotFoundException(ERROR, new Exception());
        assertEquals(EXCEPTION + ": " + ERROR,
            exception.toString());
        exception = new HandlerNotFoundException(ERROR);
        assertEquals(ERROR, exception.getMessage());
    }

}
