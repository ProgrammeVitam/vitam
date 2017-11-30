package fr.gouv.vitam.security.internal.rest.resource;


import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

public class AdminPersonalCertificateResourceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private AdminPersonalCertificateResource adminPersonalCertificateResource;

    @Mock
    private PersonalCertificateService personalCertificateService;

    @Test
    public void should_create_certificate() throws Exception {

        InputStream stream = getClass().getResourceAsStream("/certificate.pem");
        byte[] certificate = toByteArray(stream);

        adminPersonalCertificateResource.createIfNotPresent(certificate);

        verify(personalCertificateService, only()).createPersonalCertificateIfNotPresent(certificate);
    }
}
