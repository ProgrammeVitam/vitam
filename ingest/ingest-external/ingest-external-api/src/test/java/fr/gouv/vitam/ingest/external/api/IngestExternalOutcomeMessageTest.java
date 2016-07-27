package fr.gouv.vitam.ingest.external.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class IngestExternalOutcomeMessageTest {

    @Test
    public void givenIngestExternalWhenGetAntiVirusResultThenSendOutcomeMessage() {
        assertEquals("Le SIP ne contient pas de virus", IngestExternalOutcomeMessage.valueOf("OK").value());
        assertEquals("SIP infecté", IngestExternalOutcomeMessage.valueOf("KO").value());
    }

}
