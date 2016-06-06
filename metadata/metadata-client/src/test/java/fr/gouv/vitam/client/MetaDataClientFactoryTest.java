package fr.gouv.vitam.client;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MetaDataClientFactoryTest {

    @Test
    public void givenMetaDataClientFactoryWhenCallingCreateThenReturnClient() {
        MetaDataClientFactory factory = new MetaDataClientFactory();
        assertTrue(factory.create("url") instanceof MetaDataClient);
    }

}
