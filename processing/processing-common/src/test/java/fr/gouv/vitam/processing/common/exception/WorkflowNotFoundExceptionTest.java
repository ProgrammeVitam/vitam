package fr.gouv.vitam.processing.common.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorkflowNotFoundExceptionTest {


    private static final String ERROR = "ERROR";
    private static final String EXCEPTION = "fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException";
    
    @Test
    public void testConstructor() {
        assertEquals(EXCEPTION + ": " + ERROR,
            new WorkflowNotFoundException(ERROR, new Exception()).toString());
        assertEquals(ERROR, new WorkflowNotFoundException(ERROR).getMessage());
    }

}
