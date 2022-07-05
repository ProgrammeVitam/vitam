
Jobs de metadata VITAM
**********************************

Liste des classes implémentant les jobs
--------

| AuditDataConsistencyMongoEsJob
| ProcessObsoleteComputedInheritedRulesJob
| PurgeDipJob
| PurgeSipJob
| ReconstructionJob
| StoreGraphJob

  ....
AuditDataConsistencyMongoEsJob.java
########################

Ce job permet de faire l'audit sur la cohérence de données MongoDB et Elasticsearch
* Période d'exécution par défaut : 0 0 0 1 JAN ? 2020

  ....

ProcessObsoleteComputedInheritedRulesJob.java
########################

Ce job permet de faire le recalcule des computedInheritedRules pour les units dont les computedInheritedRules sont marquées comme obsolètes.
* Période d'exécution par défaut : 0 30 2 * * ?
  ....

PurgeDipJob.java
########################

Ce job permet de faire le nettoyage des exports DIPs expirés.
* Période d'exécution par défaut : 0 0 * * * ?
  ....

PurgeSipJob.java
########################

Ce job permet de faire le nettoyage des exports transfers expirés.
* Période d'exécution par défaut : 0 25 2 * * ?
  ....

ReconstructionJob.java
########################

Ce job permet de faire la reconstruction des données portées par le composant metadata.
* Période d'exécution par défaut : 0 0/5 * * * ?
  ....

StoreGraphJob.java
########################

Ce job permet de faire le Log shipping des données graphes portées par le composant metadata.
* Période d'exécution par défaut : 0 10/30 * * * ?
  ....