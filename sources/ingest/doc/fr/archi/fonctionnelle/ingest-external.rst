Généralités
***********
Le rôle de l'ingest-external est de réaliser un upload d'un SIP provenant d'une application externe à vitam et 
de télécharger les fichiers sauvegardé au serveur après l'opération ingest (accusé de réception et seda).

La procédure de upload d'un SIP via ingest-externe est le suivant :  

    - sauvegarder le fichier SIP temporaire dans le système
	- préparer logbook opération (START)   
	- scan le fichier SIP sauvegardé temporaire pour détecter des virus 
	 et - préparer logbook opération (FIN) 	
	- si le fichier n'est pas infecté : appel client ingest-internal  pour continuer  
	 le procédure de dépôt en utilisant ingest-internal pour un dépôt dans la base VITAM.
		