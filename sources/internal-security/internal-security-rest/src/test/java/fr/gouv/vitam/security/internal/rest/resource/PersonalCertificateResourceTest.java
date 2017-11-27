package fr.gouv.vitam.security.internal.rest.resource;


import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import fr.gouv.vitam.security.internal.rest.service.IdentityService;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;


public class PersonalCertificateResourceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private PersonalCertificateResource personalCertificateResource;

    @Mock
    private PersonalCertificateService personalCertificateService;

    @Test
    public void should_check_personal_certificate() throws Exception {
        // Given
        byte[] bytes = new byte[] {1, 2};

        doThrow(PersonalCertificateException.class).when(personalCertificateService)
            .checkPersonalCertificateExistence(bytes);

        // When / Then
        assertThatThrownBy(() -> personalCertificateResource.checkPersonalCertificate(bytes))
            .isInstanceOf(PersonalCertificateException.class);
    }



}
