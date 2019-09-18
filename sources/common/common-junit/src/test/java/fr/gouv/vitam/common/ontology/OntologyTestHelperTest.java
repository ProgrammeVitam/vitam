package fr.gouv.vitam.common.ontology;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class OntologyTestHelperTest {

    @Test
    public void loadOntologies() throws IOException {
        try (InputStream is = OntologyTestHelper.loadOntologies()) {
            assertNotNull(is);
        }
    }
}