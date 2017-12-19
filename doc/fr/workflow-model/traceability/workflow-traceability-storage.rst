Création de journal sécurisé des journaux des écritures sécurisés
#################################################################

Introduction
============

Cette section décrit la sécurisation des journaux des écriture mis en place dans la solution logicielle Vitam. Contrairement aux autres sécurisations de journaux de cycles de vie ou du journal des opérations, celle-ci n'est pas utilisée au sein d'un workflow.

Sécurisation des journaux des écritures (vision métier)
=======================================================

Le processus de sécurisation des journaux des écritures consiste en la création d'un fichier .zip contenant :

	- Des logs des journaux sécurisés (logFile.log). Ces logs comportent un certain nombre d'informations comme la date des écritures, l'empreinte des fichiers concernés, le tenant, l'adresse des offres...

	Ces logs sont un extrait des logs du moteur de stockage, sélectionnés entre deux intervalles de dates.

	- Un fichier d'information décrivant le périmètre du fichier des logs des journaux sécurisés associé : date de début et date de fin définissant l'intervalle de sélection des logs à sécuriser, ainsi que l'empreinte du fichier logFile et la date de création du .zip

Au niveau du journal des opérations, cette action est entièrement réalisée dans une seule étape (STP_STORAGE_SECURISATION)

* **Status** :

	* OK : le tampon d'horodatage est calculé (STP_STORAGE_SECURISATION.OK=Succès du processus de sécurisation du journal des écritures)
	* KO : pas de cas KO
	* FATAL : une erreur technique est survenue lors de l'horodatage (STP_STORAGE_SECURISATION.FATAL=Erreur fatale lors du processus de sécurisation du journal des écritures)

