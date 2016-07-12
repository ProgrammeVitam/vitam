ingest-rest
############

Présentation
------------

|*Proposition de package : **fr.gouv.vitam.ingest.upload.rest**

Module utilisant le service REST avec Jersey pour charger SIP et faire l'appel des autres modules (workspace, processing et logbook, etc ...).

La logique technique actuelle est la suivante :

	1)lancement du serveur d'application en appelant le fichier ingest-rest.properties ( voir le document d'exploitation).
	
	2)Créer une méthode upload du fichier sip
	
		2.1) Appel du journal pour la création des opérations (suivi du SIP).
		
		2.2) Push SIP dans le workspace.
		
		2.3) Appel du processing (journalisation des opération).
		
		2.4) Fermeture de la page des opérations.

