Notes et procédures spécifiques V5
##################################

.. caution:: Pour une montée de version depuis la R16 de Vitam, veuillez appliquer les procédures spécifiques de la V5RC en complément des procédures suivantes. Pour une montée de version depuis la V5RC, vous pouvez appliquer la procédure suivante directement.

Migration des unités archivistiques
-----------------------------------

.. caution:: Cette migration doit être effectuée avant la montée de version V5. Une fois appliquée, il ne faut pas redémarrer Vitam avant d'avoir terminé la montée de version.

Cette migration de données consiste à :

- Supprimer le champ ``us_sp`` et rendre inactive l'indexation des champs dynamiques créés au niveau des régles de gestion héritées au niveau de la propriété ``endDates``.

- Ajouter les champs ``_acd`` (date de création approximative) et ``_aud`` (date de modification approximative) dans la collection Unit.

Elle est réalisée en exécutant la commande suivante (sur le site primaire uniquement, dans le cas d'une installation multi-sites) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5.yml --ask-vault-pass

Après le passage du script de migration, il faut procéder à la réindexation de toutes les unités archivistiques :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags unit --ask-vault-pass

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

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_accession_register_details_v5.yml --vault-password-file vault_pass.txt``

En cas d'installation multi-sites, il faut obligatoirement lancer cette migration sur le site 1 et sur les autres sites si les reconstructions ont été faites correctement et la collection ``logbook`` est synchrone par rapport à celle du site 1.

Mise à jour des certificats
---------------------------

Cette migration de données consiste à mettre à jour le champ ``ExpirationDate`` pour les anciens certificats existants dans la base de donnée.

Elle est réalisée en exécutant la commande suivante (sur tous les sites, dans le cas d'une installation multi-sites) :

.. code-block:: bash

  ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5_certificate.yml --ask-vault-pass

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

Ce module a pour but de faciliter l'intégration d'archives dans Vitam via une API constructive de SIP.

Le module de `collect` nécessite la configuration et l'ajout d'une
- autre instance de metadata appelée `metadata-collect`
- autre instance de workspace appelée `workspace-collect`

