Validation de la procédure
##########################

.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments-rpm``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam-rpm``

La procédure de validation est commune aux différentes méthodes d'installation.

Validation par ansible
=======================

Pour tester le déploiement de VITAM, il faut se placer dans le répertoire |repertoire_deploiement| et entrer la commande suivante :

``ansible-playbook`` |repertoire_playbook ansible| ``/vitam.yml -i`` |repertoire_inventory| ``/<ficher d'inventaire> --check``

.. note:: A l'issue du passage du playbook, les étapes doivent toutes passer en vert.

Validation manuelle
===================

Chaque service VITAM (en dehors de bases de données) expose des URL de statut présente à l'adresse suivante : 
``<protocole web https ou https>://<host>:<port>/<composant>/v1/status``
Cette URL doit retourner une réponse HTTP 200 (sans body) sur une requête HTTP GET.

``<protocole web https ou https>://<host>:<port>/admin/v1/status`` => renvoie un statut HTTP 204 si OK

Validation via Consul
======================

Consul possède une :term:`IHM` pour afficher l'état des services VITAM et supervise le "/admin/v1/status" de chaque composant VITAM, ainsi que des check TCP sur les bases de données.

Pour se connecter à Consul : ``http//<Nom du 1er host dans le groupe ansible hosts-consul-server>:8500/ui``

Pour chaque service, la couleur à gauche du composant doit être verte (correspondant à un statut OK).

Si une autre couleur apparaît, cliquer sur le service "KO" et vérifier le test qui ne fonctionne pas.

Validation via SoapUI
=====================

.. TODO:: penser à ajouter la partie liée à SoapUI. Définition du formalisme.

.. Validation via IHM technique
.. ============================

.. .. TODO:: pour le moment, cette IHM n'existe pas. Penser aux copies écran quand...

Post-installation : administration fonctionnelle
================================================

A l'issue de l'installation, puis la validation, un **administrateur fonctionnel** doit s'assurer que :

- le référentiel PRONOM ( `lien vers pronom <http://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm>`_  ) est correctement importé depuis "Import du référentiel des formats" et correspond à celui employé dans Siegfried
- le fichier "rules" a été correctement importé via le menu "Import du référentiel des règles de gestion"
- à terme, le registre des fonds a été correctement importé

Les chargements sont effectués depuis l':term:`IHM` demo.