package fr.gouv.vitam.worker.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PluginPropertiesTest {

    @Test
    public void testPluginProperties() {
        PluginProperties pluginProp = new PluginProperties("name", "propertiesFile");
        assertEquals("name", pluginProp.getClassName());
        assertEquals("propertiesFile", pluginProp.getPropertiesFile());
        pluginProp.setClassName("newName");
        pluginProp.setPropertiesFile("newFIle");
        assertEquals("newName", pluginProp.getClassName());
        assertEquals("newFIle", pluginProp.getPropertiesFile());
    }

}
