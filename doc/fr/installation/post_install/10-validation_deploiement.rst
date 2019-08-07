Validation du déploiement
#########################

.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

La procédure de validation est commune aux différentes méthodes d'installation.

Sécurisation du fichier ``vault_pass.txt``
==========================================

Le fichier ``vault_pass.txt`` est très sensible ; il contient le mot de passe du fichier ``environments/group_vars/all/vault.yml`` qui contient les divers mots de passe de la plate-forme. A l'issue de l'installation, il est primordial de le sécuriser (suppression du fichier ou application d'un ``chmod 400``).

.. Validation par ansible
.. =======================

.. Pour tester le déploiement de VITAM, il faut se placer dans le répertoire |repertoire_deploiement| et entrer la commande suivante :

.. ``ansible-playbook`` |repertoire_playbook ansible| ``/vitam.yml -i`` |repertoire_inventory| ``/<ficher d'inventaire> --ask-vault-pass --check``

.. .. note:: A l'issue du passage du playbook, les étapes doivent toutes passer en vert.

Validation manuelle
===================  

Chaque service :term:`VITAM` (en dehors de bases de données) expose des URL de statut à l'adresse suivante : ``<protocole web http ou https>://<host>:<port>/admin/v1/status``
Cette URL doit retourner une réponse HTTP 204 sur une requête HTTP GET, si OK.

Un playbook d'appel de l'intégralité des autotests est également inclus (``deployment/ansible-vitam-exploitation/status_vitam.yml``). Il est à lancer de la même manière que pour l'installation de :term:`VITAM` (en renommant le playbook à exécuter).

Il est également possible de vérifier la version installée de chaque composant par l'URL :

``<protocole web http ou https>://<host>:<port>/admin/v1/version``

Validation via Consul
======================

Consul possède une :term:`IHM` pour afficher l'état des services :term:`VITAM` et supervise le "/admin/v1/status" de chaque composant :term:`VITAM`, ainsi que des check TCP sur les bases de données.

Pour se connecter à Consul : ``http//<Nom du 1er host dans le groupe ansible hosts-consul-server>:8500/ui``

Pour chaque service, la couleur à gauche du composant doit être verte (correspondant à un statut OK).

Si une autre couleur apparaît, cliquer sur le service "KO" et vérifier le test qui ne fonctionne pas.


Post-installation : administration fonctionnelle
================================================

A l'issue de l'installation, puis la validation, un **administrateur fonctionnel** doit s'assurer que :

- le référentiel PRONOM ( `lien vers pronom <http://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm>`_  ) est correctement importé depuis "Import du référentiel des formats" et correspond à celui employé dans Siegfried
- le fichier "rules" a été correctement importé via le menu "Import du référentiel des règles de gestion"
- à terme, le registre des fonds a été correctement importé

Les chargements sont effectués depuis l':term:`IHM` demo.
