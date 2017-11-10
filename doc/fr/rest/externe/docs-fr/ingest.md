
*L'API d'Entrées* propose les points d'entrées et les méthodes pour récupérer les informations des **Entrées**.

Cette API contiendra à termes les possibilités de processus d'entrées en mode programmatique, c'est à dire en proposant les mêmes types d'accès à *Units* et *Objects*, mais selon les modes exclusifs de création et de mise à jour. La mise à jour est à ce stade non supportée (**UNSUPPORTED**) en mode Ingest.

- Il faut noter que les opérations sur des *Units* pré-existantes ne seront autorisées uniquement en mode mise à jour. Toutes les autres opérations s'appliquent uniquement aux *Units* et *Objects* nouvellement créés.

Actuellement, seule l'API compatible strictement SEDA **ArchiveTransfer** est proposée :
- le body transmis contient un ZIP contenant lui-même :
  - un fichier manifest.xml qui est le fichier SEDA répondant à la description d'un **ArchiveTransfer**
  - un répertoire "content" contenant les objets numériques d'archives
- la réponse, une fois le traitement terminé, est un fichier XML répondant à la description d'un **ArchiveTransferReply**

# Ingests

**Ingests** est le point de lancement des opérations d'entrées.

- L'opération d'entrée commence par un POST sur la collection *Ingests*, qui retourne une réponse 202 (Accepted) avec un identifiant de la collection **Operations** avec l'Identifiant fourni par *Ingests*.
- Le client pourra requêter ensuite (sur le point d'API Administration Management) de manière itérative cet item et obtiendra la réponse :
  - 202 si le traitement est toujours en cours
  - un code d'erreur si le traitement est en erreur

## Statut après soumission d'une entrée

La structuration d'un Statut est la suivante :
```json
{
  "#id": "idIngests",
  "httpCode" : 202,
  "code" : 123456,
  "context": "ingest",
  "state": "Running",
  "message": "The ingest is in progress",
  "description": "The application 'Xxxx' requested an ingest operation and this operation is in progress.",
  "start_date": "2014-01-10T03:06:17.396Z"
}
```
Actuellement le mode opératoire est le suivant :
- Le client lance une opération d'entrée en faisant un POST sur la collection *Ingests*. Une réponse 202 (Accepted) est retournée avec un identifiant de la collection **Operations**.
- Le client lance une requête sur *l'API d'administration fonctionnelle* afin de récupérer le statut de l'opération d'Ingest
- Le client peut récupérer un ou plusieurs rapports une fois l'opération terminée en faisant un GET sur la collection *Ingests* (archivetransferreply ou manifests)

## Rapport final d'une entrée

Le modèle de réponse est selon la demande (Accept) le message ArchiveTransferReply en mode XML ou JSON.
Actuellement, seul le mode XML est proposé.

Deux rapports sont disponibles une fois l'opération terminée :
- **Reports (ArchiveTransferReply)** via GET /ingests/id/archivetransferreply
- **Manifests (fichier SEDA d'origine)** via GET /ingests/id/manifests
