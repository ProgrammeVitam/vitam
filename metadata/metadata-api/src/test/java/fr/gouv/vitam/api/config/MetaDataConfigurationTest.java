package fr.gouv.vitam.api.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetaDataConfigurationTest {
    
    @Test
    public void testSetterGetter() {
        MetaDataConfiguration config = new MetaDataConfiguration();
        assertEquals("host", config.setHost("host").getHost());
        assertEquals(1234, config.setPort(1234).getPort());
        assertEquals("dbNameTest", config.setDbName("dbNameTest").getDbName());
        assertEquals("jettyConfig", config.setJettyConfig("jettyConfig").getJettyConfig());
    }
}
