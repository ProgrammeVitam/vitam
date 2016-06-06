package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;

public class WorkParamsTest {

    private static final String TEST = "test";
    @Test
    public void testConstructor() {
        assertEquals("", new WorkParams().getGuuid());
        assertEquals("", new WorkParams().getContainerName());
        assertEquals("", new WorkParams().getMetaDataRequest());
        assertEquals("", new WorkParams().getObjectId());
        assertEquals("", new WorkParams().getObjectName());
        assertEquals("", new WorkParams().getServerConfiguration().getUrlMetada());
        
        assertEquals(TEST, new WorkParams().setGuuid(TEST).getGuuid());
        assertEquals(TEST, new WorkParams().setContainerName(TEST).getContainerName());
        assertEquals(TEST, new WorkParams().setMetaDataRequest(TEST).getMetaDataRequest());
        assertEquals(TEST, new WorkParams().setObjectId(TEST).getObjectId());
        assertEquals(TEST, new WorkParams().setObjectName(TEST).getObjectName());
        ServerConfiguration config = new ServerConfiguration().setUrlMetada(TEST);
        assertEquals(TEST, new WorkParams().setServerConfiguration(config).getServerConfiguration().getUrlMetada());
    }

}
