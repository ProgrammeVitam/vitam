Workflow d'import d'un référentiel des contrats d'accès
#######################################################

Introduction
============

Cette section décrit le processus de création d'un contrat d'accès.

Processus d'import  et mise à jour d'un contrat d'accès (vision métier)
========================================================================

Le processus d'import d'un contrat d'accès permet à la fois de vérifier qu'il contient les information minimales obligatoire, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisées au cours de ce processus sont éxécutés dans une seule étape.

Import d'un contrat d'accès (STP_IMPORT_ACCESS_CONTRACT)
----------------------------------------------------------

* Vérification de la présence des informations miniamles, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Règle** : vérification et enregistrement du contrat

  + **Statuts** :

    - OK : Le contrat répond aux exigences suivantes :

      + Les données suivantes sont obligatoirement remplies :

        * Le champ Name est peuplé d'une chaîne de caractères
        * Le champs Identifier est peuplé d'une chaîne de caractères si le référentiel des contrats d'entrée est configuré en mode esclave sur le tenant séléctionné

      + Les données suivantes optionnelles si elles sont remplies le sont en respectants les règles énnonées pour chacune :
  
        * Le champ Description est peuplé avec une chaîne de caractères
        * Le champ Status est peuplé soit de la valeur ACTIVE ou INACTIVE
        * Le champs DataObjectVersion est soit vide soit peuplé avec un tableau de un ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre à un des usages défini dans les GOT  pris en charge dans la solution logicielle Vitam.
        * Le champs OriginatingAgencies est soit vide soit peuplé avec un tableau de un ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre au champ Identifier d'un service agent contenu dans le référentiel des services agents.
        * WritingPermission : doit être à true ou false
        * EveryOriginatingAgency : doit être à true ou false
        * EveryDataObjectVersion : doit être à true ou false
        * RootUnit :  est soit vide soit peuplé avec un tableau de un ou plusieurs chaînes de caractère. Chacune des chaînes de caractère doit correspondre au GUID d'une AU prise en charge dans la solution logicielle Vitam.

    - KO : Une des règles ci-dessus n'a pas été respecté

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du contrat (STP_IMPORT_ACCESS_CONTRACT.FATAL=Erreur fatale lors de l'import du contrat d'accès)

Update d'un contrat d'entrée (STP_UPDATE_ACCESS_CONTRACT)
----------------------------------------------------------

La modification d'un contrat d'entrée doit suivre les mêmes règles que celles décrites pour la création.