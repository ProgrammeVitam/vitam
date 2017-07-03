Architecture fonctionnelle
##########################

.. KWA FIXME : dans la suite, éviter de mélanger (ou plutôt bien définir) les notions de traitemtn et d'exécution.

.. figure:: images/vitam-functional-architecture.png
	:align: center

	Architecture fonctionnelle cible de VITAM

VITAM est constitué de différents composants liés aux fonctionnalités attendues :

* API externes : exposition des API REST (aux front-offices, aux applications tierces)
* Moteur d’exécution : gestion de toutes les tâches massives/asynchrones . Exemples de moteurs :

   - Workflow de transformation : sert à la transformation des documents dans des formats pérennes (versement) ou pour résister à l’obsolescence des formats stockés (préservation)
   - Workflow d’audit

* Moteur de stockage : stockage pérenne des données (méta-données et objets numériques)
* Moteur de données : stockage accessible et requêtable des méta-données
* Journalisation fonctionnelle : traçabilité fonctionnelle (dont à valeur probante)
* IHM d’administration : interface d’administration technique et fonctionnelle

Pour l’exploitabilité de la solution , on peut rajouter les composants suivants : :

* Moteur de déploiement et de configuration
* Composants d’assistances/hook à l’exploitabilité (sauvegarde, supervision, ordonnancement)
* Journalisation technique : concentration des logs techniques
