ingest-rest
############

Présentation
------------

|*Proposition de package : **fr.gouv.vitam.ingest.upload.rest**

Module utilisant le service REST avec Jersey pour charger SIP et faire l'appel des autres modules (workspace, processing et logbook, etc ...).

La logique technique actuelle est la suivante :

	1)lancement du serveur d'application en appelant le fichier ingest-rest.properties
	
	#Configuration upload sip
	ingest.core.port=8083
	ingest.core.rest.upload=ingest/upload
	ingest.core.status=ingest/status
	ingest.core.uploaded.dir=/vitam/data
	ingest.core.log.dir=/vitam/log

	# Configuration workspace
	ingest.core.workspace.client.protocol=http
	ingest.core.workspace.client.host=localhost
	ingest.core.workspace.client.uri=workspace/v1
	ingest.core.workspace.client.port=8085

	#Configuration processing
	ingest.core.processing.protocal=http
	ingest.core.processing.host=localhost
	ingest.core.processing.uri=workspace/v1
	ingest.core.processing.port=8084
	
	
	2)Créer une méthode upload du fichier sip
	
		2.1) Appel du journal pour la création des opérations (suivi du SIP)
		
		2.2) Push SIP dans le workspace.
		
		2.3) Appel du processing (journalisation des opération)
		
		2.4) Fermeture de la page des opérations.

