package fr.gouv.vitam.metadata.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

public class UnitNodeTest {

    private static final String AU2_ID = "AU2";
    private static final String AU1_ID = "AU1";
    private static final String AU5_ID = "AU5";
    private static final String AU3_ID = "AU3";
    private static final String AU4_ID = "AU4";
    
    private final static String AU1_MGT = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rule\" : \"R1\"," +
        "      \"FinalAction\" : \"RestrictedAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }," +
        "    \"AccessRule\" : {" +
        "      \"Rule\" : \"R2\"," +
        "      \"FinalAction\" : \"RestrictedAccess\"," +
        "      \"StartDate\" : \"01/01/2017\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }" +
        "  }";
    
    private final static String AU5_MGT = "{" +
        "    \"DissiminationRule\" : {" +
        "      \"Rule\" : \"R1\"" +
        "    }" +
        "  }";
    
    private final static String AU3_MGT = "{" +
        "    \"AccessRule\" : {" +
        "      \"Rule\" : \"R2\"," +
        "      \"FinalAction\" : \"AU3Access\"," +
        "      \"EndDate\" : \"01/01/2019\"" +
        "    }" +
        "  }";

    private ObjectNode node = JsonHandler.createObjectNode();
    private List<String> emptyParent = new ArrayList<String>();
    private List<String> upAU2 = new ArrayList<String>();
    private List<String> upAU3 = new ArrayList<String>();
    private List<String> upAU4 = new ArrayList<String>();
    
    @Test
    public void testTree() throws InvalidParseOperationException {
        // Test case 
        // AU1 -> AU2 -> AU3 -> AU4
        // AU1 -> AU2 -> AU4
        // AU5 -> AU3 -> AU4
        
        upAU2.add(AU1_ID);
        
        upAU3.add(AU2_ID);
        upAU3.add(AU5_ID);
        
        upAU4.add(AU3_ID);
        upAU4.add(AU2_ID);
        
        UnitSimplified AU1 = new UnitSimplified(AU1_ID, (ObjectNode) JsonHandler.getFromString(AU1_MGT), emptyParent);
        UnitSimplified AU5 = new UnitSimplified(AU5_ID, (ObjectNode) JsonHandler.getFromString(AU5_MGT), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID, node, upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID, (ObjectNode) JsonHandler.getFromString(AU3_MGT), upAU3);
        UnitSimplified AU4 = new UnitSimplified(AU4_ID, node, upAU4);
        Map<String, UnitSimplified> parentMap = new HashedMap<>();
        parentMap.put(AU1_ID, AU1);
        parentMap.put(AU5_ID, AU5);
        parentMap.put(AU2_ID, AU2);
        parentMap.put(AU3_ID, AU3);
        Map<String, UnitNode> allUnitNode = new HashMap<String, UnitNode>();
        
        UnitNode treeAU4 = new UnitNode(AU4);
        Set<String> rootList = new HashSet<>();
        treeAU4.buildAncestors(parentMap, allUnitNode, rootList);
        
        
        assertEquals(rootList.size(), 2);
        assertEquals(allUnitNode.size(), 5);
        
        UnitNode treeAU3FromAllUnitNode = allUnitNode.get(AU3_ID);
        UnitNode treeAU3FromAU4 = treeAU4.getDirectParent().get(AU3_ID);
        
        UnitNode treeAU2FromAllUnitNode = allUnitNode.get(AU2_ID);
        UnitNode treeAU2FromAU4 = treeAU4.getDirectParent().get(AU2_ID);
        
        assertEquals(treeAU3FromAU4, treeAU3FromAllUnitNode);
        assertEquals(treeAU2FromAU4, treeAU2FromAllUnitNode);
        
    }

}
