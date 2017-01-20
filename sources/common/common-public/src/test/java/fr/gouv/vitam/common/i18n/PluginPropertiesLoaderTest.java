package fr.gouv.vitam.common.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PluginPropertiesLoaderTest {

    @Test
    public void testWithoutLocale() {
        PluginPropertiesLoader.loadProperties("handlerId", "messages_fr.properties");
        assertEquals("Test", PluginPropertiesLoader.getString("handlerId"));
        assertEquals("KO {0} on {1} !", PluginPropertiesLoader.getString("HelloWorld.KO"));
    }

}
