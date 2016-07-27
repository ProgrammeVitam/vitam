package fr.gouv.vitam.ingest.external.common.model.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class RequestResponseErrorTest {

    @Test
    public void testRequestResponseError() {
        RequestResponseError requestError = new RequestResponseError();
        requestError.setError(new VitamError(1)
            .setCode(2)
            .setContext("context")
            .setDescription("description")
            .setMessage("message")
            .setState("state")
            .setErrors(new ArrayList<>()));
        assertEquals("state", requestError.getError().getState());
        assertEquals(2, requestError.getError().getCode());
        assertEquals("message", requestError.getError().getMessage());
        assertEquals("description", requestError.getError().getDescription());
        assertEquals("context", requestError.getError().getContext());
        assertTrue(requestError.getError().getErrors().isEmpty());
    }

}
