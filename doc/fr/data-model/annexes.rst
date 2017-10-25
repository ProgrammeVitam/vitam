Annexes
#######

Valeurs possibles pour le champ evType du LogBook Operation
------------------------------------------------------------

L'ensemble des étapes, tâches et traitements sont détaillés dans la documentation modèle de workflow

Valeurs possibles pour le champ evType du LogBook LifeCycle
------------------------------------------------------------
  
L'ensemble des étapes, tâches et traitements sont détaillées dans la documentation modèle de workflow

Valeurs possibles pour le champ evTypeProc (type de processus)
--------------------------------------------------------------

.. csv-table::
  :header: "Process Type","Valeur", "Description"

  "Audit type process","AUDIT", "Audit"
  "Check type process","CHECK", "Vérification"
  "Destruction type process (v2)","DESTRUCTION", "Élimination"
  "Ingest type process","INGEST", "Entrée"
  "Preservation type process","PRESERVATION", "Préservation"
  "Rules Manager process","MASTERDATA", "Données de base"
  "Traceability type process","TRACEABILITY", "Sécurisation"
  "Update process","UPDATE", "Mise à jour"

Catégories de règles possibles
--------------------------------

.. csv-table::
  :header: "Prefixe (Peut être modifié)", "Type de règle correspondante", "Description du type de règle"

  "ACC", "AccessRule", "Règle d'accès / délai de communicabilité"
  "APP", "Appraisal", "Règle correspondant à la durée d'utilité administrative (DUA)/ Durée de rétention / conservation"
  "CLASS", "ClassificationRule", "Règle de classification"
  "DIS", "DisseminationRule", "Règle de diffusion"
  "REU", "ReuseRule", "Règle de réutilisation"
  "STO", "StorageRule", "Durée d'utilité courante / durée de conservation au sens de la loi Informatique et Libertés"

Valeurs possibles pour le champ Status de la collection AccessionRegisterDetail
-------------------------------------------------------------------------------

.. csv-table::
  :header: "Status type", "Valeur"

  "Le fonds est complet sauvegardé", "STORED_AND_COMPLETED"
  "Le fonds est mis à jour et sauvegardé", "STORED_AND_UPDATED"
  "Le fonds n'est pas sauvegardé", "UNSTORED"

Valeurs possibles pour le champ Name de la collection VitamSecquence
--------------------------------------------------------------------

.. csv-table::
  :header: "Prefixe", "Type de collection correspondante", "Decription"

  "AC", "AccessContract", "Contrat d'accès"
  "IC", "IngestContract", "Contrat d'entrée"
  "PR", "Profile", "Profils d'archivage"
  "CT", "Context", "Contextes applicatifs"
  
Diagramme du modèle de données Vitam
------------------------------------

.. figure:: images/vitam-class-diagram.png
    :align: center
    :height: 15 cm

    Modèle de données métier : vue d'ensemble.