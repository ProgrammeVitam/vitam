package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamDescriptionType;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamCardinality.one;
import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamType.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MongoDbInMemoryTest {
    private final static String jsonNodeValue =
        "{" + "\"oldValue\": \"value\"," + "\"oldField\": \"valueOld\"," + "\"nullField\": null," +
            "\"numberTen\": 10," + "\"numberAsString\": \"2\"," + "\"arrayToPop\": [\"val1\", \"val2\", \"val3\"]," +
            "\"arrayToPull\": [\"v1\", \"v2\", \"v3\"]," + "\"ArrayToAdd\": [\"val1\", \"val2\"]," +
            "\"ArrayToPush\": [\"v1\", \"v2\"]," + "\"subItem\": {\"subArray\": [\"subValue\"], \"subInt\": 42}," +
            "\"expandField\": \"XXX\", \"arrayRegex\": [ \"V1\", \"\", \"V2\"] }";
    private static JsonNode jsonDocument;
    private static MongoDbInMemory mDIM;
    private static AbstractParser<?> parser = new UpdateParserSingle(new SingleVarNameAdapter());
    private final static AbstractParser<?> emptyParser = new UpdateParserSingle(new SingleVarNameAdapter());

    private static String requestAdd = "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val3\"] } }]}";
    private static String requestAddMultiple =
        "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val3\", \"val4\"] } }]}";
    private static String requestAddSubField =
        "{\"$action\": [{ \"$add\": {\"subItem.subArray\": [\"val3\"] } }]}";
    private static String requestAddWrongField =
        "{\"$action\": [{ \"$add\": {\"numberAsString\": [\"val3\"] } }]}";
    private static String requestAddDuplicate =
        "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val2\"] } }]}";
    private static String requestAddOnNull =
        "{\"$action\": [{ \"$add\": {\"nullField\": [\"val2\"] } }]}";

    private static String requestInc = "{\"$action\": [{ \"$inc\": {\"numberTen\": 2 } }]}";
    private static String requestIncSubField = "{\"$action\": [{ \"$inc\": {\"subItem.subInt\": 2 } }]}";
    private static String requestIncWrongField = "{\"$action\": [{ \"$inc\": {\"numberAsString\": 2 } }]}";
    private static String requestIncWrongValue = "{\"$action\": [{ \"$inc\": {\"numberTen\": \"b\" } }]}";
    private static String requestIncOnNull = "{\"$action\": [{ \"$inc\": {\"nullField\": 1 } }]}";

    private static String requestMin = "{\"$action\": [{ \"$min\": {\"numberTen\": 1 } }]}";
    private static String requestMinSubField = "{\"$action\": [{ \"$min\": {\"subItem.subInt\": 1 } }]}";
    private static String requestMinInDb = "{\"$action\": [{ \"$min\": {\"numberTen\": 100 } }]}";
    private static String requestMinWrongField = "{\"$action\": [{ \"$min\": {\"numberAsString\": 1 } }]}";
    private static String requestMinWrongValue = "{\"$action\": [{ \"$min\": {\"numberTen\": \"b\" } }]}";
    private static String requestMinOnNull = "{\"$action\": [{ \"$min\": {\"nullField\": 1 } }]}";

    private static String requestMax = "{\"$action\": [{ \"$max\": {\"numberTen\": 100 } }]}";
    private static String requestMaxSubField = "{\"$action\": [{ \"$max\": {\"subItem.subInt\": 100 } }]}";
    private static String requestMaxInDb = "{\"$action\": [{ \"$max\": {\"numberTen\": 1 } }]}";
    private static String requestMaxWrongField = "{\"$action\": [{ \"$max\": {\"numberAsString\": 1 } }]}";
    private static String requestMaxWrongValue = "{\"$action\": [{ \"$max\": {\"numberTen\": \"b\" } }]}";
    private static String requestMaxOnNull = "{\"$action\": [{ \"$max\": {\"nullField\": 1 } }]}";

    private static String requestPopSubField = "{\"$action\": [{ \"$pop\": {\"subItem.subArray\": -1 } }]}";
    private static String requestPopFirst = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": -1 } }]}";
    private static String requestPopLast = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": 1 } }]}";
    private static String requestPopMultiple = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": 2 } }]}";
    private static String requestPopWrongField = "{\"$action\": [{ \"$pop\": {\"numberAsString\": -2 } }]}";
    private static String requestPopWrongValue = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": \"b\" } }]}";
    private static String requestPopOnNull = "{\"$action\": [{ \"$pop\": {\"nullField\": 1 } }]}";

    private static String requestPullSubField =
        "{\"$action\": [{ \"$pull\": {\"subItem.subArray\": [\"subValue\"] } }] }";
    private static String requestPullFirst =
        "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v1\"] } }] }";
    private static String requestPullMultiple =
        "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v1\", \"v3\"] } }]}";
    private static String requestPullUnknow =
        "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v6\"] } }] }";
    private static String requestPullWrongField = "{\"$action\": [{ \"$pull\": {\"numberAsString\": \"2\"} }]}";
    private static String requestPullOnNull =
        "{\"$action\": [{ \"$pull\": {\"nullField\": [\"v1\"] } }] }";

    private static String requestPush =
        "{\"$action\": [{ \"$push\": {\"ArrayToPush\": [\"v3\", \"v4\"]}}]}";
    private static String requestPushSubField =
        "{\"$action\": [{ \"$push\": {\"subItem.subArray\": [\"v3\", \"v4\"]}}]}";
    private static String requestPushDuplicate =
        "{\"$action\": [{ \"$push\": {\"ArrayToPush\": [\"v1\", \"v1\"]}}]}";
    private static String requestPushWrongField =
        "{\"$action\": [{ \"$push\": {\"numberAsString\": [\"v1\"]}}]}";
    private static String requestPushOnNull =
        "{\"$action\": [{ \"$push\": {\"nullField\": [\"v3\", \"v4\"]}}]}";

    private static String requestRename = "{\"$action\": [{ \"$rename\": {\"oldField\": \"renamedField\" } }]}";
    private static String requestRenameSubField =
        "{\"$action\": [{ \"$rename\": {\"subItem.subInt\": \"newSubItem.newName\" } }]}";
    private static String requestRenameUnknowField = "{\"$action\": [{ \"$rename\": {\"unknowField\": \"field\" } }]}";

    private static String requestSetRegexRemovePrefix = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"oldField\", " +
        "\"$controlPattern\": \"value\", " +
        "\"$updatePattern\": \"\" } } ]}";

    private static String requestSetRegexReplaceAll = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"oldValue\", " +
        "\"$controlPattern\": \"^.*$\", " +
        "\"$updatePattern\": \"newValue\" } } ]}";

    private static String requestSetRegexStringArray = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"arrayRegex\", " +
        "\"$controlPattern\": \"V\", " +
        "\"$updatePattern\": \"W\" } } ]}";

    private static String requestSetRegexExpand = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"expandField\", " +
        "\"$controlPattern\": \"X\", " +
        "\"$updatePattern\": \"XX\" } } ]}";

    private static String requestSetRegexNonExitingField = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"subItem.NonExistingField\", " +
        "\"$controlPattern\": \"^.*$\", " +
        "\"$updatePattern\": \"XX\" } } ]}";

    private static String requestSetRegexSubField = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"subItem.subArray\", " +
        "\"$controlPattern\": \"subValue\", " +
        "\"$updatePattern\": \"newSubValue\" } } ]}";

    private static String requestSetRegexNullField = "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"nullField\", " +
        "\"$controlPattern\": \"oldValue\", " +
        "\"$updatePattern\": \"newValue\" } } ]}";

    private static String requestSet = "{\"$action\": [{ \"$set\": { \"oldValue\": \"newValue\"} }]}";
    private static String requestSetSubField = "{\"$action\": [{ \"$set\": { \"subItem.subInt\": \"newValue\"} }]}";

    private static String requestUnset = "{\"$action\": [{ \"$unset\": [ \"oldValue\", \"oldField\" ] }]}";
    private static String requestUnsetSubField = "{\"$action\": [{ \"$unset\": [ \"subItem.subInt\" ] }]}";
    private static String requestNonExistingSubSubField = "{\"$action\": [{ \"$unset\": [ \"subItem.nonExisting.NonExistingSubField\" ] }]}";

    @Before
    public void setUp() throws InvalidParseOperationException {
        // Init jsonDocument with values (from real document or only some json ?)
        jsonDocument = JsonHandler.getFromString(jsonNodeValue);
        HashMap<String, VitamDescriptionType> descriptionTypeByName = new HashMap<>();
        descriptionTypeByName.put("OriginatingAgency", new VitamDescriptionType("Title", text, one, true));
        DynamicParserTokens parserTokens = new DynamicParserTokens(descriptionTypeByName, Collections.emptyList());
        mDIM = new MongoDbInMemory(jsonDocument, parserTokens);
    }

    @Test
    public void testReset() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestSet));

        final JsonNode resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertNotEquals("Json should be updated", jsonDocument, resultAfterUpdate);
        mDIM.resetUpdatedAU();
        final JsonNode resultAfterReset = mDIM.getUpdateJson(emptyParser);
        assertEquals("Json should be reseted to original value", jsonDocument, resultAfterReset);
    }

    @Test
    public void testAddActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestAdd));
        JsonNode result = mDIM.getUpdateJson(parser);
        ArrayNode array = (ArrayNode) result.get("ArrayToAdd");
        assertEquals("Should add a value", 3, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("ArrayToAdd");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestAddMultiple));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("ArrayToAdd");
        assertEquals("Multiples values should be added", 4, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("ArrayToAdd");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestAddDuplicate));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("ArrayToAdd");
        assertEquals("Duplicated values should not be added", 2, array.size());
        assertThat(mDIM.getUpdatedFields()).isEmpty();

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestAddOnNull));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("nullField");
        assertEquals("Value must be added on new array", 1, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("nullField");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestAddSubField));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) JsonHandler.getNodeByPath(result, "subItem.subArray", true);
        assertEquals("Value must be added on new array", 2, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subArray");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestAddWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is not an array");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testIncActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestInc));
        JsonNode result = mDIM.getUpdateJson(parser);
        JsonNode response = result.get("numberTen");
        assertTrue(response.isNumber());
        assertEquals("Should add action value (2) to original value (10)", 12, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("numberTen");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestIncSubField));
        result = mDIM.getUpdateJson(parser);
        response = JsonHandler.getNodeByPath(result, "subItem.subInt", true);
        assertTrue(response.isNumber());
        assertEquals("Should add action value (2) to original value (42)", 44, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestIncWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestIncOnNull));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is null");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestIncWrongValue));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because action value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testMinActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestMin));
        JsonNode result = mDIM.getUpdateJson(parser);
        JsonNode response = result.get("numberTen");
        assertTrue(response.isNumber());
        assertEquals("Action value should be taken", 1, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("numberTen");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMinSubField));
        result = mDIM.getUpdateJson(parser);
        response = JsonHandler.getNodeByPath(result, "subItem.subInt", true);
        assertTrue(response.isNumber());
        assertEquals("Action value should be taken", 1, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMinInDb));
        result = mDIM.getUpdateJson(parser);
        response = result.get("numberTen");
        assertTrue(response.isNumber());
        assertEquals("Original value should be taken", 10, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("numberTen");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMinWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMinOnNull));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is null");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMinWrongValue));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because action value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testMaxActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestMax));
        JsonNode result = mDIM.getUpdateJson(parser);
        JsonNode response = result.get("numberTen");
        assertTrue(response.isNumber());
        assertEquals("Action value should be taken", 100, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("numberTen");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMaxSubField));
        result = mDIM.getUpdateJson(parser);
        response = JsonHandler.getNodeByPath(result, "subItem.subInt", true);
        assertTrue(response.isNumber());
        assertEquals("Action value should be taken", 100, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMaxInDb));
        result = mDIM.getUpdateJson(parser);
        response = result.get("numberTen");
        assertTrue(response.isNumber());
        assertEquals("Original value should be taken", 10, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("numberTen");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMaxWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMaxOnNull));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is null");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestMaxWrongValue));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because action value is a string");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testPopActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestPopFirst));
        JsonNode result = mDIM.getUpdateJson(parser);
        ArrayNode array = (ArrayNode) result.get("arrayToPop");
        assertEquals("Should pop a value", 2, array.size());
        assertNotEquals("First element should be removed", "val1", array.get(0).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("arrayToPop");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopSubField));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) JsonHandler.getNodeByPath(result, "subItem.subArray", true);
        assertEquals("Should pop a value", 0, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subArray");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopLast));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("arrayToPop");
        assertEquals("Should pop a value", 2, array.size());
        assertNotEquals("Last element should be removed", "val3", array.get(1).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("arrayToPop");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopMultiple));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("arrayToPop");
        assertEquals("Should pop a value", 1, array.size());
        assertEquals("Only first element should remains", "val1", array.get(0).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("arrayToPop");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestPopWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is not an array");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestPopOnNull));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is null");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestPopWrongValue));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because action value is not a number");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testPullActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestPullFirst));
        JsonNode result = mDIM.getUpdateJson(parser);
        ArrayNode array = (ArrayNode) result.get("arrayToPull");
        assertEquals("Should pull first value", 2, array.size());
        assertNotEquals("First element should be removed", "v1", array.get(0).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("arrayToPull");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPullSubField));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) JsonHandler.getNodeByPath(result, "subItem.subArray", true);
        assertEquals("Should pull first value", 0, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subArray");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPullMultiple));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("arrayToPull");
        assertEquals("Should pop a value", 1, array.size());
        assertEquals("Last element should be removed", "v2", array.get(0).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("arrayToPull");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPullUnknow));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("arrayToPull");
        assertEquals("Shouldn't pop a value", 3, array.size());
        assertThat(mDIM.getUpdatedFields()).isEmpty();

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPullOnNull));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("nullField");
        assertEquals("Shouldn't pop a value", 0, array.size());
        assertThat(mDIM.getUpdatedFields()).isEmpty();

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestPullWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is not an array");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testPushActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestPush));
        JsonNode result = mDIM.getUpdateJson(parser);
        ArrayNode array = (ArrayNode) result.get("ArrayToPush");
        assertEquals("Should push values", 4, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("ArrayToPush");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPushSubField));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) JsonHandler.getNodeByPath(result, "subItem.subArray", true);
        assertEquals("Value must be added on new array", 3, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subArray");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPushDuplicate));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("ArrayToPush");
        assertEquals("Should push duplicate values", 4, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("ArrayToPush");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPushOnNull));
        result = mDIM.getUpdateJson(parser);
        array = (ArrayNode) result.get("nullField");
        assertEquals("Should push values in new array", 2, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("nullField");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestPushWrongField));
            result = mDIM.getUpdateJson(parser);
            fail("Should throw InvalidParseOperationException because original value is not an array");
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testRenameActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestRename));
        JsonNode result = mDIM.getUpdateJson(parser);
        assertNull("Old name should be deleted", result.get("oldField"));
        assertEquals("New field should have oldValue", "valueOld", result.get("renamedField").asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("oldField", "renamedField");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestRenameSubField));
        result = mDIM.getUpdateJson(parser);
        final JsonNode oldSubItem = result.get("subItem");
        assertNull("Old name should be deleted", oldSubItem.get("subInt"));
        final JsonNode newSubItem = result.get("newSubItem");
        assertEquals("New field should have oldValue", 42, newSubItem.get("newName").asInt());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt", "newSubItem.newName");

        try {
            mDIM.resetUpdatedAU();
            parser.parse(JsonHandler.getFromString(requestRenameUnknowField));
            result = mDIM.getUpdateJson(parser);
        } catch (final InvalidParseOperationException e) {
            // Normal Path of the unit test
        }
    }

    @Test
    public void testSetRegexActions() throws InvalidParseOperationException {

        AbstractParser<?> parserMulti = new UpdateParserMultiple(new SingleVarNameAdapter());

        // Remove prefix
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexRemovePrefix));
        JsonNode result = mDIM.getUpdateJson(parserMulti);
        assertEquals("Old", result.get("oldField").asText());

        // Replace all text
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexReplaceAll));
        result = mDIM.getUpdateJson(parserMulti);
        assertEquals("newValue", result.get("oldValue").asText());

        // String array
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexStringArray));
        result = mDIM.getUpdateJson(parserMulti);
        assertEquals("[\"W1\",\"\",\"W2\"]", JsonHandler.unprettyPrint(result.get("arrayRegex")));

        // Expand X to XX
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexExpand));
        result = mDIM.getUpdateJson(parserMulti);
        assertEquals("XXXXXX", result.get("expandField").asText());

        // Non existing field
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexNonExitingField));
        mDIM.getUpdateJson(parserMulti);
        // Then : No error

        // Sub field
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexSubField));
        mDIM.getUpdateJson(parserMulti);
        assertEquals("[\"newSubValue\"]", JsonHandler.unprettyPrint(result.get("subItem").get("subArray")));

        // Null field
        parserMulti.parse(JsonHandler.getFromString(requestSetRegexNullField));
        mDIM.getUpdateJson(parserMulti);
        // Then : No error
    }

    @Test
    public void testSetUnsetActions() throws InvalidParseOperationException {
        parser.parse(JsonHandler.getFromString(requestSet));

        JsonNode resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertNotEquals("Json should be updated", "newValue", resultAfterUpdate.get("oldValue"));
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("oldValue");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestSetSubField));
        resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertEquals("Json should be updated", "[\"newValue\"]",
            JsonHandler.getNodeByPath(resultAfterUpdate, "subItem.subInt", true).toString());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString("{\"$action\": [{ \"$set\": { \"OriginatingAgency\": \"newValue\"} }]}"));
        resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertEquals("Json should be updated", "newValue",
            JsonHandler.getNodeByPath(resultAfterUpdate, "OriginatingAgency", true).asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("OriginatingAgency");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestUnset));
        final JsonNode resultAfterReset = mDIM.getUpdateJson(parser);
        assertNull("Json should unset value", resultAfterReset.get("oldValue"));
        assertNull("Json should unset value", resultAfterReset.get("oldField"));
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("oldValue", "oldField");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestUnsetSubField));
        resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertNull("Json should unset value", JsonHandler.getNodeByPath(resultAfterUpdate, "subItem.subInt", true));
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");
    }

    @Test
    public void testSetUnsetActionsForNonExistingSubSubFieldBug5195() throws InvalidParseOperationException {

        // Given
        parser.parse(JsonHandler.getFromString(requestSet));
        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestNonExistingSubSubField));

        // When
        mDIM.getUpdateJson(parser);

        // Then : NO NPE
    }
}
