package fr.gouv.vitam.api.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetaDataConfigurationTest {
    
    private final String hostTest = "host";
    private final int portTest = 1234;
    private final String dbNameTest = "dbNameTest";

    private MetaDataConfiguration config ;
    @Test
    public void testSetterGetter()
    {
        config = new MetaDataConfiguration();
        config.setHost(hostTest);
        assertEquals(config.getHost(), hostTest);        
        config.setPort(portTest);
        assertEquals(config.getPort(), portTest);      
        config.setDbName(dbNameTest);
        assertEquals(config.getDbName(), dbNameTest);          
    }
}
