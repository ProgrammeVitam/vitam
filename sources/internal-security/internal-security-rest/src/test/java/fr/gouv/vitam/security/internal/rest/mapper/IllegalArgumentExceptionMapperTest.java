package fr.gouv.vitam.security.internal.rest.mapper;

import org.junit.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public class IllegalArgumentExceptionMapperTest {
    private IllegalArgumentExceptionMapper illegalArgumentExceptionMapper = new IllegalArgumentExceptionMapper();

    @Test
    public void should_convert_exception_to_response() throws Exception {
        // Given
        String message = "certificate is empty";
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException(message);

        // When
        Response response = illegalArgumentExceptionMapper.toResponse(illegalArgumentException);

        // Then
        assertThat(response.getStatusInfo()).isEqualTo(BAD_REQUEST);
        assertThat(response.getEntity()).isEqualTo(message);
    }

}