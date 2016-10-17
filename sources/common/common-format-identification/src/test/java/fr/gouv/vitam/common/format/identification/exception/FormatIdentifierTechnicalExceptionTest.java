

package fr.gouv.vitam.common.format.identification.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FormatIdentifierTechnicalExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testFormatIdentifierNotFoundExceptionThrowable() {
        assertNotNull(new FormatIdentifierTechnicalException((String) null));
        assertNotNull(new FormatIdentifierTechnicalException(MESSAGE));
        assertNotNull(new FormatIdentifierTechnicalException(exception));
        assertNotNull(new FormatIdentifierTechnicalException(MESSAGE, exception));
    }

}
