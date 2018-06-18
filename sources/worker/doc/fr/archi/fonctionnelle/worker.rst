Worker
******

Ce module traite une étape précise dans l'ensemble des opérations de workflow.
Pour chaque étape, une liste d'actions est lancée via des ActionHandler.

Le worker offre une API Rest permettant (via un client spécifique) de lancer les différentes méthodes désirées 
(pour l'instant submitStep est la seule méthode disponible).
