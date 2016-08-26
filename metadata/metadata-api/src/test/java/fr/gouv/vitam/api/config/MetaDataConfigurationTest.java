package fr.gouv.vitam.api.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetaDataConfigurationTest {

    private static final String HOST = "host";
    private static final int PORT = 1234;
    private static final String DB_NAME = "dbNameTest";
    private static final String JETTY_CONF="jettyConfig";


    @Test
    public void testSetterGetter()
    {
        MetaDataConfiguration config1 = new MetaDataConfiguration();
        assertEquals(config1.setHost(HOST).getHost(), HOST);        
        assertEquals(config1.setPort(PORT).getPort(), PORT);      
        assertEquals(config1.setDbName(DB_NAME).getDbName(), DB_NAME);   
        assertEquals(JETTY_CONF, config1.setJettyConfig(JETTY_CONF).getJettyConfig());
        
        MetaDataConfiguration config2 = new MetaDataConfiguration(HOST,PORT,DB_NAME);
        assertEquals(config2.getHost(), HOST);   
        assertEquals(config2.getPort(), PORT);      
        assertEquals(config2.getDbName(),DB_NAME);
        
    }
}
