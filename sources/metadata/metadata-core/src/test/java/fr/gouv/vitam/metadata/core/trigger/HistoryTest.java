/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.junit.Test;

public class HistoryTest {

    @Test
    public void testGetArrayNode() {
        final String TEST_FIELD = "testField";
        final String TEST_VALUE = "testValue";
        final String TEST_PATH = "testLevel1.testLevel2";
        final int TEST_VERSION = 1;

        JsonNode nodeToHistory = JsonHandler.createObjectNode().put(TEST_FIELD, TEST_VALUE);
        History history = new History(TEST_PATH, TEST_VERSION, nodeToHistory);

        JsonNode arrayNode = history.getArrayNode();
        Assert.assertTrue(arrayNode.isArray());

        String arrayNodeString = arrayNode.toString();
        JsonPath path = JsonPath.from(arrayNodeString);
        Assert.assertNotNull(path.get("[0].ud"));
        Assert.assertNotNull(path.get("[0].data"));
        Assert.assertEquals(TEST_VERSION, path.getInt("[0].data._v"));
        Assert.assertEquals(TEST_VALUE, path.getString("[0].data." + TEST_PATH + "." + TEST_FIELD));
    }

    @Test
    public void testGetNode() {
        final String TEST_FIELD = "testField";
        final String TEST_VALUE = "testValue";
        final String TEST_PATH = "testLevel1.testLevel2";
        final int TEST_VERSION = 1;

        JsonNode nodeToHistory = JsonHandler.createObjectNode().put(TEST_FIELD, TEST_VALUE);
        History history = new History(TEST_PATH, TEST_VERSION, nodeToHistory);

        JsonNode node = history.getNode();
        Assert.assertTrue(node.isObject());

        String arrayNodeString = node.toString();
        JsonPath path = JsonPath.from(arrayNodeString);
        Assert.assertNotNull(path.get("ud"));
        Assert.assertNotNull(path.get("data"));
        Assert.assertEquals(TEST_VERSION, path.getInt("data._v"));
        Assert.assertEquals(TEST_VALUE, path.getString("data." + TEST_PATH + "." + TEST_FIELD));
    }
}
