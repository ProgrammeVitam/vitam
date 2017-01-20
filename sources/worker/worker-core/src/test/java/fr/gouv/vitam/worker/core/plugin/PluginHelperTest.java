package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.worker.common.PluginProperties;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

public class PluginHelperTest {


    private static final String MESSAGE_PROPERTIES = "message_fr.properties";
    private static final String DUMMY_HANDLER = "fr.gouv.vitam.worker.core.handler.DummyHandler";
    private static final String ACTION_NAME = "actionName";

    @Test
    public void testPluginHelper() throws PluginNotFoundException {
        assertEquals(0, PluginHelper.getPluginList().size());
        ActionHandler handler = PluginHelper.loadActionHandler(ACTION_NAME, new PluginProperties(DUMMY_HANDLER, 
            MESSAGE_PROPERTIES));
        assertNotNull(handler);
    }

}
