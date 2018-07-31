Métadata
########

Utilisation
===========

Paramètres
**********

Calcul des règles de gestion pour une unité archivistique via API dédiée
************************************************************************

Un endpoint (GET /unitsWithInheritedRules) permet le calcul des règles de gestion ainsi que les propriétés associées
(de type FinalAction...).

Pour chaque catégorie de règles de gestion (AppraisalRule, ReuseRule...), les règles et les propriétés sont calculées
d'une unité archivistique sont héritées des parents. Excepté les cas suivants :

La prévention d'héritage
------------------------
L’intégration d’une balise <PreventInheritance> dans le SEDA
Si le champ est « true », toutes les règles héritées des parents sont ignorées sur le nœud courant

L'exclusion d'héritage
----------------------
L’intégration d’une balise <RefNonRuleId> dans le SEDA indiquant les règles à désactiver à partir de ce niveau.

La redéfinition de règles ou de propriétés
------------------------------------------
Le nœud courant peut redéclarer une règle (même identifiant) et/ou une propriété déjà déclarées dans des parents.
Dans ce cas, les règles et propriétés des unités parentes ne seront pas héritées.


Calcul des règles de gestion pour une unité archivistique (déprécié)
********************************************************************

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
