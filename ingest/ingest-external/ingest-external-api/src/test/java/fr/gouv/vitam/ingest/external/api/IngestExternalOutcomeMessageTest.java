package fr.gouv.vitam.ingest.external.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class IngestExternalOutcomeMessageTest {

    @Test
    public void givenIngestExternalWhenGetAntiVirusResultThenSendOutcomeMessage() {
        assertEquals("Contrôle sanitaire réalisé avec succès : aucun virus détecté", IngestExternalOutcomeMessage.valueOf("OK_VIRUS").value());
        assertEquals("Echec Contrôle sanitaire : virus détecté SIP infecté",
            IngestExternalOutcomeMessage.valueOf("KO_VIRUS").value());
    }

}
