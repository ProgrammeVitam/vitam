
*L'API d'Entrées* propose les points d'entrées et les méthodes pour récupérer les informations des **Entrées**.

L'entrée se fait via une API compatible strictement SEDA **ArchiveTransfer** :
- le body transmis contient un ZIP contenant lui-même :
  - un fichier manifest.xml qui est le fichier SEDA répondant à la description d'un **ArchiveTransfer**
  - un répertoire "content" contenant les objets numériques d'archives
- la réponse, une fois le traitement terminé, est un fichier XML répondant à la description d'un **ArchiveTransferReply**

# Ingests

**Ingests** est le point de lancement des opérations d'entrées.

Le mode opératoire est le suivant :
- Le client lance une opération d'entrée en faisant un POST sur la collection *Ingests*. Une réponse 202 (Accepted) est retournée avec un identifiant de la collection **Operations**.
- Le client lance une requête sur *l'API d'administration fonctionnelle* afin de récupérer le statut de l'opération d'Ingest. Le client peut réinterroger le statut de l'opération d'ingest, et ce de manière raisonnée.
- Le client peut récupérer un ou plusieurs rapports une fois l'opération terminée en faisant un GET sur la collection *Ingests* (archivetransferreply ou manifests) en précisant l'identifiant de l'opération.

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

## Rapport final d'une entrée

Deux rapports sont disponibles une fois l'opération terminée :
- **Accusé de Réception (ArchiveTransferReply)** au format XML via GET /ingests/{id}/archivetransferreply
- **Bordereau de versement (fichier SEDA d'origine)** via GET /ingests/{id}/manifests
