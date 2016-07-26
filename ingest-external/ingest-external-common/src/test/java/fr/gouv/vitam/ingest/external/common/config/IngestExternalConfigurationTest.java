package fr.gouv.vitam.ingest.external.common.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class IngestExternalConfigurationTest {

    @Test
    public void givenIngestExternalConfiguration() {
        IngestExternalConfiguration config = new IngestExternalConfiguration();
        assertEquals("path_test", config.setPath("path_test").getPath());
        assertEquals("host_test", config.setHost("host_test").getHost());
        assertEquals(8082, config.setPort(8082).getPort());
    }

}
