Introduction
***********

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.ingest**


Itération 4
-----------
modules pour l'ingest.

| - vitam-ingest-api :  Classes d'interface communes aux différents modules.
| - vitam-ingest-core : module lié au module rest d'ingest.
| - vitam-ingest-model : module lié aux fichiers modeles de status reponse, status resquest et upload response.
| - vitam-ingest-rest : module pour charger SIP.

Modules - packages
------------------

|  ingest
|     /ingest-api
|        fr.gouv.vitam.ingest.api.response
|        fr.gouv.vitam.ingest.api.upload
|        
|     /ingest-core
		 fr.gouv.vitam.ingest.upload.core
			
|     /ingest-model
		 fr.gouv.vitam.ingest.model
			
|     /ingest-rest
		fr.gouv.vitam.ingest.upload.rest
		

