Workflow d'import d'un référentiel des vocabulaires de l'ontologie
###########################################################################

Introduction
============

Cette section décrit le processus permettant d'importer des vocabulaires de l'ontologie. Cette opération n'est réalisable que sur le tenant 1 admin.  

Processus d'import et mise à jour des vocabulaires de l'ontologie (vision métier)
=================================================================================

Le processus d'import d'une ontologie permet d'ajouter des vocabulaires qui seront utilisés dans les documents types. 

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import des métadonnées d'une ontologie (IMPORT_ONTOLOGY) 
--------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** : l'ontologie répond aux exigences suivantes :
 
    + Le fichier est au format Json.

    + Les données suivantes sont obligatoires :

	      * Le champ "Identifier" est peuplé d'une chaîne de caractères
	      * Le champs "Type" est peuplé par une valeur comprise dans la liste : 
									- Text 
									- Keyword
									- Date 
									- Long
									- Double 
									- Boolean 
									- Geo-point
									- Enumération de valeur 
	      * Le champs "Origin" est peuplé par la valeur "EXTERNAL" ou "INTERNAL". (L'"INTERNAL" correspond à l'ontologie interne de VITAM embarquée dans la solution)
	   
      + Les données suivantes sont facultatives:

	      * Le champs "SedaField" est peuplé d'une chaîne de caractères
	      * Le champs "ApiField" est peuplé d'une chaîne de caractères
	      * Le champs "Description" est peuplé d'une chaîne de caractères
	      * Le champs "ShortName" correspond au champ traduction, il est peuplé par une chaîne de valeur
	      * Le champs "Collections" indique la collection dans laquelle le vocabulaire est rataché. ex : [ "Unit" ] 

Exemple ontologie :

	[ {
	  "Identifier" : "AcquiredDate",
	  "SedaField" : "AcquiredDate",
	  "ApiField" : "AcquiredDate",
	  "Description" : "unit-es-mapping.json",
	  "Type" : "DATE",
	  "Origin" : "INTERNAL",
	  "ShortName" : "AcquiredDate",
	  "Collections" : [ "Unit" ]
	}, {
	  "Identifier" : "BirthDate",
	  "SedaField" : "BirthDate",
	  "ApiField" : "BirthDate",
	  "Description" : "unit-es-mapping.json",
	  "Type" : "DATE",
	  "Origin" : "INTERNAL",
	  "ShortName" : "BirthDate",
	  "Collections" : [ "Unit" ]
	}]




  + **Statuts** :

    - OK : les règles ci-dessus sont respectées (IMPORT_ONTOLOGY.OK=Succés du processus d'import de l'ontologie)

    - KO : une des règles ci-dessus n'a pas été respectée (IMPORT_ONTOLOGY.KO=Echec du processus d'import de l'ontologie)

    - FATAL : une erreur technique est survenue lors de la vérification de l'import de l'ontologie (IMPORT_ONTOLOGY.FATAL=Erreur fatale lors du processus d'import de l'ontologie)

    - STARTED : Début du processus d'import de l'ontologie( IMPORT_ONTOLOGY.STARTED=Début du processus d'import de l'ontologie) 

    - WARNING : Avertissement lors du processus d'import de l'ontologie ( IMPORT_ONTOLOGY.WARNING=Avertissement lors du processus d'import de l'ontologie )

 
Mise à jour d'une ontologie
---------------------------

La modification d'une ontologie s'effectue par ré-import du fichier Json. Le nouvel import annule et remplace l'ontologie précédente. Ce ré-import observe les règles décrites dans le processus d'import, décrit plus haut.  

NB : Concernant la modification d'une valeur "type" déclaré dans un premier import d'une ontologie et employé dans un Document Type, il est necessaire d'observer les règles de compatibilité suivante :

"type" dans le Document Type -> "type" compatible dans l'ontologie
- Text -> Keyword, Text
- Keyword -> Keyword, Text
- Date -> Keyword, Text
- Long -> Keyword, Text, Double
- Double -> Keyword, Text
- Boolean -> Keyword, Text
- Geo-point -> Keyword, Text
- Enumération de valeur -> Keyword, Text



Sauvegarde du JSON (BACKUP_ONTOLOGY)
-----------------------------------------------

Cette tâche est appellée en import de l'ontologie. 

  + **Règle** : enregistrement d'une copie de la base de données des métadonnées sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (BACKUP_ONTOLOGY.OK=Succés du processus de sauvegarde des ontologies)

      - KO : Echec du processus de sauvegarde de l'ontologie (BACKUP_ONTOLOGY.KO=Echec du processus de sauvegarde des ontologies)





