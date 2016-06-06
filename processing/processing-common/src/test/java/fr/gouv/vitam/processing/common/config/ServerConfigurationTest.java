package fr.gouv.vitam.processing.common.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServerConfigurationTest {

    private static final String Test = "test";
    @Test
    public void testConstructor() {
        assertEquals("", new ServerConfiguration().getUrlMetada());
        assertEquals("", new ServerConfiguration().getUrlWorkspace());
        assertEquals(Test, new ServerConfiguration().setUrlMetada(Test).getUrlMetada());
        assertEquals(Test, new ServerConfiguration().setUrlWorkspace(Test).getUrlWorkspace());
    }

}
