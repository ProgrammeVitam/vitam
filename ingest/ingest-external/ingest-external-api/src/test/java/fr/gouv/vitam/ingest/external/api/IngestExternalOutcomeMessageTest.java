package fr.gouv.vitam.ingest.external.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class IngestExternalOutcomeMessageTest {

    @Test
    public void givenIngestExternalWhenGetAntiVirusResultThenSendOutcomeMessage() {
        assertEquals("Le SIP ne contient pas de virus", IngestExternalOutcomeMessage.valueOf("OK_VIRUS").value());
        assertEquals("Échec du contrôle sanitaire du SIP : fichier %s détecté comme infecté",
            IngestExternalOutcomeMessage.valueOf("KO_VIRUS").value());
    }

}
