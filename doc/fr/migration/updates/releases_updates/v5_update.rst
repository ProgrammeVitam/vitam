Notes et procédures spécifiques V5
##################################

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


Migrations des unités achivestiques
-----------------------------------

La migration des données est réalisée en exécutant la commande suivante (sur le site primaire uniquement, dans le cas d'une installation multi-sites) :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_v5.yml --ask-vault-pass``

L'indexation des champs dynamiques, créés au niveau des régles de gestion héritées, et précisément au niveau de la propriété ``endDates`` est rendue inactive. Il faudrait ainsi réindexer toutes les unités archivitiques.

.. note:: Durant la migration, il est fortement recommandé de ne pas procéder à des versements de données.


