Validation de la procédure
##########################

La procédure de validation est commune aux différentes méthodes d'installation.

Validation manuelle
===================

Chaque service VITAM (en dehors de bases de données) expose des URL de statut présente à l'adresse suivante : 
``<protocole web https ou https>://<host>:<port>/<composant>/v1/status``
Cette URL doit retourner une réponse HTTP 200 (sans body) sur une requête HTTP GET.

``<protocole web https ou https>://<host>:<port>/admin/v1/status`` => renvoie un statut HTTP 204 si OK

Validation via Consul
======================

Consul possède une :term:`IHM` pour afficher l'état des services VITAM et supervise le "/admin/v1/status" de chaque composant VITAM, ainsi que des check TCP sur les bases de données.

Pour chaque service, la couleur à gauche du composant doit être verte (correspondant à un statut OK).

Si une autre couleur apparaît, cliquer sur le service "KO" et vérifier quel test ne fonctionne pas.

Validation via SoapUI
=====================

.. TODO:: penser à ajouter la partie liée à SoapUI. Définition du formalisme.

Validation via IHM technique
============================

.. TODO:: pour le moment, cette IHM n'existe pas. Penser aux copies écran quand...

Post-installation : administration fonctionnelle
================================================

A l'issue de l'installation, puis la validation, un administrateur fonctionnel doit s'assurer que :

- le référentiel PRONOM ( `lien vers pronom <http://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm>`_  ) est correctement importé depuis "Import du référentiel des formats" et correspond à celui employé dans Siegfried
- le fichier "rules" a été correctemebt importé via le menu "Import du référentiel des règles de gestion"
- à terme, le registre des fonds a été correctement importé

Les chargement sont effectués depuis l':term:`IHM` demo.