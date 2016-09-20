package fr.gouv.vitam.ingest.external.common.model.response;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class IngestExternalErrorTest {
    private int code = 123;
    private List<IngestExternalError> errors = new ArrayList<IngestExternalError>();

    @Test
    public void testSettersandGetters() {
        IngestExternalError ingestExtError = new IngestExternalError(code);
        errors.add(ingestExtError);
        assertEquals(code, ingestExtError.setCode(code).getCode());
        assertEquals("description", ingestExtError.setDescription("description").getDescription());
        assertEquals("context", ingestExtError.setContext("context").getContext());
        assertEquals("message", ingestExtError.setMessage("message").getMessage());
        assertEquals("state", ingestExtError.setState("state").getState());
        assertEquals(code, ingestExtError.setErrors(errors).getErrors().get(0).getCode());
    }
}
