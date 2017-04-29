Conventions REST Générales
##########################

Ici sont présentées des conventions dans le cadre des API Vitam.

Modèle REST
===========

Les URL sont découpées de la façon suivantes :
- protocole: https
- FQDN : exemple api.vitam.fr avec éventuellement un port spécifique
- Base : <nom du service>/<version>
- Resource : le nom d'une collection
- L'URL peut contenir d'autres éléments (item, sous-collections)

Exemple: https://api.vitam.fr/access/v1/units/id

Les méthodes utilisées :
- GET: pour l'équivalent de "Select" (possibilité d'utiliser POST avec X-Http-Method-Override: GET dans le Header)
- POST: pour l'équivalent de "Insert"
- PUT: pour l'équivalent de "Update"
- DELETE: pour l'équivalent de "Delete" (possibilité d'utiliser POST avec X-Http-Method-Override: DELETE dans le Header)
- HEAD: pour l'équivalent du "Test d'existence"
- OPTIONS: pour l'équivalent de "Lister les commandes disponibles"

Les codes retours HTTP standards utilisés sont :
- 200: Sur des opérations de GET, PUT, DELETE, HEAD, OPTIONS
- 201: Sur l'opération POST (sans X-Http-Method-Override)
- 202: Pour les réponses asynchrones
- 204: Pour des réponses sans contenu (type HEAD sans options)
- 206: Pour des réponses partielles ou des réponses en mode Warning (OK mais avec une alerte)

Les codes d'erreurs HTTP standards utilisés sont :
- 400: Requête mal formulée
- 401: Requête non autorisée
- 404: Resource non trouvée
- 409: Requête en conflit
- 412: Des préconditions ne sont pas respectées
- 413: La requête dépasse les capacités du service
- 415: Le Media Type demandé n'est pas supporté
- 500: si une erreur interne est survenue dans le back-office (peut être un bug ou un effet de bord d'une mauvaise commande)
- 501: Le service n'est pas implémenté

Modèle asynchrone
=================

Dans le cas d'une opération asynchrone, deux options sont possibles :

Mode Pooling
------------

Dans le mode pooling, le client est responsable de requêter de manière répétée l'URI de vérification du statut, et ce de manière raisonnée (pas trop souvent).

Le principe est le suivant :
- Création de l'opération à effectuer
  - Exemple: POST /ingests et retourne 202 + X-Request-Id noté id
- Pooling sur l'opération demandée
  - Exemple: GET /operations/id et retourne 202 + X-Request-Id tant que non terminé
  - Intervalle recommandé : pas moins que la minute
- Fin de pooling sur l'opération demandée
  - Exemple : GET /operations/id et retourne 200 + le résultat

Mode Callback **UNSUPPORTED**
-----------------------------

Dans le mode Callback, le client soumet une création d'opération et simultanément propose une URI de Callback sur laquelle Vitam rappellera le client pour lui indiquer que cette opération est terminée.

Le principe est le suivant :
- Création de l'opération à effectuer avec l'URI de Callback
  - Exemple: POST /ingests + dans le Header X-Callback: https://uri?id={id}&status={status} et retourne 202 + #id + Header X-Callback confirmé
- A la fin de l'opération, Vitam rappelle le client avec l'URI de Callback
  - Exemple: GET /uri?id=idop&status=OK
- Le client rappelle alors Vitam pour obtenir l'information
  - Exemple: GET /ingests/#id et retourne 200 + le résultat

Perspectives d'évolution
------------------------

Dans le cas où l'accès au résulat final ne doit pas se faire sur l'URI /resources/id, il faudra ajouter une réponse 303.

Authentification
================

L'authentification dans Vitam authentifie l'application Front-Office qui se connecte à ses API. Cette authentification s'effectue en trois temps :
- Un premier composant authentifie la nouvelle connexion
  - La première implémentation sera basée sur une authentification du certificat client dans la connexion TLS
- Le premier composant passe au service REST la variable Header "X-Identity" contenant l'identifiant de l'application Front-Office.
  - Comme cette identification est actuellement interne, ce Header est actuellement non généré.
- Le service REST, sur la base de cette authentification, s'assure que l'application Front-Office ait bien l'habilitation nécessaire pour effectuer la requête exprimée.


Identifiant de corrélation
==========================

Vitam étant un service REST, il est "State Less". Il ne dispose donc pas de notion de session en propre.
Cependant chaque requête retourne un identifiant de requête "**X-Request-Id**" qui est traçé dans les logs et journaux du SAE et permet donc de faire une corrélation avec les événements de l'application Front-Office cliente si celle-ci enregistre elle-aussi cet identifiant.

**UNSUPPORTED** Considérant que cela peut rendre difficile le suivi d'une session utilisateur connecté sur un Front-Office, il est proposé que l'application Front-Office puisse passer en paramètre dans le Header l'argument "**X-Application-Id**" correspondant à un identifiant de session de l'utilisateur connecté. Cet identifiant DOIT être non signifiant car il sera lui aussi dans les logs et les journaux de Vitam. Il est inclus dans chaque réponse de Vitam si celui-ci est exprimé dans la requête correspondante.
Grâce à cet identifiant externe de session, il est alors plus facile de retracer l'activité d'un utilisateur grâce d'une part au regroupement de l'ensemble des actions dans Vitam au travers de cet identifiant, et d'autre part grâce aux logs de l'application Front-Office utilisant ce même identifiant de session.

Afin de gérer plusieurs tenants, il est imposé (pour le moment) que l'application Front-Office puisse passer en paramètre
dans le Header l'argument **X-Tenant-Id** correspondant au tenant sur lequel se baser pour exécuter la requête.

Pagination
==========

Vitam ne dispose pas de notion de session en raison de son implémentation « State Less ». Néanmoins, pour des raisons d'optimisations sur des requêtes où le nombre de résultats serait important, il est proposé une option tendant à améliorer les performances : X-Cursor et X-Cursor-Id.

Méthode standard
----------------

De manière standard, il est possible de paginer les résultats en utilisant le DSL avec les arguments suivants dans la requête : (pour GET uniquement)
- **$limit** : le nombre maximum d'items retournés (limité à 1000 par défaut, maximum à 100000)
- **$per_page** : le nombre maximum des premiers items retournés (limité à 100 par défaut, maximum à 100) (**UNSUPPORTED**)
- **$offset** : la position de démarrage dans la liste retournée (positionné à 0 par défaut, maximum à 100000)

En raison du principe State-less, les requêtes suivantes (en manipulant notamment $offset) seront à nouveau exécutées, conduisant à des performances réduites.

Méthode optimisée **UNSUPPORTED**
---------------------------------

Afin d'optimiser, il est proposé d'ajouter de manière optionnelle dans le Header lors de la première requête le champs suivant : **X-Cursor: true**
Si la requête nécessite une pagination (plus d'une page de réponses possible), le SAE répondra alors la première page (dans le Body) et dans le Header :
- **X-Cursor-Id**: id (identifiant du curseur)
- **X-Cursor-Timeout**: datetime (date limite de validité du curseur)

Le client peut alors demander les pages suivantes en envoyant simplement une requête GET avec un Body vide et dans le Header : **X-Cursor-Id**: id.
