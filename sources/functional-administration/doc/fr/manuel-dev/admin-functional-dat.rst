DAT : module functional-administration
#######################################

Ce document présente l'ensemble du manuel développement concernant le développment du module
functional-administration qui identifie par la user story #71, qui contient :

- modules & packages
- classes métiers

--------------------------


Modules et packages
====================

functional-administration

- functional-administration-common : contenant des classes pour des traitements communs concernant le format référentiels, l'opération auprès de la base de données

- functional-administration-format : fournir des traitements de base pour les formats référentiels de VITAM

	- functional-administration-format-api  : définitions des APIs
	- functional-administration-format-core : implémentations des APIs
	- functional-administration-format-import

- functional-administration-rule : fournir des traitements de base pour la gestion de règles administratives

	- functional-administration-rule-api  : Définition des APIs
	- functional-administration-rule-core : Impélmentation des APIs

- functional-administration-accession-register : fournir des traitements de base pour la gestion des registres de fonds

	- functional-administration-accession-register-core : Impélmentation des traitements des registres de fonds

- functional-administration-rest   : le serveur REST de functional-administration qui donnes des traitement sur les traitements de format référentiel et gestion de règles administratives.
- functional-administration-client  : client functional-administration qui sera utilisé par les autres modules pour les appels de traitement sur le format référentiel & gestion de règles.
- functional-administration-contract	: fournis les traitements de base pour les contrat d'accès et les contrat d'entrées
- functional-administration-profile	: fournis les traitements de base pour les profile.
- functional-administration-context  : fournis les traitements de base pour les contextes

Classes métiers
================

Dans cette section, nous présentons quelques classes principales dans des modules/packages
abordés ci-dessus.

functional-administration-common
---------------------------------

fr.gouv.vitam.functional.administration.common

- FileFormat.java : 

une extension de VitamDocument définissant le référentiel des formats.

- ReferentialFile.java : 

interface définissant des opérations liées au référentiel des format : importation du fichier PRONOM, vérificaton du fichier PRONOM soumis, recherche d'un format existant et suppression du référentiel des formats.

- IngestContract.java : 

Le modèle de données des contracts d'entrée, ce modèle étend VitamDocument.

- AccessContract.java :

Le modèle de données des contracts d'accès, ce modèle étend VitamDocument.

- ManagementContract.java :

Le modèle de données des contracts de gestion, ce modèle étend VitamDocument.

- Profile.java : 

Le modèle de données des profiles, ce modèle étend VitamDocument.

- Context.java : 

Le modèle de données des contextes, ce modèle étend VitamDocument.


fr.gouv.vitam.functional.administration.common.embed
ProfileFormat.class: Une enum embeded dans le profile qui sert à définir le format du fichier profile (xsd, rng)
ProfileStatus.class: Une enum embeded dans le profile qui sert à définir le status (ACTIVE, INACTIVE)


fr.gouv.vitam.functional.administration.common.exception : définir des exceptions concernant de opération sur le
référentiel des formats

fr.gouv.vitam.functional.administration.common.server
les classe de traitement auprès de la base de données mongodb pour les opérations de référentiel de format.

- FunctionalAdminCollections.java : 

définir la collection dans mongodb pour des données de formats référentiels

- MongoDbAccessReferential.java : 

interface définissant des opérations sur le format de fichier auprès de la base mongodb: insert d'une base de PRONOM, delete de la collection, recherche d'un format par son Id dans la base,recherche des format par conditions

- MongoDbAccessAdminImpl.java : 

une implémentation de l'interface MongoDbAccessReferential en extension le traitementMongoDbAccess commun pour mongodb

functional-administration-format
---------------------------------

	+ functional-administration-format-api
	+ functional-administration-format-core

	- PronomParser.java : le script de traitement permettant de de récupérer l'ensemble de format en format json depuis d'un fichier PRONOM stantard en format XML contient des différents formats référentiels
	- ReferentialFormatFileImpl.java : implémentation de base des opération sur le format référentiel de fichier à partir d'un fichier PRONOM jusqu'à la base MongoDB.

	+ functional-administration-format-import

functional-administration-rest
-------------------------------

- AdminManagementResource.java : définir des ressources différentes pour le serveur REST functional-administration
- AdminManagementApplication.java : créer & lancer le serveur d'application avec une configuration
- ContractResource.java : Définir l'endpoints de l'api rest des contrats (entrée et accès)
- ProfileResource.java : Définir l'endpoint de l'api rest du profile
- ContextResource.java : Définir l'endpoint de l'api rest du contexte

functional-administration-client
---------------------------------

- AdminManagementClientRest.java : créer le client de et des fonctionnalités en se connectant au serveur REST
- AdminManagementClientMock.java : créer le client et des fonctionnalités en se connectant au mock de serveur

functional-administration-rules
--------------------------------

	+ functional-administration-rules-api
	+ functional-administration-rules-core

	- RulesManagerParser.java :permett de de parser le fichier de référentiel de règle de gestion d'extension .CSV et récupérer le contenu en ArrayNode
	- RulesManagerFileImpl.java : implémentation de base des opération sur les paramètres de référentiel de regle de gestion à partir de l'array Node générer après le parse de CSV File jusqu'à la base MongoDB.

      Le contrôle au niveau de RulesManagerFileImpl de fichier CSV a été mis à jour .

      Définition d'un référentiel valide en se basant sur les critères ci_dessous :


      Chaque RuleId doit être UNIQUE dans le référentiel
        RuleType doit être dans l'énumération suivante, non sensible à la casse : (AppraisalRule, AccessRule, StorageRule, DisseminationRule, ClassificationRule, ReuseRule)
        RuleDuration :

           * Depuis le fichier CSV, peut être un entier positif ou nul ou "unlimited" (insensible à la casse). La valeur réelle de l'enregistrement dans la collection est laissée à la discrétion des équipes de développements (ex "-1" si on veut garder une valeur numérique)
           * Permettre les manipulations sur des nombres (plus grand que.. plus petit que... Et calcul de date). Actuellement le champ est de type string, ce qui semble poser de nombreuses contraintes

           RuleMeasurement:

             RuleMeasurement doit être dans l'énumération suivante, non sensible à la casse : (year, month, day)
             RuleMeasurement peut aussi avoir comme valeur, non sensible à la casse "second". Cette demande est dans l'optique de la story #740 et n'a de sens qu'à des fins de tests.
             L'association de RuleDuration et RuleMeasurement doit être inférieure ou égale à 999 ans. (Mettre "15000 jours est donc autorisé)

             L'unité de mesure (RuleMeasurement) doit être écrite en français dans l'interface, comme c'est déjà le cas actuellement : année(s), mois, jour(s), seconde(s)

             Dans le cas des règles unlimited

             - La valeur que doit renvoyer l'API lorsque la règle a une durée 'unlimited' dépend du choix de design effectué pour l'enregistrement de la valeur 'unlimited'
             - Dans l'IHM standard, la date de fin doit être au choix marquée comme :

             * "Illimitée (date de début inconnue)" : dans le cas où la date de fin n'est pas connue car la startDate n'est pas connue
             * "Illimitée (règle à durée illimitée)" : dans le cas où la date de fin ne peut pas être calculée car la durée de la règle est 'unlimited'

           *  Les durées des règles du fichier en cours d'import doivent être strictement supérieures ou égales aux durées minimales
              demandées dans la configuration du tenant, pour cette catégorie de règle sur ce tenant
              (la durée de la règle est la valeur de la durée RuleDuration + l'unité de mesure RuleMeasurement.)

functional-administration-accession-register
---------------------------------------------

	+ functional-administration-accession-register-api
	+ functional-administration-accession-register-core

	- ReferentialAccessionRegisterImpl.java :implémentation de base des opération sur la collection registre de fond .
	
	permet de créer une collection registre de fond et de faire la recherche par Service Producteur et l'affichage de détaile.

functional-administration-contract
------------------------------------

fr.gouv.vitam.functional.administration.contract.api

- ContractService.java :   Interface définissant les différentes opérations sur les contrats (contrat d'accès et contrat d'entrée)

fr.gouv.vitam.functional.administration.contract.core

- AccessContractImpl.java : Classe d'implémentation pour la gestion des contrats d'accès
- ContractStatus.java : Enum pour les différents status des contrat d'accès et des contrat d'entrées
- ContractValidator.java : Interface fonctionnelle de validations des contrats
- GenericContractValidator.java : Interface fonctionnelle de validations des contrats
- IngestContractImpl.java : Classe d'implémentation pour la gestion des contrats d'entrées
- ManagementContractImpl.java : Classe d'implémentation pour la gestion des contrats de gestion


functional-administration-profile
----------------------------------

fr.gouv.vitam.functional.administration.profile.api

- ProfileService.java :   Interface définissant les différentes opérations sur les profiles.

fr.gouv.vitam.functional.administration.profile.api.impl

- ProfileServiceImpl.java :   Implémentation du service ProfileService.

fr.gouv.vitam.functional.administration.profile.core

- ProfileManager.java : Gère toutes les opérations du logbook et toutes les opérations de validation concernant les profiles. Lors de la validation, il vérifie (si déjà existence dans la base de données, champs obligatoires, fichiers au format xsd ou rng valides, ..).
- ProfileValidator.java : Interface fonctionnelle de validations des contrats

functional-administration-context
----------------------------------

fr.gouv.vitam.functional.administration.context.api

-ContextService.java : Interface définissant les différentes opérations sur les contextes

fr.gouv.vitam.functional.administration.context.core

-ContextServiceImpl.java : Implémentation du Service ContextService
-ContextValidator.java : Interface fonctionnelle de validations des contextes

functional-administration-security-profile
-------------------------------------------

fr.gouv.vitam.functional.administration.profile.api.impl

- SecurityProfileService.java : Service gérant les différentes opérations sur les profiles de sécurité.

fr.gouv.vitam.functional.administration.security.profile.core
