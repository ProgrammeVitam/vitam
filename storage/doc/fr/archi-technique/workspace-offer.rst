Workspace Offer
##############

Présentation
************

|  *Parent package:* **fr.gouv.vitam.storage.offers**
|  *Package proposition:* **fr.gouv.vitam.storage.offers.workspace**

Module embarquant l'offre de stockage Vitam utilisant une partie du workspace.

L'offre de stockage workspace est séparé en deux parties :

- le serveur de l'offre de stockage workspace
- l'implémentation du driver associé à l'offre de stockage workspace


Driver
******

TODO

Serveur
*******

Description
===========

Les fonctionnalités sont :

- récupérer la capacité et disponibilité de l'offre
- envoyer un objet en mode chunk
- récupérer un objet
- tester l'existence d'un objet
- récupérer l'empreinte d'un objet

REST
====

Description
-----------

L'API REST, trois header spécifiques sont définis :

- X-Tenant-Id : l'identifiant du Tenant

- X-Command : utilisée pour l'envoi d'un objet par fragments (chunk)

  - INIT : création de l'objet donc l'offre de stockage, sans données envoyés
  - WRITE : envoi d'un fragment de data
  - END : indique que l'objet est fini d'être créé

- X-Type : permets de préciser le résultat attendu pour la recupération de l'objet

  - DATA : l'objet en lui-même (valeur par défaut)
  - DIGEST : empreinte de l'objet

Les réponses en erreur définies par l'API Vitam sont respéctées (400, 401, 404, etc)

REST API
--------

**GET /objects**

- description : recupération des informations de l'offre

- response :

  - code : 200
  - contenu : information sur l'offre (capacité, disponibilité, ...)


**GET /objects/{id}**

- description : recupération sur l'offre d'un objet ou de son empreinte

- headers :

  - X-Type: DATA / DIGEST
  - X-Tenant-Id: id du tenant

- path :

  - {id} : id de l'objet

- response :

  - code : 200
  - contenu : data ou empreinte de l'objet


**POST /objects**

- description : création d'un nouvel objet vide sur l'offre

- headers :

  - X-Command: INIT
  - X-Tenant-Id: id du tenant

- body :

  - GUID
  - taille (taille finale)
  - type (unit/objectgroup/logbook/etc)
  - digest-type (type de digest)

- response :

  - code : 201
  - contenu : identifiant de l'objet créé


**PUT /objects/{id}**

- description : écriture  et finalisation d'objet de l'offre

- headers :

  - X-Command: WRITE / END
  - X-Tenant-Id: id du tenant

- path :

  - {id} : id de l'objet

- body :

  - flux : data ou digest

- response :

  - code : 200


**HEAD /objects/{id}**

- description : existance de l'objet sur l'offre

- headers :

  - X-Tenant-Id: id du tenant

- path :

  - {id} : id de l'objet

- response :

  - code : 204


**DELETE /objects/{id}**

- description : suppression d'un objet de l'offre

- headers :

  - X-Tenant-Id: id du tenant

- path :

  - {id} : id de l'objet

- response :

  - code : 200
  - contenu : l'id de l'objet supprimé


**GET /status**

- description : état du serveur

- reponse :

  - code : 200
  - contenu : statut


Intégration du workspace
========================

Le module core du Workspace est utilisé pour gérer le stockage des objets.

L'arborescence cible est la suivante :

- pour chaque tenant un dossier est créé dans le workspace (équivalent du container) : /idTenant
- pour chaque type d'objet, un sous-dossier est créé dans le dossier tenant (units, logbooks, objectgroups, objects) : /idTenant/objectgroups

Chaque objet, selon son type et le tenant associé, est stocké dans le dossier adéquat.
Si le dossier cible n'existe pas à l'écriture de l'objet, il est créé automatiquement.
