Collection FileRules
####################

Utilisation de la collection FileRules
======================================

La collection FileRules permet de référencer et décrire unitairement les différentes règles de gestion utilisées dans la solution logicielle Vitam pour calculer les échéances associées aux unités archivistiques.

Cette collection est alimentée par l'import d'un fichier CSV contenant l'ensemble des règles. Celui-ci doit être structuré comme ceci :

.. csv-table::
  :header: "RuleId","RuleType","RuleValue","RuleDescription","RuleDuration","RuleMeasurement"

  "Id de la règle","Type de règle","Intitulé de la règle","Description de la règle","Durée de la règle","Unité de mesure de la durée de la règle"

La liste des types de règle disponibles est en annexe.

Les valeurs renseignées dans la colonne unité de mesure doivent correspondre à une valeur de l'énumération RuleMeasurementEnum, à savoir :

  * MONTH
  * DAY
  * YEAR

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection FileRules
==============================================================================================

::

 {
   "_id": "aeaaaaaaaahbl62nabduoak3jc4avsyaaaha",
   "RuleId": "ACC-00011",
   "RuleType": "AccessRule",
   "RuleValue": "Communicabilité des informations portant atteinte au secret de la défense nationale",
   "RuleDescription": "Durée de communicabilité applicable aux informations portant atteinte au secret de la défense nationale\nL’échéance est calculée à partir de la date du document ou du document le plus récent inclus dans le dossier",
   "RuleDuration": "50",
   "RuleMeasurement": "YEAR",
   "CreationDate": "2017-11-02T13:50:28.922",
   "UpdateDate": "2017-11-06T09:11:54.062",
   "_v": 0,
   "_tenant": 0
  }



Détail des champs
=================

**"_id":** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"RuleId":** identifiant unique par tenant de la règle dans le référentiel utilisé.

  * Il s'agit d'une chaîne de caractères.
  * La valeur est reprise du champ RuleId du fichier d'import. Par commodité, les exemples sont composés d'un préfixe puis d'un nombre, séparés par un tiret, mais ce formalisme n'est pas obligatoire.
  * Cardinalité : 1-1

Par exemple :

::

 ACC-00027



**"RuleType":** type de règle.

  * Il s'agit d'une chaîne de caractères.
  * Il correspond à la valeur située dans la colonne RuleType du fichier d'import. Les valeurs possibles pour ce champ sont indiquées en annexe.
  * Cardinalité : 1-1

**"RuleValue":** intitulé de la règle.

  * Il s'agit d'une chaîne de caractères.
  * Elle correspond à la valeur de la colonne RuleValue du fichier d'import.
  * Cardinalité : 1-1

**"RuleDescription":** description de la règle.

  * Il s'agit d'une chaîne de caractères.
  * Elle correspond à la valeur de la colonne RuleDescription du fichier d'import.
  * Cardinalité : 1-1

**"RuleDuration":** durée de la règle.

  * Il s'agit d'un entier compris entre 0 et 999.
  * Associé à la valeur indiqué dans RuleMeasurement, il permet de décrire la durée d'application de la règle de gestion. Il correspond à la valeur de la colonne RuleDuration du fichier d'import.
  * Cardinalité : 1-1

**"RuleMeasurement":** unité de mesure de la durée décrite dans la colonne RuleDuration du fichier d'import.

    * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de l'énumération RuleMeasurementEnum, à savoir :

      - MONTH
      - DAY
      - YEAR

  * Cardinalité : 1-1

**"CreationDate":** date de création de la règle dans la collection FileRule.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

``Exemple : "2017-11-02T13:50:28.922"``


**"UpdateDate":** Date de dernière mise à jour de la règle dans la collection FileRules.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

``Exemple : "2017-11-02T13:50:28.922"``


**"_v":** version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1
