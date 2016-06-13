package fr.gouv.vitam.logbook.common.server.database.exception;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

public class LogbookExceptionTest {

    private static final String MESSAGE = "message";
    private static final Exception exception = new Exception();

    @Test
    public final void testLogbookAlreadyExistsException() {
        assertNotNull(new LogbookAlreadyExistsException());
        assertNotNull(new LogbookAlreadyExistsException(MESSAGE));
        assertNotNull(new LogbookAlreadyExistsException(exception));
        assertNotNull(new LogbookAlreadyExistsException(MESSAGE, exception));
        assertNotNull(new LogbookAlreadyExistsException(MESSAGE, exception, true, true));
    }

    @Test
    public final void testLogbookDatabaseException() {
        assertNotNull(new LogbookDatabaseException());
        assertNotNull(new LogbookDatabaseException(MESSAGE));
        assertNotNull(new LogbookDatabaseException(exception));
        assertNotNull(new LogbookDatabaseException(MESSAGE, exception));
        assertNotNull(new LogbookDatabaseException(MESSAGE, exception, true, true));
    }

    @Test
    public final void testLogbookExceptionThrowable() {
        assertNotNull(new LogbookException());
        assertNotNull(new LogbookException(MESSAGE));
        assertNotNull(new LogbookException(exception));
        assertNotNull(new LogbookException(MESSAGE, exception));
        assertNotNull(new LogbookException(MESSAGE, exception, true, true));
    }

    @Test
    public final void testLogbookNotFoundExceptionStringThrowable() {
        assertNotNull(new LogbookNotFoundException());
        assertNotNull(new LogbookNotFoundException(MESSAGE));
        assertNotNull(new LogbookNotFoundException(exception));
        assertNotNull(new LogbookNotFoundException(MESSAGE, exception));
        assertNotNull(new LogbookNotFoundException(MESSAGE, exception, true, true));
    }

}
