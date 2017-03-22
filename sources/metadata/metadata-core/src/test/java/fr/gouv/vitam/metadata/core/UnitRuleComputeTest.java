package fr.gouv.vitam.metadata.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class UnitRuleComputeTest {

    private static final String AU2_ID = "AU2";
    private static final String AU1_ID = "AU1";
    private static final String AU5_ID = "AU5";
    private static final String AU3_ID = "AU3";
    private static final String AU4_ID = "AU4";
    
    private final static String MGT1 = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"R1\"" +
        "    }]," +
        "    \"AccessRule\" : [{" +
        "      \"Rule\" : \"ACC-0001\"" +
        "    }]" +
        "  }";
    
    private final static String MGT10 = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"R10\"" +
        "    }]," +
        "    \"AccessRule\" : " +
        "    [" +
        "    {" +
        "      \"Rule\" : \"ACC-0011\"" +
        "    }," +
        "    {" +
        "      \"Rule\" : \"ACC-0010\"" +
        "    }" +
        "    ]" +
        "  }";
    
    private final static String MGT3 = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"R3\"," +
        "      \"PreventInheritance\" : \"true\"" +
        "    }]" +
        "  }";
    
    private final static String MGT4 = "{" +
        "    \"StorageRule\" : " +
        "    [" +
        "    {" +
        "      \"Rule\" : \"R4\"" +
        "    }," +
        "    {" +
        "      \"Rule\" : \"R4bis\"" +
        "    }" +
        "    ]" +
        "  }";
    
    
    private final static String AU1_MGT = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"str1\"," +
        "      \"FinalAction\" : \"RestrictedAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }]," +
        "    \"AccessRule\" : [{" +
        "      \"Rule\" : \"acc1\"," +
        "      \"FinalAction\" : \"RestrictedAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }]" +
        "  }";
    
    private final static String AU5_MGT = "{" +
        "    \"DissiminationRule\" : [{" +
        "      \"Rule\" : \"dis5\"" +
        "    }]" +
        "  }";
    
    private final static String AU3_MGT = "{" +
        "    \"AccessRule\" : [{" +
        "      \"Rule\" : \"acc3\"," +
        "      \"FinalAction\" : \"AU3Access\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }]," +
        "    \"DissiminationRule\" : [{" +
        "      \"Rule\" : \"dis3\"," +
        "      \"PreventInheritance\" : \"true\"" +
        "    }]" +
        "  }";
    
    private final static String AU2_MGT = "{" +
        "    \"AccessRule\" : [{" +
        "      \"PreventInheritance\" : \"true\"" +
        "    }]" +
        "  }";
    
    private final static String AU5_MGT_NONREF = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"str5\"" +
        "    }]," +
        "    \"AccessRule\" : [{" +
        "      \"Rule\" : \"acc5\"" +
        "    }]" +
        "  }";
    
    private final static String AU3_MGT_NONREF = "{" +
        "    \"AccessRule\" : [{" +
        "      \"Rule\" : \"acc3\"," +
        "      \"RefNonRuleId\" : \"acc3\"" +
        "    }]" +
        "  }";
    
    private final static String AU2_MGT_NONREF = "{" +
        "    \"StorageRule\" : [{" +
        "      \"Rule\" : \"str2\"" +
        "    }]," +
        "    \"AccessRule\" : [{" +
        "      \"RefNonRuleId\" : [\"acc1\", \"acc404\"]" +
        "    }]" +
        "  }";
    
    private final static String AU4_MGT_NONREF = "{" +
        "    \"StorageRule\" : [{" +
        "      \"RefNonRuleId\" : [\"str1\", \"str2\", \"str5\"]" +
        "    }]" +
        "  }";

    private ObjectNode node = JsonHandler.createObjectNode();
    private List<String> emptyParent = new ArrayList<String>();
    private static List<String> upAU2 = new ArrayList<String>();
    private static List<String> upAU3 = new ArrayList<String>();
    private static List<String> upAU4 = new ArrayList<String>();
    
    @BeforeClass
    public static void setupBeforeClass(){
      // Test case 
      // AU1 -> AU2 -> AU3 -> AU4
      // AU1 -> AU2 -> AU4
      // AU5 -> AU3 -> AU4
      
      upAU2.add(AU1_ID);
      
      upAU3.add(AU2_ID);
      upAU3.add(AU5_ID);
      
      upAU4.add(AU3_ID);
      upAU4.add(AU2_ID);
    }
    
    @Test
    public void testTree() throws InvalidParseOperationException {
        
        UnitSimplified AU1 = new UnitSimplified(AU1_ID, (ObjectNode) JsonHandler.getFromString(AU1_MGT), emptyParent);
        UnitSimplified AU5 = new UnitSimplified(AU5_ID, (ObjectNode) JsonHandler.getFromString(AU5_MGT), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID, (ObjectNode) JsonHandler.getFromString(AU2_MGT), upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID, (ObjectNode) JsonHandler.getFromString(AU3_MGT), upAU3);
        UnitSimplified AU4 = new UnitSimplified(AU4_ID, node, upAU4);
        Map<String, UnitSimplified> parentMap = new HashedMap<>();
        parentMap.put(AU1_ID, AU1);
        parentMap.put(AU5_ID, AU5);
        parentMap.put(AU2_ID, AU2);
        parentMap.put(AU3_ID, AU3);
        Map<String, UnitNode> allUnitNode = new HashMap<String, UnitNode>();
        
        UnitRuleCompute treeAU4 = new UnitRuleCompute(AU4);
        Set<String> rootList = new HashSet<>();
        treeAU4.buildAncestors(parentMap, allUnitNode, rootList);
        
        
        assertEquals(rootList.size(), 2);
        assertEquals(allUnitNode.size(), 5);
        
        UnitRuleCompute unitRule = new UnitRuleCompute(treeAU4);
        
        unitRule.computeRule();
        String ruleAU4 = JsonHandler.prettyPrint(unitRule.getHeritedRules());
        System.out.print(ruleAU4);
        assertNotNull(ruleAU4);
    }
    
    @Test
    public void testExclusionOfInheritance() throws InvalidParseOperationException{
        
        UnitSimplified AU1 = new UnitSimplified(AU1_ID, (ObjectNode) JsonHandler.getFromString(AU1_MGT), emptyParent);
        UnitSimplified AU5 = new UnitSimplified(AU5_ID, (ObjectNode) JsonHandler.getFromString(AU5_MGT_NONREF), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID, (ObjectNode) JsonHandler.getFromString(AU2_MGT_NONREF), upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID, (ObjectNode) JsonHandler.getFromString(AU3_MGT_NONREF), upAU3);
        UnitSimplified AU4 = new UnitSimplified(AU4_ID, (ObjectNode) JsonHandler.getFromString(AU4_MGT_NONREF), upAU4);
        Map<String, UnitSimplified> parentMap = new HashedMap<>();
        parentMap.put(AU1_ID, AU1);
        parentMap.put(AU5_ID, AU5);
        parentMap.put(AU2_ID, AU2);
        parentMap.put(AU3_ID, AU3);
        Map<String, UnitNode> allUnitNode = new HashMap<String, UnitNode>();
        
        UnitRuleCompute treeAU4 = new UnitRuleCompute(AU4);
        Set<String> rootList = new HashSet<>();
        treeAU4.buildAncestors(parentMap, allUnitNode, rootList);
        
        
        assertEquals(rootList.size(), 2);
        assertEquals(allUnitNode.size(), 5);
        
        UnitRuleCompute unitRule = new UnitRuleCompute(treeAU4);
        
        unitRule.computeRule();
        String ruleAU4 = JsonHandler.prettyPrint(unitRule.getHeritedRules());
        System.out.print(ruleAU4);
        assertNotNull(ruleAU4);        
    }
    
    @Test
    public void testPreventInheritance() throws InvalidParseOperationException{
        //  AU1 [R1, ACC-00001] -> AU2 -> AU3 [R3] -> AU4
        //  AU10 [R6, ACC-00010, ACC-00011] -> AU2
        String ID1 = "AU1";
        String ID10 = "AU10";
        String ID2 = "AU2";
        String ID3 = "AU3";
        String ID4 = "AU4";
        
        List<String> up2 = new ArrayList<String>();
        List<String> up3 = new ArrayList<String>();
        List<String> up4 = new ArrayList<String>();
        
        up2.add(ID1);
        up2.add(ID10);
        up3.add(ID2);
        up4.add(ID3);
        
        UnitSimplified AU1 = new UnitSimplified(ID1, (ObjectNode) JsonHandler.getFromString(MGT1), emptyParent);
        UnitSimplified AU10 = new UnitSimplified(ID10, (ObjectNode) JsonHandler.getFromString(MGT10), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(ID2, node, up2);
        UnitSimplified AU3 = new UnitSimplified(ID3, (ObjectNode) JsonHandler.getFromString(MGT3), up3);
        UnitSimplified AU4 = new UnitSimplified(ID4, (ObjectNode) JsonHandler.getFromString(MGT4), up4);
        
        Map<String, UnitSimplified> parentMap = new HashedMap<>();
        parentMap.put(ID1, AU1);
        parentMap.put(ID10, AU10);
        parentMap.put(ID2, AU2);
        parentMap.put(ID3, AU3);
        Map<String, UnitNode> allUnitNode = new HashMap<String, UnitNode>();
        
        UnitRuleCompute treeAU4 = new UnitRuleCompute(AU4);
        Set<String> rootList = new HashSet<>();
        treeAU4.buildAncestors(parentMap, allUnitNode, rootList);
        
        
        assertEquals(rootList.size(), 2);
        assertEquals(allUnitNode.size(), 5);
        
        UnitRuleCompute unitRule = new UnitRuleCompute(treeAU4);
        
        unitRule.computeRule();
        String ruleAU4 = JsonHandler.prettyPrint(unitRule.getHeritedRules());
        System.out.print(ruleAU4);
    }

}
