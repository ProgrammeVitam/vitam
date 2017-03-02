Généralités
***********
Le rôle de l'ingest-external est de réaliser un upload d'un SIP provenant d'une application externe à vitam et 
de télécharger les fichiers sauvegardé au serveur après l'opération ingest (accusé de réception et seda).

Téléchargement standard  et test à blanc d'un SIP:
**************************************************

La procédure de upload d'un SIP via ingest-externe est le suivant :  

    - sauvegarder le fichier SIP temporaire dans le système
	- préparer logbook opération (START)   
	- scan le fichier SIP sauvegardé temporaire pour détecter des virus 
	- préparer logbook opération (FIN)

	- si le fichier n'est pas infecté : appel client ingest-internal  pour continuer le procédure de test à blanc (sans stockage des objets, sans indexations) ou le de dépôt en utilisant ingest-internal pour un dépôt dans la base VITAM.


Autres Fonctionnalités:
***********************
On peut également :
    - Relancer un processus workflow (production / test blanc) en pause :
           - En Mode étape par étape pour éxécuter l'etape suivante.
           - En Mode continu pour exécuter toutes les etapes.
        

    - Mettre en pause un processus workflow en cours d'exécution.
    - Annuler un processus workflow en cours d'exécution ou en pause.
		