Introduction
*******************

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.metadata**

Itération 4
-----------
4 sous-modules pour le metadata. Dans metadata (parent).

| - vitam-metadata-api :  Classes et exception communes aux différents modules
| - vitam-metadata-builder: module pour creer les objets des réquetes select, update,insert etc..
| - vitam-metadata-client: module client pour metadata (units, groupe d'objets ...)
| - vitam-metadata-parser : module client pour parser les réqutes Jsons.

Modules - packages
------------------

|  metadata
|     /metadata-api
|        fr.gouv.vitam.api
|        fr.gouv.vitam.config
|        fr.gouv.vitam.exception
|        fr.gouv.vitam.model
|     /metadata-builder
|        fr.gouv.vitam.builder.request
|        	fr.gouv.vitam.builder.request.construct
|        		fr.gouv.vitam.builder.request.construct.action
|        		fr.gouv.vitam.builder.request.construct.configuration
|        		fr.gouv.vitam.builder.request.construct.query
|        	fr.gouv.vitam.builder.request.exception
|       
|     /metadata-client
|       fr.gouv.vitam.client
|     /metadata-parser
|     fr.gouv.vitam.parser.request
|     	fr.gouv.vitam.parser.request.construct.query
|     	fr.gouv.vitam.parser.request.parser
|     /metadata-rest
|     fr.gouv.vitam.metadata.rest
|    

