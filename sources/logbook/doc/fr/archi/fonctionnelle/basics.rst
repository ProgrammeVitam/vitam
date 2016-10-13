Généralités
***********
Le rôle du journal d'opération est de conserver une trace des oprétations réalisées au sein du système lors de traitements
sur des lots d'archives.

Chaque opération est tracée sous la forme de 2 enregistrements (début et fin).

Évènements tracés par exemple :

* Démarrage de Ingest avec affectation d'un eventIdentifierProcess = GUID (OperationId) (création)

  * A partir d'ici tous seront en mode **update**
  
* Stockage du lot d'archives dans l'espace de travail
* Démarrage d'un workflow
* Démarrage d'une étape de workflow
* Fin d'une étape de workflow
* Fin d'un workflow
* Fin du Stockage du lot
* Fin de Ingest

// TODO compléter la liste
