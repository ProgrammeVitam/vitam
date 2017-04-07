IHM Filter for X-Request-ID
###########################

Description 
===========
En cas d'erreur technique, depuis le IHM demo, nous pouvons trouver le X-Request-ID affiche dans un popup. 
Le code d'erreur a une valeur 500 renvoyé par les APIs externes. 

Côté serveur 
============
Le filtre RequestIdContainerFilter (package fr.gouv.vitam.common.server.*) est ajouté dans l'application serveur 
IHM demo pour envoyer X-Request-ID dans le VitamSession en cas d'erreur. (fr.gouv.vitam.common.server.RequestIdHeaderHelper 
est mis à jour pour traiter des X-Request-ID en cas d'erreur)

Côté IHM Front 
==============
On ajoute aussi un intercepteur filter pour récupérer le X-Request-ID dans le cas d'erreur dans la session.
On utilise d'un intercepteur angular sur $httpProvider.