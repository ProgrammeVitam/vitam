package fr.gouv.vitam.ingest.external.client;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory.IngestExternalClientType;

public class IngestExternalClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        IngestExternalClientFactory.getInstance().changeConfigurationFile("ingest-external-client.conf");
    }
    
    @Test
    public void givenRestClient() throws VitamException {
        IngestExternalClientFactory.setConfiguration(IngestExternalClientType.REST_CLIENT, "localhost", 8082);
        final IngestExternalClient client 
            = IngestExternalClientFactory.getInstance().getIngestExternalClient();
        assertNotNull(client);
    }
    
    @Test
    public void givenMockClient() throws VitamException {
        IngestExternalClientFactory.setConfiguration(IngestExternalClientType.MOCK_CLIENT, null, 0);
        final IngestExternalClient client 
            = IngestExternalClientFactory.getInstance().getIngestExternalClient();
        assertNotNull(client);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void givenClientWhenWrongTypeThenThrowException() {
        IngestExternalClientFactory.setConfiguration(null, null, 0);
    }
}
