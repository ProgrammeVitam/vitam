
Jobs de functional-administration VITAM
****************************************

Liste des classes implémentant les jobs
--------

| ReconstructionAccessionRegisterJob
| ReconstructionReferentialJob
| ReferentialCreateSymblolicAccessionRegisterJob
| RuleManagementAuditJob

  ....
ReconstructionAccessionRegisterJob.java
########################################

Ce job permet de faire la reconstruction des données concernant AccessionRegisterSymbolic et AccessionRegisterDetail portées par le composant functional-administration uniquement sur le site secondaire:

* Périodicité d'exécution par défaut pour les unités : 0 0/5 * * * ?

  ....
ReconstructionReferentialJob.java
###################################

Ce job permet de faire la reconstruction des données portées par le composant functional-administration uniquement sur le site secondaire:

* Périodicité d'exécution par défaut : 0 0/5 * * * ?

  ....
ReferentialCreateSymblolicAccessionRegisterJob.java
####################################################

Ce job permet de déclencher une commande qui va calculer le registre des fonds symbolique et les ajoute dans les bases de données  uniquement sur le site primaire:

* Périodicité d'exécution par défaut : 0 50 0 * * ?

  ....
RuleManagementAuditJob.java
################################

Ce job permet de faire la validation de la cohérence des règles de gestion entre les offres de stockage et les bases de données uniquement sur le site primaire:

* Périodicité d'exécution par défaut : 0 40 * * * ?