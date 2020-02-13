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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * POJO of simplified of Unit
 * @deprecated : Use the new api /unitsWithInheritedRules instead. To be removed in future releases.
 */
public class UnitSimplified {
    private String id;
    private ObjectNode management;
    private List<String> directParent;
    
    /**
     * COnstructor with ObjectNode
     * 
     * @param unitNode for building UnitSimplified 
     */
    public UnitSimplified(ObjectNode unitNode) {
        ParametersChecker.checkParameterDefault("unitNode", unitNode);
        this.id = unitNode.get(PROJECTIONARGS.ID.exactToken()).asText();
        this.management = (ObjectNode) unitNode.get(PROJECTIONARGS.MANAGEMENT.exactToken());
        this.directParent = new ArrayList<>();
        ArrayNode parentArray = (ArrayNode) unitNode.get(PROJECTIONARGS.UNITUPS.exactToken());
        for (JsonNode parentId : parentArray) {
            this.directParent.add(parentId.asText());
        }
    }
    
    /**
     * Constructor with id, management, parent direct
     * @param id the id of UnitSimplified
     * @param mgt the management of UnitSimplified
     * @param up list of direct parent of UnitSimplified
     */
    public UnitSimplified(String id, ObjectNode mgt, List<String> up) {
        this.id = id;
        this.management = mgt;
        this.directParent = up;
    }

    /**
     * @return id of Unit as String 
     */
    public String getId() {
        return id;
    }

    /**
     * @param id as String
     * @return UnitSimplified where id is setted
     */
    public UnitSimplified setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return management data of Unit as ObjectNode 
     */
    public ObjectNode getManagement() {
        if (management == null) {
            return JsonHandler.createObjectNode();
        }
        return management;
    }

    /**
     * @param management of type ObjectNode
     * @return UnitSimplified where management is setted
     */
    public UnitSimplified setManagement(ObjectNode management) {
        this.management = management;
        return this;
    }

    /**
     * @return list of unit parent as a List of String
     */
    public List<String> getDirectParent() {
        return directParent;
    }

    /**
     * @param directParent as a list of String
     * @return UnitSimplified where directParent is setted 
     */
    public UnitSimplified setDirectParent(List<String> directParent) {
        this.directParent = directParent;
        return this;
    }
    
    /**
     * @param unitList list of units as ArrayNode
     * @return a map of unitId and UnitSimplified
     */
    public static Map<String, UnitSimplified> getUnitIdMap(Iterable<JsonNode> unitList) {
        Map<String, UnitSimplified> unitSimplifiedMap = new HashedMap<>();
        for (JsonNode unit : unitList) {
            UnitSimplified unitSimplified = new UnitSimplified((ObjectNode) unit);
            unitSimplifiedMap.put(unitSimplified.getId(), unitSimplified);
        }
        return unitSimplifiedMap;
    }
}
