Généralités
***********
Le rôle de l'ingest-internal est de réaliser un upload d'un SIP comme un InputStream, 
transféré  de l'ingest-interne, qui viens d'une application externe via l'ingest-externe. 
La procédure de upload d'un SIP est le suivant :  

	- appeller le service journalisation logbook pour créer des log  
		
	- Pousser le document le SIP dans le workspace.
		
	- Appeller le service processing pour les opérations de workflow (y compris la journalisation des opérations).
		
