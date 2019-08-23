Notes et procédures spécifiques R12
###################################

.. caution:: Rappel : la montée de version vers la *release* R11 s’effectue depuis la *release* R9 (LTS V2) ou la *release* R10 (V2, *deprecated*) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 

Prérequis à la montée de version
================================

Montée de version MongoDB 4.0 vers 4.2
--------------------------------------

La montée de version R9 (ou R10 ou R11) vers R12 comprend une montée de version de la bases de données MongoDB de la version 4.0 à la version 4.2. 

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

* Arrêt de :term:`VITAM` (`playbook` ``ansible-vitam-exploitation/stop_vitam.yml``)

.. warning:: A partir de là, la solution logicielle :term:`VITAM` est arrêtée ; elle ne sera redémarrée qu'au déploiement de la nouvelle version.

* Démarrage des différents cluster mongodb (playbook ``ansible-vitam-exploitation/start_mongodb.yml``)
* Upgrade de mongodb en version 4.2 (`playbook` ``ansible-vitam-exploitation/migration_mongodb_42.yml``)