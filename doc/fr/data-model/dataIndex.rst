Objectif du document
====================

Ce document a pour but de décrire les index spécifiés pour les modèles de données utilisés dans MongoDB.

Base Logbooks
=============

Il s'agit des collections relatives aux journaux d'opérations et de cycles de vie des archives et des objets numériques.


Collection LogbookOperation
---------------------------

Les champs suivants sont indexés nativement dans MongoDB:
- \_tenant qui stocke l'identifiant du tenant
- evType qui stocke le type d'événement
- evTypeProc qui stocke le type de processus
- outcome qui stocke le statut de l'événement
- events.evType qui stocke le type d'événement d'un sous-event
- events.outcome qui stocke le statut de l'événement d'un sous-event


Collection LogbookLifeCycleUnit & LogbookLifeCycleObjectGroup
-------------------------------------------------------------

Les champs suivants sont indexés nativement dans MongoDB:
- \_tenant qui stocke l'identifiant du tenant
- evType qui stocke le type d'événement
- evIdProc qui stocke l'identifiant du processus associé à cet événement
- evTypeProc qui stocke le type de processus
- outcome qui stocke le statut de l'événement
- events.evType qui stocke le type d'événement d'un sous-event
- events.outcome qui stocke le statut de l'événement d'un sous-event
- events.evIdProc qui stocke l'identifiant du processus associé à ce sous-événement
 

Base Metadata
=============

Il s'agit des collections relatives aux métadonnées des archives et des objets numériques.

Collection Unit
---------------

Les champs suivants sont indexés nativement dans MongoDB:
- \_dom (devrait être nommé \_tenant)  qui stocke l'identifiant du tenant concerné
- ArchiveUnitProfile (devrait être nommé \_type) qui stocke le type d'archive
- \_up qui stocke les parents immédiats de l'archive
- \_og qui stocke l'object group lié s'il existe
- \_us qui stocke la liste de tous les parents (jusqu'à la racine) de l'archive
- \_uds qui stocke la liste de tous les parents (jusqu'à la racine) de l'archive avec la distance respective
- \_min qui stocke la profondeur minimale de l'archive
- \_max qui stocke la profondeur maximale de l'archive
- \_ops qui stocke l'ensemble des opérations auxquelles a participé l'archive

A l'étude, sont concernés aussi les éléments relatifs aux Règles de gestion : le couple (\_mgt.XxxxRule.Rules.Rule et \_mgt.XxxxRule.Rules.\_end) où Xxxxx parmi Storage, Appraisal, Access, Dissemination, Classification, Reuse.

Collection ObjectGroup
----------------------

Les champs suivants sont indexés nativement dans MongoDB:
- \_tenantId (devrait être nommé \_tenant)  qui stocke l'identifiant du tenant concerné
- \_type qui stocke le type d'objet numérique
- \_up qui stocke les parents immédiats de l'objet numérique
- \_ops qui stocke l'ensemble des opérations auxquelles a participé l'objet numérique
- \_qualifiers.Xxxx.versions.\_id qui stocke les identifiants des sous-objets pour les qualifiers PhysicalMaster, BinaryMaster, Dissemination, Thumbnail et TextContent
- \_qualifiers.Xxxx.versions.FormatIdentification.FormatId qui stocke les formats (PUID) des sous-objets pour les qualifiers BinaryMaster, Dissemination, Thumbnail et TextContent

A l'étude, sont concernés aussi les éléments relatifs aux empreintes : le couple (\_qualifiers.Xxxx.versions.MessageDigest et \_qualifiers.Xxxx.versions.Algorithm) où Xxxxx parmi BinaryMaster, Dissemination, Thumbnail et TextContent.

Base Masterdata
===============

Il s'agit des collections relatives aux référentiels utilisés par Vitam.

Collection FileRules
--------------------

La collection contient les règles de gestion instanciées par tenant et les champs suivants sont indexés nativement dans MongoDB:
- \_tenant (non existant à ce jour) qui stocke l'identifiant du tenant concerné
- RuleId qui contient l'identifiant de la règle de gestion
- RuleType qui contient le type de règle de gestion concerné (parmi Storage, Appraisal, Access, Dissemination, Classification, Reuse)
- le couple (\_tenant + RuleId) couple unique (quand \_tenant existera)


Collection FileFormat
---------------------

La collection contient le référentiel des formats Pronom et les champs suivants sont indexés nativement dans MongoDB:
- PUID (clef unique) contenant l'identifiant PRONOM unique


Collection AccessRegisterSummary
--------------------------------

La collection contient le registre des fonds dans sa version sommaire instanciées par tenant et les champs suivants sont indexés nativement dans MongoDB:
- \_tenant (non existant à ce jour) qui stocke l'identifiant du tenant concerné
- OriginatingAgency (clef unique) qui contient le service producteur associé à un ensemble d'archives versées

Colection AccessRegisterDetail
------------------------------

La collection contient le registre des fonds dans sa version détaillée instanciées par tenant et les champs suivants sont indexés nativement dans MongoDB:
- \_tenant (non existant à ce jour) qui stocke l'identifiant du tenant concerné
- OriginatingAgency qui contient le service producteur associé à un ensemble d'archives versées
- SubmissionAgency qui contient le service versant associé à un ensemble d'archives versées

