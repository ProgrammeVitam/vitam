Notes et procédures spécifiques V5
##################################

Contournement d'un problème induit par la montée de version logstash
--------------------------------------------------------------------

Suite à la montée de version du composant logstash, il est constaté un problème durant la phase de montée de version (exécution du script vitam.yml)  dans certains contextes d'usage.
Un correctif est en cours d'élaboration, mais pour sécuriser le processus, il est recommandé de réaliser les opérations suivantes manuellement sur le serveur logtstash AVANT de mettre en oeuvre le processus de montée de version:


.. code-block:: bash

    sudo mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-patterns-core-4.3.1/patterns
    sudo chown -R logstash:logstash /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-patterns-core-4.3.1/


Migrations offres Swift V2 & V3 en cas de présence d'objets très volumineux (4Go+)
----------------------------------------------------------------------------------

Si vous disposez d'une instance R16.3 ou inférieur (4.0.3-), vers une version V5.rc ou supérieur, et que vous utilisez des offres Swift V2/V3 (providers openstack-swift-v2 et/ou openstack-swift-v3), il est nécessaire de procéder à une migration des données :

.. code-block:: bash

    $ ansible-playbook ansible-vitam-exploitation/migration_swift_v2_and_v3.yml -i environments/hosts.{env} --ask-vault-pass

    # Confirm playbook execution
    # Enter swift offer id (ex offer-swift-1)
    # Select migration mode
    # > Enter '0' for analysis only mode : This mode will only log anomalies (in offer technical logs), no update will be proceeded
    # > Enter '1' to fix inconsistencies : This mode will update swift objects to fix inconsistencies. However, this does not prune objects (delete partially written or eliminated objects segments to free space).
    # > Enter '2' to fix inconsistencies and purge all deleted objects segments to free storage space.
    # Reconfirm playbook execution

Il est recommandé de lancé la procédure en mode 0 (analyse seule) et de vérifier les erreurs de cohérence dans les logs
Seules les offres Swift V2/V3 avec des objets volumineux (>= 4Go) nécessitent migration. Un exemple d'incohérences journalisées dans les logs (/vitam/log/offers) est donnée ici : ::

    INCONSISTENCY FOUND : Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq has old segment names [aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/2, aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/1]. Run migration script with fix inconsistencies mode to prune container.
    INCONSISTENCY FOUND : Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq has missing metadata. Run migration script with fix inconsistencies mode enabled to set object metadata.

Si la détection des anomalies est terminée en succès, et que des anomalies sont trouvées, il est recommandé de lancer le mode 1 (correction des anomalies). Les migrations de données sont également journalisées dans les logs (/vitam/log/offers) : ::

    Renaming segment env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/2 to env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/00000002
    Renaming segment env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/1 to env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/00000001
    Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq migrated successfully. Digest: 8959ea1290aa064a3c64d332f31e049bd4f9d4e95bebe0b46d38613bb079761d52c865dce64c88fd7e02313d340f9a2f8c0c6b5dbf8909a3cbda071d26ce21d4

Si des problèmes de cohérence de type "Orphan large object segments" persistent ::

    INCONSISTENCY FOUND : Orphan large object segments [...] without parent object manifest: env_2_object/aeaaaaaaaagbcaacaamboal2tk7dzmiaaaaq. Eliminated object? Incomplete write? Run migration script with delete mode to prune container.

Dans ce cas, il est recommandé de vérifier préalablement que les objets concernés n'existent pas sur les autres offres (mêmes container & objectName).
Si les objets n'existent pas dans les autres offres, il s'agit alors de reliquats d'objets non complètement éliminés. Le lancement du mode 2 (correction des anomalies + purge des objets) est à réaliser.
Dans le cas contraire (cas où l'objet existe dans les autres offres), il faudra envisager la "Procédure de resynchronisation ciblée d’une offre" décrite dans la Documentation d’EXploitation (DEX) de Vitam pour synchroniser l'offre Swift pour les éléments concernés.

.. note:: Cette procédure doit être lancée une seule fois, et pour chaque offre Swift V2/V3, APRES upgrade Vitam.


Migration des unités archivistiques
-----------------------------------

Cette migration de données consiste à :

   -Supprimer le champ ``us_sp`` et rendre inactive l'indexation des champs dynamiques, créés au niveau des régles de gestion héritées au niveau de la propriété ``endDates``.

   -Ajouter les champs ``_fuzzyCD`` (date de création approximative) et ``_fuzzyUD`` (date de modification approximative) dans la collection Unit.


ELle est réalisée en exécutant la commande suivante (sur le site primaire uniquement, dans le cas d'une installation multi-sites) :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5.yml --ask-vault-pass``

Après le passage du script de migration, il faut procéder à la réindexation de toutes les unités archivistiques :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass``

Cette migration devrait être faite avant la montée de version V5.

.. note:: Durant la migration, il est fortement recommandé de ne pas procéder à des versements de données.

Augmenter la précision sur le nombre de résultats retournés dépassant 10000
---------------------------------------------------------------------------

Suite à une évolution d'ElasticSearch ( à partir de la version 7.6 ), le nombre maximum de résultats retournés est limité à 10000. Ceci est dû à la haute consommation de ressources de ce calcul, qui est parfois inutile au niveau de la réponse.
En cas de besoin pour avoir le nombre exact de résultats retournés, il faut, en premier temps, activer le paramètre nommé ``authorizeTrackTotalHits`` qui existe au niveau du fichier de configuration ``access-external.conf``.
Ci-dessous, un exemple du fichier en question :

.. literalinclude:: ../../../../../deployment/ansible-vitam/roles/vitam/templates/access-external/access-external.conf.j2
   :language: yaml

Ensuite, si l'API de recherche utilise le type d'entrée de DSL "SELECT_MULTIPLE", il faut ajouter ``$track_total_hits : true`` au niveau de la partie "filter" de la requête d'entrée.
Ci-dessous, un exemple de requête d'entrée :

.. code-block:: json

    {
      "$roots": [],
      "$query": [
       {
         "$match": {
            "Title": "héritage"
         }
       }
      ],
      "$filter": {
        "$offset": 0,
        "$limit": 100,
        "$track_total_hits": true
      },
      "$projection": {}
    }

Migration des registres de fonds en détails
-------------------------------------------

Suite à l'ajout des nouvelles propriétés ``Comment`` ( Commentaire ) et ``obIdInd`` (Identifiant du message ) au niveau de la collection ``AccessionRegisterDetail``,
il faut lancer une migration sur les anciennes données à travers la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_accession_register_details_v5.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_accession_register_details_v5.yml --ask-vault-pass``

En cas d'installation multi-sites, il faut obligatoirement lancer cette migration sur le site 1 et sur les autres sites si les reconstructions ont été faites correctement
et la collection ``logbook`` est synchrone par rapport à celle du site 1.

Cette migration devrait être faite après la montée de version V5.


Mise à jour des certificats
-----------------------------------

Cette migration de données consiste à mettre à jour le champ ``ExpirationDate`` pour les anciens certificats existants dans la base de donnée.

Elle est réalisée en exécutant la commande suivante (sur tous les sites, dans le cas d'une installation multi-sites) :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5_certificate.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5_certificate.yml --ask-vault-pass``

