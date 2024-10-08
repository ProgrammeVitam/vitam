/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.metadata.core.reconstruction.domain.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.core.reconstruction.model.PurgedPersistentIdentifier;
import fr.gouv.vitam.metadata.core.reconstruction.model.ReconstructionOperation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UnitPurgedPersistentIdentifierExtractorTest {

    public static final String PERSISTENT_IDENTIFIER =
        "[{\"PersistentIdentifierType\":\"ark\",\"PersistentIdentifierContent\":\"ark:/666567/001a957db5eadaac\"},{\"PersistentIdentifierType\":\"ark\",\"PersistentIdentifierContent\":\"ark:/26661/001d957db5eadaac\"}]";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private UnitPurgedPersistentIdentifierExtractor unitPurgedPersistentIdentifierExtractor;

    @Before
    public void setup() throws Exception {
        unitPurgedPersistentIdentifierExtractor = new UnitPurgedPersistentIdentifierExtractor();
    }

    @Test
    public void testExtractAndSavePurgedPersistentIdentifier() throws Exception {
        // Given
        JsonNode element = createSampleJsonNode();
        ReconstructionOperation operation = createSampleOperation();

        // When
        List<PurgedPersistentIdentifier> purgedPersistentIdentifiers =
            unitPurgedPersistentIdentifierExtractor.extractPurgedPersistentIdentifier(element, operation);

        // Then
        assertThat(purgedPersistentIdentifiers).isNotEmpty();
    }

    @Test
    public void testExtractAndSavePurgedPersistentIdentifierEmpty() throws Exception {
        // Given
        ObjectNode element = createSampleJsonNode();
        element.set("persistentIdentifier", JsonHandler.createArrayNode());
        ReconstructionOperation operation = createSampleOperation();

        // When
        List<PurgedPersistentIdentifier> purgedPersistentIdentifiers =
            unitPurgedPersistentIdentifierExtractor.extractPurgedPersistentIdentifier(element, operation);

        // Then
        assertThat(purgedPersistentIdentifiers).isEmpty();
    }

    @Test
    public void testExtractAndSavePurgedPersistentIdentifierNull() throws Exception {
        // Given
        ObjectNode element = createSampleJsonNode();
        element.set("persistentIdentifier", null);
        ReconstructionOperation operation = createSampleOperation();

        // When
        List<PurgedPersistentIdentifier> purgedPersistentIdentifiers =
            unitPurgedPersistentIdentifierExtractor.extractPurgedPersistentIdentifier(element, operation);

        // Then
        assertThat(purgedPersistentIdentifiers).isEmpty();
    }

    @Test
    public void testBuildUnitPurgedPersistentIdentifier() throws Exception {
        // Given
        JsonNode element = createSampleJsonNode();
        ReconstructionOperation operation = createSampleOperation();

        // When
        final PurgedPersistentIdentifier purgedPersistentIdentifier =
            unitPurgedPersistentIdentifierExtractor.buildUnitPurgedPersistentIdentifier(element, operation);

        // Then
        assertThat(purgedPersistentIdentifier.getId()).isEqualTo("aeaqaaaaaae6eg5mabudoamkdsdghiiaaaba");
        assertThat(purgedPersistentIdentifier.getTenant()).isEqualTo(0);
        assertThat(purgedPersistentIdentifier.getType()).isEqualTo("Unit");
        assertThat(purgedPersistentIdentifier.getPersistentIdentifier().size()).isEqualTo(2);
        assertThat(purgedPersistentIdentifier.getArchivalAgencyIdentifier()).isEqualTo("identifier4");
        assertThat(purgedPersistentIdentifier.getObjectGroupId()).isNull();
    }

    private ObjectNode createSampleJsonNode() throws Exception {
        ObjectNode node = JsonHandler.createObjectNode();
        node.put("id", "aeaqaaaaaae6eg5mabudoamkdsdghiiaaaba");
        node.set("persistentIdentifier", new ObjectMapper().readTree(PERSISTENT_IDENTIFIER));
        node.put("type", "Unit");
        node.put("archivalAgencyIdentifier", "identifier4");
        return node;
    }

    private ReconstructionOperation createSampleOperation() {
        return ReconstructionOperation.builder()
            .setTenant(0)
            .setId("aeeaaaaaace6eg5mabvsaamkdsdfzeaaaaaq")
            .setType("ELIMINATION_ACTION")
            .build();
    }
}
