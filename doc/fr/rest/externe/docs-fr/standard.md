
Ici sont présentées des conventions dans le cadre des API Vitam.

# Modèle REST

Les URL sont découpées de la façon suivantes :
- protocole: https
- FQDN : exemple api.vitam.fr avec éventuellement un port spécifique
- Base : <nom du service>/<version>
- Resource : le nom d'une collection
- L'URL peut contenir d'autres éléments (item, sous-collections)

Exemple: https://api.vitam.fr/access-external/v1/units/id

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

## Entêtes HTTPs des APIs

L'appel aux APIs REST nécessite le passage de certains entêtes dans la requête :
- **X-Application-Id** : pour conserver la session (valeur non signifiante) dans les journaux et logs du SAE associés à l'opération demandée
- **X-Tenant-Id** : pour chaque requête, le tenant sur lequel doit être exécutée la requête
- **X-Access-Contract-Id** : pour les APIs nécessitant un contrat d'accès.
- **X-Personal-Certificate** : pour les API sensibles nécessitant une authentification personae.
- **X-Qualifier** et **X-Version** : pour une requête GET sur un **Object** pour récupérer un usage et une version particulière
- **X-Http-Method-Override** : pour permettre aux clients HTTP ne supportant pas tous les modes de la RFC 7231 (GET/PUT/DELETE avec Body) de faire des appels POST en passant le verb HTTP cible (GET/PUT/DELETE) dans l'entête.

La réponse des APIs REST contient dans les headers de réponse :
- **X-Request-Id** : pour chaque requête, un identifiant unique de corrélation est fourni en réponse.

## Modèle asynchrone

Dans le cas d'une opération asynchrone, une API d'interrogation de type "pooling" est disponible.
Le client peut requêter de manière répétée une URI de vérification du statut, et ce de manière raisonnée (pas trop souvent).

Le principe est le suivant :
- Création de l'opération à effectuer
  - Exemple: POST /ingests et retourne 202 + X-Request-Id noté id
- Pooling sur l'opération demandée
  - Exemple: GET /operations/id et retourne 202 + X-Request-Id tant que non terminé
  - Intervalle recommandé : pas moins que la minute
- Fin de pooling sur l'opération demandée
  - Exemple : GET /operations/id et retourne 200 + le résultat

## Authentification

Vitam authentifie l'application Front-Office qui se connecte à ses API via le certificat client de la connexion TLS.
Chaque certificat enregistré dans le référentiel Vitam est rattaché à un Contexte.
Vitam s'assure que l'application Front-Office ait bien l'habilitation nécessaire pour effectuer la requête exprimée.

## Authentification Personae

Certaines APIs dites "sensibles" nécessitent une authentification renforcée de l'utilisateur. L'utilisateur souhaitant avoir accès à ces API doit s'authentifier de manière forte avec un certificat X509 auprès du SIA. Ce certificat doit être passé par SIA lors de l'appel aux APIs sensisbles dans un dédié header "**X-Personal-Certificate**".

## Identifiant de corrélation

Vitam étant un service REST, il est "State Less". Il ne dispose donc pas de notion de session en propre.
Cependant chaque requête retourne un identifiant de requête "**X-Request-Id**" qui est traçé dans les logs et journaux du SAE et permet donc de faire une corrélation avec les événements de l'application Front-Office cliente si celle-ci enregistre elle-aussi cet identifiant.

Considérant que cela peut rendre difficile le suivi d'une session utilisateur connecté sur un Front-Office, il est proposé que l'application Front-Office puisse passer en paramètre dans le Header l'argument "**X-Application-Id**" correspondant à un identifiant de session de l'utilisateur connecté. Cet identifiant DOIT être non signifiant car il sera lui aussi dans les logs et les journaux de Vitam. Il est inclus dans chaque réponse de Vitam si celui-ci est exprimé dans la requête correspondante.
Grâce à cet identifiant externe de session, il est alors plus facile de retracer l'activité d'un utilisateur grâce d'une part au regroupement de l'ensemble des actions dans Vitam au travers de cet identifiant, et d'autre part grâce aux logs de l'application Front-Office utilisant ce même identifiant de session.

## Pagination

Vitam ne dispose pas de notion de session en raison de son implémentation « State Less ».
Néanmoins, il est possible de paginer les résultats en utilisant le DSL avec les arguments suivants dans la requête : (pour GET uniquement)
- **$limit** : le nombre maximum d'items retournés (limité à 10000 par défaut)
- **$offset** : la position de démarrage dans la liste retournée (positionné à 0 par défaut)

A noter qu'en raison du modèle State-less, les requêtes suivantes (en manipulant notamment $offset) seront à nouveau exécutées, conduisant à des performances réduites.


