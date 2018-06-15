Workflow de mise à jour des unités archivistiques
#################################################

Introduction
============

Cette section décrit le processus permettant la mise à jour des unités archivistiques, hors mise à jour des règles de gestion déclenchée par une modification du référentiel des règles de gestion. Ce cas est détaillé dans un autre paragraphe de cette section. Le processus de mise à jour n'est pas configurable.

Processus de mise à jour des unités archivistiques (vision métier)
==================================================================

Le processus de mise à jour des unités archivistiques est lancé lors d'une mise à jour de n'importe quelle métadonnée d'une unité archivistique. On distingue cependant deux cas de modifications, liées à des droits, gérés via les contrats d'accès: 
Soit les utilisateurs auront le droit à la modification des métadonnées descriptives seulement, soit ils auront des droits pour modifier les métadonnées descriptives et liées aux règles de gestion. 

Un certain nombre d'étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent ce processus de mise à jour (clé et description de la clé associée dans le journal des opérations).



Mise à jour des unités archivistiques
======================================


Selon le type de modifications, une des deux étapes peut être déclenchée: 


Si le type de modification concerne une métadonnée descriptive: STP_UPDATE_UNIT (AccessInternalModuleImpl.java)
---------------------------------------------------------------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : la mise à jour de l'unité archivistique a bien été effectuée (STP_UPDATE_UNIT.OK = Succès du processus de mise à jour des métadonnées de l''unité archivistique)

  + KO : la mise à jour de l'unité archivistique n'a pas été effectuée en raison d'une erreur (STP_UPDATE_UNIT.KO = Échec du processus de mise à jour des métadonnées de l''unité archivistique)

  + FATAL : une erreur fatale est survenue lors de la mise à jour de l'unité archivistique (STP_UPDATE_UNIT.FATAL = Erreur fatale lors du processus de mise à jour des métadonnées de l''unité archivistique)





Si le type de modification concerne une métadonnée de règle de gestion et descriptive: STP_UPDATE_UNIT_DESC ((AccessInternalModuleImpl.java)
--------------------------------------------------------------------------------------------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : la mise à jour de l'unité archivistique a bien été effectuée (STP_UPDATE_UNIT_DESC.OK = Succès du processus de mise à jour des métadonnées descriptives de l''unité archivistique)

  + KO : la mise à jour de l'unité archivistique n'a pas été effectuée en raison d'une erreur (STP_UPDATE_UNIT_DESC.KO = Échec du processus de mise à jour des métadonnées descriptives de l''unité archivistique)

  + FATAL : une erreur fatale est survenue lors de la mise à jour de l'unité archivistique (STP_UPDATE_UNIT_DESC.FATAL = Erreur fatale lors du processus de mise à jour des métadonnées descriptives de l''unité archivistique)




Vérification des permissions de modifications UNIT_METADATA_UPDATE_CHECK_PERMISSION 
====================================================================================

Cette étape permet d'effectuer les contrôles sur le champ présent dans le contrat d'accès, qui autorise ou non la modification de métadonnées descriptives seulement ou bien des métadonnées descriptives et de gestion. 

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Succès de la vérification des droits de mise à jour des métadonnées de l'unité archivistique ((UNIT_METADATA_UPDATE_CHECK_PERMISSION OK = Succès du processus de mise à jour des métadonnées de l''unité archivistique)

  + KO : la mise à jour de l'unité archivistique n'a pas été effectuée en raison d'une erreur (STP_UPDATE_UNIT.KO = Échec du processus de mise à jour des métadonnées de l''unité archivistique)

  + FATAL : une erreur fatale est survenue lors de la mise à jour de l'unité archivistique (STP_UPDATE_UNIT.FATAL = Erreur fatale lors du processus de mise à jour des métadonnées de l''unité archivistique)




Vérification des règles de gestion UNIT_METADATA_UPDATE_CHECK_RULES (AccessInternalModuleImpl.java)
---------------------------------------------------------------------------------------------------

+ **Règle** : vérification des règles de gestion

+ **Type** : bloquant

+ **Statuts** :

    - OK : le rapport est généré (UNIT_METADATA_UPDATE_CHECK_RULES.OK = Succès de la génération du rapport d'analyse du rérentiel des règles de gestion)

    - KO : pas de cas KO

    - FATAL : une erreur fatale est survenue lors de la création du rapport (UNIT_METADATA_UPDATE_CHECK_RULES.FATAL = Erreur fatale lors de la génération du rapport d'analyse du référentiel des règles de gestion)



Indexation des métadonnées UNIT_METADATA_UPDATE (ArchiveUnitUpdateUtils.java)
-----------------------------------------------------------------------------

  + **Règle** : Indexation des métadonnées des unités archivistiques dans les bases internes de la solution logicielle Vitam, c'est à dire le titre des unités, leurs descriptions, leurs dates extrêmes, etc. C'est également dans cette tâche que le journal du cycle de vie est enregistré dans la base de données.

  + **Type** : bloquant

  + **Statuts** :

    - OK : Succès de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.OK=Succès de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

    - KO : Échec de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.KO=Échec de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

    - STARTED : Début de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.STARTED=Début de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

    - FATAL : Erreur fatale lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.FATAL=Erreur fatale lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

    - WARNING : Avertissement lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.WARNING=Avertissement lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques)


Enregistrement du journal du cycle de vie des unités archivistiques
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sécurisation en base des journaux du cycle de vie des unités archivistiques (avant cette étape, les journaux du cycle de vie des unités archivistiques sont dans une collection temporaire afin de garder une cohérence entre les métadonnées indexées et les journaux lors d'une entrée en succès ou en échec).

Cette action n'est pas journalisée.


Écriture des métadonnées de l'unité archivistique sur l'offre de stockage UNIT_METADATA_STORAGE (AccessInternalModuleImpl.java)
-------------------------------------------------------------------------------------------------------------------------------

  + **Règle** : Sauvegarde des métadonnées des unités archivistiques sur les offres de stockage en fonction de la stratégie de stockage.(Pas d'évènements stockés dans le journal de cycle de vie)

  + **Type** : bloquant

  + **Statuts** :

    - OK : la sécurisation des journaux du cycle de vie s'est correctement déroulée (UNIT_METADATA_UPDATE.OK = Succès de l'enregistrement des journaux du cycle de vie des groupes d'objets)

    - FATAL : une erreur fatale est survenue lors de la sécurisation du journal du cycle de vie (UNIT_METADATA_UPDATE.FATAL = Erreur fatale lors de l'enregistrement des journaux du cycle de vie des groupes d'objets)
