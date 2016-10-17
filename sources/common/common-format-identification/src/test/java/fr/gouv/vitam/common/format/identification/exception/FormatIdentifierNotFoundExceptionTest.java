

package fr.gouv.vitam.common.format.identification.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FormatIdentifierNotFoundExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testFormatIdentifierNotFoundExceptionThrowable() {
        assertNotNull(new FormatIdentifierNotFoundException((String) null));
        assertNotNull(new FormatIdentifierNotFoundException(MESSAGE));
        assertNotNull(new FormatIdentifierNotFoundException(exception));
        assertNotNull(new FormatIdentifierNotFoundException(MESSAGE, exception));
    }

}
