Notes et procédures spécifiques V6
##################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: R16 -> V5RC -> V5 -> V6RC.

Adaptation des sources de déploiement ansible
=============================================

Mise à jour de l'architecture du module de collecte
---------------------------------------------------

* Ajouter les composants suivants à votre fichier d'inventaire
  * ``[hosts_collect_external]`` dans la ``[zone_access:children]``
  * ``[hosts_collect_internal]`` dans la ``[zone_applicative:children]``

* Modifier le fichier ``environments/group_vars/all/main/vault-keystores.yml``

  .. code-block:: diff

   keystores:
     server:
  -    collect: changeit6kQ16eyDYAoPS9fy
  +    collect_external: changeit6kQ16eyDYAoPS9fy
     client_external:
  -    collect: changeitz6xZe5gDu7nhDZA12
  +    collect_external: changeitz6xZe5gDu7nhDZA12

  ..

* Création de certificats dédiés au module de collecte

  * Supprimer, si ils existent, les certificats de l'ancien module de collecte ``rm -rf environments/certs/client-external/clients/collect/ environments/certs/server/hosts/*/collect.{crt,key}``.

  * Créer un certificat client et un certificat serveur dédiés au module de collecte à l'aide de votre ``PKI`` et le mettre dans les chemins attendus (``environments/certs/client-external/clients/collect-external/`` et ``environments/certs/server/hosts/{hosts}``).

  * Dans le cas de l'utilisation de la PKI de test de Vitam, vous pouvez simplement re-générer de nouveaux certificats à l'aide de la commande: ``./pki/scripts/generate_certs.sh <fichier_inventaire>``

  * Re-générer les stores: ``./generate_stores.sh``

  * Ajouter le contexte de sécurité pour le module de collecte dans le fichier ``environments/group_vars/all/advanced/vitam_security.yml``:

    .. code-block:: diff

       admin_context_certs:
    -    - "collect/collect.crt"
    +    - "collect-external/collect-external.crt"

    ..

* Ne pas oublier les paramètres de configuration associés aux jvms de ces nouveaux composants dans le fichier ``environments/group_vars/all/main/jvm_opts.yml``

  .. code-block:: yaml
      collect_internal:
          jvm_opts:
              # memory: "-Xms512m -Xmx512m"
              # gc: ""
              # java: ""
      collect_external:
          jvm_opts:
              # memory: "-Xms512m -Xmx512m"
              # gc: ""
              # java: ""
  ..

Modification de l'indexation par défaut dans elasticsearch des indexes de collecte
----------------------------------------------------------------------------------

.. caution:: Attention, ce changement d'indexation vous fera perdre les données en cours dans le module de collecte. Il est conseillé de terminer et de purger les transactions en cours avant de procéder à la montée de version. Si malgré tout, vous souhaitez conserver l'indexation actuelle, il vous faudra supprimer les lignes de la variable ``collect_grouped_tenants``.

Initialement, il n'était pas possible de définir une configuration spécifique lié à l'indexation des unit & objectgroup pour le module de collecte.

Ainsi, la mécanique de personnalisation des indexes Vitam a été mise en oeuvre pour les indexes du module de collecte. Par défaut, la configuration ainsi proposée regroupe l'ensemble des tenants dans un indexe unique pour chacun des index unit & objectgroup.

Le module de collecte a pour vocation de sas tampon de transfert, il n'est donc pas nécessaire d'allouer un shard par tenant.

La configuration par défaut permet de limiter l'empreinte mémoire et l'utilisation du cluster elasticsearch-data. En fonction de votre besoin, vous pouvez rajouter des shards ou bien découper sur des indexes dédiés certains tenants de Vitam.

Dans le fichier de configuration suivant: ``environments/group_vars/all/main/main.yml``

.. code-block:: yaml

  vitam_elasticsearch_tenant_indexation:
    default_config:
      # Default settings for collect_unit indexes
      collect_unit:
        number_of_shards: 1
        number_of_replicas: 2
      # Default settings for collect_objectgroup indexes
      collect_objectgroup:
        number_of_shards: 1
        number_of_replicas: 2

    collect_grouped_tenants:
    - name: 'all'
      # Group all tenants for collect's indexes (collect_unit & collect_objectgroup)
      tenants: "{{ vitam_tenant_ids | join(',') }}"

..

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V6

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass

    # Si Version < V6RC:
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

    # Si Version >= V6RC:
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..

Nettoyage des anciens fichiers du module de collecte suite au changement d'architecture
---------------------------------------------------------------------------------------

.. caution:: Cette étape doit être effectuée AVANT la montée de version V6 de vitam et seulement si la V6RC ou V5 a été déployée avec le module de collecte.

.. caution:: Attention, cette procédure va supprimer l'ensemble des éléments stockés dans la partie externe du module de collecte. Veuillez vous assurer que les transactions en cours sont bien purgées avant de procéder à la montée de version.

Exécutez le playbook suivant à partir de l'ansiblerie de la V6 **sur le site primaire** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_collect.yml --ask-vault-pass

..

Ce playbook supprime les anciens éléments suite aux modifications de l'architecture du module de collecte sur les machines ``[hosts_collect]``.

Après exécution de ce playbook, vous pouvez supprimer de votre inventaire le groupe ``[hosts_collect]``.

Montée de version mineure de mongo 5.0.13 -> 5.0.14
---------------------------------------------------

.. caution:: Cette montée de version doit être effectuée AVANT la montée de version V6 de Vitam et après l'arrêt des Timers et des externals.

.. caution:: Cette opération doit être effectuée après avoir mis à jour les dépôts en V6.

Exécutez le playbook suivant à partir de l'ansiblerie de la V6 **sur tous les sites** :

.. code-block:: bash

    # Mise à jour mongo
    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_50.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V6

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Application de la montée de version
===================================

.. caution:: L'application de la montée de version s'effectue d'abord sur les sites secondaires puis sur le site primaire.

.. caution:: Sous Debian, si vous appliquez la montée de version depuis la V6.RC, vous devrez rajouter le paramètre ``-e force_vitam_version=6.0`` aux commandes suivantes. Sinon les packages vitam ne seront pas correctement mis à jour. En effet, Debian considère que 6.rc.X > 6.X.

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
