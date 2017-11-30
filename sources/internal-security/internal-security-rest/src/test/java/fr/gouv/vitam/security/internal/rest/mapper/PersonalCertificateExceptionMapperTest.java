package fr.gouv.vitam.security.internal.rest.mapper;


import fr.gouv.vitam.security.internal.rest.exeption.PersonalCertificateException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

public class PersonalCertificateExceptionMapperTest {

    private PersonalCertificateExceptionMapper certificateExceptionMapper = new PersonalCertificateExceptionMapper();

    @Test
    public void should_convert_exception_to_response() throws Exception {
        // Given
        String message = "invalid certificate";
        PersonalCertificateException personalCertificateException = new PersonalCertificateException(message);

        // When
        Response response = certificateExceptionMapper.toResponse(personalCertificateException);

        // Then
        assertThat(response.getStatusInfo()).isEqualTo(UNAUTHORIZED);
        //assertThat(response.getEntity()).isEqualTo(message);
    }
}
