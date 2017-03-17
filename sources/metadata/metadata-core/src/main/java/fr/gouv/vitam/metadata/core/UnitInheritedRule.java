/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.metadata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * POJO for the result of Inherited Rule
 */
public class UnitInheritedRule {

	private static final String SEPERATOR = "-";
	private static final String OVERRIDE_BY = "OverridedBy";
	private static final String PATH = "path";
	public static final String RULE = "Rule";

	public static final String INHERITED_RULE = "inheritedRule";

	private static final VitamLogger LOGGER =
			VitamLoggerFactory.getInstance(UnitInheritedRule.class);

	@JsonProperty("inheritedRule")
	private Map<String, ObjectNode> inheritedRule;

	/**
	 * empty constructor for every unitNode except root
	 */
	public UnitInheritedRule() {
		inheritedRule = new HashMap<>();
	}

	/**
	 * constructor for rootNode only with ObjectNode and unitId
	 * if unit is not root then unitId should be null
	 * 
	 * @param rule
	 * @param unitId
	 */
	public UnitInheritedRule(ObjectNode rule, String unitId) {
		this();
		initRules(rule, unitId);

	}

	/**
	 * Transformation of unit management
	 * Example: Unit Root AU1 with  
	 * {"_mgt" : {"StorageRule": {"Rule": "R1", "StartDate": "S1", "EndDate": "E1" }}}
	 *  
	 *  will be converted to
	 *   
	 * { "inheritedRule" : {"StorageRule": {"R1": {"AU1": {"StartDate": "S1", "EndDate": "E1", "path": ["AU1"]}}}} }
	 */
	private void initRules(ObjectNode unitManagement, String unitId) {
		Iterator<String> fieldNames = unitManagement.fieldNames(); 
		while(fieldNames.hasNext()){
			String fieldName = fieldNames.next();
			
			if (unitManagement.get(fieldName).isObject()) {
				ObjectNode fieldValue = (ObjectNode) unitManagement.get(fieldName);
				ObjectNode ruleCategories = createRuleCategories(fieldValue, unitId);
				inheritedRule.put(fieldName, ruleCategories);
            } else if (unitManagement.get(fieldName).isArray()) {
                ObjectNode newCategories = JsonHandler.createObjectNode();
                for (JsonNode rule : (ArrayNode) unitManagement.get(fieldName)) {
                    ObjectNode ruleCategories = createRuleCategories((ObjectNode) rule, unitId);
                    newCategories.set(rule.get(RULE).asText(), ruleCategories.get(rule.get(RULE).asText()));
                }
                inheritedRule.put(fieldName, newCategories);
            }
		}
	}

	/**
	 * @return
	 */
	public Map<String, ObjectNode> getInheritedRule() {
		return inheritedRule;
	}

	/**
	 * @param rules
	 * @return
	 */
	public UnitInheritedRule setInheritedRule(Map<String, ObjectNode> rules) {
		this.inheritedRule = rules;
		return this;
	}

	/**
	 * @param ruleCategory
	 * @param ruleResults
	 */
	public UnitInheritedRule addInheritedRule(String ruleCategory, ObjectNode ruleResults) {
		inheritedRule.put(ruleCategory, ruleResults);
		return this;
	}

	/**
	 * Concat UnitInheritedRule when unit have many parent
	 * 
	 * @param parentRule
	 * @return
	 */
	public void concatRule(UnitInheritedRule parentRule) {
		for (Entry<String, ObjectNode> entry : parentRule.inheritedRule.entrySet()) {
			String parentCategoryName = entry.getKey();
			ObjectNode parentCategoryNode = entry.getValue();
			if (inheritedRule.containsKey(parentCategoryName)) {
				// 2 Rules have the same rule category 
				Iterator<String> parentRuleIds = parentCategoryNode.fieldNames(); 
				while(parentRuleIds.hasNext()){
					String ruleId = parentRuleIds.next();
					ObjectNode selfCategoryNode = inheritedRule.get(parentCategoryName);
					ObjectNode selfOriginNode = (ObjectNode) selfCategoryNode.get(ruleId);
					if (selfOriginNode != null) {
						// 2 Rules have the same rule category, same ruleId
						Iterator<String> parentOriginIds = parentCategoryNode.get(ruleId).fieldNames();
						while(parentOriginIds.hasNext()){
							String parentOriginId = parentOriginIds.next();
							ObjectNode selfOriginDetailNode = (ObjectNode) selfOriginNode.get(parentOriginId);
							ObjectNode parentOriginDetailNode = (ObjectNode) parentCategoryNode.get(ruleId).get(parentOriginId);
							if (selfOriginDetailNode != null) {
								// 2 Rules have the same rule category, same ruleId, same rule Origin
								if (!selfOriginDetailNode.get(PATH).equals(parentOriginDetailNode.get(PATH))) {
									((ArrayNode) selfOriginDetailNode.get(PATH)).addAll((ArrayNode) parentOriginDetailNode.get(PATH));
								}
							} else {
								selfOriginNode.set(parentOriginId, parentOriginDetailNode);
							}
							// Remove duplicated rules 
							if (parentOriginDetailNode.get(OVERRIDE_BY) != null) {
								for (JsonNode reference: (ArrayNode) parentOriginDetailNode.get(OVERRIDE_BY)) {
									String referenceId = reference.asText();
									selfOriginNode.remove(referenceId);
								}
							}
							if (selfOriginDetailNode != null && selfOriginDetailNode.get(OVERRIDE_BY) != null) {
								for (JsonNode reference: (ArrayNode) selfOriginDetailNode.get(OVERRIDE_BY)) {
									String referenceId = reference.asText();
									selfOriginNode.remove(referenceId);
								}
							}
						}
					} else {
						selfCategoryNode.set(ruleId, parentCategoryNode.get(ruleId)); 
					}
				}
			} else {
				inheritedRule.put(parentCategoryName, parentCategoryNode);
			}
		}
	}

	/**
	 * Herite rule from parent with unit management 
	 * 
	 * @param unitManagement
	 * @param unitId
	 * @return
	 */
	public UnitInheritedRule createNewInheritedRule(ObjectNode unitManagement, String unitId) {
		UnitInheritedRule newRule = deepCopy(this);
		Map<String, ObjectNode> ruleCategoryFromUnit = new HashMap<>();
		Map<String, String> ruleIdTodReplace = new HashMap<>();
		List<String> parentCategoryList = new ArrayList<>();

		for (Entry<String, ObjectNode> entry : newRule.inheritedRule.entrySet()) {
			String categoryName = entry.getKey();
			parentCategoryList.add(categoryName);
			ObjectNode categoryNode = entry.getValue();
			ruleCategoryFromUnit.remove(categoryName);
			if (unitManagement.get(categoryName) == null) {
				// Unit management does not contain category rule
				Iterator<String> ruleIds = categoryNode.fieldNames(); 
				while(ruleIds.hasNext()){
					String ruleId = ruleIds.next();
					ObjectNode originNode = (ObjectNode) categoryNode.get(ruleId);
					Iterator<String> originIds = originNode.fieldNames(); 
					while(originIds.hasNext()){
						String originId = originIds.next();
						ArrayNode pathNode = (ArrayNode) originNode.get(originId).get(PATH);
						updateOriginPath(pathNode, unitId);
					}
				}
			} else {
				// Unit management contains category rule
				Iterator<String> ruleIds = categoryNode.fieldNames(); 
				while(ruleIds.hasNext()){
					String ruleId = ruleIds.next();
					ObjectNode ruleNode = (ObjectNode) categoryNode.get(ruleId);
					
                    if (unitManagement.get(categoryName).isObject()){
                        ObjectNode unitRuleNode = (ObjectNode) unitManagement.get(categoryName);
                        compareInheritedRuleWithManagement(unitRuleNode, ruleId, ruleNode, unitId, ruleIdTodReplace);
                    } else if (unitManagement.get(categoryName).isArray()) {
                        ArrayNode unitRuleNode = (ArrayNode) unitManagement.get(categoryName);
                        for (JsonNode node : unitRuleNode){
                            if (node.has(RULE)) {
                                compareInheritedRuleWithManagement((ObjectNode) node, ruleId, ruleNode, unitId, ruleIdTodReplace);
                            }
                        }
                    }
					
				}
				for (Entry<String, String> ruleIdEntry : ruleIdTodReplace.entrySet()) {
					String originId = ruleIdEntry.getKey().split(SEPERATOR, 2)[0];
					String ruleId = ruleIdEntry.getKey().split(SEPERATOR, 2)[1];
					JsonNode ruleNode = categoryNode.get(ruleId).get(originId);
					((ObjectNode) categoryNode.get(ruleId)).remove(originId);
					((ObjectNode) categoryNode.get(ruleId)).set(ruleIdEntry.getValue(), ruleNode);
				}
				ruleIdTodReplace.clear();
			}
		}

		Iterator<String> fieldNames = unitManagement.fieldNames();
		while (fieldNames.hasNext()) {
			String unitRuleCategory = fieldNames.next();
			if (!parentCategoryList.contains(unitRuleCategory)) {
				ObjectNode ruleCategories = createRuleCategories((ObjectNode) unitManagement.get(unitRuleCategory), unitId);
				ruleCategoryFromUnit.put(unitRuleCategory, ruleCategories);
			} else {
                JsonNode unitCategory = unitManagement.get(unitRuleCategory);
                if (unitCategory.isArray()) {
                    for (JsonNode unitRuleNode : unitCategory) {
                        String ruleId = unitRuleNode.get(RULE).asText();
                        if (newRule.inheritedRule.get(unitRuleCategory).get(ruleId) == null) {
                            ObjectNode unitNode = createNewRuleWithOrigin((ObjectNode) unitRuleNode, unitId);
                            newRule.inheritedRule.get(unitRuleCategory).set(ruleId, unitNode);
                        }
                    }
                }

			}
		}
		newRule.inheritedRule.putAll(ruleCategoryFromUnit);
		return newRule;
	}

	private void compareInheritedRuleWithManagement(ObjectNode unitRuleNode, String ruleId, ObjectNode ruleNode, String unitId, 
        Map<String, String> ruleIdTodReplace){
        
        String unitRuleId = unitRuleNode.get(RULE).asText();

        if (!unitRuleId.equals(ruleId)) {
            // Unit management contains category rule but not the ruleId
            Iterator<String> originIds = ruleNode.fieldNames(); 
            while(originIds.hasNext()){
                String originId = originIds.next();
                ArrayNode pathNode = (ArrayNode) ruleNode.get(originId).get(PATH);
                updateOriginPath(pathNode, unitId);
            }

        } else {
            // Unit management contains category rule with the same ruleId
            Iterator<String> originIterator = ruleNode.fieldNames();
            while(originIterator.hasNext()){
                String originId = originIterator.next();
                ObjectNode originNode = (ObjectNode) ruleNode.get(originId);
                ruleNode.set(originId, createNewOrigin(unitRuleNode, 
                        JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId))));
                ruleIdTodReplace.put(originId + SEPERATOR + ruleId, unitId);
                if (originNode.get(OVERRIDE_BY) != null) {
                    ((ArrayNode) originNode.get(OVERRIDE_BY)).add(unitId);
                } else {
                    originNode.set(OVERRIDE_BY, JsonHandler.createArrayNode().add(unitId));
                }
            }
        }
    }

	private ObjectNode createRuleCategories(ObjectNode fieldValue, String unitId) {
		String ruleId = "";
		if (fieldValue.has(RULE)) {
			ruleId = fieldValue.get(RULE).textValue();
		}
		ObjectNode newValue = JsonHandler.createObjectNode();
		newValue.setAll(fieldValue);
		newValue.remove(RULE);
		newValue.set(PATH, JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId)));

		//Create Origin of rule (arrayNode)
		ObjectNode ruleOrigin = JsonHandler.createObjectNode();
		ruleOrigin.set(unitId, newValue);

		//Create Category of rule (arrayNode)
		ObjectNode ruleCategory = JsonHandler.createObjectNode();
		ruleCategory.set(ruleId, ruleOrigin);                
		return ruleCategory;
	}

	private void updateOriginPath(ArrayNode pathNode, String unitId) {
		for (JsonNode subPath: pathNode) {
			ArrayNode pathArray = (ArrayNode) subPath;
			boolean shouldBeAdded = true;
			for (JsonNode path: pathArray) {
				shouldBeAdded = path.asText().equals(unitId) ? false : shouldBeAdded;
			}
			if (shouldBeAdded) {
				pathArray.add(unitId);
			}
		}
	}

	private ObjectNode createNewOrigin(ObjectNode unitRuleNode, ArrayNode pathNode) {
		ObjectNode newValue = JsonHandler.createObjectNode();
		newValue.setAll(unitRuleNode);
		newValue.remove(RULE);
		newValue.set(PATH, pathNode);
		return newValue;
	}

	private ObjectNode createNewRuleWithOrigin(ObjectNode unitRuleNode, String unitId) {
		ArrayNode pathNode = JsonHandler.createArrayNode().add(JsonHandler.createArrayNode().add(unitId));
		ObjectNode newOrigin = JsonHandler.createObjectNode();
		ObjectNode newRule = JsonHandler.createObjectNode();
		newOrigin.setAll(unitRuleNode);
		newOrigin.remove(RULE);
		newOrigin.set(PATH, pathNode);
		newRule.set(unitId, newOrigin);
		return newRule;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return inheritedRule.isEmpty();
	}

	/**
	 * Deep copy a new unit rule
	 * @param unit
	 * @return
	 */
	public UnitInheritedRule deepCopy(UnitInheritedRule unit) {
		UnitInheritedRule newRule = new UnitInheritedRule();
		for (Entry<String, ObjectNode> entry : unit.inheritedRule.entrySet()) {
			try {
				newRule.inheritedRule.put(entry.getKey(), (ObjectNode) JsonHandler.getFromString(entry.getValue().toString()));
			} catch (InvalidParseOperationException e) {
				SysErrLogger.FAKE_LOGGER.ignoreLog(e);
			}
		}
		return newRule;
	}

}
