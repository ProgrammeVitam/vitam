

package fr.gouv.vitam.common.format.identification.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FormatIdentifierFactoryExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testFormatIdentifierFactoryExceptionThrowable() {
        assertNotNull(new FormatIdentifierFactoryException((String) null));
        assertNotNull(new FormatIdentifierFactoryException(MESSAGE));
        assertNotNull(new FormatIdentifierFactoryException(exception));
        assertNotNull(new FormatIdentifierFactoryException(MESSAGE, exception));
    }

}

