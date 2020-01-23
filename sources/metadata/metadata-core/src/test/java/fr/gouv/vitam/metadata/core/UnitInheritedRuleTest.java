/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.metadata.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@Deprecated
public class UnitInheritedRuleTest {

    private final static String EXPECTED_RESULT = "UnitInheritedRule/EXPECTED_RESULT.json";

    private final static String EXPECTED_CONCAT_RESULT = "UnitInheritedRule/EXPECTED_CONCAT_RESULT.json";

    private final static String AU1_MGT = "UnitInheritedRule/AU1_MGT.json";
    private final static String AU2_MGT = "UnitInheritedRule/AU2_MGT.json";
    private final static String AU4_MGT_KO = "UnitInheritedRule/AU4_MGT_KO.json";
    private final static String AU5_MGT = "UnitInheritedRule/AU5_MGT.json";

    // Normal Inheritance part

    private final static String TWO_IN_ONE = "UnitInheritedRule/TWO_IN_ONE.json";

    private final static String LEVEL_0 = "UnitInheritedRule/LEVEL_0.json";
    private final static String LEVEL_1 = "UnitInheritedRule/LEVEL_1.json";
    private final static String LEVEL_2 = "UnitInheritedRule/LEVEL_2.json";

    private final static String EXPECTED_TWO_RULES = "{" + "\"inheritedRule\":{\"AccessRule\":{" +
        "\"ACC-00003\":{\"AU0\":{\"StartDate\":\"2000-01-01\",\"EndDate\":\"2025-01-01\",\"path\":[[\"AU0\"]]}}," +
        "\"ACC-00002\":{\"AU0\":{\"StartDate\":\"2000-01-01\",\"EndDate\":\"2025-01-01\",\"path\":[[\"AU0\"]]}}" +
        "}}}";

    private final static String EXPECTED_MULTI_PATH = "UnitInheritedRule/EXPECTED_MULTI_PATH.json";

    private final static String EXPECTED_LONG_PATH = "{" + "\"inheritedRule\":{\"StorageRule\":" +
        "{\"STR1\":{\"AU0\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\"," + "\"path\":[[\"AU0\",\"AU2\",\"AU3\",\"AU4\"]]}" + "}}}}";

    private final static String EXPECTED_CUMUL = "{" + "\"inheritedRule\":{\"StorageRule\":" +
        "{\"STR2\":{\"AU2\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\"," + "\"path\":[[\"AU2\"]]}}," +
        "\"STR1\":{\"AU1\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\"," + "\"path\":[[\"AU1\"]]}" + "}}}}";

    private final static String EXPECTED_REPLACE_SAME_RULE = "{" + "\"inheritedRule\":{\"StorageRule\":" +
        "{\"STR1\":{\"AU1\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\"," + "\"path\":[[\"AU1\"]]}" + "}}}}";

    private final static String EXPECTED_NEW_RULE_CHILD = "{" + "\"inheritedRule\":{\"StorageRule\":" +
        "{\"STR1\":{\"AU1\":{\"StartDate\":\"01/01/2019\",\"FinalAction\":\"Copy\",\"path\":[[\"AU1\"]]}" + "}}}}";

    private final static String EXPECTED_NEW_RULE_PARENT = "{" + "\"inheritedRule\":{\"StorageRule\":{" +
        "\"STR1\":{\"AU0\":{\"StartDate\":\"01/01/2019\",\"FinalAction\":\"Copy\",\"path\":[[\"AU0\",\"AU1\"]]}" + "}}}}";

    private final static String EMPTY = "{}";

    // RefNonRuleId part

    private final static String CHAPELLE = "UnitInheritedRule/CHAPELLE.json";

    private final static String MARX_DORMOY = "UnitInheritedRule/MARX_DORMOY.json";

    private final static String SAINT_LAZARE = "UnitInheritedRule/SAINT_LAZARE.json";

    private final static String PLEYEL = "UnitInheritedRule/PLEYEL.json";

    private final static String NEW_RULE_REF_NON_RULE_ID = "UnitInheritedRule/NEW_RULE_REF_NON_RULE_ID.json";

    private final static String MULTIPLE_NEW_RULE_REF_NON_RULE_ID =
        "UnitInheritedRule/MULTIPLE_NEW_RULE_REF_NON_RULE_ID.json";

    private final static String R1_NON_REF_ID = "UnitInheritedRule/R1_NON_REF_ID.json";

    private final static String ROOT_MULTIPLE_RULES = "UnitInheritedRule/ROOT_MULTIPLE_RULES.json";

    private final static String MULTIPLE_REF_NON_RULE_ID = "UnitInheritedRule/MULTIPLE_REF_NON_RULE_ID.json";

    private final static String ROOT_NODE_WITH_MD = "UnitInheritedRule/ROOT_NODE_WITH_MD.json";

    private final static String EXPECTED_FOR_MARX_DORMOY = "UnitInheritedRule/EXPECTED_FOR_MARX_DORMOY.json";

    private final static String EXPECTED_MULTIPLE_REF_NON_RULE_ID = "{" + "\"inheritedRule\":{\"StorageRule\":{" +
        "\"STR2\":{\"AU0\":{\"StartDate\":\"02/01/2019\",\"path\":[[\"AU0\",\"AU2\"]]}}" + "}}}";

    private final static String EXPECTED_REF_NON_RULE_ID = "UnitInheritedRule/EXPECTED_REF_NON_RULE_ID.json";

    private final static String EXPECTED_NEW_RULE_REF_NON_RULE_ID =
        "UnitInheritedRule/EXPECTED_NEW_RULE_REF_NON_RULE_ID.json";

    private final static String EXPECTED_MULTIPLE_NEW_RULE_REF_NON_RULE_ID =
        "{" + "\"inheritedRule\":{\"StorageRule\":{" +
            "\"STR2\":{\"AU2\":{\"StartDate\":\"02/01/2019\",\"path\":[[\"AU2\",\"AU3\"]]}}" +
            "}}}";

    private final static String EXPECTED_METADATA_REF_NON_RULE_ID =
        "UnitInheritedRule/EXPECTED_METADATA_REF_NON_RULE_ID.json";

    // PreventInheritance part

    private final static String STORAGE_PREVENTINHERITANCE = "UnitInheritedRule/STORAGE_PREVENTINHERITANCE.json";

    private final static String STORAGE_PREVENTINHERITANCE_NEW =
        "UnitInheritedRule/STORAGE_PREVENTINHERITANCE_NEW.json";

    private final static String STORAGE_NPE_REPRODUCTION = "UnitInheritedRule/STORAGE_NPE_REPRODUCTION.json";

    private final static String EXPECTED_PREVENT_WILE_DECLARE = "UnitInheritedRule/EXPECTED_PREVENT_WILE_DECLARE.json";

    private final static String EXPECTED_CHILD_PREVENT_WILE_DECLARE = "{" + "\"inheritedRule\":{\"StorageRule\":{" +
        "\"STR2\":{\"AU2\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\",\"path\":[[\"AU2\",\"AU3\"]]}}," +
        "\"STR1\":{\"AU3\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\",\"path\":[[\"AU3\"]]}}" + "}}}";

    private final static String EXPECTED_FINAL_PREVENT_WHILE_DECLARE = "{" + "\"inheritedRule\":{\"StorageRule\":{" +
        "\"STR2\":{\"AU2\":{\"StartDate\":\"02/01/2019\",\"FinalAction\":\"Copy\",\"path\":[[\"AU2\",\"AU3\"]]}}" + "}}}";

    private final static String EXPECTED_PREVENTED_RULE = "UnitInheritedRule/EXPECTED_PREVENTED_RULE.json";

    private final static String EXPECTED_PREVENTED_RULE_INHERIT =
        "UnitInheritedRule/EXPECTED_PREVENTED_RULE_INHERIT.json";

    private final static String EXPECTED_PREVENT_AND_DECLARE = "UnitInheritedRule/EXPECTED_PREVENT_AND_DECLARE.json";

    private final static String EXPECTED_FINAL_PREVENT_AND_DECLARE =
        "UnitInheritedRule/EXPECTED_FINAL_PREVENT_AND_DECLARE.json";

    private final static String EXPECTED_FOR_SAINT_LAZARE = "{" + "\"inheritedRule\":{" + "\"ReuseRule\":{" +
        "\"REU-00001\":{\"Carrefour Pleyel\":{\"StartDate\":\"2000-01-01\",\"EndDate\":\"2010-01-01\",\"path\":[[\"Carrefour Pleyel\",\"Saint Lazare\"]]}}}," +
        "\"AccessRule\":{" +
        "\"ACC-00002\":{\"Porte de la Chapelle\":{\"StartDate\":\"2002-01-01\",\"EndDate\":\"2027-01-01\",\"path\":[[\"Porte de la Chapelle\",\"Marx Dormoy\",\"Saint Lazare\"]]}}" +
        "}}}";

    private final static String EXPECTED_FOR_NPE_REPRODUCTION = "UnitInheritedRule/EXPECTED_FOR_NPE_REPRODUCTION.json";

    @Test
    public void testUnitRuleResult() throws Exception {

        UnitInheritedRule au1RulesResult = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU1_MGT)),
            "AU1");
        UnitInheritedRule au2RulesResult = au1RulesResult.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT)), "AU2");
        UnitInheritedRule au3RulesResult = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU1_MGT)),
            "AU3");

        assertEquals(
            JsonHandler.unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_RESULT))),
            JsonHandler.unprettyPrint(au2RulesResult));

        au1RulesResult.concatRule(au3RulesResult);
        assertEquals(
            JsonHandler.unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_CONCAT_RESULT))),
            JsonHandler.unprettyPrint(au1RulesResult));

        au1RulesResult.concatRule(au1RulesResult);
        assertEquals(
            JsonHandler.unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_CONCAT_RESULT))),
            JsonHandler.unprettyPrint(au1RulesResult));

        UnitInheritedRule au5RulesResult = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU5_MGT)), "AU5");

        assertNotNull(au5RulesResult);
        try {
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU4_MGT_KO)),
                "AU4");
            fail("Should raise an exception");
        } catch (ClassCastException e) {
            // nothing to do
        }

    }

    // Normal Inheritance part

    @Test
    public void testTwoRulesDefineInSameCategoryAtRoot() throws Exception {
        UnitInheritedRule root =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TWO_IN_ONE)), "AU0");

        assertEquals(EXPECTED_TWO_RULES, JsonHandler.unprettyPrint(root));
    }

    @Test
    public void testNewRuleFromParent() throws Exception {
        UnitInheritedRule root = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_0)), "AU0");
        UnitInheritedRule emptyAu = new UnitInheritedRule();
        emptyAu.concatRule(root.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU1"));

        assertEquals(EXPECTED_NEW_RULE_PARENT, JsonHandler.unprettyPrint(emptyAu));
    }

    @Test
    public void testNewRuleOnChild() throws Exception {
        UnitInheritedRule emptyRoot = new UnitInheritedRule();
        UnitInheritedRule au1 =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_0)), "AU1");
        au1.concatRule(emptyRoot);

        assertEquals(EXPECTED_NEW_RULE_CHILD, JsonHandler.unprettyPrint(au1));
    }

    @Test
    public void testReplaceSameRuleID() throws Exception {
        UnitInheritedRule root =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_0)), "AU0");

        UnitInheritedRule au1 = new UnitInheritedRule();
        au1.concatRule(root.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU1"));

        assertEquals(EXPECTED_REPLACE_SAME_RULE, JsonHandler.unprettyPrint(au1));
    }

    @Test
    public void testCumulDifferentRuleID() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU1");
        UnitInheritedRule au2 =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_2)), "AU2");
        au2.concatRule(au1);

        assertEquals(EXPECTED_CUMUL, JsonHandler.unprettyPrint(au2));
    }

    @Test
    public void testSpecificPathValue() throws Exception {
        UnitInheritedRule marxDormoy = new UnitInheritedRule();
        UnitInheritedRule laChapelle = new UnitInheritedRule();
        UnitInheritedRule frontPopulaire = new UnitInheritedRule()
            .createNewInheritedRule((ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TWO_IN_ONE)),
                "FrontPopulaire");

        UnitInheritedRule rule1 = frontPopulaire.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(CHAPELLE)), "LaChapelle");
        laChapelle.concatRule(rule1);

        UnitInheritedRule rule2 = laChapelle.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MARX_DORMOY)), "MarxDormoy");
        marxDormoy.concatRule(rule2);

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_FOR_MARX_DORMOY))),
            JsonHandler.unprettyPrint(marxDormoy));
    }

    @Test
    public void testLongPathValue() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU2"));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));

        UnitInheritedRule au4 = new UnitInheritedRule();
        au4.concatRule(au3.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU4"));

        assertEquals(EXPECTED_LONG_PATH, JsonHandler.unprettyPrint(au4));
    }

    @Test
    public void testMultiplePath() throws Exception {
        // Prepare root
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU0");

        // Prepare 1st level childrend
        UnitInheritedRule intermedEmpty1 = new UnitInheritedRule();
        intermedEmpty1.concatRule(au1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty1"));

        UnitInheritedRule intermedEmpty2 = new UnitInheritedRule();
        intermedEmpty2.concatRule(au1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty2"));

        // Prepare child with 2 father
        UnitInheritedRule finalAu2 = new UnitInheritedRule();
        UnitInheritedRule au2 =
            intermedEmpty1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU2");
        finalAu2.concatRule(au2);
        UnitInheritedRule au2bis =
            intermedEmpty2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU2");
        finalAu2.concatRule(au2bis);

        // Prepare 1st level childrend
        UnitInheritedRule intermedEmpty3 = new UnitInheritedRule();
        intermedEmpty3
            .concatRule(finalAu2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty3"));

        UnitInheritedRule intermedEmpty4 = new UnitInheritedRule();
        intermedEmpty4
            .concatRule(finalAu2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty4"));

        // Prepare last child with 2 father
        UnitInheritedRule finalAu3 = new UnitInheritedRule();
        UnitInheritedRule au3 =
            intermedEmpty3.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3");
        finalAu3.concatRule(au3);
        UnitInheritedRule au3bis =
            intermedEmpty4.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3");
        finalAu3.concatRule(au3bis);

        // assert 2 path from fathers
        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_MULTI_PATH))),
            JsonHandler.unprettyPrint(finalAu3));
    }

    // RefNonRuleId part

    @Test
    public void testRefNonRuleId() throws Exception {

        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(R1_NON_REF_ID)), "AU2"));

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_REF_NON_RULE_ID))),
            JsonHandler.unprettyPrint(au2));
    }

    @Test
    public void testRefNonRuleIdOnNewRule() throws Exception {
        UnitInheritedRule au1 =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(NEW_RULE_REF_NON_RULE_ID)), "AU2"));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));
        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_NEW_RULE_REF_NON_RULE_ID))),
            JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testMultipleRefNonRuleIdOnNewRule() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MULTIPLE_NEW_RULE_REF_NON_RULE_ID)),
            "AU2"));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));
        assertEquals(EXPECTED_MULTIPLE_NEW_RULE_REF_NON_RULE_ID, JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testMultipleRefNonRuleId() throws Exception {
        UnitInheritedRule au1 =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(ROOT_MULTIPLE_RULES)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MULTIPLE_REF_NON_RULE_ID)), "AU2"));

        assertEquals(EXPECTED_MULTIPLE_REF_NON_RULE_ID, JsonHandler.unprettyPrint(au2));
    }

    @Test
    public void testMDRefNonRuleId() throws Exception {
        UnitInheritedRule au0 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(ROOT_NODE_WITH_MD)), "AU0");

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_METADATA_REF_NON_RULE_ID))),
            JsonHandler.unprettyPrint(au0));
    }

    // PreventInheritance part

    @Test
    public void testPreventInheritance() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_PREVENTINHERITANCE)),
            "AU2"));

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_PREVENTED_RULE))),
            JsonHandler.unprettyPrint(au2));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_PREVENTED_RULE_INHERIT))),
            JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testPreventInheritanceWhileDeclaringRuleInCateg() throws Exception {
        UnitInheritedRule au1 =
            new UnitInheritedRule().createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_PREVENTINHERITANCE_NEW)),
            "AU2"));

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_PREVENT_WILE_DECLARE))),
            JsonHandler.unprettyPrint(au2));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));

        assertEquals(EXPECTED_FINAL_PREVENT_WHILE_DECLARE, JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testPreventInheritanceAndDeclareNewRuleInCateg() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT)), "AU0");

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_PREVENTINHERITANCE_NEW)),
            "AU2"));

        assertEquals(JsonHandler
            .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_PREVENT_AND_DECLARE))),
            JsonHandler.unprettyPrint(au2));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3"));

        assertEquals(
            JsonHandler.unprettyPrint(
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_FINAL_PREVENT_AND_DECLARE))),
            JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testPreventInheritanceFromMetadataManagement() throws Exception {
        UnitInheritedRule au1 =
            new UnitInheritedRule().createNewInheritedRule((ObjectNode) JsonHandler
                .getFromFile(PropertiesUtils.getResourceFile(STORAGE_PREVENTINHERITANCE_NEW)), "AU2");

        assertEquals(
            JsonHandler
                .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_PREVENT_WILE_DECLARE))),
            JsonHandler.unprettyPrint(au1));

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(LEVEL_1)), "AU3"));

        assertEquals(EXPECTED_CHILD_PREVENT_WILE_DECLARE, JsonHandler.unprettyPrint(au3));
    }

    @Test
    public void testSaintLazare() throws Exception {
        UnitInheritedRule root = new UnitInheritedRule()
            .createNewInheritedRule((ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TWO_IN_ONE)),
                "Front Populaire");

        UnitInheritedRule au1 = new UnitInheritedRule();
        au1.concatRule(root.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(CHAPELLE)), "Porte de la Chapelle"));

        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au1.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MARX_DORMOY)), "Marx Dormoy"));

        UnitInheritedRule root2 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PLEYEL)), "Carrefour Pleyel");

        UnitInheritedRule au3 = new UnitInheritedRule();
        au3.concatRule(root2.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(SAINT_LAZARE)), "Saint Lazare"));
        au3.concatRule(au2.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(SAINT_LAZARE)), "Saint Lazare"));

        assertEquals(EXPECTED_FOR_SAINT_LAZARE, JsonHandler.unprettyPrint(au3));
    }

    @Test
    // Should not throw NPE (Old error when other field than rule category is in Management)
    public void testNPEReproduction() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_NPE_REPRODUCTION)), "AU1");

        UnitInheritedRule au0 =
            new UnitInheritedRule().createNewInheritedRule(
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_PREVENTINHERITANCE_NEW)),
                "AU0");
        UnitInheritedRule au2 = new UnitInheritedRule();
        au2.concatRule(au0.createNewInheritedRule(
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(STORAGE_NPE_REPRODUCTION)),
            "AU2"));

        assertEquals(
            JsonHandler
                .unprettyPrint(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(EXPECTED_FOR_NPE_REPRODUCTION))),
            JsonHandler.unprettyPrint(au2));
    }

}
