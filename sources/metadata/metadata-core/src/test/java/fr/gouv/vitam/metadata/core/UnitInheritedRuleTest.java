package fr.gouv.vitam.metadata.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.json.JsonHandler;

public class UnitInheritedRuleTest {

    private final static String EXPECTED_RESULT = "{\"inheritedRule\":" +
        "{\"StorageRule\":" +
        "{\"R1\":" +
        "{\"AU2\":{" +
        "\"FinalAction\":\"NoAccess\"," +
        "\"StartDate\":\"01/01/2017\"," +
        "\"path\":[[\"AU2\"]]}}}," +
        "\"AccessRule\":" +
        "{\"R2\":" +
        "{\"AU1\":{" +
        "\"FinalAction\":\"Access\"," +
        "\"StartDate\":\"01/01/2017\"," +
        "\"EndDate\":\"01/01/2019\"," +
        "\"path\":[[\"AU1\",\"AU2\"]]}}," +
        "\"R4\":{\"AU2\":{" +
        "\"FinalAction\":\"Access\"," +
        "\"StartDate\":\"01/01/2017\"," +
        "\"EndDate\":\"01/01/2019\"," +
        "\"path\":[[\"AU2\"]]}}}}}";

    private final static String EXPECTED_CONCAT_RESULT = "{\"inheritedRule\":" +
        "{\"StorageRule\":" +
        "{\"R1\":" +
        "{\"AU1\":{" +
        "\"FinalAction\":\"RestrictedAccess\"," +
        "\"EndDate\":\"01/01/2019\"," +
        "\"path\":[[\"AU1\"]]}}}," +
        "\"AccessRule\":" +
        "{\"R2\":" +
        "{\"AU1\":{" +
        "\"FinalAction\":\"Access\"," +
        "\"StartDate\":\"01/01/2017\"," +
        "\"EndDate\":\"01/01/2019\"," +
        "\"path\":[[\"AU1\"]]}}}}}";
    private final static String AU1_MGT = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"R1\"," +
        "      \"FinalAction\" : \"RestrictedAccess\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }," +
        "    \"AccessRule\" : {" +
        "      \"Rule\" : \"R2\"," +
        "      \"FinalAction\" : \"Access\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }, \"NeedAuthentication\" : false" +
        "  }";

    private final static String AU2_MGT = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"R1\"," +
        "      \"FinalAction\" : \"NoAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"" +
        "    }," +
        "    \"AccessRule\" : {" +
        "      \"Rule\" : \"R4\"," +
        "      \"FinalAction\" : \"Access\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }" +
        "  }";

    private final static String AU4_MGT_KO = "{" +
        "    \"StorageRule\" : [ [ {" +
        "      \"Rule\" : \"R1\"," +
        "      \"FinalAction\" : \"NoAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"RefNonRuleId\" : \"acc3\"" +
        "    } ] ]"+ 
        "  }";

    private final static String AU5_MGT = "{" +
        "    \"AccessRule\" : [{" +
        "      \"PreventInheritance\" : \"true\"" +
        "    }]" +
        "  }";
    
    private final static String LEVEL_0 = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"STR1\"," +
        "      \"StartDate\" : \"01/01/2019\"" +
        "    }" +
        "  }";
    
    private final static String LEVEL_1 = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"STR1\"," +
        "      \"StartDate\" : \"02/01/2019\"" +
        "    }" +
        "  }";
    
    private final static String LEVEL_2 = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"STR2\"," +
        "      \"StartDate\" : \"02/01/2019\"" +
        "    }" +
        "  }";

    private final static String EXPECTED_MULTI_PATH = "{"
        + "\"inheritedRule\":{\"StorageRule\":"
        + "{\"STR1\":{\"AU0\":{\"StartDate\":\"02/01/2019\","
        + "\"path\":["
        + "[\"AU0\",\"Empty1\",\"AU2\",\"Empty3\",\"AU3\"],"
        + "[\"AU0\",\"Empty2\",\"AU2\",\"Empty3\",\"AU3\"],"
        + "[\"AU0\",\"Empty1\",\"AU2\",\"Empty4\",\"AU3\"],"
        + "[\"AU0\",\"Empty2\",\"AU2\",\"Empty4\",\"AU3\"]"
        + "]}"
        + "}}}}";

    private final static String EXPECTED_LONG_PATH = "{"
        + "\"inheritedRule\":{\"StorageRule\":"
        + "{\"STR1\":{\"AU0\":{\"StartDate\":\"02/01/2019\","
        + "\"path\":[[\"AU0\",\"AU2\",\"AU3\",\"AU4\"]]}"
        + "}}}}";

    private final static String EXPECTED_CUMUL = "{"
        + "\"inheritedRule\":{\"StorageRule\":"
        + "{\"STR2\":{\"AU2\":{\"StartDate\":\"02/01/2019\","
        + "\"path\":[[\"AU2\"]]}},"
        + "\"STR1\":{\"AU1\":{\"StartDate\":\"02/01/2019\","
        + "\"path\":[[\"AU1\"]]}"
        + "}}}}";

    private final static String EXPECTED_REPLACE_SAME_RULE = "{"
        + "\"inheritedRule\":{\"StorageRule\":"
        + "{\"STR1\":{\"AU1\":{\"StartDate\":\"02/01/2019\","
        + "\"path\":[[\"AU1\"]]}"
        + "}}}}";

    private final static String EXPECTED_NEW_RULE_CHILD = "{"
        + "\"inheritedRule\":{\"StorageRule\":"
        + "{\"STR1\":{\"AU1\":{\"StartDate\":\"01/01/2019\",\"path\":[[\"AU1\"]]}"
        + "}}}}";

    private final static String EXPECTED_NEW_RULE_PARENT = "{"
        + "\"inheritedRule\":{\"StorageRule\":{"
        + "\"STR1\":{\"AU0\":{\"StartDate\":\"01/01/2019\",\"path\":[[\"AU0\",\"AU1\"]]}"
        + "}}}}";
    
    private final static String EMPTY = "{}";
    
    @Test
    public void testUnitRuleResult() throws Exception {
        UnitInheritedRule au1RulesResult =
            new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(AU1_MGT), "AU1");
        UnitInheritedRule au2RulesResult =
            au1RulesResult.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(AU2_MGT), "AU2");
        UnitInheritedRule au3RulesResult =
            new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(AU1_MGT), "AU3");
        assertEquals(EXPECTED_RESULT, JsonHandler.unprettyPrint(au2RulesResult));

        au1RulesResult.concatRule(au3RulesResult);
        assertEquals(EXPECTED_CONCAT_RESULT, JsonHandler.unprettyPrint(au1RulesResult));

        au1RulesResult.concatRule(au1RulesResult);
        assertEquals(EXPECTED_CONCAT_RESULT, JsonHandler.unprettyPrint(au1RulesResult));

        UnitInheritedRule au5RulesResult =
            new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(AU5_MGT), "AU5");
        
        assertNotNull(au5RulesResult);
        try {
            new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(AU4_MGT_KO), "AU4");
            fail("Should raise an exception");
        } catch (ClassCastException e) {
            // nothing to do
        }

    }

    @Test
    public void testNewRuleFromParent() throws Exception {
        UnitInheritedRule root = new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_0), "AU0");
        UnitInheritedRule emptyAu = new UnitInheritedRule();
        emptyAu.concatRule(root.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU1"));

        assertEquals(EXPECTED_NEW_RULE_PARENT, JsonHandler.unprettyPrint(emptyAu));
    }

    @Test
    public void testNewRuleOnChild() throws Exception {
        UnitInheritedRule emptyRoot = new UnitInheritedRule();
        UnitInheritedRule au1 = new UnitInheritedRule().createNewInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_0), "AU1");
        au1.concatRule(emptyRoot);

        assertEquals(EXPECTED_NEW_RULE_CHILD, JsonHandler.unprettyPrint(au1));
    }

    @Test
    public void testReplaceSameRuleID() throws Exception {
        UnitInheritedRule root = new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_0), "AU0");

        UnitInheritedRule au1 = new UnitInheritedRule();
        au1.concatRule(root.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_1), "AU1"));

        assertEquals(EXPECTED_REPLACE_SAME_RULE, JsonHandler.unprettyPrint(au1));
    }
    
    @Test
    public void testCumulDifferentRuleID() throws Exception {
        UnitInheritedRule au1 = new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_1), "AU1");
        UnitInheritedRule au2 = new UnitInheritedRule().createNewInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_2), "AU2");
        au2.concatRule(au1);

        assertEquals(EXPECTED_CUMUL, JsonHandler.unprettyPrint(au2));
    }
    
    @Test
    public void testLongPathValue() throws Exception {
    	UnitInheritedRule au1 = new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_1), "AU0");
    	
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
        UnitInheritedRule au1 = new UnitInheritedRule((ObjectNode) JsonHandler.getFromString(LEVEL_1), "AU0");

        // Prepare 1st level childrend
        UnitInheritedRule intermedEmpty1 = new UnitInheritedRule();
        intermedEmpty1.concatRule(au1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty1"));
        System.out.println(JsonHandler.unprettyPrint(intermedEmpty1));

        UnitInheritedRule intermedEmpty2 = new UnitInheritedRule();
        intermedEmpty2.concatRule(au1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty2"));
        System.out.println(JsonHandler.unprettyPrint(intermedEmpty2));

        // Prepare child with 2 father
        UnitInheritedRule finalAu2 = new UnitInheritedRule();
        UnitInheritedRule au2 = intermedEmpty1.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU2");
        finalAu2.concatRule(au2);
        UnitInheritedRule au2bis = intermedEmpty2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU2");
        finalAu2.concatRule(au2bis);
        System.out.println(JsonHandler.unprettyPrint(finalAu2));

        // Prepare 1st level childrend
        UnitInheritedRule intermedEmpty3 = new UnitInheritedRule();
        intermedEmpty3.concatRule(finalAu2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty3"));
        System.out.println(JsonHandler.unprettyPrint(intermedEmpty3));

        UnitInheritedRule intermedEmpty4 = new UnitInheritedRule();
        intermedEmpty4.concatRule(finalAu2.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "Empty4"));
        System.out.println(JsonHandler.unprettyPrint(intermedEmpty4));

        // Prepare last child with 2 father
        UnitInheritedRule finalAu3 = new UnitInheritedRule();
        UnitInheritedRule au3 = intermedEmpty3.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3");
        finalAu3.concatRule(au3);
        UnitInheritedRule au3bis = intermedEmpty4.createNewInheritedRule((ObjectNode) JsonHandler.getFromString(EMPTY), "AU3");
        finalAu3.concatRule(au3bis);
        System.out.println(JsonHandler.unprettyPrint(finalAu3));

        // assert 2 path from fathers
        assertEquals(EXPECTED_MULTI_PATH, JsonHandler.unprettyPrint(finalAu3));
    }
    
    @Test
    public void testCompleteGraphInheritance() throws Exception {
    	// TODO
    }
}
