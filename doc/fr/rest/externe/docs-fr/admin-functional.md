*L'API d'administration fonctionnelle* propose les points d'entrées et les méthodes pour requêter et récupérer les informations des collections suivantes :

- Référentiel des Formats basé sur PRONOM (TNA)
- Référentiel des Règles de Gestion
- Référentiel des Contrats d'Entrées
- Référentiel des Contrats d'Accès
- Référentiel des Contextes
- Référentiel des Profiles de Sécurité
- Registre des Fonds
- Référentiel des Services Agents
- Référentiel des griffons
- Référentiel des scénarios de préservation
- Opérations

## Tenant d'administration

Certaines APIs dites "cross-tenants" nécessitent une vérification spécifique.
Ces opérations doivent être exécutées à partir d'un tenant dit tenant d'administration (configuré comme tel à l'intérieur de VITAM).
Il s'agit de s'assurer que pour les collections Formats, Contextes et Profils de sécurité, le tenant utilisé pour l'import soit conforme à celui configuré dans VITAM.
En cas de différence, une erreur 401 sera retournée.


# Référentiel des Formats

Ce référentiel est basé sur PRONOM (TNA) mais il peut être étendu. Il est trans-tenant.
- Il est possible de mettre à jour ce référentiel via les API.
- Notez cependant que la mise à jour des outils utilisant ce référentiel n'est pas encore opérationnelle. Il n'est donc pas recommandé de mettre à jour ce référentiel avec une autre version que celle livrée par Vitam.

# Référentiel des Règles de Gestion

Il est possible de mettre à jour ce référentiel via les API. Il est par tenant.

Actuellement ce référentiel est utilisé lors du processus d'entrée mais il n'est pas encore utilisé par les accès.

# Référentiel des Contrats d'Entrées

Il est possible de mettre à jour ce référentiel via les API. Il est par tenant.

Actuellement ce référentiel est utilisé lors du processus d'entrées.

# Référentiel des Contrats d'Accès

Il est possible de mettre à jour ce référentiel via les API. Il est par tenant.

Actuellement ce référentiel est utilisé lors des accès.

# Référentiel des Contextes

Il est possible de mettre à jour ce référentiel via les API. Il est par tenant.

Actuellement ce référentiel n'est pas utilisé lors du processus d'entrée ou des accès.

Il doit faire le lien entre l'authentification (TLS) et les droits et contrats de l'application externe partenaire.

# Référentiel des Profiles de Sécurité

Il est possible de mettre à jour ce référentiel via les API.

Actuellement ce référentiel pour le contrôle d'accès aux API.

# Référentiel des Services Agents

Actuellement ce référentiel est utilisé lors du processus d'entrées.

# Référentiel des griffons

Il est possible de mettre à jour ce référentiel via les API, sur le tenant d'administration.

Ce référentiel est disponible sur tous les tenants.

Actuellement ce référentiel est utilisé lors du processus de préservation

# Référentiel des scénarios de préservation

 Il est possible de mettre à jour ce référentiel via les API. Il est par tenant.

 Actuellement ce référentiel est utilisé lors du processus de préservation.

# Registre des Fonds

Ce référentiel est utilisé et mis à jour lors du processus d'entrée.

# Gestion des processus

Il est possible de gérer les processus en mode administrateur (CANCEL, PAUSE, NEXT, REPLAY, RESUME).

# Sécurisation des journaux - vérification

**traceability/checks** est le point d'entrée pour la vérification de la sécurisation des journaux d'opérations dans Vitam.

# Audit d'existence et d'intégrité

Il est possible de vérifier l'existence ou l'intégrité des objets binaires et physiques des groupes d'objets pour :
* un tenant
* un service producteur
* une requête DSL sur des unités archivistiques

Il y a 4 paramètres dans le body :
* **auditActions** (obligatoire): qui peut avoir comme valeur  *AUDIT_FILE_EXISTING*  ou *AUDIT_FILE_INTEGRITY* pour lancer respectivement l'audit d'existence ou l'audit d'intégrité.
* **auditType** (obligatoire): avec  *originatingagency* ou *tenant* ou *dsl*
* **objectId** (optionel): doit prendre la valeur de ce qui va être audité. Si auditType indique *tenant*, objectId doit prendre *la valeur du tenant* sur lequel on exécute la requête. Si auditType est *originatingagency*, alors objectId doit être *l'identifiant du service producteur* dont tous les objets vont être audité. Le paramètre n'est pas utilisé si auditType indique *dsl*
* **query** (optionel): doit prendre une requête DSL au format multiple pour batch (doit contenir *$roots*, *$query* et *$threshold*)
 
Trois exemples :
 
* Audit d'existence sur le producteur dont l'identifiant est "FRAN_09905"

```JSON
{
  "auditActions": "AUDIT_FILE_EXISTING",
  "auditType": "originatingagency",
  "objectId": "FRAN_09905"
}
```
 
* Audit d'intégrité sur tout le tenant 9 avec cette requête lancée sur ce même tenant 9 :

```JSON
{
  "auditActions":"AUDIT_FILE_INTEGRITY",
  "auditType":"tenant",
  "objectId": "9"
}
```

* Audit d'existence pour des objets liés à des opérations d'ingests :

```JSON
{
  "auditActions": "AUDIT_FILE_EXISTING",
  "auditType": "dsl",
  "query": {
    "$roots": [],
    "$query": [
      {
        "$in": {
          "#operations": [
            "aeaaaaaaaahgotryaauzialjp5zkhgiaaaaq",
            "aecaaaaabohmh3nzab37maljtitg4viaaaaq",
            "aedqaaaaaohmh3nzab37maljtithxsyaaaaq"
          ]
        }
      }
    ],
    "$threshold": 1000
  }
}
```

# Audit de cohérence

Il est possible de définir les requêtes pour lancer un audit de cohérence sur des unités archivistiques, objets, groupe d'objets ou sur une opération

Deux exemples de requête :

* Audit de cohérence d'une ou plusieurs opérations :

```JSON
{
  "$roots": [],
  "$query": [
    {
      "$and": [
        {
          "$in": {
            "#operations": [ "#id1", "#id2" ]
            }
        },
        {
          "$exists" : "Title"
        }
      ]
    }],
    "$projection": {}
}
```
* Audit de cohérence des unités archivistiques:

```JSON
{
  "$query": [
    {
      "$or": [
        {
          "$in": {
            "#id": [
              "aeaqaaaaaee5z5a6aarswalkj64wixyaaaaq",
              "aeaqaaaaaee5z5a6aarswalkj64wguaaaaba",
              "aeaqaaaaaee5z5a6aarswalkj64wizqaaaaq"
            ]
          }
        },
        {
          "$in": {
            "#allunitups": []
          }
        }
      ]
    }
  ],
  "$filter": {},
  "$projection": {}
}
```
