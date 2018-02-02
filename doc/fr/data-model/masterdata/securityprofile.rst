Collection SecurityProfile
##########################

Utilisation de collection
=========================

Cette collection contient les profils de sécurité mobilisés par les contextes applicatifs.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
      "_id": "aegqaaaaaaeucszwabglyak64gjmgbyaaaba",
      "Identifier": "SEC_PROFILE-000002",
      "Name": "demo-security-profile",
      "FullAccess": false,
      "Permissions": [
          "securityprofiles:create",
          "securityprofiles:read",
          "securityprofiles:id:read",
          "securityprofiles:id:update",
          "accesscontracts:read",
          "accesscontracts:id:read",
          "contexts:id:update"
      ],
      "_v": 1
  }

Détail des champs
=================

**"_id":** identifiant unique du profil de sécurité.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"Identifier":** identifiant signifiant donné au profil de sécurité.
  
  * Il est consituté du préfixe "SEC_PROFILE-" suivi d'une suite de 6 chiffres tant qu'il est définit par la solution logicielle Vitam. Par exemple : SEC_PROFILE-001573. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du profil de sécurité.
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name":** nom du profil de sécurité.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"FullAccess":** mode super-administrateur donnant toutes les permissions.
  
  * Il s'agit d'un booléen.
  * S'il est à "false", le mode super-administrateur n'est pas activé et les valeurs du champ permission sont utilisées. S'il est à "true", le champ permission doit être vide.
  * Cardinalité : 1-1

"Permissions": décrit l'ensemble des permissions auxquelles le profil de sécurité donne accès. Chaque API externe contient un verbe OPTION qui retourne la liste des services avec leur description et permissions associées.
  
  * Il s'agit d'un tableau de chaînes de caractères.
  * Peut être vide
  * Cardinalité : 0-1

**"_v":** version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1