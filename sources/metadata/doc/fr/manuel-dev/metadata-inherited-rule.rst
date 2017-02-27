Métadata
########

Utilisation
###########

Paramètres
**********


Calcule des règles de gestion pour une unité archivistique 
*********


.. code-block exemple :: java

	1. Requête DSL 
	
	Pour calculer les règles héritées de l'archive Unit. Il faut ajouter "$rules : 1" dans le filtre de la requête 
	DSL.
        
	2. Calculer des règles de gestion pour une unité archivistique
	
	Le serveur vérifie la requête, si son filtre contient "$rules : 1". On démarre la procédure de calcul des règles héritées

	2.1 Rechercher les règles de gestion des parents et lui même

	 	createSearchParentSelect(List<String> unitList)

	2.1 Construire le graphe DAG avec tous les unité archivistique 
	
	ArrayNode unitParents = selectMetadataObject(newSelectQuery.getFinalSelect(), null, null);

        Map<String, UnitSimplified> unitMap = UnitSimplified.getUnitIdMap(unitParents);

        UnitRuleCompute unitNode = new UnitRuleCompute(unitMap.get(unitId));

        unitNode.buildAncestors(unitMap, allUnitNode, rootList);

	2.3 Calculer des règles de gestion et mettre dans le résultat final

        unitNode.computeRule();

        JsonNode rule = JsonHandler.toJsonNode(unitNode.getHeritedRules().getInheritedRule());

        ((ObjectNode)arrayNodeResponse.get(0)).set(UnitInheritedRule.INHERITED_RULE, rule);



