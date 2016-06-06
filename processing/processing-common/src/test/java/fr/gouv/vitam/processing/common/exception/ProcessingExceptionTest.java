package fr.gouv.vitam.processing.common.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessingExceptionTest {

    private static final String ERROR = "ERROR";
    private static final String JAVA_ERROR = "java.lang.Exception";
    private static final String EXCEPTION = "fr.gouv.vitam.processing.common.exception.ProcessingException";
    
    @Test
    public void testConstructor() {
        assertEquals(EXCEPTION + ": " + ERROR,
            new ProcessingException(ERROR, new Exception()).toString());
        assertEquals(ERROR, new ProcessingException(ERROR).getMessage());
        assertEquals(JAVA_ERROR, new ProcessingException(new Exception()).getMessage());
    }
}
