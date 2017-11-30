package fr.gouv.vitam.security.internal.rest.resource;

import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import fr.gouv.vitam.security.internal.rest.service.PermissionService;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.ERROR_UNKNOWN_PERMISSION;
import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.IGNORED_PERSONAL_CERTIFICATE;
import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.REQUIRED_PERSONAL_CERTIFICATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PersonalCertificateResourceTest {

    public static final String PERMISSION = "permission";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PersonalCertificateService personalCertificateService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PersonalCertificateResource personalCertificateResource;

    @Test
    public void should_check_personal_certificate() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};

        doThrow(PersonalCertificateException.class).when(personalCertificateService)
            .checkPersonalCertificateExistence(bytes, PERMISSION);

        // When / Then
        assertThatThrownBy(() -> personalCertificateResource.checkPersonalCertificate(bytes, PERMISSION))
            .isInstanceOf(PersonalCertificateException.class);
    }

    @Test
    public void isPersonalCertificateRequiredForPermission() throws Exception {

        IsPersonalCertificateRequiredModel response = mock(IsPersonalCertificateRequiredModel.class);
        when(permissionService.isPersonalCertificateRequiredForPermission(PERMISSION))
            .thenReturn(response);

        assertThat(personalCertificateResource.isPersonalCertificateRequiredForPermission(PERMISSION))
            .isEqualTo(response);
    }
}
