Introduction
#############

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.metadata**

Itération 4
-----------
6 sous-modules pour le metadata. Dans metadata (parent).


| - vitam-metadata-api :  Classes et exception, model communes aux différents modules
| - vitam-metadata-builder: module pour creer les objets des réquetes select, update,insert etc..
| - vitam-metadata-client: module client pour metadata (units, groupe d'objets ...)
| - vitam-metadata-core :  
| - vitam-metadata-parser : module client pour parser les réqutes Jsons.
| - vitam-metadata-rest :  

Modules - packages
------------------

|  metadata
|     /metadata-api
|        fr.gouv.vitam.api
		 fr.gouv.vitam.api.config
		 fr.gouv.vitam.api.exception
		 fr.gouv.vitam.api.model
| 
|     /metadata-builder
|        fr.gouv.vitam.builder.request
|        fr.gouv.vitam.builder.request.construct
|        fr.gouv.vitam.builder.request.construct.action
|        fr.gouv.vitam.builder.request.construct.configuration
|        fr.gouv.vitam.builder.request.construct.query
|        fr.gouv.vitam.builder.request.exception
|       
|     /metadata-client
|       fr.gouv.vitam.client

|	 /metadata-core
|		fr.gouv.vitam.core.database.collections
|		fr.gouv.vitam.core.database.configuration
|		fr.gouv.vitam.core.utils
|     
	/metadata-parser
|     fr.gouv.vitam.parser.request.construct.query
|	  fr.gouv.vitam.parser.request.parser.action
|     fr.gouv.vitam.parser.request.parser
	  fr.gouv.vitam.parser.request.parser.query
|     
	/metadata-rest
|     fr.gouv.vitam.metadata.rest
|    