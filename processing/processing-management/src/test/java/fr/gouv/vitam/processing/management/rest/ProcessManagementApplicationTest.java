package fr.gouv.vitam.processing.management.rest;

import org.junit.Before;
import org.junit.Test;

public class ProcessManagementApplicationTest {
    
    private ProcessManagementApplication application;    
    
    @Before
    public void setup() throws Exception {
        application = new ProcessManagementApplication();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application.configure(new String[0]);
    }   
    
    @Test(expected = Exception.class)
    public void givenFileNotFoundWhenConfigureApplicationThenRaiseAnException() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test
    public void givenFileExistsWhenConfigureApplicationThenRunServer() throws Exception {
        application.configure("src/test/resources/processing.conf", "8090");
    }

}
