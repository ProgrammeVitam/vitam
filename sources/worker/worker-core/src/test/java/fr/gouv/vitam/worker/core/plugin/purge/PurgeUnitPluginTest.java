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
package fr.gouv.vitam.worker.core.plugin.purge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import fr.gouv.vitam.common.model.objectgroup.PersistentIdentifierModel;
import java.util.List;
import org.junit.Test;

public class PurgeUnitPluginTest {

  @Test
  public void testExtractPersistentIdentifiersFromUnit() {
    JsonNodeFactory factory = JsonNodeFactory.instance;
    ArrayNode identifierArray = factory.arrayNode();

    JsonNode identifier1 = factory.objectNode()
        .put("PersistentIdentifierType", "ark")
        .put("PersistentIdentifierOrigin", "OriginatingAgency")
        .put("PersistentIdentifierReference", "Agency-00021")
        .put("PersistentIdentifierContent", "ark:/23567/001a9d7db5eadabac_binary_master");

    JsonNode identifier2 = factory.objectNode()
        .put("PersistentIdentifierType", "doi")
        .put("PersistentIdentifierOrigin", "OriginatingAgency")
        .put("PersistentIdentifierReference", "Agency-00221")
        .put("PersistentIdentifierContent", "doi:10.1234/example");

    identifierArray.add(identifier1);
    identifierArray.add(identifier2);

    JsonNode unitJson = factory.objectNode().set("PersistentIdentifier", identifierArray);

    List<PersistentIdentifierModel> identifiers = PurgeUnitPlugin.extractPersistentIdentifiersFromUnit(unitJson);

    assertThat(2).isEqualTo(identifiers.size());

    PersistentIdentifierModel identifierModel1 = identifiers.get(0);
    assertThat("ark").isEqualTo(identifierModel1.getPersistentIdentifierType());
    assertThat("OriginatingAgency").isEqualTo(identifierModel1.getPersistentIdentifierOrigin());
    assertThat("Agency-00021").isEqualTo(identifierModel1.getPersistentIdentifierReference());
    assertThat("ark:/23567/001a9d7db5eadabac_binary_master").isEqualTo(identifierModel1.getPersistentIdentifierContent());

    PersistentIdentifierModel identifierModel2 = identifiers.get(1);
    assertThat("doi").isEqualTo(identifierModel2.getPersistentIdentifierType());
    assertThat("OriginatingAgency").isEqualTo(identifierModel2.getPersistentIdentifierOrigin());
    assertThat("Agency-00221").isEqualTo(identifierModel2.getPersistentIdentifierReference());
    assertThat("doi:10.1234/example").isEqualTo(identifierModel2.getPersistentIdentifierContent());
  }

  @Test
  public void testMapJsonToPersistentIdentifierModel() {
    JsonNodeFactory factory = JsonNodeFactory.instance;

    JsonNode identifierNode = factory.objectNode()
        .put("PersistentIdentifierType", "ark")
        .put("PersistentIdentifierOrigin", "OriginatingAgency")
        .put("PersistentIdentifierReference", "Agency-00021")
        .put("PersistentIdentifierContent", "ark:/23567/001a9d7db5eadabac_binary_master");

    PersistentIdentifierModel identifierModel = PurgeUnitPlugin.mapJsonToPersistentIdentifierModel(identifierNode);

    assertThat("ark").isEqualTo(identifierModel.getPersistentIdentifierType());
    assertThat("OriginatingAgency").isEqualTo(identifierModel.getPersistentIdentifierOrigin());
    assertThat("Agency-00021").isEqualTo(identifierModel.getPersistentIdentifierReference());
    assertThat("ark:/23567/001a9d7db5eadabac_binary_master").isEqualTo(identifierModel.getPersistentIdentifierContent());
  }

}