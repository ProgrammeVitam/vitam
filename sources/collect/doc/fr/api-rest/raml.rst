API Rest
########

GET est utilisé pour generer un ID de transaction
    
GET, /v1/transaction
Request:
	Headers:
		Content-Type: application/json
		Accept: application/json
	Body:
		Media Type: application/json
Response:
	HTTP status code 200: Retourner un Id de transaction Ex Format: 39057d48-0a5d-4fe8-a812-9502bc7cac82
	HTTP status code 401: Non autorisée, l'authentification par erreur
	HTTP status code 404: Introuvable, ressource demandée n'existe pas

POST /v1/transaction/{idTransaction}/upload
Request:
	URI Parameters
    	idTransaction: required (string)
	Headers
		Content-Type: application/json
		Accept: application/json
Response:
	HTTP status code 200: upload réussi.
	HTTP status code 401: Non autorisée, l'authentification par erreur
	HTTP status code 404: Introuvable, ressource demandée n'existe pas
	HTTP status code 400: id de la transaction non trouvée


