Workflow d'administration et de mise à jour d'un référentiel des contrats d'entrée
###########################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un contrat d'entrée.

Processus d'import  et mise à jour d'un contrat d'entrée (vision métier)
========================================================================

Le processus d'import d'un contrat d'entrée permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un contrat d'entrée (STP_IMPORT_INGEST_CONTRACT)
----------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** : vérification et enregistrement du contrat :

    * Le champ "Name" est peuplé d'une chaîne de caractères
    * Si le tenant concerné est en mode "esclave", le champ "Identifier" doit être rempli. Sinon, il est automatiquement complété par la solution logicielle Vitam

    + Les données suivantes optionnelles si elles sont remplies le sont en respectant les règles énnonées pour chacune :

    * Le champ "Description" est peuplé avec une chaîne de caractères
    * Le champ "Status" est peuplé soit de la valeur ACTIVE ou INACTIVE
    * Le champ "ArchiveProfile" est peuplé avec un tableau d'une ou plusieurs chaînes de caractère. Chacune de ces chaînes de caractère doit correspondre au champ "Identifier" d'un profil d'archivage contenu dans le référentiel des profils
    * Le champ "LinkedParentId" est peuplé par une chaîne de caractères devant correspondre au GUID d'une AU de plan de classement ou d'arbre de positionnement pris en charge par la solution logicielle Vitam sur le même tenant

  + **Statuts** :

    - OK : le contrat répond aux exigences des règles

    - KO : une des règles ci-dessus n'a pas été respectée

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du contrat (STP_IMPORT_INGEST_CONTRACT.FATAL=Erreur fatale lors de l''import du contrat d''entrée)

Mise à jour d'un contrat d'entrée (STP_UPDATE_INGEST_CONTRACT)
----------------------------------------------------------

La modification d'un contrat d'entrée doit suivre les mêmes règles que celles décrites pour la création.
