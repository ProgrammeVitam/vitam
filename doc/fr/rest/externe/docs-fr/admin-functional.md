*L'API d'administration fonctionnelle* propose les points d'entrées et les méthodes pour requêter et récupérer les informations des collections suivantes :

- Référentiel des Formats basé sur PRONOM (TNA)
- Référentiel des Règles de Gestion

Cette API est dans une étape de création, elle est donc en statut **Alpha**.

# Référentiel des Formats

Ce référentiel est basé sur PRONOM (TNA) mais il peut être étendu. Il est trans-tenant.
- L'extension est à ce jour non supporté (**UNSUPPORTED**)
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

# Registre des Fonds

**NOTE IMPORTANTE** : Ce service sera déplacé dans la partie administration fonctionnelle. Il est pour le moment dans l'Accès.

# Gestion des processus

Il est possible de gérer les processus en mode administrateur (CANCEL, PAUSE, NEXT).

**NOTE IMPORTANTE** : Ce service sera déplacé dans la partie administration fonctionnelle. Il est pour le moment dans l'Entrée.


# Sécurisation des journaux

Il est possible de gérer la sécurisation des journaux et de vérifier a posteriori leur conformité.

**NOTE IMPORTANTE** : Ce service sera déplacé dans la partie administration fonctionnelle. Il est pour le moment dans la partie Logbook.
