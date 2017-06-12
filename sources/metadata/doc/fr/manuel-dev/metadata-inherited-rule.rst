Métadata
########

Utilisation
===========

Paramètres
**********


Calcul des règles de gestion pour une unité archivistique 
************************************************************


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


L'algorithme pour Calculer les règles de gestion
*************************************************

1. Initialiser les règles de gestion pour les units racines
    
    Si (unit n'a pas le parent direct, s'est-à-dire, il est racine)
    	Initialiser un objet UnitInheritedRule avec management et Id du unit
    Autrement
    	Créer un objet UnitInheritedRule vide
    Fin Si

2. Ajouter les règles de gestion qui est hérité par les units parents.

   Cependant, il y a deux cas particuliers -- la prévention d'héritage et l'exclusion d'héritage.
    
    Pour (chaque parentId dans la liste de parent direct)
    	Créer un objet UnitRuleCompute avec UnitSimplified qui contient management et Id du unit et la liste de son parent
    	Calculer le règle (Cette étape est récursive. Il va calculer les règles jusqu'à la racine)
    	Créer un objet UnitInheritedRule qui contient les règles hérités
    	Concaténer les règles par défaut et les règles hérités
    Fin Pour

2.1 La prévention d'héritage

	L’intégration d’une balise <PreventInheritance> dans le SEDA
	Si le champ est « true », toutes les règles héritées des parents sont ignorées sur le nœud courant    	

2.2 L'exclusion d'héritage

	L’intégration d’une balise <RefNonRuleId> dans le SEDA indiquant la règle à désactiver à partir de ce niveau.

		