package fr.gouv.vitam.security.internal.rest.mapper;

import org.junit.Test;

import javax.ws.rs.core.Response;
import java.security.cert.CertificateException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public class CertificateExceptionMapperTest {

    private CertificateExceptionMapper certificateExceptionMapper = new CertificateExceptionMapper();

    @Test
    public void should_convert_exception_to_response() throws Exception {
        // Given
        String message = "invalid certificate";
        CertificateException certificateException = new CertificateException(message);

        // When
        Response response = certificateExceptionMapper.toResponse(certificateException);

        // Then
        assertThat(response.getStatusInfo()).isEqualTo(BAD_REQUEST);
        assertThat(response.getEntity()).isEqualTo(message);
    }

}