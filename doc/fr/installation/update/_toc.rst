Procédures de mise à jour de la configuration
#############################################


Cette section décrit globalement les processus de reconfiguration d'une solution logicielle :term:`VITAM` déjà en place et ne peut se substituer aux recommandations effectuées dans la "release-notes" associée à la fourniture des composants mis à niveau.

Se référer également aux :term:`DEX` pour plus de procédures.


Cas d'une modification du nombre de tenants
===========================================

Modifier dans le fichier d'inventaire la directive ``vitam_tenant_ids``, et dans toutes les directives concernées (ex. ``api_output_index_tenants``, ``rules_index_tenants``, ``vitam_removed_tenants``, ``dedicated_tenants``, ``grouped_tenants``...)

Exemple :

.. code-block:: text

	vitam_tenant_ids=[0,1,2]

A l'issue, il faut lancer le playbook de déploiement de :term:`VITAM` (et, si déployé, les extras) avec l'option supplémentaire ``--tags update_vitam_configuration``.

Exemple:

.. code-block:: console

      ansible-playbook ansible-vitam/vitam.yml -i environments/hosts.<environnement> --ask-vault-pass --tags update_vitam_configuration
      ansible-playbook ansible-vitam-extra/extra.yml -i environments/hosts.<environnement> --ask-vault-pass --tags update_vitam_configuration

.. note:: Si une offre froide est configurée, la liste des buckets configurés doit être mise à jour en conséquence.

Cas d'une modification des paramètres :term:`JVM`
=================================================

Se référer à :ref:`update_jvm`

Pour les partitions sur lesquelles une modification des paramètres :term:`JVM` est nécessaire, il faut modifier les "hostvars" associées.

A l'issue, il faut lancer le playbook de déploiement de :term:`VITAM` (et, si déployé, les *extras*) avec l'option supplémentaire ``--tags update_jvmoptions_vitam``.

Exemple:

.. code-block:: console

      ansible-playbook ansible-vitam/vitam.yml -i environments/hosts.<environnement> --ask-vault-pass --tags update_jvmoptions_vitam
      ansible-playbook ansible-vitam-extra/extra.yml -i environments/hosts.<environnement> --ask-vault-pass --tags update_jvmoptions_vitam

.. caution:: Limitation technique à ce jour ; il n'est pas possible de définir des variables :term:`JVM` différentes pour des composants colocalisés sur une même partition.

Cas de la mise à jour des *griffins*
========================================

Modifier la directive ``vitam_griffins`` contenue dans le fichier ``environments/group_vars/all/main/main.yml``.

.. note:: Dans le cas d'une montée de version des composant *griffins*, ne pas oublier de mettre à jour l'URL du dépôt de binaire associé.

Relancer le script de déploiement en ajoutant en fin de ligne ``--tags griffins`` pour ne procéder qu'à l'installation/mise à jour des *griffins*.
