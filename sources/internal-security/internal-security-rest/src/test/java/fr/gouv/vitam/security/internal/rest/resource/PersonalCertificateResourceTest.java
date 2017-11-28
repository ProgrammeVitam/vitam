package fr.gouv.vitam.security.internal.rest.resource;

import com.google.common.collect.Sets;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import fr.gouv.vitam.security.internal.rest.server.PersonalCertificatePermissionConfig;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

public class PersonalCertificateResourceTest {

    public static final String PERM_1 = "perm1";
    public static final String PERM_2 = "perm2";
    public static final String PERM_3 = "perm3";
    public static final String PERM_4 = "perm4";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PersonalCertificateService personalCertificateService;

    @Test
    public void should_check_personal_certificate() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};

        doThrow(PersonalCertificateException.class).when(personalCertificateService)
            .checkPersonalCertificateExistence(bytes, PERM_1);

        PersonalCertificateResource personalCertificateResource =
            new PersonalCertificateResource(null, personalCertificateService);

        // When / Then
        assertThatThrownBy(() -> personalCertificateResource.checkPersonalCertificate(bytes, PERM_1))
            .isInstanceOf(PersonalCertificateException.class);
    }

    @Test
    public void isPersonalCertificateRequiredForPermission() throws Exception {

        PersonalCertificatePermissionConfig config = new PersonalCertificatePermissionConfig();
        config.setPermissionsRequiringPersonalCertificate(Sets.newHashSet(PERM_1, PERM_2));
        config.setPermissionsWithoutPersonalCertificate(Sets.newHashSet(PERM_3));

        PersonalCertificateResource instance = new PersonalCertificateResource(config,
            personalCertificateService);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_1).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.REQUIRED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_3).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.IGNORED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_4).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.ERROR_UNKNOWN_PERMISSION);
    }
}
