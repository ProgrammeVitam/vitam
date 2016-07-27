package fr.gouv.vitam.ingest.external.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class IngestExternalExceptionTest {
    
    @Test
    public final void givenIngestExternalExceptionWhenOccurThenThrow() {
        assertEquals(null, new IngestExternalException((String) null).getMessage());
        assertEquals("test", new IngestExternalException("test").getMessage());
        assertNotNull(new IngestExternalException(new Exception()).getCause());
    }

}
