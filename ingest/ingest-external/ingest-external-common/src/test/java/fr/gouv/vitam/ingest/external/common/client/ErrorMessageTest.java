package fr.gouv.vitam.ingest.external.common.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class ErrorMessageTest {

    @Test
    public void givenErrorMessage() {
        assertEquals("Ingest external upload failed ", ErrorMessage.valueOf("INGEST_EXTERNAL_UPLOAD_ERROR").getMessage());
    }

}
