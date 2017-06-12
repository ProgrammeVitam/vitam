Requêtes DSL
#############

Introduction
============

La partie "Requêtes DSL" permet de tester ses propres requêtes DSL sur un tenant, un contrat d'accès, une collection et une action donnés afin d'en analyser les résultats.

Elle est accessible depuis l'IHM de recette, par le menu Menu > Requête DSL


Page Requête DSL
=================

Le tenant utilisé est celui sur lequel la requête est effectuée.

En plus du tenant, la page contient 3 filtres et un champ texte :

  * Contrat : permet de sélectionner un contrat d'accès à appliquer
  * Collection : permet de sélectionner une collection définie
  * Action : permet de sélectioner un type d'action (pour l'instant seule la recherche est disponible)
  * Identifiant (champ texte) (optionel) : l'identifiant d'un SIP

A ces filtres, vient s'ajouter un champ permettant d'entrer une requête DSL au format JSON.

.. image:: images/RECETTE_requetesdsl_ecran_principal.png

Deux boutons sont situés en bas de page :

  * Vérifier le JSON : permet de vérifier la validité du code entré
  * Envoyer requête : permet d'envoyer la requête et d'obtenir un résultat

  .. image:: images/RECETTE_requetesdsl_boutons.png

  En cas de fichier JSON invalide, un message s'affiche.

  Une fois la requête effectuée, un nouveau champ affiche les résultats.

  .. image:: images/RECETTE_requetesdsl_ecran_reponse.png
