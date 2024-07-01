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
package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.collections.VitamDescriptionType;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.massupdate.RuleAction;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.model.massupdate.RuleCategoryAction;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamCardinality.one;
import static fr.gouv.vitam.common.database.collections.VitamDescriptionType.VitamType.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MongoDbInMemoryTest {

    private static final String jsonNodeValue =
        "{" +
        "\"oldValue\": \"value\"," +
        "\"oldField\": \"valueOld\"," +
        "\"nullField\": null," +
        "\"numberTen\": 10," +
        "\"numberAsString\": \"2\"," +
        "\"arrayToPop\": [\"val1\", \"val2\", \"val3\"]," +
        "\"arrayToPull\": [\"v1\", \"v2\", \"v3\"]," +
        "\"ArrayToAdd\": [\"val1\", \"val2\"]," +
        "\"ArrayToPush\": [\"v1\", \"v2\"]," +
        "\"subItem\": {\"subArray\": [\"subValue\"], \"subInt\": 42}," +
        "\"expandField\": \"XXX\", \"arrayRegex\": [ \"V1\", \"\", \"V2\"] }";
    private static JsonNode jsonDocument;
    private static MongoDbInMemory mDIM;
    private static final AbstractParser<?> parser = new UpdateParserSingle(new SingleVarNameAdapter());
    private static final AbstractParser<?> emptyParser = new UpdateParserSingle(new SingleVarNameAdapter());

    private static final String requestAdd = "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val3\"] } }]}";
    private static final String requestAddMultiple =
        "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val3\", \"val4\"] } }]}";
    private static final String requestAddSubField =
        "{\"$action\": [{ \"$add\": {\"subItem.subArray\": [\"val3\"] } }]}";
    private static final String requestAddWrongField =
        "{\"$action\": [{ \"$add\": {\"numberAsString\": [\"val3\"] } }]}";
    private static final String requestAddDuplicate = "{\"$action\": [{ \"$add\": {\"ArrayToAdd\": [\"val2\"] } }]}";
    private static final String requestAddOnNull = "{\"$action\": [{ \"$add\": {\"nullField\": [\"val2\"] } }]}";

    private static final String requestInc = "{\"$action\": [{ \"$inc\": {\"numberTen\": 2 } }]}";
    private static final String requestIncSubField = "{\"$action\": [{ \"$inc\": {\"subItem.subInt\": 2 } }]}";
    private static final String requestIncWrongField = "{\"$action\": [{ \"$inc\": {\"numberAsString\": 2 } }]}";
    private static final String requestIncWrongValue = "{\"$action\": [{ \"$inc\": {\"numberTen\": \"b\" } }]}";
    private static final String requestIncOnNull = "{\"$action\": [{ \"$inc\": {\"nullField\": 1 } }]}";

    private static final String requestMin = "{\"$action\": [{ \"$min\": {\"numberTen\": 1 } }]}";
    private static final String requestMinSubField = "{\"$action\": [{ \"$min\": {\"subItem.subInt\": 1 } }]}";
    private static final String requestMinInDb = "{\"$action\": [{ \"$min\": {\"numberTen\": 100 } }]}";
    private static final String requestMinWrongField = "{\"$action\": [{ \"$min\": {\"numberAsString\": 1 } }]}";
    private static final String requestMinWrongValue = "{\"$action\": [{ \"$min\": {\"numberTen\": \"b\" } }]}";
    private static final String requestMinOnNull = "{\"$action\": [{ \"$min\": {\"nullField\": 1 } }]}";

    private static final String requestMax = "{\"$action\": [{ \"$max\": {\"numberTen\": 100 } }]}";
    private static final String requestMaxSubField = "{\"$action\": [{ \"$max\": {\"subItem.subInt\": 100 } }]}";
    private static final String requestMaxInDb = "{\"$action\": [{ \"$max\": {\"numberTen\": 1 } }]}";
    private static final String requestMaxWrongField = "{\"$action\": [{ \"$max\": {\"numberAsString\": 1 } }]}";
    private static final String requestMaxWrongValue = "{\"$action\": [{ \"$max\": {\"numberTen\": \"b\" } }]}";
    private static final String requestMaxOnNull = "{\"$action\": [{ \"$max\": {\"nullField\": 1 } }]}";

    private static final String requestPopSubField = "{\"$action\": [{ \"$pop\": {\"subItem.subArray\": -1 } }]}";
    private static final String requestPopFirst = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": -1 } }]}";
    private static final String requestPopLast = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": 1 } }]}";
    private static final String requestPopMultiple = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": 2 } }]}";
    private static final String requestPopWrongField = "{\"$action\": [{ \"$pop\": {\"numberAsString\": -2 } }]}";
    private static final String requestPopWrongValue = "{\"$action\": [{ \"$pop\": {\"arrayToPop\": \"b\" } }]}";
    private static final String requestPopOnNull = "{\"$action\": [{ \"$pop\": {\"nullField\": 1 } }]}";

    private static final String requestPullSubField =
        "{\"$action\": [{ \"$pull\": {\"subItem.subArray\": [\"subValue\"] } }] }";
    private static final String requestPullFirst = "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v1\"] } }] }";
    private static final String requestPullMultiple =
        "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v1\", \"v3\"] } }]}";
    private static final String requestPullUnknow = "{\"$action\": [{ \"$pull\": {\"arrayToPull\": [\"v6\"] } }] }";
    private static final String requestPullWrongField = "{\"$action\": [{ \"$pull\": {\"numberAsString\": \"2\"} }]}";
    private static final String requestPullOnNull = "{\"$action\": [{ \"$pull\": {\"nullField\": [\"v1\"] } }] }";

    private static final String requestPush = "{\"$action\": [{ \"$push\": {\"ArrayToPush\": [\"v3\", \"v4\"]}}]}";
    private static final String requestPushSubField =
        "{\"$action\": [{ \"$push\": {\"subItem.subArray\": [\"v3\", \"v4\"]}}]}";
    private static final String requestPushDuplicate =
        "{\"$action\": [{ \"$push\": {\"ArrayToPush\": [\"v1\", \"v1\"]}}]}";
    private static final String requestPushWrongField = "{\"$action\": [{ \"$push\": {\"numberAsString\": [\"v1\"]}}]}";
    private static final String requestPushOnNull = "{\"$action\": [{ \"$push\": {\"nullField\": [\"v3\", \"v4\"]}}]}";

    private static final String requestRename = "{\"$action\": [{ \"$rename\": {\"oldField\": \"renamedField\" } }]}";
    private static final String requestRenameSubField =
        "{\"$action\": [{ \"$rename\": {\"subItem.subInt\": \"newSubItem.newName\" } }]}";
    private static final String requestRenameUnknowField =
        "{\"$action\": [{ \"$rename\": {\"unknowField\": \"field\" } }]}";

    private static final String requestSetRegexRemovePrefix =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"oldField\", " +
        "\"$controlPattern\": \"value\", " +
        "\"$updatePattern\": \"\" } } ]}";

    private static final String requestSetRegexReplaceAll =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"oldValue\", " +
        "\"$controlPattern\": \"^.*$\", " +
        "\"$updatePattern\": \"newValue\" } } ]}";

    private static final String requestSetRegexStringArray =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"arrayRegex\", " +
        "\"$controlPattern\": \"V\", " +
        "\"$updatePattern\": \"W\" } } ]}";

    private static final String requestSetRegexExpand =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"expandField\", " +
        "\"$controlPattern\": \"X\", " +
        "\"$updatePattern\": \"XX\" } } ]}";

    private static final String requestSetRegexNonExitingField =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"subItem.NonExistingField\", " +
        "\"$controlPattern\": \"^.*$\", " +
        "\"$updatePattern\": \"XX\" } } ]}";

    private static final String requestSetRegexSubField =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"subItem.subArray\", " +
        "\"$controlPattern\": \"subValue\", " +
        "\"$updatePattern\": \"newSubValue\" } } ]}";

    private static final String requestSetRegexNullField =
        "{\"$action\": [{\"$setregex\": { " +
        "\"$target\": \"nullField\", " +
        "\"$controlPattern\": \"oldValue\", " +
        "\"$updatePattern\": \"newValue\" } } ]}";

    //private static String requestSet = "{\"$action\": [{ \"$set\": { \"oldValue\": \"newValue\"} }]}";
    private static final String requestSet =
        "{\"$action\": [{ \"$set\": { \"oldValue\": { \"subItem\" : \"newValue\"} } }]}";
    private static final String requestSetSubField =
        "{\"$action\": [{ \"$set\": { \"subItem.subInt\": \"newValue\"} }]}";

    private static final String requestUnset = "{\"$action\": [{ \"$unset\": [ \"oldValue\", \"oldField\" ] }]}";
    private static final String requestUnsetSubField = "{\"$action\": [{ \"$unset\": [ \"subItem.subInt\" ] }]}";
    private static final String requestNonExistingSubSubField =
        "{\"$action\": [{ \"$unset\": [ \"subItem.nonExisting.NonExistingSubField\" ] }]}";

    private static final String UNIT7_DELETED_PREVENT_RULES_ID_FOR_APPRAISAL_RULE =
        "UpdateRules/Units/unit7_deletedPreventRulesIdForAppraisalRule.json";
    private static final String UNIT6 = "UpdateRules/Units/unit6.json";
    private static final String UNIT7 = "UpdateRules/Units/unit7.json";
    private static final String UNIT8 = "UpdateRules/Units/unit8.json";
    private static final String UNIT9 = "UpdateRules/Units/unit9.json";
    private static final String UNIT1 = "UpdateRules/Units/unit1.json";
    private static final String UNIT2 = "UpdateRules/Units/unit2.json";
    private static final String UNIT3 = "UpdateRules/Units/unit3.json";
    private static final String DELETED_RULES_ID_FOR_APPRAISAL_CATEGORY =
        "UpdateRules/RuleActions/deleteRulesIdForAppraisalCategory.json";
    private static final String BAD_UPDATE_MGT_RULE_REQUEST = "UpdateRules/RuleActions/badUpdateMgtRuleRequest.json";
    private static final String UNIT7_DELETED_LIST_PREVENT_RULES_ID =
        "UpdateRules/Units/unit7_deleted_ListOfPreventRulesId.json";
    private static final String UNIT9_DELETED_LIST_PREVENT_RULES_ID_FOR_MULTIPLE_CATEGORY =
        "UpdateRules/Units/unit9_deletedListOfPreventRulesForMultipleCategory.json";
    private static final String BAD_ADD_MGT_RULE_REQUEST = "UpdateRules/RuleActions/badAddMgtRuleRequest.json";
    private static final String DELETE_PREVENT_RULES_MGT_REQUEST =
        "UpdateRules/RuleActions/deletePreventRulesInMgtRulesRequest.json";
    private static final String DELETE_PREVENT_RULES_FOR_MULTIPLE_CATEGORY =
        "UpdateRules/RuleActions/deletePreventRulesForMultipleRuleCategory.json";
    private static final String UNIT7_ADD_PREVENT_RULES_TO_HOLD_CATEGORY =
        "UpdateRules/Units/unit7_addedNewPreventRulesToHoldCategory.json";
    private static final String ADD_NEW_PREVENT_RULES_TO_HOLD_CATEGORY =
        "UpdateRules/RuleActions/addNewPreventRulesIdToHoldRuleCategory.json";
    private static final String UNIT7_ADD_PREVENT_RULES_TO_APPRAISAL_CATEGORY =
        "UpdateRules/Units/unit7_addedListofNewPreventRulesIdToAppraisalRule.json";
    private static final String ADD_NEW_PREVENT_RULES_TO_APPRAISAL_CATEGORY =
        "UpdateRules/RuleActions/addNewPreventRulesIdToAppraisalCategory.json";
    private static final String ADD_NEW_PREVENT_RULES_TO_MULTIPLE_RULE_CATEGORY =
        "UpdateRules/RuleActions/addNewPreventRulesIdToMultipleRuleCategory.json";
    private static final String UNIT9_ADD_PREVENT_RULES_TO_MULTIPLE_RULE_CATEGORY =
        "UpdateRules/Units/unit9_addedListOfNewPreventRulesToMultipleRuleCategory.json";

    @Before
    public void setUp() throws InvalidParseOperationException {
        // Init jsonDocument with values (from real document or only some json ?)
        jsonDocument = JsonHandler.getFromString(jsonNodeValue);
        List<VitamDescriptionType> descriptions = Collections.singletonList(
            new VitamDescriptionType("OriginatingAgency", null, text, one, true)
        );
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            new VitamDescriptionResolver(descriptions),
            Collections.emptyList()
        );
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
        assertNotNull(array);
        assertEquals("Value must be added on new array", 2, array.size());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subArray");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestAddWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(response);
        assertTrue(response.isNumber());
        assertEquals("Should add action value (2) to original value (42)", 44, response.asLong());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestIncWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestIncOnNull));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestIncWrongValue));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(response);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMinWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMinOnNull));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMinWrongValue));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(response);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMaxWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMaxOnNull));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestMaxWrongValue));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(array);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopOnNull));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPopWrongValue));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(array);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPullWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotNull(array);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestPushWrongField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestRenameUnknowField));
        assertThatCode(() -> mDIM.getUpdateJson(parser)).isInstanceOf(InvalidParseOperationException.class);
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
        assertNotEquals("Json should be updated", "newValue", resultAfterUpdate.get("oldValue").asText());
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("oldValue");

        mDIM.resetUpdatedAU();
        parser.parse(JsonHandler.getFromString(requestSetSubField));
        resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertEquals(
            "Json should be updated",
            "[\"newValue\"]",
            JsonHandler.getNodeByPath(resultAfterUpdate, "subItem.subInt", true).toString()
        );
        assertThat(mDIM.getUpdatedFields()).containsExactlyInAnyOrder("subItem.subInt");

        mDIM.resetUpdatedAU();
        parser.parse(
            JsonHandler.getFromString("{\"$action\": [{ \"$set\": { \"OriginatingAgency\": \"newValue\"} }]}")
        );
        resultAfterUpdate = mDIM.getUpdateJson(parser);
        assertEquals(
            "Json should be updated",
            "newValue",
            JsonHandler.getNodeByPath(resultAfterUpdate, "OriginatingAgency", true).asText()
        );
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

        // When + Then : NO NPE
        assertThatCode(() -> mDIM.getUpdateJson(parser)).doesNotThrowAnyException();
    }

    @Test
    public void should_not_throw_npe_when_update_with_rule_without_start_date() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);

        DurationData duration = new DurationData();
        duration.setDurationUnit(ChronoUnit.FOREVER);
        duration.setDurationValue(12);

        String ruleId = "Rule";

        Map<String, DurationData> durations = new HashMap<>();
        durations.put(ruleId, duration);

        RuleAction ruleToUpdate = new RuleAction();
        ruleToUpdate.setOldRule("ACC-00001");
        ruleToUpdate.setRule(ruleId);

        RuleCategoryAction accessRuleToUpdate = new RuleCategoryAction();
        accessRuleToUpdate.setRules(Collections.singletonList(ruleToUpdate));

        HashMap<String, RuleCategoryAction> updates = new HashMap<>();
        updates.put("AccessRule", accessRuleToUpdate);

        RuleActions ruleActions = new RuleActions();
        ruleActions.setAdd(Collections.emptyList());
        ruleActions.setDelete(Collections.emptyList());
        ruleActions.setUpdate(Collections.singletonList(updates));

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule).doesNotThrowAnyException();
    }

    @Test
    public void addAccessRuleWithLimitedDurationWithStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addAccessRuleWithLimitedDurationWithStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addAccessRuleWithLimitedDurationWithStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addAccessRuleWithUnlimitedDurationWithStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addAccessRuleWithUnlimitedDurationWithStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addAccessRuleWithUnlimitedDurationWithStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addAccessRuleIgnoredWithExistingRule() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/addAccessRuleIgnoredWithExistingRule.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT1);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addAccessRuleWithoutStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/addAccessRuleWithoutStartDate.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addAccessRuleWithoutStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithLimitedDurationAndNoStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithLimitedDurationAndNoStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithLimitedDurationAndNoStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithLimitedDurationAndStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithLimitedDurationAndStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithLimitedDurationAndStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithoutDurationWithHoldEndDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithoutDurationWithHoldEndDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithoutDurationWithHoldEndDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithoutDurationWithoutHoldEndDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithoutDurationWithoutHoldEndDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithoutDurationWithoutHoldEndDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithUnlimitedDurationAndNoStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithUnlimitedDurationAndNoStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithUnlimitedDurationAndNoStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleWithUnlimitedDurationAndStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleWithUnlimitedDurationAndStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit1_updated_addHoldRuleWithUnlimitedDurationAndStartDate.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void addHoldRuleKoWhenEndDateBeforeStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleKoWhenEndDateBeforeStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue("RuleUpdateErrorCode", RuleUpdateErrorCode.HOLD_END_DATE_BEFORE_START_DATE);
    }

    @Test
    public void addHoldRuleKoWhenHoldEndDateForRuleWithLimitedDuration() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleKoWhenHoldEndDateForRuleWithLimitedDuration.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue(
                "RuleUpdateErrorCode",
                RuleUpdateErrorCode.HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION
            );
    }

    @Test
    public void addHoldRuleKoWhenHoldEndDateForRuleWithUnlimitedDuration() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/addHoldRuleKoWhenHoldEndDateForRuleWithUnlimitedDuration.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue(
                "RuleUpdateErrorCode",
                RuleUpdateErrorCode.HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION
            );
    }

    @Test
    public void addMultipleRulesToEmptyManagement() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory("UpdateRules/Units/unit5.json");
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/addMultipleRulesToEmptyManagement.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit5_updated_addMultipleRulesToEmptyManagement.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void updateHoldRuleIgnoredWithNoExistingRule() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT2);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/updateHoldRuleIgnoredWithNoExistingRule.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT2);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void updateHoldRuleUpdateRuleId() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT2);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/updateHoldRuleUpdateRuleId.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit2_updated_updateHoldRuleUpdateRuleId.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void updateHoldRuleDeleteAllAttributes() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT3);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/updateHoldRuleDeleteAllAttributes.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit3_updated_updateHoldRuleDeleteAllAttributes.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void updateHoldRuleSetAllAttributes() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory("UpdateRules/Units/unit4.json");
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/updateHoldRuleSetAllAttributes.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit4_updated_updateHoldRuleSetAllAttributes.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void updateHoldRuleKoWhenEndDateBeforeStartDate() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT3);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/updateHoldRuleKoWhenEndDateBeforeStartDate.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue("RuleUpdateErrorCode", RuleUpdateErrorCode.HOLD_END_DATE_BEFORE_START_DATE);
    }

    @Test
    public void updateHoldRuleKoWhenHoldEndDateForRuleWithLimitedDuration() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT3);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/updateHoldRuleKoWhenHoldEndDateForRuleWithLimitedDuration.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue(
                "RuleUpdateErrorCode",
                RuleUpdateErrorCode.HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION
            );
    }

    @Test
    public void updateHoldRuleKoWhenHoldEndDateForRuleWithUnlimitedDuration() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT3);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/updateHoldRuleKoWhenHoldEndDateForRuleWithUnlimitedDuration.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        ThrowingCallable updateForRule = () -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        assertThatCode(updateForRule)
            .isInstanceOf(RuleUpdateException.class)
            .hasFieldOrPropertyWithValue(
                "RuleUpdateErrorCode",
                RuleUpdateErrorCode.HOLD_END_DATE_ONLY_ALLOWED_FOR_HOLD_RULE_WITH_UNDEFINED_DURATION
            );
    }

    @Test
    public void deleteHoldRuleIgnoredWhenRuleNotExists() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT2);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/deleteHoldRuleIgnoredWhenRuleNotExists.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT2);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void deleteHoldRuleCategoryIgnoredWhenCategoryNotExists() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT1);
        RuleActions ruleActions = getRuleActions(
            "UpdateRules/RuleActions/deleteHoldRuleCategoryIgnoredWhenCategoryNotExists.json"
        );
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT1);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void deleteHoldRule() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT2);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/deleteHoldRule.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit2_updated_deleteHoldRule.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void deleteHoldRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT2);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/deleteHoldRuleCategory.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit2_updated_deleteHoldRuleCategory.json"
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void complexAddUpdateDeleteQuery() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT6);
        RuleActions ruleActions = getRuleActions("UpdateRules/RuleActions/complexAddUpdateDeleteQuery.json");
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            "UpdateRules/Units/unit6_updated_complexAddUpdateDeleteQuery.json"
        );

        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testDeletePreventRuleIdForHoldRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT7);
        RuleActions ruleActions = getRuleActions(DELETE_PREVENT_RULES_MGT_REQUEST);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT7_DELETED_LIST_PREVENT_RULES_ID);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testDeletePreventRuleIdForMultipleRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT9);
        RuleActions ruleActions = getRuleActions(DELETE_PREVENT_RULES_FOR_MULTIPLE_CATEGORY);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            UNIT9_DELETED_LIST_PREVENT_RULES_ID_FOR_MULTIPLE_CATEGORY
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testDeletePreventRuleIdWithBadUpdateRequest() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT8);
        RuleActions ruleActions = getRuleActions(BAD_UPDATE_MGT_RULE_REQUEST);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When / Then
        assertThatThrownBy(() -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations)).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    public void testDeletePreventRuleIdForAppraisalRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT7);
        RuleActions ruleActions = getRuleActions(DELETED_RULES_ID_FOR_APPRAISAL_CATEGORY);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            UNIT7_DELETED_PREVENT_RULES_ID_FOR_APPRAISAL_RULE
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testAddNewPreventRuleIdForAppraisalRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT7);
        RuleActions ruleActions = getRuleActions(ADD_NEW_PREVENT_RULES_TO_APPRAISAL_CATEGORY);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            UNIT7_ADD_PREVENT_RULES_TO_APPRAISAL_CATEGORY
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testAddNewPreventRuleIdForMultipleRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT9);
        RuleActions ruleActions = getRuleActions(ADD_NEW_PREVENT_RULES_TO_MULTIPLE_RULE_CATEGORY);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(
            UNIT9_ADD_PREVENT_RULES_TO_MULTIPLE_RULE_CATEGORY
        );
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testAddNewPreventRuleIdForHoldRuleCategory() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT7);
        RuleActions ruleActions = getRuleActions(ADD_NEW_PREVENT_RULES_TO_HOLD_CATEGORY);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When
        JsonNode updatedJson = mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations);

        // Then
        String expectedUnitAfterUpdates = PropertiesUtils.getResourceAsString(UNIT7_ADD_PREVENT_RULES_TO_HOLD_CATEGORY);
        JsonAssert.assertJsonEquals(expectedUnitAfterUpdates, JsonHandler.unprettyPrint(updatedJson));
    }

    @Test
    public void testAddPreventRuleIdWithBadUpdateRequest() throws Exception {
        // Given
        MongoDbInMemory mongoDbInMemory = createMongoDbInMemory(UNIT8);
        RuleActions ruleActions = getRuleActions(BAD_ADD_MGT_RULE_REQUEST);
        Map<String, DurationData> durations = getTestRuleDurations();

        // When / Then
        assertThatThrownBy(() -> mongoDbInMemory.getUpdateJsonForRule(ruleActions, durations)).isInstanceOf(
            IllegalStateException.class
        );
    }

    private MongoDbInMemory createMongoDbInMemory(String resourcesFile)
        throws InvalidParseOperationException, FileNotFoundException {
        DynamicParserTokens parserTokens = new DynamicParserTokens(
            new VitamDescriptionResolver(Collections.emptyList()),
            Collections.emptyList()
        );
        JsonNode documentToUpdate = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(resourcesFile));
        return new MongoDbInMemory(documentToUpdate, parserTokens);
    }

    private RuleActions getRuleActions(String resourcesFile)
        throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler.getFromString(PropertiesUtils.getResourceAsString(resourcesFile), RuleActions.class);
    }

    private Map<String, DurationData> getTestRuleDurations() {
        return Map.of(
            "REU-00001",
            new DurationData(10, ChronoUnit.YEARS),
            "ACC-00001",
            new DurationData(10, ChronoUnit.YEARS),
            "ACC-00002",
            new DurationData(1, ChronoUnit.YEARS),
            // No duration for "ACC-00003" (unlimited)
            "APP-00001",
            new DurationData(10, ChronoUnit.YEARS),
            "DIS-00001",
            new DurationData(25, ChronoUnit.YEARS),
            "DIS-00002",
            new DurationData(75, ChronoUnit.YEARS),
            "CLASS-00001",
            new DurationData(1, ChronoUnit.YEARS),
            "HOL-00001",
            new DurationData(1, ChronoUnit.YEARS),
            "HOL-00002",
            new DurationData(null, null),
            "HOL-00003",
            new DurationData(10, ChronoUnit.YEARS)
            // No duration for "HOL-00004" (unlimited)
        );
    }
}
