package fr.gouv.vitam.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class VitamConfigurationTest {

    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";

    @Test
    public void testPojo() {
        VitamConfiguration vitamConfiguration = new VitamConfiguration();
        assertNull(vitamConfiguration.getConfig());
        assertNull(vitamConfiguration.getLog());
        assertNull(vitamConfiguration.getData());
        assertNull(vitamConfiguration.getTmp());
        
        vitamConfiguration.setDefault();
        assertNotNull(vitamConfiguration.getConfig());
        assertNotNull(vitamConfiguration.getLog());
        assertNotNull(vitamConfiguration.getData());
        assertNotNull(vitamConfiguration.getTmp());
        
        VitamConfiguration vitamConfiguration2 = VitamConfiguration.getConfiguration();
        assertEquals(vitamConfiguration.getConfig(), vitamConfiguration2.getConfig());
        assertEquals(vitamConfiguration.getLog(), vitamConfiguration2.getLog());
        assertEquals(vitamConfiguration.getData(), vitamConfiguration2.getData());
        assertEquals(vitamConfiguration.getTmp(), vitamConfiguration2.getTmp());
        assertNotEquals(vitamConfiguration, vitamConfiguration2);
        
        VitamConfiguration.setConfiguration(vitamConfiguration);
        vitamConfiguration2 = VitamConfiguration.getConfiguration();
        assertEquals(vitamConfiguration.getConfig(), vitamConfiguration2.getConfig());
        assertEquals(vitamConfiguration.getLog(), vitamConfiguration2.getLog());
        assertEquals(vitamConfiguration.getData(), vitamConfiguration2.getData());
        assertEquals(vitamConfiguration.getTmp(), vitamConfiguration2.getTmp());
        assertNotEquals(vitamConfiguration, vitamConfiguration2);
        
        VitamConfiguration.setConfiguration(vitamConfiguration.getConfig(), 
            vitamConfiguration.getLog(), 
            vitamConfiguration.getData(), 
            vitamConfiguration.getTmp());
        assertEquals(vitamConfiguration.getConfig(), vitamConfiguration2.getConfig());
        assertEquals(vitamConfiguration.getLog(), vitamConfiguration2.getLog());
        assertEquals(vitamConfiguration.getData(), vitamConfiguration2.getData());
        assertEquals(vitamConfiguration.getTmp(), vitamConfiguration2.getTmp());
        assertNotEquals(vitamConfiguration, vitamConfiguration2);
        
        vitamConfiguration2 = 
            new VitamConfiguration(vitamConfiguration.getConfig(), 
                vitamConfiguration.getLog(), 
                vitamConfiguration.getData(), 
                vitamConfiguration.getTmp());
        assertEquals(vitamConfiguration.getConfig(), vitamConfiguration2.getConfig());
        assertEquals(vitamConfiguration.getLog(), vitamConfiguration2.getLog());
        assertEquals(vitamConfiguration.getData(), vitamConfiguration2.getData());
        assertEquals(vitamConfiguration.getTmp(), vitamConfiguration2.getTmp());
        
        try {
            vitamConfiguration2.setConfig(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            vitamConfiguration2.setData(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            vitamConfiguration2.setLog(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            vitamConfiguration2.setTmp(null);
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

}
