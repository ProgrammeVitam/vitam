package fr.gouv.vitam.security.internal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.service.PermissionService;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;

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
