package fr.gouv.vitam.worker.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SedaVersionTest {
    
    @Test
    public void testSedaVersion() {
        final SedaVersion sedaVersion = new SedaVersion();
        sedaVersion.setBinaryDataObjectVersions(new String[] {"TEST1","TEST2"});
        sedaVersion.setPhysicalDataObjectVersions(new String[] {"TEST3","TEST4"});
        assertNotNull(sedaVersion.getVersionForType("BinaryDataObject"));
        assertNotNull(sedaVersion.getVersionForType("PhysicalDataObject"));
        assertNull(sedaVersion.getVersionForType("Bleah"));
        assertEquals("TEST1", sedaVersion.getVersionForType("BinaryDataObject").get(0));
        assertEquals("TEST4", sedaVersion.getVersionForType("PhysicalDataObject").get(1));
    }


}
