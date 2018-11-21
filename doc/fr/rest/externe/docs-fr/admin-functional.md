*L'API d'administration fonctionnelle* propose les points d'entrées et les méthodes pour requêter et récupérer les informations des collections suivantes :

- Référentiel des Formats basé sur PRONOM (TNA)
- Référentiel des Règles de Gestion
- Référentiel des Contrats d'Entrées
- Référentiel des Contrats d'Accès
- Référentiel des Contextes
- Référentiel des Profiles de Sécurité
- Registre des Fonds
- Référentiel des Services Agents
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

# Registre des Fonds

Ce référentiel est utilisé et mis à jour lors du processus d'entrée.

# Gestion des processus

Il est possible de gérer les processus en mode administrateur (CANCEL, PAUSE, NEXT, REPLAY, RESUME).

# Sécurisation des journaux - vérification

**traceability/checks** est le point d'entrée pour la vérification de la sécurisation des journaux d'opérations dans Vitam.
