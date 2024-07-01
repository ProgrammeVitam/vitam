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
package fr.gouv.vitam.common.auth.web.filter;

import fr.gouv.vitam.common.GlobalDataRest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CertUtilsTest {

    private static final String HTTPD_CERTIFICATE_FORMAT =
        "" +
        "-----BEGIN CERTIFICATE----- " +
        "MIIDeTCCAmGgAwIBAgIUSCGUFJwxYU4vMvX1nbRiqHIDUOUwDQYJKoZIhvcNAQEL " +
        "BQAwGjEYMBYGA1UEAwwPaW50ZXJtZWRpYXRlLWNhMB4XDTIzMDMxNTA3NDcyMloX " +
        "DTQzMDMxMDA3NDcyMlowETEPMA0GA1UEAwwGY2xpZW50MIIBIjANBgkqhkiG9w0B " +
        "AQEFAAOCAQ8AMIIBCgKCAQEApvEtXTG0+XF9jjKZJANVVvz09uHAegZk/31PaWCr " +
        "1Uakq3UlG4sMO7/gYpam/sbOnA/6qNGmVWsh1O0kI6+eKTA3H4cWpqrGJ1UCNFpV " +
        "Uu5rWg8NCFR0Vc40TRaJYb0mCE0Tz5qSXw/tJwiHekpI3xyAlIZRax8hxiViOCKs " +
        "bhgCTTTjiVwcjwyBnlCOqT/d3ZGOLWaHDMvSKc20TctuhLlmblTwYoWoaCHN3zlC " +
        "d3j3oyZ+WQKU3KvRF6lKhS1pug5thEQPGrP4/W1A7W1367BOEnYZI3rzAnTO8WB4 " +
        "kBi/j3pmoPJRBT8fmlIW4qecRiGlhksLgkeLHaAAC0ZH0QIDAQABo4G/MIG8MB0G " +
        "A1UdDgQWBBSXiTHUVeNgrAKV/XGt4J7QEKmMazBNBgNVHSMERjBEgBSmY2e3H14U " +
        "4xvnvLwZDxVkeI1xG6EWpBQwEjEQMA4GA1UEAwwHcm9vdC1jYYIUO/EBBATyqs07 " +
        "wOzMDxZ/D9QlHLswCQYDVR0SBAIwADAMBgNVHRMBAf8EAjAAMAsGA1UdDwQEAwIH " +
        "gDARBglghkgBhvhCAQEEBAMCB4AwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDQYJKoZI " +
        "hvcNAQELBQADggEBAETePX/A5kUDedD9jJR1aPhMF6VSU55Fh9DXeDrVyTRae46P " +
        "TV4LYF7fmgI+jBTSxTCzAzZzMXmLK5UIUGH62X9vgT34F5Btk3KE4jBfsaWMn3Mc " +
        "WNtZsomhnVekrLe1ZBBhlNwRF5WaX9zYk8kyMw3ZWKDwb/dXnikqqIK2+E2WuPgU " +
        "t1ef0wHJRmaDnoox6vm/K1rYYo4jykuhdVYXxBXz7Vm0i7jUoN+BYmAgQ3zRFIv9 " +
        "bwPhO3KheGRPkB8ZUtZuSfxBTTLX+AVGjCHGC/SB4O5HMdwe+QANdyr61RQZV39C " +
        "WK31zmcgpI+s9vol2bn/VQL6szy47RXmctnkZVk= " +
        "-----END CERTIFICATE----- ";

    private static final String NGINX_CERTIFICATE_FORMAT =
        "" +
        "-----BEGIN%20CERTIFICATE-----%0A" +
        "MIIDeTCCAmGgAwIBAgIUSCGUFJwxYU4vMvX1nbRiqHIDUOUwDQYJKoZIhvcNAQEL%0A" +
        "BQAwGjEYMBYGA1UEAwwPaW50ZXJtZWRpYXRlLWNhMB4XDTIzMDMxNTA3NDcyMloX%0A" +
        "DTQzMDMxMDA3NDcyMlowETEPMA0GA1UEAwwGY2xpZW50MIIBIjANBgkqhkiG9w0B%0A" +
        "AQEFAAOCAQ8AMIIBCgKCAQEApvEtXTG0%2BXF9jjKZJANVVvz09uHAegZk%2F31PaWCr%0A" +
        "1Uakq3UlG4sMO7%2FgYpam%2FsbOnA%2F6qNGmVWsh1O0kI6%2BeKTA3H4cWpqrGJ1UCNFpV%0A" +
        "Uu5rWg8NCFR0Vc40TRaJYb0mCE0Tz5qSXw%2FtJwiHekpI3xyAlIZRax8hxiViOCKs%0A" +
        "bhgCTTTjiVwcjwyBnlCOqT%2Fd3ZGOLWaHDMvSKc20TctuhLlmblTwYoWoaCHN3zlC%0A" +
        "d3j3oyZ%2BWQKU3KvRF6lKhS1pug5thEQPGrP4%2FW1A7W1367BOEnYZI3rzAnTO8WB4%0A" +
        "kBi%2Fj3pmoPJRBT8fmlIW4qecRiGlhksLgkeLHaAAC0ZH0QIDAQABo4G%2FMIG8MB0G%0A" +
        "A1UdDgQWBBSXiTHUVeNgrAKV%2FXGt4J7QEKmMazBNBgNVHSMERjBEgBSmY2e3H14U%0A" +
        "4xvnvLwZDxVkeI1xG6EWpBQwEjEQMA4GA1UEAwwHcm9vdC1jYYIUO%2FEBBATyqs07%0A" +
        "wOzMDxZ%2FD9QlHLswCQYDVR0SBAIwADAMBgNVHRMBAf8EAjAAMAsGA1UdDwQEAwIH%0A" +
        "gDARBglghkgBhvhCAQEEBAMCB4AwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDQYJKoZI%0A" +
        "hvcNAQELBQADggEBAETePX%2FA5kUDedD9jJR1aPhMF6VSU55Fh9DXeDrVyTRae46P%0A" +
        "TV4LYF7fmgI%2BjBTSxTCzAzZzMXmLK5UIUGH62X9vgT34F5Btk3KE4jBfsaWMn3Mc%0A" +
        "WNtZsomhnVekrLe1ZBBhlNwRF5WaX9zYk8kyMw3ZWKDwb%2FdXnikqqIK2%2BE2WuPgU%0A" +
        "t1ef0wHJRmaDnoox6vm%2FK1rYYo4jykuhdVYXxBXz7Vm0i7jUoN%2BBYmAgQ3zRFIv9%0A" +
        "bwPhO3KheGRPkB8ZUtZuSfxBTTLX%2BAVGjCHGC%2FSB4O5HMdwe%2BQANdyr61RQZV39C%0A" +
        "WK31zmcgpI%2Bs9vol2bn%2FVQL6szy47RXmctnkZVk%3D%0A" +
        "-----END%20CERTIFICATE-----%0A";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    public void givenNoCertificateWithHeaderUsageDisabledThenNoCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(null).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).isNull();
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenNoCertificateWithHeaderUsageEnabledThenNoCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(null).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).isNull();
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInRequestContextWithHeaderUsageDisabledThenCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(null).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInRequestContextWithHeaderUsageEnabledThenCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(null).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInHttpdFormatHeaderWithHeaderUsageEnabledThenCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(HTTPD_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0].getSubjectDN().getName()).isEqualTo("CN=client");
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInNginxFormatHeaderWithHeaderUsageEnabledThenCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(NGINX_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0].getSubjectDN().getName()).isEqualTo("CN=client");
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInHttpdFormatHeaderWithHeaderUsageDisabledThenNoCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(HTTPD_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).isNull();
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInNginxFormatHeaderWithHeaderUsageDisabledThenNotCertificateFound() {
        // Given
        doReturn(null).when(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(NGINX_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).isNull();
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
    }

    @Test
    public void givenCertificateInRequestContextAndInHttpdFormatHeaderWithHeaderUsageEnabledThenRequestContextCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(HTTPD_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInRequestContextAndInNginxFormatHeaderWithHeaderUsageEnabledThenRequestContextCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(NGINX_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInRequestContextAndInHttpdFormatHeaderWithHeaderUsageDisabledThenRequestContextCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(HTTPD_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }

    @Test
    public void givenCertificateInRequestContextAndInNginxFormatHeaderWithHeaderUsageDisabledThenRequestContextCertificateFound() {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        doReturn(new X509Certificate[] { cert })
            .when(servletRequest)
            .getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        doReturn(NGINX_CERTIFICATE_FORMAT).when(servletRequest).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);

        // When
        X509Certificate[] result = CertUtils.extractCert(servletRequest, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo(cert);
        verify(servletRequest).getAttribute(CertUtils.JAVAX_SERVLET_REQUEST_X_509_CERTIFICATE);
        verify(servletRequest, never()).getHeader(GlobalDataRest.X_SSL_CLIENT_CERT);
        verifyNoMoreInteractions(servletRequest);
    }
}
