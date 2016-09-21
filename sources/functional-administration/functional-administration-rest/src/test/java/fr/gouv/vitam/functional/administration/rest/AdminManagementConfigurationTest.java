package fr.gouv.vitam.functional.administration.rest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdminManagementConfigurationTest {

    @Test
    public void givenAdminManagementConfiguration() {
        AdminManagementConfiguration config = new AdminManagementConfiguration();

        assertEquals("jettyFakeConfig", config.setJettyConfig("jettyFakeConfig").getJettyConfig());
    }
}
