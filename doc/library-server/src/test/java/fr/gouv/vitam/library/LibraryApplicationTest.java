package fr.gouv.vitam.library;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kw on 05/09/2016.
 */
public class LibraryApplicationTest {

    @Test
    public void shouldStopServerWhenStopApplicationWithFileExistAndRunOnDefaultPort() throws Exception {
        // Given
        final LibraryApplication application = new LibraryApplication();
        // When
        application.start(new String[]{"src/test/resources/library.conf"});
        // Then
        Assert.assertTrue(application.isStarted());
        // When
        application.stop();
        // Then
        Assert.assertFalse(application.isStarted());
    }
}
