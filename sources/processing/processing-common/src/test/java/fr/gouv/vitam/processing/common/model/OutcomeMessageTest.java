package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OutcomeMessageTest {
    @Test
    public void testEnum() {
        assertEquals(OutcomeMessage.CHECK_CONFORMITY_OK.value(),
            "Contrôle de conformité des objets réalisé avec succès");
        assertEquals(OutcomeMessage.CHECK_CONFORMITY_KO.value(), "Erreur de contrôle de conformité des objets");
        assertEquals(OutcomeMessage.CHECK_OBJECT_NUMBER_OK.value(),
            "Contrôle du nombre des objets réalisé avec succès");
        assertEquals(OutcomeMessage.CHECK_OBJECT_NUMBER_KO.value(), "Erreur de contrôle du nombre des objets");
        assertEquals(OutcomeMessage.CHECK_VERSION_OK.value(), "Contrôle des versions réalisé avec succès");
        assertEquals(OutcomeMessage.CHECK_VERSION_KO.value(), "Erreur de contrôle des versions");
        assertEquals(OutcomeMessage.CHECK_MANIFEST_OK.value(), "Contrôle du bordereau réalisé avec succès");
        assertEquals(OutcomeMessage.CHECK_MANIFEST_KO.value(), "Erreur de contrôle du bordereau");
        assertEquals(OutcomeMessage.EXTRACT_MANIFEST_OK.value(), "Extraction du bordereau réalisé avec succès");
        assertEquals(OutcomeMessage.EXTRACT_MANIFEST_KO.value(), "Erreur de l'extraction du bordereau");
        assertEquals(OutcomeMessage.WORKFLOW_INGEST_OK.value(), "Entrée effectuée avec succès");
        assertEquals(OutcomeMessage.WORKFLOW_INGEST_KO.value(), "Entrée en échec");
    }

}
