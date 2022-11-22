Notes et procédures spécifiques V5
##################################

Adaptation des sources de déploiement ansible
=============================================

Classement des offres dans une stratégie
----------------------------------------

Dans une stratégie de stockage, chaque offre renseignée déclare un ordre de lecture. Cet ordre est manifeste à travers la propriété ``rank``. Il est obligatoire
de la renseigner dans chaque offre utilisée. La lecture depuis les offres se fait selon un ordre ascendant en se basant sur cette propriété.
Ci-dessous un exemple de déclaration de stratégie de stockage et ses offres, dans le fichier de configuration ``deployment/environments/group_vars/all/offer_opts.yml`` :

    .. code-block:: yaml

        vitam_strategy:
        - name: offer-1
          referent: true
          rank: 10
        - name: offer-2
          referent: false
          rank: 20
        - name: offer-3
          referent: false
          rank: 30

        vitam_offers:
            offer-1:
                provider: filesystem
            offer-2:
                provider: filesystem
            offer-3:
                provider: filesystem

    ..

Ajout d'un nouveau module VITAM : Module de collecte
----------------------------------------------------

.. caution:: À préparer dans les sources de déploiement AVANT le déploiement de la V5. Ce module est optionnel, si vous ne souhaitez pas l'activer, laissez les groupes de hosts vides dans le fichier d'inventaire.

Ce module a pour but de faciliter l'intégration d'archives dans Vitam via une API constructive de SIP.

Le module de `collect` nécessite la configuration et l'ajout :
- d'une autre instance de metadata appelée `metadata-collect`
- d'une autre instance de workspace appelée `workspace-collect`

Pour la mise en oeuvre de cette nouvelle application, veuillez éditer les paramètres suivants:

- Ajout des groupes de hosts du module de collect à votre fichier d'inventaire (cf. fichier d'inventaire d'exemple: ``environments/hosts.example``).

  .. code-block:: ini

    [zone_applicative:children]
    hosts_collect
    hosts_metadata_collect
    hosts_workspace_collect

    [hosts_collect]
    # TODO: Put here servers where this service will be deployed : collect


    [hosts_metadata_collect]
    # TODO: Put here servers where this service will be deployed : metadata_collect


    [hosts_workspace_collect]
    # TODO: Put the server where this service will be deployed : workspace_collect
    # WARNING: put only one server for this service, not more !

  ..

- Ajout des bases mongo pour le module de collect dans le fichier ``environments/group_vars/all/vault-vitam.yml``:

  .. caution:: Pensez à éditer les password avec des passwords sécurisés.

  .. code-block:: yaml

    mongodb:
      mongo-data:
        collect:
          user: collect
          password: change_it_m39XvRQWixyDX566
        metadataCollect:
          user: metadata-collect
          password: change_it_37b97KVaDV8YbCwt

  ..

- Création de certificats dédiés au module de collecte

  - Créer un certificat client et un certificat serveur dédiés au module de collecte à l'aide de votre ``PKI`` et le mettre dans les chemins attendus (``environments/certs/client-external/clients/collect/`` et ``environments/certs/server/hosts/{hosts}``).

    - Dans le cas de l'utilisation de la PKI de test de Vitam, vous pouvez simplement re-générer de nouveaux certificats à l'aide de la commande: ``./pki/scripts/generate_certs.sh <fichier_inventaire>``

  - Re-générer les stores: ``./generate_stores.sh``

  - Ajouter le contexte de sécurité pour le module de collecte dans le fichier ``environments/group_vars/all/vitam_security.yml``:

    .. code-block:: yaml

      admin_context_certs:
        - "collect/collect.crt"

    ..

Procédures à exécuter AVANT la montée de version
================================================

.. caution:: Pour une montée de version depuis la R16 de Vitam, veuillez appliquer les procédures spécifiques de la V5RC en complément des procédures suivantes. Pour une montée de version depuis la V5RC, vous pouvez appliquer la procédure suivante directement.

Réinitialisation de la reconstruction des registres de fond des sites secondaires
---------------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure) ou v5.rc.3- (v5.rc.3 ou inférieure). Elle permet la réinitialisation de la reconstruction des registre de fonds sur les sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que les timers de Vitam aient bien été préalablement arrêtés (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_accession_register_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure) ou v5.rc.3- (v5.rc.3 ou inférieure). Elle permet le contrôle et la purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Procédures à exécuter APRÈS la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

..

Migration des unités archivistiques
-----------------------------------

.. caution:: Cette migration doit être effectuée APRÈS la montée de version V5 mais avant la réouverture du service aux utilisateurs.

Cette migration de données consiste à ajouter les champs ``_acd`` (date de création approximative) et ``_aud`` (date de modification approximative) dans la collection Unit.

Exécutez les commandes suivantes sur **tous les sites** (primaire et secondaire(s)) :

- Migration des données mongo (le playbook va stopper les externals et les timers de Vitam avant de procéder à la migration)

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v5.yml --ask-vault-pass

- Réindexation de toutes les unités archivistiques sur elastic-search :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags unit --ask-vault-pass

..

Mise à jour des certificats
---------------------------

Cette migration de données consiste à mettre à jour le champ ``ExpirationDate`` pour les anciens certificats existants dans la base de donnée.

Elle est réalisée en exécutant les commandes suivantes sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

  ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v5_certificate.yml --ask-vault-pass

..

Migration des registres de fonds en détails
-------------------------------------------

.. caution:: Cette migration doit être effectuée APRÈS la montée de version V5 mais avant la réouverture du service aux utilisateurs.

Suite à l'ajout des nouvelles propriétés ``Comment`` ( Commentaire ) et ``obIdIn`` (Identifiant du message ) au niveau de la collection ``AccessionRegisterDetail``, il faut lancer une migration sur les anciennes données.

Exécutez la commande suivante uniquement sur **le site primaire** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_accession_register_details_v5.yml --vault-password-file vault_pass.txt

..

Recalcul du graph des métadonnées des sites secondaires
-------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure) ou v5.rc.3- (v5.rc.3 ou inférieure). Elle permet le recalcul du graphe des métadonnées sur les sites secondaires

La procédure est à réaliser sur tous les **sites secondaires** de Vitam APRÈS l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_metadata_graph_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Redémarrage des timers et des accès externes à Vitam
----------------------------------------------------

La montée de version est maintenant terminée, vous pouvez réactiver les services externals ainsi que les timers sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --ask-vault-pass
