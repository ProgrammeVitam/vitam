package fr.gouv.vitam.security.internal.rest.service;

import com.google.common.collect.Sets;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.server.PersonalCertificatePermissionConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionServiceTest {

    public static final String PERM_1 = "perm1";
    public static final String PERM_2 = "perm2";
    public static final String PERM_3 = "perm3";
    public static final String PERM_4 = "perm4";

    @Test
    public void isPersonalCertificateRequiredForPermission() throws Exception {

        PersonalCertificatePermissionConfig config = new PersonalCertificatePermissionConfig();
        config.setPermissionsRequiringPersonalCertificate(Sets.newHashSet(PERM_1, PERM_2));
        config.setPermissionsWithoutPersonalCertificate(Sets.newHashSet(PERM_3));

        PermissionService instance = new PermissionService(config);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_1).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.REQUIRED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_3).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.IGNORED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_4).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.ERROR_UNKNOWN_PERMISSION);
    }
}
