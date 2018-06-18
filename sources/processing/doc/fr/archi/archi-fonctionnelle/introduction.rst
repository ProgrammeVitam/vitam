Introduction
############

But de cette documentation
**************************

L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.


Processing
**********

Mot-clé
 - workflow : une processus de traitement des opérations
 - paramètre d'exécution : en ensemble des données founi précisé les paramètres d'exécution 

Le module processing de VITAM fournit des services qui permet réaliser une chaine des opérations quand il y a une requête depuis le côté client via le service ingest. 
Il va procéder via plusieurs étapes qui correspondent à des modules de traitement suivant :

- process management
- process engine
- process distributor
- process worker

