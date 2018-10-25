Collection EliminationActionUnit
################################

Utilisation de la collection
============================

La collection EliminationActionUnit permet à la solution logicielle Vitam de construire des rapports d'éliminations d'unité archivistiques. Les données de cette collection sont temporaires et sont supprimées dès que les rapports correspondants sont créés. Il est donc possible de trouver la collection vide.

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

**_metadata** objet contenant une liste de paramètres concernant les métadonnées de l'unité archivistiques. Il est composé comme suit :

  - "id" : identifiant unique de l'enregistrement
  - "status" : statut de l'action d'élimination pour cette unité archivistique. La valeur est dans l'énumération : DELETED, NON_DESTROYABLE_HAS_CHILD_UNITS, GLOBAL_STATUS_KEEP, GLOBAL_STATUS_CONFLICT
  - "opi" : identifiant de l'opération d'entrée de cette unité archivistique
  - "originatingAgency" : identifiant du service producteur de cette unité archivistique
  - "objectGroupId" : identifiant du groupe d'objets attaché à cette unité archivistique

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"creationDateTime":** date d'enregistrement du document

  * Il s'agit d'une date.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
