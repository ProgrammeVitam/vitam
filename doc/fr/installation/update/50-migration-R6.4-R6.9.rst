Migration R6.4 vers R6.9
########################

Playbooks pré-installation
==========================

Cas des contextes applicatifs
-----------------------------

Le champ ``permission._tenant`` lié aux contextes applicatifs a été mis à jour en version 1.0.9 ("R6.9") et doit être migré avant le déploiement de la nouvelle version de la solution logicielle :term:`VITAM` (le champ permission._tenant doit être transformé en permission.tenant dans le cadre de la correction du bug #4317).

Sous ``deployment``, il faut lancer la commande :

``ansible-playbook ansible-vitam-exploitation/migration_r6.4_r6.9.yml --ask-vault-pass``

Si le playbook ne remonte pas d'erreur, la pré-migration des contextes applicatifs a été réalisée avec succès ; vous pouvez alors procéder au déploiement classique.