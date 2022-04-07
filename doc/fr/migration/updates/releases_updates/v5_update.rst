Notes et procédures spécifiques V5
##################################

.. caution:: Pour une montée de version depuis la R16 de Vitam, veuillez appliquer les procédures spécifiques de la V5RC en complément des procédures suivantes. Pour une montée de version depuis la V5RC, vous pouvez appliquer la procédure suivante directement.

Migration des unités archivistiques
-----------------------------------

.. caution:: Cette migration doit être effectuée après la montée de version V5 mais avant la réouverture du service aux utilisateurs.

Cette migration de données consiste à :

- Ajouter les champs ``_acd`` (date de création approximative) et ``_aud`` (date de modification approximative) dans la collection Unit.

Elle est réalisée en exécutant la commande suivante (sur le site primaire uniquement, dans le cas d'une installation multi-sites) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v5.yml --ask-vault-pass

Après le passage du script de migration, il faut procéder à la réindexation de toutes les unités archivistiques :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags unit --ask-vault-pass

Puis redémarrer les externals qui ont été coupés durant la migration :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass

Contournement d'un problème induit par la montée de version logstash
--------------------------------------------------------------------

Suite à la montée de version du composant logstash, il est constaté un problème durant la phase de montée de version (exécution du script vitam.yml)  dans certains contextes d'usage.
Un correctif est en cours d'élaboration, mais pour sécuriser le processus, il est recommandé de réaliser les opérations suivantes manuellement sur le serveur logtstash AVANT de mettre en oeuvre le processus de montée de version:


.. code-block:: bash

    sudo mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-patterns-core-4.3.1/patterns
    sudo chown -R logstash:logstash /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-patterns-core-4.3.1/

Migration des registres de fonds en détails
-------------------------------------------

Cette migration doit être effectuée après la montée de version V5.

Suite à l'ajout des nouvelles propriétés ``Comment`` ( Commentaire ) et ``obIdInd`` (Identifiant du message ) au niveau de la collection ``AccessionRegisterDetail``, il faut lancer une migration sur les anciennes données à travers la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_accession_register_details_v5.yml --vault-password-file vault_pass.txt``

En cas d'installation multi-sites, il faut obligatoirement lancer cette migration sur le site 1 et sur les autres sites si les reconstructions ont été faites correctement et la collection ``logbook`` est synchrone par rapport à celle du site 1.

Mise à jour des certificats
---------------------------

Cette migration de données consiste à mettre à jour le champ ``ExpirationDate`` pour les anciens certificats existants dans la base de donnée.

Elle est réalisée en exécutant la commande suivante (sur tous les sites, dans le cas d'une installation multi-sites) :

.. code-block:: bash

  ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v5_certificate.yml --ask-vault-pass

..

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

Ajout d'un nouveau module VITAM : Module de collecte
----------------------------------------------------

.. caution:: À préparer dans les sources de déploiement AVANT le déploiement de la V5. Ce module est optionnel, si vous ne souhaitez pas l'activer, vous pouvez conserver vos sources de déploiement et ne pas appliquer la procédure suivante.

Ce module a pour but de faciliter l'intégration d'archives dans Vitam via une API constructive de SIP.

Le module de `collect` nécessite la configuration et l'ajout d'une
- autre instance de metadata appelée `metadata-collect`
- autre instance de workspace appelée `workspace-collect`

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

- Utilisation d'un certificat dédié au module de collecte:

- Ajouter le contexte de sécurité pour le module de collecte dans le fichier ``environments/group_vars/all/vitam_security.yml``:

  .. code-block:: yaml

    admin_context_certs:
      - "collect/collect.crt"

  ..

- Regénérer les certificats pour créer ceux du module de collect: ``./pki/scripts/generate_certs.sh <fichier_inventaire>``

- Regénérer les stores: ``./generate_stores.sh``

