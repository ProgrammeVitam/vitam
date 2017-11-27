package fr.gouv.vitam.security.internal.rest.resource;


import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.security.cert.CertificateException;

import static org.mockito.BDDMockito.given;

public class AdminPersonalCertificateResourceTest {

    @Before
    public void setUp() throws Exception {
        given(uriInfo.getRequestUri()).willReturn(new URI(""));
    }

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @InjectMocks
    private AdminPersonalCertificateResource adminPersonalCertificateResource;
    @Mock
    private PersonalCertificateService personalCertificateService;

    @Mock
    private UriInfo uriInfo;

    @Test
    public void should_create_certificate() throws CertificateException, InvalidParseOperationException {
        byte[] bytes = new byte[] {0, 2};

        adminPersonalCertificateResource.createIfNotPresent(bytes, uriInfo);

    }

}
