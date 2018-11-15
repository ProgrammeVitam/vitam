Upgrade Mongodb 3.4 vers 4.0
############################

Prérequis
#########

Avant d'upgrader la version de mongodb, il est nécessaire de mettre à disposition les packages mongodb-org 3.6.15 et 4.0.1 sur les dépots.

Il est aussi nécessaire de s'assurer que la version de Vitam qui sera utilisée au moment où Vitam sera démarré est compatible (R8 minimum).

Procédure
#########

* Stopper Vitam (playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
* Démarrer les différents cluster mongodb (playbook ``ansible-vitam-exploitation/start_mongodb.yml``)
* Upgrader mongodb en 3.6 (playbook ``ansible-vitam-exploitation/migration_mongodb_36.yml``)
* Upgrader mongodb en 4.0 (playbook ``ansible-vitam-exploitation/migration_mongodb_40.yml``)
* Démarrer Vitam (playbook ``ansible-vitam-exploitation/start_vitam.yml``)
