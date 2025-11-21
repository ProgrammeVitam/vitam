Notes et procédures spécifiques V9.1
####################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: V7.1 -> V8.0 -> V8.1 -> V9.0 -> V9.1

Adaptation des sources de déploiement ansible
=============================================

Configuration du nombre de shards/replicas pour chaque indices elasticsearch-log
--------------------------------------------------------------------------------

Ces modifications impliquent des changements de la configuration Ansible associée aux indices pour plus de cohérence avec les autres variables de configuration.

Ainsi, les clés de configurations suivantes ont étés retirées:

* ``kibana.log.metrics.replica``
* ``kibana.log.metrics.shards``

Elles ont étés remplacées par ces variables spécifiques pour chacun de vos indices:

.. code-block:: yaml

  elasticsearch:
    log:
      index_templates:
        default: # Default configuration if others index_templates are undefined
          number_of_shards: 1
          number_of_replicas: 1
        vitam: # Configuration for indexes logstash-vitam-*
          number_of_shards: 1
          number_of_replicas: 1
        access: # Configuration for indexes logstash-access-*
          number_of_shards: 1
          number_of_replicas: 1

..

* Si aucune valeur pour les indices ``vitam`` ou ``access`` n'est spécifiée, c'est la configuration ``default`` qui sera appliquée.
* Si la configuration ``default`` n'est pas spécifiée, les valeurs par défaut sont ``shards: 1`` et ``replicas: 1``.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.1

.. caution:: Cette opération doit être effectuée avec les sources de déploiement de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Pour le dépôt vitam-external, vous devez renseigner la version adaptée à votre système d'exploitation (par exemple pour la version 9.1.0):

* AlmaLinux 9: https://download.programmevitam.fr/vitam_repository/9.1.0/rpm/vitam-external/9/
* Debian 12: https://download.programmevitam.fr/vitam_repository/9.1.0/deb/vitam-external/12/

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..


Application de la montée de version
===================================

.. caution:: L'application de la montée de version s'effectue d'abord sur les sites secondaires puis sur le site primaire.

Lancement du master playbook vitam
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam/vitam.yml --ask-vault-pass

..

Lancement du master playbook extra
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/extra.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

N/A
