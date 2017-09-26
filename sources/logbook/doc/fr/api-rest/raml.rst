API Rest
########

GET est utilisé pour l'équivalent de "Select" (possibilité d'utiliser POST avec X-Http-Method-Override=GET dans le Header)
Il y a deux méthodes de "Select", l'une est la sélection des operations par DSL, l'autre est la sélection des opérations par id.
    
GET, POST /logbooks
Request:
	Headers:
		X-Application-Id: (string)
		
		Content-Type: application/json
		Accept: application/json
		X-Http-Method-Override: GET
	Body:
		Media Type: application/json
Response:
	HTTP status code 200: Retourner la liste des opérations de Logbook sélectionnés
	HTTP status code 206: Retourner la liste des opérations de Logbook sélectionnésen cursor
	HTTP status code 401: Non autorisée, l'authentification par erreur
	HTTP status code 404: Introuvable, ressource demandée n'existe pas
	HTTP status code 412: Échec de précondition, certains prédicats sont incorrects, donc l'opération est impossible

GET, POST /logbooks/{idop}
Request:
	URI Parameters
    	idop: required (string)
	Headers
		X-Application-Id: (string)
		
		Content-Type: application/json
		Accept: application/json
		X-Http-Method-Override: GET
Response:
	HTTP status code 200: Retourner un document de logbook.
	HTTP status code 401: Non autorisée, l'authentification par erreur
	HTTP status code 404: Introuvable, ressource demandée n'existe pas
	HTTP status code 412: Échec de précondition, certains prédicats sont incorrects, donc l'opération est impossible


