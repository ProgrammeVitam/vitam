Procédure de mise à niveau
##########################


Cette section décrit globalement le processus de mise à niveau d'une solution VITAM déjà en place et ne peut se substituer aux recommandations effectuées dans la "release note" associée à la fourniture des composants mis à niveau.

La mise à jour peut actuellement être effectuée comme une "première installation".

Modification de comportement de VITAM
======================================

Cas d'une modification du nombre de tenants
--------------------------------------------

Modifier dans le fichier d'inventaire  la directive ``vitam_tenant_ids``

Exemple :

.. code-block:: text

	vitam_tenant_ids=[0,1,2]

A l'issue, il faut lancer le playbook de déploiement de VITAM (et, si déployé, les extra) avec l'option supplémentaire ``--tags update_vitam_configuration``.

Exemple:

.. code-block:: bash

	ansible-playbook -i environments/hosts.deployment ansible-vitam/vitam.yml --ask-vault-pass --tags update_vitam_configuration
	ansible-playbook -i environments/hosts.deployment ansible-vitam-extra/extra.yml --ask-vault-pass --tags update_vitam_configuration


Cas d'une modification des paramètres JVM
--------------------------------------------

Se référer à :ref:`update_jvm`

Pour les partitions sur lesquelles une modification des paramètres JVM est nécessaire, il faut modifier les "hostvars" associées.

A l'issue, il faut lancer le playbook de déploiement de VITAM (et, si déployé, les extra) avec l'option supplémentaire ``--tags update_jvmoptions_vitam``.

Exemple:

.. code-block:: bash

	ansible-playbook -i environments/hosts.deployment ansible-vitam/vitam.yml --ask-vault-pass --tags update_jvmoptions_vitam
	ansible-playbook -i environments/hosts.deployment ansible-vitam-extra/extra.yml --ask-vault-pass --tags update_jvmoptions_vitam

.. caution:: Limitation technique à ce jour ; il n'est pas possible de définir des variables JVM différentes pour des composants colocalisés sur une même partition.
