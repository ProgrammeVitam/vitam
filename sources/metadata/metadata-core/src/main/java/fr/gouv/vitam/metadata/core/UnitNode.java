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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Object to build the graph of unit
 * @deprecated : Use the new api /unitsWithInheritedRules instead. To be removed in future releases.
 */
public class UnitNode {
    private UnitSimplified unit;
    private Map<String, UnitNode> childs;
    private Map<String, UnitNode> allUnitNode;

    /**
     * Empty Constructor
     */
    public UnitNode() {}

    /**
     * constructor with UnitSimplified
     *
     * @param u UnitSimplified
     */
    public UnitNode(UnitSimplified u) {
        this.unit = u;
        this.childs = new HashMap<>();
        this.allUnitNode = new HashMap<>();
    }

    /**
     * Build a graph of all parents
     *
     * @param parentMap map of parent unit
     * @param allUnitNode map a all unit
     * @param rootList list of root
     */
    public void buildAncestors(Map<String, UnitSimplified> parentMap,
        Map<String, UnitNode> allUnitNode, Set<String> rootList) {
        this.allUnitNode = allUnitNode;
        allUnitNode.put(unit.getId(), this);
        for (String parentId : this.unit.getDirectParent()) {
            UnitNode parentNode;
            if (allUnitNode.containsKey(parentId)) {
                parentNode = allUnitNode.get(parentId);
                parentNode.addChild(this);
            } else {
                parentNode = new UnitNode(parentMap.get(parentId));
                if (parentNode.unit.getDirectParent().isEmpty()) {
                    rootList.add(parentId);
                }
                addParent(parentNode, parentMap, rootList);
            }
        }
    }

    private void addChild(UnitNode childNode) {
        this.childs.put(childNode.unit.getId() , childNode);
    }


    private void addParent(UnitNode parentNode,
        Map<String, UnitSimplified> parentMap, Set<String> rootList) {
        parentNode.addChild(this);
        parentNode.buildAncestors(parentMap, allUnitNode, rootList);
    }

    /**
     * @return DirectParent node
     */
    public Map<String, UnitNode> getDirectParent() {
        Map<String, UnitNode> directParent = new HashMap<String, UnitNode>();
        for (String parentId : this.unit.getDirectParent()) {
            directParent.put(parentId, this.allUnitNode.get(parentId));
        }

        return directParent;
    }

    /**
     * @return UnitSimplified
     */
    public UnitSimplified getUnit() {
        return unit;
    }

    /**
     * @return the map of all unit node 
     */
    public Map<String, UnitNode> getAllUnitNode() {
        return allUnitNode;
    }

    /**
     * @param allUnitNode map of all unit node
     * @return UnitNOde and allUnitNode is setted 
     */
    public UnitNode setAllUnitNode(Map<String, UnitNode> allUnitNode) {
        this.allUnitNode = allUnitNode;
        return this;
    }
}
