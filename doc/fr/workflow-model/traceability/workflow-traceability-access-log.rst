Création de journal sécurisé des journaux des accès
###################################################


Introduction
============

Cette section décrit la sécurisation des journaux des accès aux binaires mis en place dans la solution logicielle Vitam. Contrairement aux autres sécurisations de journaux de cycles de vie ou du journal des opérations, celle-ci n’est pas utilisée au sein d’un workflow.

Sécurisation des journaux des accès (vision métier)
===================================================

Le processus de sécurisation des journaux des *accès aux binaires* consiste en la création d’un fichier *.log* contenant : un certain nombre d’informations comme la date des accès, l'ID du document récupéré, le contrat utilisé, l'AU donnant accès au binaire, et l'ID de la requête d'accès.

Ces logs sont un extrait des logs du moteur de stockage, sélectionnés entre deux intervalles de dates.

Lors de la copie du Moteur de stockage vers les Offres, les fichiers sont renomés pour utilisée en date de début la date de début de chaque fichier de log et en date de fin la date du traitement. 

Au niveau du journal des opérations, cette action est entièrement réalisée dans une seule étape 


* **Status** :

	* OK : sauvegarde des journaux d'accès (STORAGE_ACCESS_BACKUP = Sauvegarde des journaux des accès)
	* WARNING : Avertissement lors de la sauvegarde des journaux des accès(STORAGE_ACCESS_BACKUP.WARNING=Avertissement lors de la sauvegarde des journaux des accès )
	* KO : Échec de la sauvegarde des journaux des accès (STORAGE_ACCESS_BACKUP.KO=Échec de la sauvegarde des journaux des accès)



