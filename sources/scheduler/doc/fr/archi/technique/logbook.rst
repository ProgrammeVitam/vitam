
Jobs de logbook VITAM
**********************************

Liste des classes implémentant les jobs
--------

| TraceabilityLFCJob
| TraceabilityJob
| TraceabilityAuditJob
| ReconstructionOperationJob

  ....
TraceabilityLFCJob.java
########################

Ce job permet de faire la sécurisation du journal du cycle de vie des unités archivistiques et des groupes d'objets uniquement sur le site primaire:

* Période d'exécution par défaut pour les unités : * 35 0/1 * * ?
* Période d'exécution par défaut pour les groupes objets : * 15 0/1 * * ?

  ....
TraceabilityJob.java
########################

Ce job permet de faire la sécurisation du journal des opérations uniquement sur le site primaire:

* Période d'exécution par défaut : * 05 0/1 * * ?

  ....
TraceabilityAuditJob.java
########################

Ce job permet de faire le contrôle de la validité de la sécurisation des journaux uniquement sur le site primaire:

* Période d'exécution par défaut : 0 55 00 * * ?

  ....
ReconstructionOperationJob.java
########################

Ce job permet de faire la reconstruction des données portées par le composant logbook uniquement sur le site secondaire (primary_site = false):

* Période d'exécution par défaut : 0 0/5 * * * ?