Workflow d'entrée
#################

Introduction
============

Cette section décrit le processus (workflow) d'entrée, utilisé lors du transfert d'un Submission Information Package (SIP) dans la solution logicielle Vitam. Ce workflow se décompose en deux grosses catégories : le processus d'entrée externe "ingest externe" et le processus d'entrée interne "ingest interne". Le premier prend en charge le SIP et effectue des contrôles techniques préalables, tandis que le second débute dès le premier traitement métier.

Toutes les étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent le processus d'entrée (clé et description de la clé associée dans le journal des opérations) tel qu'implémenté dans la version actuelle de la solution logicielle Vitam.

Le processus d'entrée externe comprend l'étape : STP_SANITY_CHECK_SIP. Les autres étapes font partie du processus d'entrée interne.

Le cas du processus d'entrée "test à blanc"
===========================================

Il est possible de procéder à un versement "à blanc", pour tester la conformité du SIP par rapport à la forme attendue par la solution logicielle Vitam sans pour autant le prendre en charge. Dans ce cas, le processus d'entrée à blanc diffère du processus d'entrée "classique" en ignorant un certain nombre d'étapes.

Les étapes non exécutées dans le processus d'entrée à blanc sont les suivantes :

- Ecriture et indexation des objets et groupes d'objets (STP_OBJ_STORING)
- Indexation des unités archivistiques (STP_UNIT_METADATA)
- Enregistrement et écriture des métadonnées des objets et groupes d'objets (STP_OG_STORING)
- Enregistrement et écriture des unités archivistiques (STP_UNIT_STORING)
- Registre des fonds (STP_ACCESSION_REGISTRATION)

Les tâches relatives à toutes ces étapes sont donc également ignorées.

Contrôles préalables à l'entrée (STP_SANITY_CHECK_SIP)
======================================================

Contrôle sanitaire (SANITY_CHECK_SIP)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : vérification de l'absence de virus dans le SIP

+ **Type** : bloquant

+ **Statuts** :

  - OK : aucun virus n'est détecté dans le SIP (SANITY_CHECK_SIP.OK = Succès du contrôle sanitaire : aucun virus détecté)

  - KO : un ou plusieurs virus ont été détectés dans le SIP (SANITY_CHECK_SIP.KO = Échec du contrôle sanitaire du SIP : fichier détecté comme infecté)

  - FATAL : la vérification de la présence de virus dans le SIP n'a pas pu être faite suite à une erreur technique (SANITY_CHECK_SIP.FATAL=Erreur fatale lors du contrôle sanitaire du SIP)

Contrôle du format du conteneur du SIP (CHECK_CONTAINER)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : vérification du format du SIP via un outil d'identification de format qui se base sur le référentiel des formats qu'il intègre

+ **Formats acceptés** : .zip, .tar, .tar.gz, .tar.bz2

+ **Type** : bloquant

+ **Statuts** :

  - OK : le conteneur du SIP est au bon format (CHECK_CONTAINER.OK = Succès du contrôle de format du conteneur du SIP)

  - KO : le conteneur du SIP n'est pas au bon format (CHECK_CONTAINER.KO = Échec du contrôle de format du conteneur du SIP)

  - FATAL : la vérification du format du conteneur du SIP n'a pas pu être faite suite à une erreur technique liée à l'outil d'identification des formats (CHECK_CONTAINER.FATAL = Erreur fatale lors du processus du contrôle de format du conteneur du SIP)


Réception dans vitam (STP_UPLOAD_SIP) : Etape de réception du SIP dans Vitam
============================================================================

* **Règle** : vérification de la bonne réception du SIP dans l'espace de travail interne ("workspace")

* **Type** : bloquant

* **Statuts** :

  + OK : le SIP a été réceptionné sur l'espace de travail interne (STP_UPLOAD_SIP.OK = Succès du processus de téléchargement du SIP)

  + KO : le SIP n'a pas été réceptionné sur l'espace de travail interne (STP_UPLOAD_SIP.KO = Échec du processus de téléchargement du SIP)

  + FATAL : la réception du SIP dans la solution logicielle Vitam n'a pas été possible suite à une erreur technique, par exemple une indisponibilité du serveur (STP_UPLOAD_SIP.FATAL = Erreur Fatale lors du processus de téléchargement du SIP)


Contrôle du SIP (STP_INGEST_CONTROL_SIP)
========================================

Vérification globale du SIP (CHECK_SEDA) : Vérification de la cohérence physique du SIP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : vérification du SIP reçu par rapport au type de SIP accepté

+ **Type de SIP accepté** : le manifeste, obligatoire dans le SIP, doit être nommé manifest.xml, doit être conforme au schéma xsd par défaut fourni avec le standard SEDA v. 2.0, doit satisfaire les exigences du document "Spécification des SIP" pour la solution logicielle Vitam et doit posséder un répertoire unique nommé "Content"

+ **Type** : bloquant

+ **Statuts** :

  - OK : le SIP est présent, nommé manifest.xml et conforme au schéma xsd par défaut fourni avec le standard SEDA v.2.0. (CHECK_SEDA.OK = Succès de la vérification globale du SIP)
  - KO :
    - Cas 1 : le manifeste est introuvable dans le SIP ou n'a pas une extension .xml (CHECK_SEDA.NO_FILE.KO = Échec de la vérification globale du SIP : le manifeste est introuvable dans le SIP)
    - Cas 2 : le manifeste n'est pas au format XML (CHECK_SEDA.NOT_XML_FILE.KO = Échec de la vérification globale du SIP : le manifeste est au mauvais format)
    - Cas 3 : le manifeste ne respecte pas le schéma par défaut fourni avec le standard SEDA 2.0 (CHECK_SEDA.NOT_XSD_VALID.KO = Échec de la vérification globale du SIP : manifeste non conforme au schéma SEDA 2.0)
    - Cas 4 : le SIP contient plus d'un dossier "Content" (CHECK_SEDA.CONTAINER_FORMAT.DIRECTORY.KO = Le SIP contient plus d'un dossier ou un dossier dont le nommage est invalide)
    - Cas 5 : le SIP contient plus d'un seul fichier à la racine (CHECK_SEDA.CONTAINER_FORMAT.FILE.KO = Le SIP contient plus d'un fichier à sa racine)
  - FATAL : le SIP n'a pas pu être contrôlé suite à une erreur technique (CHECK_SEDA.FATAL = Erreur fatale lors de la vérification globale du SIP)

Vérification de l'en-tête du manifeste (CHECK_HEADER)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règles** : vérification des informations du manifest.xml (nommées "header") et de l'existence du service producteur (OriginatingAgencyIdentifier)

+ **Type** : bloquant

+ **Statuts** :

  - OK : les informations du manifeste sont conformes et le service producteur est déclaré (CHECK_HEADER.OK = Succès de la vérification générale du bordereau)

  - KO : les informations du manifeste ne sont pas conformes ou il n'y a pas de service producteur déclaré (CHECK_HEADER.KO = Échec de la vérification générale du bordereau)

  - FATAL : une erreur technique est survenue lors des contrôles sur les informations générales du manifeste (CHECK_HEADER.FATAL = Erreur fatale lors de la vérification générale du bordereau)


La tâche contient les traitements suivants
******************************************

* Vérification de la relation entre le contrat et le profil SEDA (CHECK_IC_AP_RELATION)

  + **Règle** : le profil SEDA déclaré dans le contrat d'entrée du SIP doit être le même que celui déclaré dans son manifeste. Si aucun profil SEDA ne s'applique au SIP, ce traitement est ignoré

  + **Statuts** :

      - OK : le profil SEDA déclaré dans le contrat d'entrée et celui déclaré dans le manifeste sont les mêmes (CHECK_HEADER.CHECK_IC_AP_RELATION.OK = Succès de la vérification de la relation entre le contrat et le profil SEDA)

      - KO : le profil déclaré dans le contrat d'entrée et celui déclaré dans le manifeste ne sont pas les mêmes (CHECK_HEADER.CHECK_IC_AP_RELATION.KO = Echec de la vérification de la relation entre le contrat et le profil SEDA)

      - FATAL : une erreur technique est survenue lors de la vérification de la relation (CHECK_HEADER.CHECK_IC_AP_RELATION.FATAL = Erreur fatale lors de la vérification de la relation entre le contrat et le profil SEDA)

* Vérification de la présence et contrôle des services agents (CHECK_AGENT)

  + **Règle** : verification du service producteur ainsi que du service versant déclarés dans le SIP par rapport au référentiel des services agents présent dans la solution logicielle VITAM
    
  + **Statuts** :
    
      - OK : le service producteur et/ou le service versant déclaré dans le SIP est valide (service agent existant dans le référentiel des services agents)
        
      - KO : le service producteur et/ou le service versant déclaré dans le SIP est invalide (service agent non trouvé dans le référentiel des services agents)  

      - FATAL : une erreur technique est survenue lors de la vérification de la présence et du contrôle des services agents

* Vérification de la présence et contrôle du contrat d'entrée (CHECK_CONTRACT_INGEST)

  + **Règle** : vérification du contrat d'entrée déclaré dans le SIP par rapport au référentiel des contrats d'entrée présent dans la solution logicielle VITAM

  + **Statuts** :

    - OK : le contrat déclaré dans le SIP est valide (contrat existant dans le référentiel des contrats et dont le statut est actif)

    - KO : le contrat déclaré dans le SIP est invalide (contrat non trouvé dans le référentiel de contrats ou contrat existant mais inactif)

    - FATAL : une erreur technique est survenue lors de la vérification de la présence et du contrôle du contrat d'entrée

* Vérification de la conformité du manifeste par le profil SEDA (CHECK_ARCHIVEPROFILE)

  + **Règle** : le manifeste du SIP doit être conforme aux exigences du profil SEDA. Si aucun profil SEDA ne s'applique au SIP, ce traitement est ignoré.

  + **Statuts** :

      - OK : le manifeste est conforme aux exigences du profil SEDA (CHECK_ARCHIVEPROFILE.OK = Succès de la vérification de la conformité au profil SEDA)

      - KO : le manifeste n'est pas conforme aux exigences du profil SEDA (CHECK_ARCHIVEPROFILE.KO = Echec de la vérification de la conformité au profil SEDA)

      - FATAL : une erreur technique est survenue lors de la vérification du manifeste par le profil SEDA (CHECK_ARCHIVEPROFILE.FATAL = Erreur fatale lors de la vérification de la conformité au profil SEDA)


Vérification du contenu du bordereau (CHECK_DATAOBJECTPACKAGE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Type** : bloquant.

Cette tâche contient plusieurs traitements, chacun ayant une finalité et des points de sorties spécifiques.

* Vérification des usages des groupes d'objets (CHECK_MANIFEST_DATAOBJECT_VERSION)

    + **Règle** : tous les objets décrits dans le manifeste du SIP doivent déclarer un usage conforme à la liste des usages acceptés dans la solution logicielle Vitam ainsi qu'un numéro de version respectant la norme de ce champ

    + **Types d'usages acceptés**: original papier (PhysicalMaster), original numérique (BinaryMaster), diffusion (Dissemination), vignette (Thumbnail), contenu brut (TextContent). Pour les numéros de version, il s'agit d'un entier positif ou nul (0, 1, 2...). La grammaire est : "usage_version"

    + **Statuts** :

      - OK : les objets contenus dans le SIP déclarent tous dans le manifeste un usage cohérent avec ceux acceptés et optionnellement un numéro de version respectant la norme de ce champ usage, par exemple "BinaryMaster_2" (CHECK_MANIFEST_DATAOBJECT_VERSION.OK = Succès de la vérification des usages des groupes d'objets)

      - KO : un ou plusieurs objets contenus dans le SIP déclarent dans le manifeste un usage ou un numéro de version incohérent avec ceux acceptés (CHECK_MANIFEST_DATAOBJECT_VERSION.KO = Échec de la vérification des usages des groupes d'objets)

      - FATAL : les usages déclarés dans le manifeste pour les objets contenus dans le SIP n'ont pas pu être contrôlés suite à une erreur technique (CHECK_MANIFEST_DATAOBJECT_VERSION.FATAL = Erreur fatale lors de la vérification des usages des groupes d'objets)


* Vérification du nombre d'objets (CHECK_MANIFEST_OBJECTNUMBER)

    + **Règle** : le nombre d'objets binaires reçus dans la solution logicielle Vitam doit être strictement égal au nombre d'objets binaires déclaré dans le manifeste du SIP

    + **Statuts** :

      - OK : le nombre d'objets reçus dans la solution logicielle Vitam est strictement égal au nombre d'objets déclaré dans le manifeste du SIP (CHECK_MANIFEST_OBJECTNUMBER.OK = Succès de la vérification du nombre d'objets)

      - KO : le nombre d'objets reçus dans la solution logicielle Vitam est inférieur ou supérieur au nombre d'objets déclaré dans le manifeste du SIP ou les balises URI du manifeste ne déclarent pas le bon chemin vers les objets (CHECK_MANIFEST_OBJECTNUMBER.KO = Échec de la vérification du nombre d'objets)

      - FATAL : une erreur technique est survenue lors de la vérification du nombre d'objets (CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_OBJECTNUMBER.FATAL = Erreur fatale lors de la vérification du nombre d'objets)

* Vérification de la cohérence du bordereau (CHECK_MANIFEST)

    + **Règle** : création des journaux du cycle de vie des unités archivistiques et des groupes d'objets, extraction des unités archivistiques, objets binaires et objets physiques, vérification de la présence de cycles dans les arborescences des unités archivistiques et création de l'arbre d'ordre d'indexation, extraction des métadonnées contenues dans la balise ManagementMetadata du manifeste pour le calcul des règles de gestion, vérification de la validité du rattachement des unités du SIP aux unités présentes dans la solution logicielle VITAM si demandé, détection des problèmes d'encodage dans le manifeste et vérification que les objets ne font pas référence directement à des unités si ces objets possèdent des groupes d'objets.

    + **Statuts** :

      - OK : les journaux du cycle de vie des unités archivistiques et des groupes d'objets ont été créés avec succès, aucune récursivité n'a été détectée dans l'arborescence des unités archivistiques, la structure de rattachement déclarée existe (par exemple, un SIP peut être rattaché à un plan de classement, mais pas l'inverse), le type de structure de rattachement est autorisé, aucun problème d'encodage détecté et les objets avec groupe d'objets ne référencent pas directement les unités (CHECK_MANIFEST.OK = Contrôle du bordereau réalisé avec succès)

      - KO : Une récursivité a été détectée dans l'arborescence des unités archivistiques, la structure de rattachement déclarée est inexistante, le type de structure de rattachement est interdit, il y a un problème d'encodage ou des objets avec groupe d'objets référencent directement des unités (CHECK_MANIFEST.KO = Échec de contrôle du bordereau)

      - FATAL : la vérification de la cohérence du bordereau n'a pas pu être réalisée suite à une erreur système, par exemple les journaux du cycle de vie n'ont pu être créés (CHECK_MANIFEST.FATAL = Erreur fatale lors de contrôle du bordereau)


* Vérification de la cohérence entre objets, groupes d'objets et unités archivistiques (CHECK_CONSISTENCY)

    + **Règle** : vérification que chaque objet ou groupe d'objets est référencé par une unité archivistique, rattachement à un groupe d'objet pour les objets sans groupe d'objet mais référencé par une unité archivistique, création de la table de concordance (MAP) pour les identifiants des objets et des unités du SIP et génération de leurs identifiants Vitam (GUID)

    + **Statuts** :

      - OK : Aucun objet ou groupe d'objet n'est orphelin (i.e. non référencé par une unité archivistique) et tous les objets sont rattachés à un groupe d'objets (CHECK_CONSISTENCY.OK = Succès de la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques)

      - KO : Au moins un objet ou groupe d'objet est orphelin (i.e. non référencé par une unité archivistique) (CHECK_CONSISTENCY.KO = Échec de la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques)

      - FATAL : la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques n'a pas pu être réalisée suite à une erreur système (CHECK_CONSISTENCY.FATAL = Erreur fatale lors de la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques)



Contrôle et traitements des objets (STP_OG_CHECK_AND_PROCESS)
=============================================================

Vérification de l'intégrité des objets (CHECK_DIGEST)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : vérification de la cohérence entre l'empreinte de l'objet binaire calculée par la solution logicielle Vitam et celle déclarée dans le manifeste. Si l'empreinte déclarée dans le manifeste n'a pas été calculée avec l'algorithme SHA-512, alors l'empreinte est recalculée avec cet algorithme. Elle sera alors enregistrée dans la solution logicielle VITAM.

+ **Algorithmes autorisés en entrée** : MD5, SHA-1, SHA-256, SHA-512

+ **Type** : bloquant

+ **Statuts** :

  - OK : tous les objets binaires reçus sont identiques aux objets binaires attendus. Tous les objets binaires disposent désormais d'une empreinte calculée avec l'algorithme SHA-512 (CHECK_DIGEST.OK = Succès de la vérification de l'intégrité des objets binaires)

  - KO : au moins un objet reçu n'est pas identique aux objets attendus (CHECK_DIGEST.KO = Échec de la vérification de l'intégrité des objets binaires)

  - FATAL : la vérification de l'intégrité des objets binaires n'a pas pu être réalisée suite à une erreur système, par exemple lorsque l'algorithme inconnu (CHECK_DIGEST.FATAL = Erreur fatale lors de la vérification des objets)



Identification des formats (OG_OBJECTS_FORMAT_CHECK)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** :  identification des formats de chaque objet binaire présent dans le SIP, afin de garantir une information homogène et objective. Cette action met en œuvre un outil d'identification prenant l'objet en entrée et fournissant des informations de format en sortie. Ces informations sont comparées avec les formats enregistrés dans le référentiel des formats interne à la solution logicielle VITAM et avec celles déclarées dans le manifeste. En cas d'incohérence entre la déclaration dans le SIP et le format identifié, le SIP sera accepté, générant un avertissement. La solution logicielle Vitam se servira alors des informations qu'elle a identifiées et non de celles fournies dans le SIP

+ **Type** : bloquant

+ **Statuts** :

  - OK : l'identification s'est bien passée, les formats identifiés sont référencés dans le référentiel interne et les informations sont cohérentes avec celles déclarées dans le manifeste (OG_OBJECTS_FORMAT_CHECK.OK = Succès de la vérification des formats)

  - KO : 
    - Cas 1 : au moins un objet reçu a un format qui n'a pas été trouvé (OG_OBJECTS_FORMAT_CHECK.KO = Échec de la vérification des formats)
    - Cas 2 : au moins un objet reçu a un format qui n'est pas référencé dans le référentiel interne (.OG_OBJECTS_FORMAT_CHECK.FILE_FORMAT.UNCHARTED.KO)
    - Cas 3 : le SIP soumis soumis à la solution logicielle Vitam contient à la fois le cas 1 et le cas 2 (OG_OBJECTS_FORMAT_CHECK.KO = Échec de la vérification des formats)

  - FATAL : l'identification des formats n'a pas été réalisée suite à une erreur technique (OG_OBJECTS_FORMAT_CHECK.FATAL = Erreur fatale lors de la vérification des formats)

  - WARNING : l'identification s'est bien passée, les formats identifiés sont référencés dans le référentiel interne mais les informations ne sont pas cohérentes avec celles déclarées dans le manifeste (OG_OBJECTS_FORMAT_CHECK.WARNING = Avertissement lors de la vérification des formats)


Contrôle et traitements des unités archivistiques (STP_UNIT_CHECK_AND_TRANSFORME)
=================================================================================

Vérification globale de l'unité archivistique (CHECK_UNIT_schéma)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** :  contrôle additionnel sur la validité des champs de l'unité archivistique par rapport au schéma prédéfini dans la solution logicielle Vitam. Par exemple, les champs obligatoires, comme les titres des unités archivistiques, ne doivent pas être vides. En plus du contrôle par le schéma, cette tâche vérifie que la date de fin des dates extrêmes soit bien supérieure ou égale à la date de début du l'unité archivistique.

+ **Type** : bloquant

+ **Statuts** :

  - OK : tous les champs de l'unité archivistique sont conformes à ce qui est attendu (CHECK_UNIT_schéma.OK = Succès du contrôle additionnel sur la validité des champs de l'unité archivistique)

  - KO : au moins un champ de l'unité archivistique n'est pas conforme à ce qui est attendu (titre vide, date incorrecte...) ou la date de fin des dates extrêmes est strictement inférieure à la date de début (CHECK_UNIT_schéma.KO = Échec lors du contrôle additionnel sur la validité des champs de l'unité archivistique)

  - FATAL : la vérification de l'unité archivistique n'a pu être effectuée suite à une erreur technique (CHECK_UNIT_schéma.FATAL = Erreur fatale du contrôle additionnel sur la validité des champs de l'unité archivistique)

Application des règles de gestion et calcul des dates d'échéances (UNITS_RULES_COMPUTE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : calcul des dates d'échéances des unités archivistiques du SIP. Pour les unités racines, c'est à dire les unités déclarées dans le SIP et n'ayant aucun parent dans l'arborescence, la solution logicielle Vitam utilise les règles de gestions incluses dans le bloc Management de chacune de ces unités ainsi que celles présentes dans le bloc ManagementMetadata. La solution logicielle Vitam effectue également ce calcul pour les autres unités archivistiques du SIP possédant des règles de gestion déclarées dans leurs balises Management, sans prendre en compte le ManagementMetadata. Le référentiel utilisé pour ces calculs est le référentiel des règles de gestion de la solution logicielle VITAM.

+ **Type** : bloquant

+ **Statuts** :

  - OK : les règles de gestion sont référencées dans le référentiel interne et ont été appliquées avec succès (UNITS_RULES_COMPUTE.OK = Succès du calcul des dates d'échéance)

  - KO : Une erreur s'est produite lors du calcul des échéances. Ceci peut-être causé par le fait que :
      
      * au moins une règle de gestion déclarée dans le manifeste n'est pas référencée dans le référentiel interne
      * une balise RefnonRuleId a un ID d'une règle d'une autre catégorie que la sienne 

  - FATAL : une erreur technique est survenue lors du calcul des dates d'échéances (UNITS_RULES_COMPUTE.FATAL = Erreur fatale lors du calcul des dates d'échéance)


Préparation de la prise en charge (STP_STORAGE_AVAILABILITY_CHECK)
==================================================================

Vérification de la disponibilité de l'offre de stockage (STORAGE_AVAILABILITY_CHECK)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** :  Vérification de la disponibilité des offres de stockage et de l'espace disponible pour y stocker le contenu du SIP compte tenu de la taille des objets à stocker

+ **Type** : bloquant

+ **Statuts** :

  - OK : les offres de stockage sont accessibles et disposent d'assez d'espace pour stocker le contenu du SIP (STORAGE_AVAILABILITY_CHECK.OK = Succès de la vérification de la disponibilité de l'offre de stockage)

  - KO : les offres de stockage ne sont pas disponibles ou ne disposent pas d'assez d'espace pour stocker le contenu du SIP (STORAGE_AVAILABILITY_CHECK.KO = Échec de la vérification de la disponibilité de l'offre de stockage)

  - FATAL : la vérification de la disponibilité de l'offre de stockage n'a pas pu être réalisée suite à une erreur technique (STORAGE_AVAILABILITY_CHECK.FATAL = Erreur fatale lors de la vérification de la disponibilité de l'offre de stockage)


Ecriture et indexation des objets et groupes d'objets (STP_OBJ_STORING)
=============================================================================

Ecriture des objets sur l'offre de stockage (OBJ_STORAGE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : écriture des objets contenus dans le SIP sur les offres de stockage en fonction de la stratégie de stockage applicable

+ **Type** : Bloquant

+ **Statuts** :

  - OK : tous les objets binaires contenus dans le SIP ont été écrits sur les offres de stockage (OBJ_STORAGE.OK = Succès de l'écriture des objets et groupes d'objets)

  - KO : au moins un des objets binaires contenus dans le SIP n'ont pas pu être écrits sur les offres de stockage (OBJ_STORAGE.KO = Échec de l'écriture des objets et groupes d'objets)

  - WARNING : le SIP ne contient pas d'objet (OBJECTS_LIST_EMPTY.WARNING = Avertissement : le SIP ne contient pas d'objet)

  - FATAL : l'écriture des objets binaires sur les offres de stockage n'a pas pu être réalisée suite à une erreur technique (OBJ_STORAGE.FATAL = Erreur fatale lors de l'écriture des objets et groupes d'objets)


Indexation des métadonnées des groupes d'objets (OG_METADATA_INDEXATION)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : indexation des métadonnées liées aux groupes d'objets, comme la taille des objets, les métadonnées liées aux formats (Type MIME, PUID, etc.), l'empreinte des objets, etc.

+ **Type** : bloquant

+ **Statuts** :

  - OK : les métadonnées des groupes d'objets ont été indexées avec succès (OG_METADATA_INDEXATION.OK = Succès de l'indexation des métadonnées des objets et groupes d'objets)

  - KO : les métadonnées des groupes d'objets n'ont pas été indexées (OG_METADATA_INDEXATION.KO = Échec de l'indexation des métadonnées des objets et groupes d'objets)

  - FATAL : l'indexation des métadonnées des groupes d'objets n'a pas pu être réalisée suite à une erreur technique (OG_METADATA_INDEXATION.FATAL = Erreur fatale lors de l'indexation des métadonnées des objets et groupes d'objets)


Indexation des unités archivistiques (STP_UNIT_METADATA)
========================================================

Indexation des métadonnées des unités archivistiques (UNIT_METADATA_INDEXATION)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : indexation des métadonnées liées aux unités archivistiques, c'est à dire le titre des unités, leurs descriptions, leurs dates extrêmes, etc.

+ **Type** : bloquant

+ **Statuts** :

  - OK : les métadonnées des unités archivistiques ont été indexées avec succès (UNIT_METADATA_INDEXATION.OK = Succès de l'indexation des métadonnées des unités archivistiques)

  - KO : les métadonnées des unités archivistiques n'ont pas été indexées (UNIT_METADATA_INDEXATION.KO = Échec de l'indexation des métadonnées des unités archivistiques)

  - FATAL : l'indexation des métadonnées des unités archivistiques n'a pas pu être réalisée suite à une erreur technique (UNIT_METADATA_INDEXATION.FATAL = Erreur fatale lors de l'indexation des métadonnées des unités archivistiques)


Enregistrement et écriture des métadonnées des objets et groupes d'objets(STP_OG_STORING)
================================================================================================================

Ecriture des métadonnées du groupe d'objet sur l'offre de stockage (OG_METADATA_STORAGE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : sauvegarde des métadonnées liées aux groupes d'objets ainsi que leurs journaux de cycle de vie sur les offres de stockage en fonction de la stratégie de stockage

+ **Type** : bloquant

+ **Statuts** :

  - OK : les métadonnées des groupes d'objets ont été sauvegardées avec succès (OG_METADATA_STORAGE.OK = Succès de l'écriture des métadonnées du groupe d'objet)

  - KO : les métadonnées des groupes d'objets n'ont pas été sauvegardées (OG_METADATA_STORAGE.KO = Échec de l'écriture des métadonnées des groupes d'objets)

Enregistrement des journaux du cycle de vie des groupes d'objets (COMMIT_LIFE_CYCLE_OBJECT_GROUP)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : sécurisation en base des journaux du cycle de vie des groupes d'objets (avant cette étape, les journaux du cycle de vie des groupes d'objets sont dans une collection temporaire afin de garder une cohérence entre les métadonnées indexées et les journaux lors d'une entrée en succès ou en échec)

+ **Type** : bloquant

+ **Statuts** :

  - OK : La sécurisation des journaux du cycle de vie s'est correctement déroulée (COMMIT_LIFE_CYCLE_OBJECT_GROUP.OK = Succès de l'enregistrement des journaux du cycle de vie des groupes d''objets)

  - FATAL : La sécurisation du journal du cycle de vie n'a pas pu être réalisée suite à une erreur technique (COMMIT_LIFE_CYCLE_OBJECT_GROUP.FATAL = Erreur fatale lors de l'enregistrement des journaux du cycle de vie des groupes d'objets)


Enregistrement et écriture des unités archivistiques (STP_UNIT_STORING)
==========================================================================

Ecriture des métadonnées de l'unité archivistique sur l'offre de stockage (UNIT_METADATA_STORAGE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : sauvegarde des métadonnées liées aux unités archivistiques ainsi que leurs journaux de cycle de vie sur les offres de stockage en fonction de la stratégie de stockage

+ **Type** : bloquant

+ **Statuts** :

  - OK : les métadonnées des unités archivistiques ont été sauvegardées avec succès (UNIT_METADATA_STORAGE.OK = Succès de l'enregistrement des métadonnées des unités archivistiques)

  - KO : les métadonnées des unités archivistiques n'ont pas pu être sauvegardées (UNIT_METADATA_STORAGE.KO = Échec de l'enregistrement des métadonnées des unités archivistiques)

Enregistrement du journal du cycle de vie des unités archivistiques (COMMIT_LIFE_CYCLE_UNIT)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : sécurisation en base des journaux du cycle de vie des unités archivistiques (avant cette étape, les journaux du cycle de vie des unités archivistiques sont dans une collection temporaire afin de garder une cohérence entre les métadonnées indexées et les journaux lors d'une entrée en succès ou en échec)

+ **Type** : bloquant

+ **Statuts** :

  - OK : La sécurisation des journaux du cycle de vie s'est correctement déroulée (COMMIT_LIFE_CYCLE_UNIT.OK = Succès de l'enregistrement des journaux du cycle de vie des unités archivistiques)

  - FATAL : La sécurisation des journaux du cycle de vie n'a pas pu être réalisée suite à une erreur système (COMMIT_LIFE_CYCLE_UNIT.FATAL = Erreur fatale lors de de l'enregistrement des journaux du cycle de vie des unités archivistiques)


Registre des fonds (STP_ACCESSION_REGISTRATION)
===============================================

Alimentation du registre des fonds (ACCESSION_REGISTRATION)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : enregistrement dans le registre des fonds des informations concernant la nouvelle entrée (nombre d'objets, volumétrie). Ces informations viennent s'ajouter aux informations existantes pour un même service producteur. Si le service producteur n'est pas présent pas dans la solution logicielle VITAM et qu'il s'agit de sa première entrée, cette entrée est enregistrée et le service producteur est créé au sein de la solution logicielle Vitam.

+ **Type** : bloquant

+ **Statuts** :

  - OK : le registre des fonds est correctement alimenté (ACCESSION_REGISTRATION.OK = Succès de l'alimentation du registre des fonds)

  - KO : le registre des fonds n'a pas pu être alimenté (ACCESSION_REGISTRATION.KO = Échec de l'alimentation du registre des fonds)

  - FATAL : l'alimentation du registre des fonds n'a pas pu être réalisée suite à une erreur système (ACCESSION_REGISTRATION.FATAL = Erreur fatale lors de l'alimentation du registre des fonds)


Finalisation de l'entrée (STP_INGEST_FINALISATION)
==================================================

Notification de la fin de l'opération d'entrée (ATR_NOTIFICATION)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : génération de la notification de réponse (ArchiveTransferReply ou ATR) une fois toutes les étapes passées avec succès ou lorsqu'une étape est en échec, puis enregistrement de cette notification dans l'offre de stockage et envoi au service versant.

+ **Type** : non bloquant

+ **Statuts** :

  - OK : Le message de réponse a été correctement généré, écrit sur l'offre de stockage et envoyé au service versant (ATR_NOTIFICATION.OK = Succès de la notification à l'opérateur de versement)

  - KO : Le message de réponse n'a pas été correctement généré, écrit sur l'offre de stockage ou reçu par le service versant (ATR_NOTIFICATION.KO = Échec de la notification à l'opérateur de versement)

  - FATAL : la notification de la fin de l'opération n'a pas pu être réalisée suite à une erreur technique (ATR_NOTIFICATION.FATAL = Erreur fatale lors de la notification à l'opérateur de versement)

Mise en cohérence des journaux du cycle de vie (ROLL_BACK)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : purge des collections temporaires des journaux du cycle de vie

+ **Type** : bloquant

+ **Statuts** :

  - OK : La purge s'est correctement déroulée (ROLL_BACK.OK = Succès de la mise en cohérence des journaux du cycle de vie)

  - FATAL : la purge n'a pas pu être réalisée suite à une erreur technique (ROLL_BACK.FATAL = Erreur fatale lors la mise en cohérence des journaux du cycle de vie)


Structure du Workflow
=====================

Le workflow actuel mis en place dans la solution logicielle Vitam est défini dans l'unique fichier "DefaultIngestWorkflow.json". Ce fichier est disponible dans /sources/processing/processing-management/src/main/resources/workflows.
Il décrit le processus d'entrée (hors Ingest externe) pour entrer un SIP, indexer les métadonnées et stocker les objets contenus dans le SIP.

D'une façon synthétique, le workflow est décrit de cette façon :


.. image:: images/workflow_ingest.png
        :align: center
        :alt: Diagramme d'état / transitions du workflow d'ingest



- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  * CHECK_SEDA (CheckSedaActionHandler.java) :

    + Test de l'existence du manifest.xml

    + Validation XSD du manifeste

    + Validation de la structure du manifeste par rapport au schéma par défaut fourni avec le standard SEDA v. 2.0.

    + Test de l'existence d'un fichier unique à la racine du SIP

    + Test de l'existence d'un dossier unique à la racine, nommé "Content" (insensible à la casse)


  * CHECK_HEADER (CheckHeaderActionHandler.java)

    + Test de l'existence du service producteur dans le bordereau

    + Contient CHECK_AGENT (CheckOriginatingAgencyHandler.java) :

      - Recherche l'identifiant du service producteur et du service versant dans le SIP

      - Vérification de la validité des services agents par rapport au référentiel des services agents présent dans la solution logicielle VITAM
     
    + Contient CHECK_CONTRACT_INGEST (CheckIngestContractActionHandler.java) :

      - Recherche l'identifier du contrat d'entrée dans le SIP

      - Vérification de la validité du contrat par rapport au référentiel de contrats présent dans la solution logicielle VITAM

    + Contient CHECK_IC_AP_RELATION, exécuté si un profil SEDA s'applique pour le SIP (CheckArchiveProfileRelationActionHandler.java) :

      - Vérification que le profil SEDA déclaré dans le contrat d'entrée et le même que celui déclaré dans le SIP

    + Contient CHECK_ARCHIVEPROFILE, exécuté si un profil SEDA s'applique pour le SIP (CheckArchiveProfileActionHandler.java) :

      - Vérification de la validité du manifeste par rapport au profil SEDA


  * CHECK_DATAOBJECTPACKAGE (CheckDataObjectPackageActionHandler.java)

    + Contient CHECK_MANIFEST_DATAOBJECT_VERSION (CheckVersionActionHandler.java) :

      - Vérification des usages et numéros de version des objets.

    + Contient CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

      - Comptage des objets (BinaryDataObject) dans le manifest.xml en s'assurant de l'absence de doublon, afin de vérifier que le nombre d'objets reçus est strictement égal au nombre d'objets attendus

      - Création de la liste des objets dans le workspace GUID/SIP/content/

      - Comparaison du nombre des objets contenus dans le SIP avec ceux définis dans le manifeste


    * Contient CHECK_MANIFEST (ExtractSedaActionHandler.java) :

      - Extraction des unités archivistiques, des BinaryDataObject, des PhysicalDataObject

      - Création des journaux du cycle de vie des unités archivistiques et des groupes d'objets

      - Vérification de la présence de cycles dans les arborescences des Units

      - Création de l'arbre d'ordre d'indexation

      - Extraction des métadonnées contenues dans le bloc ManagementMetadata du manifeste pour le calcul des règles de gestion

      - Vérification du GUID de la structure de rattachement

      - Vérification de la cohérence entre l'unité archivistique rattachée et l'unité archivistique de rattachement

      - Vérification de l'absence de problèmes d'encodage dans le manifeste

      - Vérification que les objets ayant un groupe d'objets ne référencent pas directement les unités archivistiques

    * Contient CHECK_CONSISTENCY (CheckObjectUnitConsistencyActionHandler.java) :

      - Extraction des métadonnées des BinaryDataObject et PhysicalDataObject du manifest.xml et création de la MAP (table de concordance) des Id BinaryDataObject ou PhysicalDataObject / Génération GUID (de ces mêmes BinaryDataObject)

      - Extraction des unités archivistiques du manifest.xml et création de la MAP des id d'unités / Génération GUID (de ces mêmes unités archivistiques),

      - Contrôle des références dans les unités archivistiques des Id BinaryDataObject et PhysicalDataObject

      - Vérification de la cohérence objet/unité archivistique

      - Stockage dans le Workspace des BinaryDataObject, PhysicalDataObject et des unités archivistiques

- **Step 2** - STP_OG_CHECK_AND_TRANSFORME : Contrôle et traitements des objets / distribution sur LIST GUID/BinaryDataObject

  * CHECK_DIGEST (CheckConformityActionPlugin.java) :

    + Contrôle de l'objet binaire correspondant : la taille et l'empreinte du BinaryDataObject

    + Calcul d'une empreinte avec l'algorithme SHA-512 si l'empreinte du manifeste n'a pas été calculée avec cet algorithme


  * OG_OBJECTS_FORMAT_CHECK (FormatIdentificationActionPlugin.java):

    + Identification du format des BinaryDataObject

    + Vérification de l'existence du format identifié dans le référentiel des formats

    + Consolidation de l'information du format dans le groupe d'objet correspondant si nécessaire

- **Step 3** - STP_UNIT_CHECK_AND_PROCESS : Contrôle et traitements des units / distribution sur LIST GUID

  * CHECK_UNIT_schéma (CheckArchiveUnitschémaActionPlugin.java) :

    + contrôle de validité des champs des unités archivistiques

  * UNITS_RULES_COMPUTE (UnitsRulesComputePlugin.java) :

    + vérification de l'existence de la règle dans le référentiel des règles de gestion

    + calcul des échéances associées à chaque unité archivistique

- **Step 4** - STP_STORAGE_AVAILABILITY_CHECK : Préparation de la prise en charge / distribution REF GUID/SIP/manifest.xml

  * STORAGE_AVAILABILITY_CHECK (CheckStorageAvailabilityActionHandler.java) :

    + Calcul de la taille totale des objets à stocker

    + Contrôle de la taille totale des objets à stocker par rapport à la capacité des offres de stockage pour une stratégie et un tenant donnés

- **Step 5** - STP_OG_STORING : Rangement et indexation des objets

  * OBJ_STORAGE (StoreObjectGroupActionPlugin.java) :

    + Écriture sur l’offre de stockage des objets binaires et des groupes d'objets

  * OG_METADATA_INDEXATION (IndexObjectGroupActionPlugin.java) :

    + Indexation des métadonnées des groupes d'objets

- **Step 6** - STP_UNIT_METADATA : Indexation des unités archivistique

  * UNIT_METADATA_INDEXATION (IndexUnitActionPlugin.java) :

    + Transformation sous la forme Json des unités archivistiques et intégration du GUID Unit et du GUID des groupes d'objets

- **Step 7** - STP_OG_STORING : Rangement des métadonnées des objets

  * OG_METADATA_STORAGE (StoreMetaDataObjectGroupActionPlugin.java) :

    + Ecriture sur les offres de stockage des métadonnées des groupes d'objets

  * COMMIT_LIFE_CYCLE_OBJECT_GROUP (CommitLifeCycleObjectGroupActionHandler.java)

    + Enregistrement en base des journaux du cycle de vie des groupes d'objets

- **Step 8** - STP_UNIT_STORING : Rangement des unités archivistique / distribution sur LIST GUID/Units

  * UNIT_METADATA_STORAGE (StoreMetaDataUnitActionPlugin.java.java) :

    + Ecriture sur les offres de stockage des métadonnées des unités archivistiques

  * COMMIT_LIFE_CYCLE_UNIT (CommitLifeCycleUnitActionHandler.java)

    + Enregistrement en base des journaux du cycle de vie des unités archivistiques

- **Step 9** - STP_ACCESSION_REGISTRATION : Alimentation du registre des fonds

  * ACCESSION_REGISTRATION (AccessionRegisterActionHandler.java) :

    + Création/Mise à jour et enregistrement des collections AccessionRegisterDetail et AccessionRegisterSummary concernant les archives prises en compte, par service producteur

- **Step 10 et finale** - STP_INGEST_FINALISATION : Finalisation de l'entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :

    + Génération de l'ArchiveTransferReply.xml (quelque soit le statut du processus d'entrée, l'ArchiveTransferReply est obligatoirement généré)

    + Écriture de l'ArchiveTransferReply sur les offres de stockage

  * ROLL_BACK (RollBackActionHandler.java)

    + Purge des collections temporaires des journaux du cycle de vie
