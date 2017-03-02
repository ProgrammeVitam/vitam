Généralités
***********
Le rôle de l'ingest-internal est de réaliser un upload d'un SIP comme un InputStream, 
transféré  de l'ingest-interne, qui viens d'une application externe via l'ingest-externe et de transférer les objets 
du serveur de stockage à ingest-externe. 
La procédure de upload d'un SIP est le suivant :  

	- appeller le service journalisation logbook pour créer des log  
		
	- Pousser le document le SIP dans le workspace.
		
	- Appeller le service processing pour:
     
                     * Lancer un workflow de production en mode continu ou étape par étape (*).
                     * Lancer un workflow pour faire un test blanc en mode antinue ou etape par etape.Dans ce cas, on n'aura pas des unités archnivistiques et des groupes d'objet indexés, et on n'aura pas des objets stockés dans les offres de stockage(*).

	- Relancer un processus workflow en pause :
           - En Mode étape par étape pour éxcuter l'etape suivante.
           - En Mode Continu pour exécuter toutes les etapes.
        


    - Mettre en pause un processus workflow en cours d'exécution.
    - Annuler un processus workflow en cours d'exécution ou en pause.

(*) : L'ingest interne est capable de déteminer l'identifiant du workflow qui sera exécuté par le moteur workflow (processEngine) grâce à l'identifant du contexte.

A titre d'exemple : le contextid c'est la combinaison mode d'exécution: production ou test à blanc, utilisateur connecté et contrat.

