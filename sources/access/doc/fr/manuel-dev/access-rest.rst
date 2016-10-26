Access-rest
***********

Présentation
************

API REST EXT appelées par le client access external. Il y a un controle des paramètres (SanityChecker.checkJsonAll) transmis
avec ESAPI.

fr.gouv.vitam.access.rest
*************************

Rest API
--------

-Unit

| GET https://vitam/access/v1/units
	récupérer la liste des units avec la filtre (le contenu de la requête)

| POST https://vitam/access/v1/units (with X-HTTP-METHOD-OVERRIDE GET)
	récupérer la liste des units avec la filtre (le contenu de la requête)

| PUT https://vitam/access/v1/units
	Mettre à jour la liste des units (non implémenté)

| PUT https://vitam/access/v1/units
	Mettre à jour l'unit avec avec le contenu de la requête

| HEAD https://vitam/access/v1/units
	Vérifier l'existence d'un unit (non implémenté) 

| GET https://vitam/access/v1/units/unit_id
	récupérer l'units avec la filtre (le contenu de la requête)

| POST https://vitam/access/v1/units/unit_id  (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer l'units avec la filtre (le contenu de la requête)

| GET https://vitam/access/v1/units/unit_id/object
	récupérer un units avec la filtre (le contenu de la requête)

| POST https://vitam/access/v1/units/unit_id/object (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer un units avec la filtre (le contenu de la requête)


-ObjectGroup

| GET https://vitam/access/v1/objects
	récupérer la liste des object group (non implémenté)

| POST https://vitam/access/v1/objects (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer la liste des object group (non implémenté)

| GET https://vitam/access/v1/objects/object_id
	récupérer une groupe d'objet avec la filtre (le contenu de la requête) et id

| POST https://vitam/access/v1/objects/objet_id (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer une groupe d'objet avec la filtre (le contenu de la requête) et id

	
