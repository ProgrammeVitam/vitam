Workflow d'import d'un référentiel des contrats d'entrée
########################################################

Introduction
============

Cette section décrit le processus de création d'un contrat d'entrée.

Processus d'import  et mise à jour d'un contrat d'entrée (vision métier)
========================================================================

Le processus d'import d'un contrat d'entrée permet à la fois de vérifier qu'il contient les information minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisées au cours de ce processus sont éxécutés dans une seule étape.

Import d'un contrat d'entrée (STP_IMPORT_INGEST_CONTRACT)
----------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

  + **Règle** : vérification et enregistrement du contrat

  + **Statuts** :

    - OK : Le contrat répond aux exigences suivantes :

      + Les données suivantes sont obligatoirement remplies :

        * Le champ Name est peuplé d'une chaîne de caractères
        * Le champs Identifier est peuplé d'une chaîne de caractère si le référentiel des contrats d'entrée est configuré en mode esclave sur le tenant séléctionné

      + Les données suivantes optionnelles si elles sont remplies le sont en respectants les règles énnonées pour chacune :
  
        * Le champ Description est peuplé avec une chaîne de caractères
        * Le champ Status est peuplé soit de la valeur ACTIVE ou INACTIVE
        * Le champs ArchiveProfile est peuplé avec un tableau de un ou plusieurs chaînes de caractère. Chacune de ces chaînes de caractère doit correspondre au champ Identifier d'un profil d'archivage contenu dans le référentiel des profils
        * Le champ LinkedParentId et peuplé par une chaîne de caractères devant correspondre au GUID d'une AU de plan ou d'arbre pris en charge par la solution logicielle Vitam sur le même tenant

    - KO : Une des règles ci-dessus n'a pas été respecté

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du contrat (STP_IMPORT_INGEST_CONTRACT.FATAL=Erreur fatale lors de l''import du contrat d''entrée)

Update d'un contrat d'entrée (STP_UPDATE_INGEST_CONTRACT)
----------------------------------------------------------

La modification d'un contrat d'entrée doit suivre les mêmes règles que celles décrites pour la création.