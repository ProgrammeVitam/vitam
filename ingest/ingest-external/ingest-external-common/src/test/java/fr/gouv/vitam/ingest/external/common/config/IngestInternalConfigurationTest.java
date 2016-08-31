package fr.gouv.vitam.ingest.external.common.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class IngestInternalConfigurationTest {

    @Test
    public void givenIngestInternalConfiguration() {
        IngestInternalConfiguration config = new IngestInternalConfiguration();
        assertEquals("host_test", config.setHost("host_test").getHost());
        assertEquals(8082, config.setPort(8082).getPort());

    }

}
