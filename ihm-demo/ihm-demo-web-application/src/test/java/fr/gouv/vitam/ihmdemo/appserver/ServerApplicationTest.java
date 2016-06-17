package fr.gouv.vitam.ihmdemo.appserver;

import java.io.FileNotFoundException;

import org.junit.Test;

public class ServerApplicationTest {

    private static final ServerApplication application =  new ServerApplication();
    
    @Test(expected = FileNotFoundException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test(expected = Exception.class)
    public void givenFileNotFoundWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        application.configure("src/test/resources/ihm-demo.conf");
    }
    
    @Test
    public void givenNullArgumentWhenConfigureApplicationOThenRunServerWithDefaultParms() throws Exception {
        application.configure(null);
    }
}
