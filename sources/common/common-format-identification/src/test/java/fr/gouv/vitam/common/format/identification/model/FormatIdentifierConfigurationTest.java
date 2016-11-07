package fr.gouv.vitam.common.format.identification.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.format.identification.FormatIdentifierType;

public class FormatIdentifierConfigurationTest {

    @Test
    public void testFormatIdentifierConfiguration() throws Exception {
        final FormatIdentifierConfiguration configuration = new FormatIdentifierConfiguration();
        configuration.setType(FormatIdentifierType.MOCK);
        configuration.getConfigurationProperties().put("host", "localhost");
        configuration.getConfigurationProperties().put("port", "55800");
        assertEquals(FormatIdentifierType.MOCK, configuration.getType());
        assertEquals(2, configuration.getConfigurationProperties().size());
        assertEquals("55800", configuration.getConfigurationProperties().get("port"));
    }
}

