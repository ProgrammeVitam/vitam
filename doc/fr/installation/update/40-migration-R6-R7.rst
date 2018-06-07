Migration R6 vers R7
####################

Playbook pré-installation
=========================

Le composant ``vitam-consul`` a été monté de version ; le script suivant a pour but de mettre en conformité les fichiers de configuration de ce service afin qu'ils soient compatibles avec la nouvelle version.

Pour jouer le(s) playbook(s) (VITAM et/ou extra), il faut rajouter à la commande de déploiement la directive : ``--tags consul_conf``.

Exemple :

``ansible-playbook ansible-vitam/vitam.yml -i environments/<ficher d'inventaire> --vault-password-file vault_pass.txt --tags consul_conf``

``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<ficher d'inventaire> --ask-vault-pass --tags consul_conf``

A l'issue du passage de ce `playbook`, s'assurer que l'état des services Consul est OK.

Si tel est le cas, la pré-migration a été effectuée avec succès ; vous pouvez alors procéder au déploiement classique.

A l'issue, appliquer la procédure de migration ; se référer à :ref:`upgrade_r6_r7`.
