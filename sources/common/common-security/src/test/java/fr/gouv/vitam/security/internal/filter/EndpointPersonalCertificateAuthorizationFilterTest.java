/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.security.internal.filter;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EndpointPersonalCertificateAuthorizationFilterTest {

    private static final String PERMISSION = "PERMISSION";
    private static final String BASE64_CERTIFICATE =
        "MIIFRjCCAy6gAwIBAgIBAjANBgkqhkiG9w0BAQsFADAtMQswCQYDVQQGEwJGUjEOMAwGA1UEBxMFUGFyaXMxDjAMBgNVBAoTBVZJVEFNMCAXDTE3MDgwMTExMTcwMFoYDzk5OTkxMjMxMjM1OTU5WjAtMQswCQYDVQQGEwJGUjEOMAwGA1UEBxMFUGFyaXMxDjAMBgNVBAoTBVZJVEFNMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAmfwb+NP44Ygv94LOOTLhQdDLwwqiwuP3fe3qFs0hCWCEOIorFcJ3cwZ2tc8udFtK8HxLrLxwi7zZweGrwXjt4zfLtfregppt0Xw5RaJtgNReu5i/2AKgtcxscYH/0yG1bDQ3vT2tv0YH4jzdfXfwTVzytqAV1M/CNZlWbcBXqDyZLeYUm5i/Dufndj16j4hw24tBsQT1o92P5qdfPaieZc4jpscGiMmyNYwEKcbqo5wiGVsiD+sU9/JXHT2q1f18JcuwJ5/fqzsADPKXudBvibCSaANf+ZNpRaWZ7y6e/kUDs8yrp4YaXzb331ioOGk4JE9ylv1hY5l8IbbvWracaxJv3xm3EnIp9M2/VMHrrGlkVjmGBUydJDiRhUAgaqXNpezwWulweQunAelBCU4PjO40J6t2wdLi5+f+b0OLJHJg0N9xdFsKrqsAVpjaYpqnDAG2Evcw0GFUuFm10JVLCAVpi6EwgxMnwExaeSrUvNE7Sdu95z2G8yBR9tYvYve+iiq/LzkR3cxK+9Pw4xDIEgQ0ZTCvY/6SBnHdAe3tqs4kODs57BZW4DD9ytpT73BKMf7EeZAE3tJd9p40uw/b41VF9bJvoW1ammZMl4H2OwdJi7+5DAMbBC1X2kMGWRo06IM99q+TKpfrUK+p4b6NfcSdrfr+n28vd8pzp16VDoMCAwEAAaNvMG0wDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUx0RltSTqHWXEisK78KQzn2SRpIkwCwYDVR0PBAQDAgSwMBEGCWCGSAGG+EIBAQQEAwIFoDAeBglghkgBhvhCAQ0EERYPeGNhIGNlcnRpZmljYXRlMA0GCSqGSIb3DQEBCwUAA4ICAQAZAZyzj7c5KBDLp0K324jUJB+oJAuf/D0vh0FqKvlCBTOLJsLfO2hsEL8ude5rVhP4goThIz1OjpnxFP+YmHUOtiQup21VGTaeTWn769/x6gRx/1eyJyws4ien/w7gBASLEKI7nGYAkeoYeZKWYTlfBgEisLwSsjcQeBeKcnUnuWJauiALPnBntkAnM7PotASA8Hk+dle9lng1sMlzHVcTVauCuvrk8WCec9ja56+b9N4JbaCwYFmMRlMzdBQU4LXrbqxlakpa2ua0mSzCKe8WHI9m5uCHhUi3fMa7KJsN5nBHkw63nFwGQwyRNQYgZiyhmzXtez/l+8f1quMAPoTIlsG+TBFW0s9+LqY8ufE9+8u8S1FynZlsgfIoKl2bKVXWWrZVfJ+S8mh6mH4V3MuhLwljv+/6HDZCc3FoY5eN/lyWI49Maz5W87bKqNyecYtrBlvML7k5UeOLtgNuUsTBlzFTxMkaQHOSpMyrHZ/yVPNVfuP3cCKvzMPHFGHzJZK0qvz4zdFdx7YzBq+I6YLvRES9b+DkvdrTOpZI2GjKuP5m13kcUjsFeqJR6rb+o1kJuCj/QMC2OjMXMlDqNa8mL5ooGQmYOzHkfq4vdKLG/Fvbpw2DDrwv9jKmw2l6eWLYzuIpvz7sqUHwi30wScXSm/FCKF9DjzODUpSkBvDiaA==";
    private static final byte[] CERTIFICATE = BaseXx.getFromBase64(BASE64_CERTIFICATE);

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    InternalSecurityClient internalSecurityClient;

    @Mock
    ContainerRequestContext context;

    private EndpointPersonalCertificateAuthorizationFilter instance;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Before
    public void setUp() throws Exception {
        instance = new EndpointPersonalCertificateAuthorizationFilter(PERMISSION, internalSecurityClient);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(internalSecurityClient, context);
    }

    @Test
    public void should_fail_when_permission_is_unknown() throws Exception {
        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION)).thenReturn(
            new IsPersonalCertificateRequiredModel(ERROR_UNKNOWN_PERMISSION)
        );

        assertThatThrownBy(() -> instance.filter(context)).isInstanceOf(IllegalStateException.class);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
    }

    @Test
    public void should_not_abort_when_personal_certificate_is_not_required() throws Exception {
        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION)).thenReturn(
            new IsPersonalCertificateRequiredModel(IGNORED_PERSONAL_CERTIFICATE)
        );

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_abort_when_personal_certificate_is_required_and_certificate_ok() throws Exception {
        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION)).thenReturn(
            new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE)
        );
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(BASE64_CERTIFICATE);
        doNothing().when(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));
    }

    @Test
    @RunWithCustomExecutor
    public void should_abort_when_personal_certificate_is_required_and_certificate_ko() throws Exception {
        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION)).thenReturn(
            new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE)
        );
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(BASE64_CERTIFICATE);
        doThrow(InternalSecurityException.class)
            .when(internalSecurityClient)
            .checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(eq(CERTIFICATE), eq(PERMISSION));
        verify(context).abortWith(any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_abort_when_personal_certificate_is_required_and_missing_certificate() throws Exception {
        when(internalSecurityClient.isPersonalCertificateRequiredByPermission(PERMISSION)).thenReturn(
            new IsPersonalCertificateRequiredModel(REQUIRED_PERSONAL_CERTIFICATE)
        );
        when(context.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE)).thenReturn(null);
        doThrow(InternalSecurityException.class)
            .when(internalSecurityClient)
            .checkPersonalCertificate(null, PERMISSION);

        instance.filter(context);

        verify(internalSecurityClient).isPersonalCertificateRequiredByPermission(PERMISSION);
        verify(context).getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        verify(internalSecurityClient).checkPersonalCertificate(null, PERMISSION);
        verify(context).abortWith(any());
    }
}
