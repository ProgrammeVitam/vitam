

package fr.gouv.vitam.common.format.identification.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FormatIdentifierBadRequestExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testFormatIdentifierFactoryExceptionThrowable() {
        assertNotNull(new FormatIdentifierBadRequestException((String) null));
        assertNotNull(new FormatIdentifierBadRequestException(MESSAGE));
        assertNotNull(new FormatIdentifierBadRequestException(exception));
        assertNotNull(new FormatIdentifierBadRequestException(MESSAGE, exception));
    }

}

