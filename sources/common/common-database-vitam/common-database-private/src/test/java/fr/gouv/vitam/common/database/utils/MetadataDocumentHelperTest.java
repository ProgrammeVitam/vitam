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
package fr.gouv.vitam.common.database.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetadataDocumentHelperTest {

    @Test
    public void should_retrieveStrategyId_when_presentInRawDocument() {
        // Given
        ObjectNode documentJson = JsonHandler.createObjectNode();
        ObjectNode storageJson = JsonHandler.createObjectNode();
        storageJson.put("strategyId", "strategyIdValue");
        documentJson.set("_storage", storageJson);

        // When
        String extractedStrategyId = MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(documentJson);

        // Then
        assertThat(extractedStrategyId).isEqualTo("strategyIdValue");
    }

    @Test
    public void should_throwIllegalArgumentException_when_StrategyIdNotPresentInRawDocument() {
        // Given
        ObjectNode documentJson = JsonHandler.createObjectNode();
        ObjectNode storageJson = JsonHandler.createObjectNode();
        documentJson.set("_storage", storageJson);

        // When + Then
        assertThatThrownBy(() -> {
            MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(documentJson);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_throwIllegalArgumentException_when_JsonNullRawUnit() {
        // Given
        ObjectNode unitJson = null;

        // When + Then
        assertThatThrownBy(() -> {
            MetadataDocumentHelper.getStrategyIdFromRawUnitOrGot(unitJson);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_retrieveStrategyId_when_presentInUnit() {
        // Given
        ObjectNode unitJson = JsonHandler.createObjectNode();
        ObjectNode storageJson = JsonHandler.createObjectNode();
        storageJson.put("strategyId", "strategyIdValue");
        unitJson.set("#storage", storageJson);

        // When
        String extractedStrategyId = MetadataDocumentHelper.getStrategyIdFromUnit(unitJson);

        // Then
        assertThat(extractedStrategyId).isEqualTo("strategyIdValue");
    }

    @Test
    public void should_throwIllegalArgumentException_when_StrategyIdNotPresentInUnit() {
        // Given
        ObjectNode unitJson = JsonHandler.createObjectNode();
        ObjectNode storageJson = JsonHandler.createObjectNode();
        unitJson.set("#storage", storageJson);

        // When + Then
        assertThatThrownBy(() -> {
            MetadataDocumentHelper.getStrategyIdFromUnit(unitJson);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_throwIllegalArgumentException_when_JsonNullUnit() {
        // Given
        ObjectNode unitJson = null;

        // When + Then
        assertThatThrownBy(() -> {
            MetadataDocumentHelper.getStrategyIdFromUnit(unitJson);
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
