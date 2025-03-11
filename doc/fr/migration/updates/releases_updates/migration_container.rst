Migration d'un vitam legacy vers un vitam conteneurisé
######################################################

Adaptation des sources de déploiement ansible
=============================================

.. caution:: Les composants ``ingest-external``, ``worker`` (en cas d'utilisation des griffons) et ``library`` ne sont actuellement pas compatible avec le mode de déploiement conteneurisé. La variable ``legacy_components_list`` permet de déployer n'importe quel composant en mode legacy.

Il faut éditer le contenu du fichier ``environments/group_vars/all/main/repositories.yml``. Pour cela il faut rajouter les paramètres présentés dans l'exemple:

.. code-block:: yaml

    install_mode: container

    legacy_components_list: [ "ingest-external", "worker", "library" ]

    container_repository:
      registry_url: https://docker.programmevitam.fr/
      username: ''
      password: ''

    vitam_container_version: <vitam_version>
..

.. warning:: Dans le cas d'utilisation d'une registry interne il vous faudra effectuer une synchronisation à partir de la registry docker du programme Vitam: https://docker.programmevitam.fr

Procédures à exécuter AVANT la migration
========================================

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la migration vers le mode conteneurisé.

Vitam doit être arrêté sur **tous les sites** (site primaire en premier):

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Application de la migration
===========================

.. caution:: L'application de la migration s'effectue d'abord sur les sites secondaires puis sur le site primaire.
.. caution:: Il faut s'assurer que la variable ``install_mode: container`` est bien configurée.

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_legacy_packages.yml --ask-vault-pass

..

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

Procédures à exécuter APRÈS la migration
========================================

N/A
