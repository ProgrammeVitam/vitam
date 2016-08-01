La procédure de validation est commune.

Validation manuelle
----------------------

Chaque service VITAM (en dehors de bases de données) expose une URL de statut présente à l'adresse suivante : ``<protocole web https ou https>://<host>:<port>/<composant>/v1/status``

Cette URL doit retourner une réponse HTTP 200 (sans body) sur une requête HTTP GET.

Validation via SoapUI
-----------------------

.. TODO:: penser à ajouter la partie liée à SoapUI. Définition du formalisme.

Validation via IHM technique
------------------------------

.. TODO:: pour le moment, cette IHM n'existe pas. Penser aux copies écran quand...