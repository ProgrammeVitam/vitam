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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Deprecated
public class UnitNodeTest {

    private static final String AU2_ID = "AU2";
    private static final String AU1_ID = "AU1";
    private static final String AU5_ID = "AU5";
    private static final String AU3_ID = "AU3";
    private static final String AU4_ID = "AU4";

    private final static String AU1_MGT = "UnitNode/AU1_MGT.json";
    private final static String AU5_MGT = "UnitNode/AU5_MGT.json";
    private final static String AU3_MGT = "UnitNode/AU3_MGT.json";

    private ObjectNode node = JsonHandler.createObjectNode();
    private List<String> emptyParent = new ArrayList<String>();
    private List<String> upAU2 = new ArrayList<String>();
    private List<String> upAU3 = new ArrayList<String>();
    private List<String> upAU4 = new ArrayList<String>();

    @Test
    public void testTree() throws InvalidParseOperationException, FileNotFoundException {
        // Test case
        // AU1 -> AU2 -> AU3 -> AU4
        // AU1 -> AU2 -> AU4
        // AU5 -> AU3 -> AU4

        upAU2.add(AU1_ID);

        upAU3.add(AU2_ID);
        upAU3.add(AU5_ID);

        upAU4.add(AU3_ID);
        upAU4.add(AU2_ID);

        UnitSimplified AU1 = new UnitSimplified(AU1_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU1_MGT)), emptyParent);
        UnitSimplified AU5 = new UnitSimplified(AU5_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU5_MGT)), emptyParent);
        UnitSimplified AU2 = new UnitSimplified(AU2_ID, node, upAU2);
        UnitSimplified AU3 = new UnitSimplified(AU3_ID,
            (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AU3_MGT)), upAU3);
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
