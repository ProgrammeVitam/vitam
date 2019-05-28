Notes et procédures spécifiques R9
##################################

.. caution:: Rappel : la montée de version vers la release R9 s'effectue depuis la release R7 (:term:`LTS` V1) ou la release R8 (V1, *deprecated*) et doit être réalisée en s'appuyant sur les dernières versions bugfixes publiées. 

Prérequis à la montée de version
================================

Gestion de la rétro-compatibilité des données des offres
---------------------------------------------------------

En préalable à l'installation, et uniquement dans le cas d'une montée de version (ne concerne pas le cas d'une installation R9 *from scratch*), il est nécessaire d'éditer le fichier d'inventaire ansible sur le modèle du fichier ``deployment/environments/hosts.example`` afin de décommenter la ligne ci-dessous : 

.. code-block:: yaml

    # On offer, value is the prefix for all containers' names. If upgrading from R8, you MUST UNCOMMENT this parameter AS IS !!!
    vitam_prefix_offer=""

Cela est dû à la mise en place, à partir de la version R9, d'un prefixe au niveau des noms de conteneurs de *tenants* logiques :term:`VITAM` sur les offres de stockage. Dans le cas d'une montée de version, cette étape préalable à l'installation permettra de garantir la rétro-compatibilité des données entre les versions précédentes et la version R9 (et supérieures). 

Arrêt des *timers* systemd
--------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les *timers* systemd ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé de ne lancer la procédure de migration qu'après s'être assuré que plus aucun `workflow` n'est en cours de traitement. 

Reprise des données de certificats
----------------------------------

La version R9 apporte une nouvelle fonctionnalité permettant la révocation des certificats :term:`SIA` et *Personae*, afin d'empecher des accès non autorisés aux :term:`API` :term:`VITAM` (vérification dans la couche https des :term:`CRL`). Cette fonctionnalité impose d'effectuer une reprise des données des certificats (base MongoDB identity, collections ``Certificate`` et ``PersonalCertificate``). 

Les commandes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook ansible-vitam-exploitation/migration_r7_certificates.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook ansible-vitam-exploitation/migration_r7_certificates.yml --ask-vault-pass``

Montée de version MongoDB 3.4 vers 4.0
--------------------------------------

La montée de version R7 vers R9 comprend une montée de version de la bases de données MongoDB de la version 3.4 à la version 4.0. 

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

* Arrêt de :term:`VITAM` (`playbook` ``ansible-vitam-exploitation/stop_vitam.yml``)

.. warning:: A partir de là, la solution logicielle :term:`VITAM` est arrêtée ; elle ne sera redémarrée qu'au déploiement de la nouvelle version.

* Démarrage des différents cluster mongodb (playbook ``ansible-vitam-exploitation/start_mongodb.yml``)
* Upgrade de mongodb en version 3.6 (`playbook` ``ansible-vitam-exploitation/migration_mongodb_36.yml``)
* Upgrade de mongodb en version 4.0 (`playbook` ``ansible-vitam-exploitation/migration_mongodb_40.yml``)

Montée de version
=================

La montée de version vers la release R9 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`. 

Etapes de migration 
===================

Dans le cadre d'une montée de version R7 vers R9, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de réinstallation de la solution logicielle :term:`VITAM`. 

.. caution:: Dans le cas particulier d'une installation multi-sites, il sera nécessaire de d'abord lancer la migration des données sur le site secondaire afin de purger les registres des fonds, puis de lancer la migration sur le site primaire, et enfin de lancer la reconstruction des registres des fonds sur le site secondaire. 

Procédure de migration des données
----------------------------------

Lancer les commandes ci-après dans l'ordre suivant :

  1. D'abord sur le site secondaire pour purger les registres des fonds
  2. Ensuite sur le site primaire pour la migration des registres des fonds.

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r7_r8.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r7_r8.yml --ask-vault-pass``

.. warning:: Selon la volumétrie des données précédement chargées, le `playbook` peut durer jusqu'à plusieurs heures.

.. note:: Durant la migration, il est fortement recommandé de ne pas procéder à des versements de données. En effet, le `playbook` se charge d'arrêter les composants "ingest-external" et "access-external" avant de réaliser les opérations de migration de données, puis de redémarrer les composants "ingest-external" et "access-external". 

Les opérations de migration réalisées portent, entre autres, sur les éléments suivants :

    - Les registres des fonds (Accession Registers)
        - Diff AccessionRegisterDetail:
            - Suppression du champs ``Identifier``, remplacé par ``Opc`` (Opération courante)
            - Suppression du champs ``OperationGroup``, remplacé par ``Opi`` (Opération d'ingest)
            - Suppression du champs ``Symbolic``
            - Suppression des champs ``attached``, ``detached``, ``symbolicRemained`` des sous objets (``TotalUnits``, ``TotalObjectGroups``, ``TotalObjects``, ``ObjectSize``)
            - Ajout d'un sous objet ``Events``

        - Diff AccessionRegisterSummary:
            - Suppression des champs ``attached``, ``detached``, ``symbolicRemained`` des sous objets (``TotalUnits``, ``TotalObjectGroups``, ``TotalObjects``, ``ObjectSize``)

    - Le journal des opérations
        - Seules seront disponibles les données du registre des fonds selon le nouveau modèle dans le ``evDetData`` du journal de l'opération d'`ingest`.

.. note:: Se reporter à la documentation du nouveau modèle de données de la release R9.

Procédure de réindexation des registres de fonds 
-------------------------------------------------

Sous ``deployment``, exécuter la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --vault-password-file vault_pass.txt --tags accessionregisterdetail``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass --tags accessionregisterdetail``

Les changement apportés touchent le mapping Elasticsearch de la collection ``AccessionRegisterDetail``. 

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de verifier que la procedure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être realisée manuellement.

Procédure de réindexation des ObjectGroup 
-----------------------------------------

Sous ``deployment``, exécuter la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r7_r9.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r7_r9.yml --ask-vault-pass``

Les changement apportés touchent le mapping Elasticsearch sur l'attribut ``qualifier.version`` de la collection ``ObjectGroup`` (passé en nested)

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de verifier que la procedure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être realisée manuellement.

Après la migration
------------------

Exécuter la commande suivante afin de réactiver les timers systemd sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --ask-vault-pass``

A l’issue de l’exécution du `playbook`, les timers systemd ont été redémarrés. 

Une fois le site secondaire `up`
--------------------------------

Sur le site secondaire, vérifier sur les machines hébergeant le composant ``functional-administration`` que le processus de reconstruction des registres des fonds a bien démarré.

La commande à exécuter (en tant que root) est la suivante :

``systemctl status vitam-functional-administration-accession-register-reconstruction.service``

Vérification de la bonne migration des données
----------------------------------------------

A l'issue de la migration, il est fortement conseillé de lancer un "Audit de cohérence" sur les différents tenants. 