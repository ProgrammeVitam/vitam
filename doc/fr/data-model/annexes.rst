Annexes
#######

Valeurs possibles pour le champ evType logBook Operation
--------------------------------------------------------

L'ensemble des étapes, tâches et traitements sont détaillées dans la documentation modèle de workflow

Valeurs possibles pour le champ evType logBook LifeCycle
--------------------------------------------------------
  
L'ensemble des étapes, tâches et traitements sont détaillées dans la documentation modèle de workflow

Valeurs possibles pour le champ evTypeProc
------------------------------------------

.. csv-table::
  :header: "Process Type","Valeur"

  "Audit type process","AUDIT"
  "Check type process","CHECK"
  "Destruction type process (v2)","DESTRUCTION"
  "Ingest type process","INGEST"
  "Preservation type process","PRESERVATION"
  "Rules Manager process","MASTERDATA"
  "Traceability type process","TRACEABILITY"
  "Update process","UPDATE"

Catégories de règles possibles
--------------------------------

.. csv-table::
  :header: "Prefixe (Peut être modifié)", "Type de règle correspondante", "Description du type de règle"

  "ACC", "AccessRule", "Règle d'accès / délais de communicabilité"
  "APP", "Appraisal", "Règle correspondant à la durée d'utilité administrative (DUA)/Durée de rétention"
  "CLASS", "ClassificationRule", "Règle de classification"
  "DIS", "DisseminationRule", "Règle de diffusion"
  "REU", "ReuseRule", "Règle de réutilisation"
  "STO", "StorageRule", "Durée d'utilité courante / durée de conservation au sens de la loi Informatique et Libertés"

Valeurs possibles pour le champ Status de la collection AccessionRegisterDetail
-------------------------------------------------------------------------------

.. csv-table::
  :header: "Status type", "Valeur"

  "Le fonds est complet sauvegardé", "STORED_AND_COMPLETED"
  "Le fonds est mis à jour est sauvegardé", "STORED_AND_UPDATED"
  "Le fonds n'est pas sauvagerdé", "UNSTORED"