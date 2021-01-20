
*L'API de Journalisation* propose les points d'entrées et les méthodes pour requêter et récupérer les informations des **Journaux**.


# API Logbook externe

Dans le projet Vitam, Les API externes supportent le POST-X-HTTP-OVERRIDE=GET. Les API internes ne supportent que le GET.

# Operations

**operations** est le point d'entrée pour tous les journaux des opérations réalisées dans Vitam (Journal du Service d'Archivage Électronique).

Le rôle du journal d'opération est de conserver une trace des opérations réalisées au sein du système lors de traitements sur des lots d'archives.

Chaque étape au sein d'une opération est tracée.

Évènements tracés par exemple :

* Démarrage de l'Ingest avec affectation d'un eventIdentifierProcess (OperationId)
* Fin d'une étape de workflow
* Fin d'un workflow
* Fin de l'Ingest

# UnitLifeCycles

**logbookunitlifecycles** est le point d'entrée pour tous les journaux de cycle de vie des _Units_ dans Vitam.
Le rôle des journaux de cycles de vie des _Units_ est de conserver l'ensemble des événements associés à une unité archivistique.
Les événements associés sont du type :
- création
- indexation des métadonnées
- modification de métadonnées
- élimination
- audit
- gel
- ...

**Important** : l'identifiant d'un Unit lifecycle est également l'identifiant du Unit correspondant dans le service Access.
Ainsi il est possible une fois une opération terminée de demander la liste des Units qui sont concernés par cette opération en demandant la liste des UnitLifeCycles de cette opération. Les identifiants remontés sont alors les mêmes pour accéder au journal du cycle de vie d'une Unit (*/unitlifecycles/{id}*) ou à sa description et ses métadonnées de gestion (*/units/{id}*).   

# ObjectLifeCycles

**logbookobjectslifecycles** est le point d'entrée pour tous les journaux de cycle de vie des objets dans Vitam.
Le rôle des journaux de cycles de vie des objets est de conserver l'ensemble des événements associés à un Objet.
Les événements associés sont du type :
- création
- check de conformité (empreinte, format, taille)
- transformation de format
- élimination
- audit
- gel
- ...

**Important** : l'identifiant d'un Object lifecycle est également l'identifiant de l'Objet correspondant dans le service Access.
Ainsi il est possible une fois une opération terminée de demander la liste des Objects qui sont concernés par cette opération en demandant la liste des ObjectLifeCycles de cette opération. Les identifiants remontés sont alors les mêmes pour accéder au journal du cycle de vie d'un Object (*/logbookobjectslifecycles/{id}*) ou à sa description technique (*/objects/{id}*).   
