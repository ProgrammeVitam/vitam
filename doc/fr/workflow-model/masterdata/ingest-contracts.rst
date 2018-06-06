Workflow d'administration d'un référentiel des contrats d'entrée
###################################################################################

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

    * Le champ "Description" doit être une chaîne de caractères
    * Le champ "Status" doit avoir la valeur ACTIVE ou INACTIVE
    * Le champ "ArchiveProfile" doit être un tableau d'une ou plusieurs chaînes de caractère. Chacune de ces chaînes de caractère doit correspondre au champ "Identifier" d'un profil d'archivage contenu dans le référentiel des profils
    * Le champ "CheckParentLink": doit avoir la valeur ACTIVE ou INACTIVE
    * Le champ "LinkedParentId" doit être une chaîne de caractères devant correspondre au GUID d'une AU de plan de classement ou d'arbre de positionnement pris en charge par la solution logicielle Vitam sur le même tenant
    * Le champ "MasterMandatory" doit avoir la valeur true ou false
    * Le champ "EveryDataObjectVersion" doit avoir la valeur true ou false
    * Le champ "DataObjectVersion" devrait être un tableau dont chaque élément est dans l'énumération suivantes (ou être vide) : "BinaryMaster", "TextContent", "Thumbnail", "PhysicalMaster", "Dissemination"
    * Le champ "FormatUnidentifiedAuthorized" doit avoir la valeur true ou false
    * Le champ "EveryFormatType" doit avoir la valeur true ou false
    * Le champ "FormatType" doit être un tableau dont chaque élément est une PUID du référentiel des formats (exemple : "fmt/17")


  + **Statuts** :

    - OK : le contrat répond aux exigences des règles (STP_IMPORT_INGEST_CONTRACT.OK=Succès du processus d'import du contrat d'entrée)

    - KO : une des règles ci-dessus n'a pas été respectée (STP_IMPORT_INGEST_CONTRACT.KO=Échec du processus d'import du contrat d'entrée)

    - FATAL : une erreur fatale est survenue lors de la vérification de l'import du contrat (STP_IMPORT_INGEST_CONTRACT.FATAL=Erreur fatale du processus d'import du contrat d'entrée)

    - WARNING : Avertissement lors du processus d'import du contrat d''entrée ( STP_IMPORT_INGEST_CONTRACT.WARNING=Avertissement lors du processus d'import du contrat d'entrée )

    - DUPLICATION : L'identifiant utilisé existe déjà ( STP_IMPORT_INGEST_CONTRACT.IDENTIFIER_DUPLICATION.KO=Échec de l'import : l'identifiant est déjà utilisé )

    - EMPTY REQUIRED FIELD : un champ obligatoire n'est pas renseigné ( STP_IMPORT_INGEST_CONTRACT.EMPTY_REQUIRED_FIELD.KO=Échec de l'import : au moins un des champs obligatoires n'est pas renseigné )

    - PROFILE NOT FOUND : Le profil d'archivage mentionné n' existe pas ( STP_IMPORT_INGEST_CONTRACT.PROFILE_NOT_FOUND.KO=Échec de l'import : profil d'archivage non trouvé )



Mise à jour d'un contrat d'entrée (STP_UPDATE_INGEST_CONTRACT)
---------------------------------------------------------------

La modification d'un contrat d'entrée doit suivre les mêmes règles que celles décrites pour la création. La clé de l'événement est "STP_UPDATE_INGEST_CONTRACT", entraînant des statuts STP_UPDATE_INGEST_CONTRACT.OK, STP_UPDATE_INGEST_CONTRACT.KO et STP_UPDATE_INGEST_CONTRACT.FATAL sur les mêmes contrôles que l'import.

Sauvegarde du JSON (STP_BACKUP_INGEST_CONTRACT)
-----------------------------------------------

Cette tâche est appellée que ce soit en import initial ou en modification.

  + **Règle** : enregistrement d'une copie de la base de données des contrats d'entrée sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (STP_BACKUP_INGEST_CONTRACT.OK = Succès du processus de sauvegarde des contrats d'entrée)

      - KO : pas de cas KO

      - FATAL : une erreur fatale est survenue lors de la copie de la base de donnée nouvellement importée (STP_BACKUP_INGEST_CONTRACT.FATAL = Erreur fatale lors du processus de sauvegarde des contrats d'entrée)
