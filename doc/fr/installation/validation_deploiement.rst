Validation de la procédure
##########################

La procédure de validation est commune aux différentes méthodes d'installation.

Validation manuelle
===================

Chaque service VITAM (en dehors de bases de données) expose une URL de statut présente à l'adresse suivante : ``<protocole web https ou https>://<host>:<port>/<composant>/v1/status``

Cette URL doit retourner une réponse HTTP 200 (sans body) sur une requête HTTP GET.

Validation via Consul
======================

Consul possède une :term:`IHM` pour afficher l'état des services VITAM.

Pour chaque service, la couleur à gauche du composant doit être verte (correspondant à un statut OK).

Si une autre couleur apparaît, cliquer sur le service "KO" et vérifier quel test ne fonctionne pas.

Validation via SoapUI
=====================

.. TODO:: penser à ajouter la partie liée à SoapUI. Définition du formalisme.

Validation via IHM technique
============================

.. TODO:: pour le moment, cette IHM n'existe pas. Penser aux copies écran quand...