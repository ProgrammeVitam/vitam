Collection EliminationActionObjectGroup
#######################################

Utilisation de la collection
============================

La collection EliminationActionObjectGroup permet à la solution logicielle Vitam de construire des rapports d'éliminations des groupes d'objets techniques. Les données de cette collection sont temporaires et sont supprimées dès que les rapports correspondants sont créés. Il est donc possible de trouver la collection vide.



Détail des champs
=================

**"_id":** identifiant unique de l'enregistrement

    * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
    * Champ peuplé par la solution logicielle Vitam.
    * Cardinalité : 1-1

**"processId":** identifiant de l'opération d'élimination

  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**_metadata** objet contenant une liste de paramètres concernant les métadonnées du groupe d'objets. Il est composé comme suit :

  - "id" : identifiant du groupe d'objets
  - "status" : statut de l'action d'élimination pour ce groupe d'objets. La valeur est dans l'énumération : DELETED, PARTIAL_DETACHMENT
  - "opi" : identifiant de l'opération d'entrée du groupe d'objets
  - "originatingAgency" : identifiant du service producteur du groupe d'objets.
  - "deletedParentUnitIds" : identifiant des unités archivistiques parentes du groupe d'objets et ayant été supprimées.
  - "objectIds" : identifiants des objets du groupe d'objets

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"creationDateTime":** date d'enregistrement du document

  * Il s'agit d'une date.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
