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
package fr.gouv.vitam.metadata.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

@Deprecated
public class UnitRuleComputeTest {

    private static final String AU2_ID = "AU2";
    private static final String AU1_ID = "AU1";
    private static final String AU5_ID = "AU5";
    private static final String AU3_ID = "AU3";
    private static final String AU4_ID = "AU4";

    private final static String MGT1 = "UnitRuleCompute/MGT1.json";
    private final static String MGT10 = "UnitRuleCompute/MGT10.json";
    private final static String MGT3 = "UnitRuleCompute/MGT3.json";
    private final static String MGT4 = "UnitRuleCompute/MGT4.json";


    private final static String AU1_MGT = "UnitRuleCompute/AU1_MGT.json";
    private final static String AU5_MGT = "UnitRuleCompute/AU5_MGT.json";
    private final static String AU3_MGT = "UnitRuleCompute/AU3_MGT.json";
    private final static String AU2_MGT = "UnitRuleCompute/AU2_MGT.json";

    private final static String AU5_MGT_NONREF = "UnitRuleCompute/AU5_MGT_NONREF.json";
    private final static String AU3_MGT_NONREF = "UnitRuleCompute/AU3_MGT_NONREF.json";
    private final static String AU2_MGT_NONREF = "UnitRuleCompute/AU2_MGT_NONREF.json";
    private final static String AU4_MGT_NONREF = "UnitRuleCompute/AU4_MGT_NONREF.json";

    private ObjectNode node = JsonHandler.createObjectNode();
    private List<String> emptyParent = new ArrayList<String>();
    private static List<String> upAU2 = new ArrayList<String>();
    private static List<String> upAU3 = new ArrayList<String>();
    private static List<String> upAU4 = new ArrayList<String>();

    @BeforeClass
    public static void setupBeforeClass() {
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
    public void testTree() throws InvalidParseOperationException, FileNotFoundException {

        UnitSimplified AU1 = new UnitSimplified(AU1_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU1_MGT)), emptyParent);
        UnitSimplified AU5 = new UnitSimplified(AU5_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU5_MGT)), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT)), upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU3_MGT)), upAU3);
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
        assertNotNull(ruleAU4);
    }

    @Test
    public void testExclusionOfInheritance() throws InvalidParseOperationException, FileNotFoundException {

        UnitSimplified AU1 = new UnitSimplified(AU1_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU1_MGT)), emptyParent);
        UnitSimplified AU5 =
            new UnitSimplified(AU5_ID,
                (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU5_MGT_NONREF)), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU2_MGT_NONREF)), upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU3_MGT_NONREF)), upAU3);
        UnitSimplified AU4 = new UnitSimplified(AU4_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU4_MGT_NONREF)), upAU4);
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
    public void testPreventInheritance() throws InvalidParseOperationException, FileNotFoundException {
        // AU1 [R1, ACC-00001] -> AU2 -> AU3 [R3] -> AU4
        // AU10 [R6, ACC-00010, ACC-00011] -> AU2
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

        UnitSimplified AU1 = new UnitSimplified(ID1,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MGT1)), emptyParent);
        UnitSimplified AU10 = new UnitSimplified(ID10,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MGT10)), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(ID2, node, up2);
        UnitSimplified AU3 =
            new UnitSimplified(ID3, (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MGT3)), up3);
        UnitSimplified AU4 =
            new UnitSimplified(ID4, (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(MGT4)), up4);

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
