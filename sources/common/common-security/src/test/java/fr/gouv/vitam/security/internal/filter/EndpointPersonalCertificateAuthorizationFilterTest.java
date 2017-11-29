package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.container.ContainerRequestContext;

import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.ERROR_UNKNOWN_PERMISSION;
import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.IGNORED_PERSONAL_CERTIFICATE;
import static fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel.Response.REQUIRED_PERSONAL_CERTIFICATE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EndpointPersonalCertificateAuthorizationFilterTest {

    private static final String PERMISSION = "PERMISSION";
    private static final String CERTIFICATE_HEADER = "TEST";
    private static final byte[] CERTIFICATE = CERTIFICATE_HEADER.getBytes();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    InternalSecurityClient internalSecurityClient;

    @Mock
    ContainerRequestContext context;

    private EndpointPersonalCertificateAuthorizationFilter instance;

    @Before
    public void setUp() throws Exception {
        instance = new EndpointPersonalCertificateAuthorizationFilter(
            PERMISSION, internalSecurityClient);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(internalSecurityClient, context);
    }

    @Test
    public void should_fail_when_permission_is_unknown() throws Exception {

        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION))
            .thenReturn(new IsPersonalCertificateRequiredModel(ERROR_UNKNOWN_PERMISSION));

        assertThatThrownBy(() -> instance.filter(context)).isInstanceOf(IllegalStateException.class);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
    }

    @Test
    public void should_not_abort_when_personal_certificate_is_not_required() throws Exception {

        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION))
            .thenReturn(new IsPersonalCertificateRequiredModel(IGNORED_PERSONAL_CERTIFICATE));

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
    }

    @Test
    public void should_not_abort_when_personal_certificate_is_required_and_certificate_ok() throws Exception {

        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION))
            .thenReturn(new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE));
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(CERTIFICATE_HEADER);
        doNothing().when(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));
    }

    @Test
    public void should_abort_when_personal_certificate_is_required_and_certificate_ko() throws Exception {

        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION))
            .thenReturn(new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE));
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(CERTIFICATE_HEADER);
        doThrow(InternalSecurityException.class).when(internalSecurityClient)
            .checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));
        verify(context).abortWith(anyObject());
    }

    @Test
    public void should_abort_when_personal_certificate_is_required_and_missing_certificate() throws Exception {

        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION))
            .thenReturn(new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE));
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(null);
        doThrow(InternalSecurityException.class).when(internalSecurityClient)
            .checkPersonalCertificate(null, PERMISSION);

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(null, PERMISSION);
        verify(context).abortWith(anyObject());
    }
}
