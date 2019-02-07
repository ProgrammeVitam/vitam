package fr.gouv.vitam.ihmdemo.appserver;

import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class WebPreservationResourceTest {

    WebPreservationResource webPreservationResource;

    public @Rule MockitoRule rule = MockitoJUnit.rule();

    @Mock AdminExternalClientFactory adminExternalClientFactory;
    @Mock AccessExternalClientFactory accessExternalClientFactory;

    @Before
    public void setUp() {
        webPreservationResource = new WebPreservationResource(adminExternalClientFactory, accessExternalClientFactory,
            UserInterfaceTransactionManager
                .getInstance(), DslQueryHelper.getInstance());
    }

    @Test
    public void shouldExportScenario() {
    }
}
