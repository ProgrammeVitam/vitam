package fr.gouv.vitam.workspace.rest;

import org.junit.Before;
import org.junit.Test;

public class WorkspaceApplicationTest {

    private WorkspaceApplication application;

    @Before
    public void setup() throws Exception {
        application = new WorkspaceApplication();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure(new String[0]);
    }

    @Test(expected = Exception.class)
    public void givenFileNotFoundWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test
    public void givenFileAlreadyExistsWhenConfigureApplicationOThenRunServer() throws Exception {
        application.configure("src/test/resources/workspace.conf", "8084");
    }

}