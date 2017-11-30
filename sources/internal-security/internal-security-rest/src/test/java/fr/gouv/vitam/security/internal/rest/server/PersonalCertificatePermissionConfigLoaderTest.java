package fr.gouv.vitam.security.internal.rest.server;

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonalCertificatePermissionConfigLoaderTest {

    @Test
    public void testLoadPersonalCertificatePermissionConfig() throws Exception {

        String configFile =
            PropertiesUtils.getResourceFile("personal-certificate-permissions-test.conf").getAbsolutePath();

        PersonalCertificatePermissionConfig conf =
            PersonalCertificatePermissionConfigLoader.loadPersonalCertificatePermissionConfig(configFile);

        assertThat(conf.getPermissionsRequiringPersonalCertificate())
            .containsExactlyInAnyOrder("contexts:read", "contexts:id:update");
        assertThat(conf.getPermissionsWithoutPersonalCertificate()).containsExactlyInAnyOrder("ingests:create");
    }

    @Test
    public void testLoadPersonalCertificatePermissionConfigWithEmptySet() throws Exception {

        String configFile =
            PropertiesUtils.getResourceFile("personal-certificate-permissions-testEmptySet.conf").getAbsolutePath();

        PersonalCertificatePermissionConfig conf =
            PersonalCertificatePermissionConfigLoader.loadPersonalCertificatePermissionConfig(configFile);

        assertThat(conf.getPermissionsRequiringPersonalCertificate())
            .isEmpty();
        assertThat(conf.getPermissionsWithoutPersonalCertificate())
            .containsExactlyInAnyOrder("contexts:read", "contexts:id:update", "ingests:create");
    }
}
