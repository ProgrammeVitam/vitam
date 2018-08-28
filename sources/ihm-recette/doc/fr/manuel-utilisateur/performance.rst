Test de performance avec IHM-recette
####################################

Cette documentation décrit le lancement de tests de performance
---------------------------------------------------------------

Une api existe dans IHM-recette pour lancer des tests de performance

Lancer un test de performance
*****************************

Pour lancer un test de performance, il faut appeler l'URL suivante en POST : ``/ihm-recette/v1/api/performance``.

Cette URL prend en paramètre un fichier JSON avec 3 attributs :
 - le nom du fichier SIP
 - le nombre d'ingest total demandé
 - le nombre d'ingest en paramètre

Exemple en ligne de commande avec `httpie`::

    http --session admin https://int.env.programmevitam.fr/ihm-recette/v1/api/performances/ secureLogbookFileName=sip.zip parallelIngest=1 numberOfIngest=1

Il n'est pas possible de lancer plusieurs tests de performance en parallèle. Pour savoir si un test est fini ou non, il est possible d'intérroger l'API avec un HEAD::

    http HEAD --session admin https://int.env.programmevitam.fr/ihm-recette/v1/api/performances/


Récupération du rapport
***********************

Une fois le test effectué, il est possible de récupérer le rapport en utilisant les points d'API suivants :

 - GET /ihm-recette/v1/api/performance/reports : liste les rapports de tests
 - GET /ihm-recette/v1/api/performance/reports/{secureLogbookFileName} : télécharge un rapport de test

Le rapport est un fichier au format CSV permettant, en se basant sur le journal des opérations, de donner les temps de traitements par étapes du workflow.

IHM des tests de performances
*****************************

Une IHM est également présente pour lancer les tests de performance et récupérer les différents rapports.
