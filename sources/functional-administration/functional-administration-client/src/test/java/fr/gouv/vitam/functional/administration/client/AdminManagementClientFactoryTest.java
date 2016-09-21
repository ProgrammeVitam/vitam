package fr.gouv.vitam.functional.administration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory.AdminManagementClientType;

public class AdminManagementClientFactoryTest {

    @Test
    public void givenRestClient() {
        AdminManagementClientFactory
            .setConfiguration(AdminManagementClientFactory.AdminManagementClientType.REST_CLIENT, "localhost", 8082);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        assertNotNull(client);
    }

    @Test
    public void givenMockClient() {
        AdminManagementClientFactory
            .setConfiguration(AdminManagementClientFactory.AdminManagementClientType.MOCK_CLIENT, "localhost", 8082);
        final AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        assertNotNull(client);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenClientWhenWrongTypeThenThrowException() {
        AdminManagementClientFactory.setConfiguration(null, null, 0);
    }

}
