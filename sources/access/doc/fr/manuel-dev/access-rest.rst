Access-rest
############

Présentation
************

API REST EXT appelées par le client access external. Il y a un controle des paramètres (SanityChecker.checkJsonAll) transmis
avec ESAPI.

fr.gouv.vitam.access.external.rest
***********************************

-- AccessExternalRessourceImpl

Rest API
--------

-Unit

| GET https://vitam/access-external/v1/units
	récupérer la liste des units avec la filtre (le contenu de la requête)

| POST https://vitam/access-external/v1/units (with X-HTTP-METHOD-OVERRIDE GET)
	récupérer la liste des units avec la filtre (le contenu de la requête)

| PUT https://vitam/access-external/v1/units
	Mettre à jour la liste des units (non implémenté)

| PUT https://vitam/access-external/v1/units/unit_id
	Mettre à jour l'unit avec avec le contenu de la requête

| HEAD https://vitam/access-external/v1/units
	Vérifier l'existence d'un unit (non implémenté)

| GET https://vitam/access-external/v1/units/unit_id
	récupérer l'units avec la filtre (le contenu de la requête)

| POST https://vitam/access-external/v1/units/unit_id  (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer l'units avec la filtre (le contenu de la requête)

| GET https://vitam/access-external/v1/units/unit_id/object
	récupérer le group d'objet par un unit (le contenu de la requête)

| POST https://vitam/access-external/v1/units/unit_id/object (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer le group d'objet par un unit (le contenu de la requête)


-ObjectGroup

| GET https://vitam/access-external/v1/objects
	récupérer la liste des object group (non implémenté)

| POST https://vitam/access-external/v1/objects (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer la liste des object group (non implémenté)

| GET https://vitam/access-external/v1/objects/object_id
	récupérer une groupe d'objet avec la filtre (le contenu de la requête) et id

| POST https://vitam/access-external/v1/objects/objet_id (avec X-HTTP-METHOD-OVERRIDE GET)
	récupérer une groupe d'objet avec la filtre (le contenu de la requête) et id


-Accession Register

| POST https://vitam/access-external/v1/accession-register
	récupérer le registre de fond

| POST https://vitam/access-external/v1/accession-register/document_id
	récupérer le registre de fond avec la filtre (le contenu de la requête) et id

| POST https://vitam/access-external/v1/accession-register/document_id/accession-register-detail
	récupérer le détail du registre de fond avec la filtre (le contenu de la requête) et id


-- LogbookRessourceImpl

Rest API
--------

-Operation

| GET https://vitam/access-external/v1/operations
	récupérer tous les journaux de l'opéraion

| POST https://vitam/access-external/v1/operations (with X-HTTP-METHOD-OVERRIDE GET)
	récupérer tous les journaux de l'opéraion

| GET https://vitam/access-external/v1/operations/{id_op}
	récupérer le journal de l'opéraion avec la filtre (le contenu de la requête) et id

| POST https://vitam/access-external/v1/operations/{id_op} (with X-HTTP-METHOD-OVERRIDE GET)
	récupérer le journal de l'opéraion avec la filtre (le contenu de la requête) et id

-Cycle de vie

| GET https://vitam/access-external/v1/unitlifecycles/{id_lc}
	récupérer le journal sur le cycle de vie d'un unit avec la filtre (le contenu de la requête) et id

| GET https://vitam/access-external/v1/objectgrouplifecycles/{id_lc}
	récupérer le journal sur le cycle de vie d'un groupe d'objet avec la filtre (le contenu de la requête) et id



-- AdminManagementResourceImpl

Rest API
--------

-Format&Rule

| PUT https://vitam/admin-external/v1/collection_id
	vérifier le format ou la règle

| POST https://vitam/admin-external/v1/collection_id
	importer le fichier du format ou de la règle

| POST https://vitam/admin-external/v1/collection_id
	récupérer le format ou la règle

| POST https://vitam/admin-external/v1/collection_id/document_id
	récupérer le format ou la règle avec la filtre (le contenu de la requête) et id

-- AdminManagementExternalResourceImpl

Rest API
--------

-Contrat d'accès

| PUT https://vitam/admin-external/v1/accesscontract
	Mise à jour du contrat d'accès

-Contrat d'entrée

|PUT https://vitam/admin-external/v1/contract
Mise à jour du contrat d'entrès

- Profiles

| POST https://vitam/admin-external/v1/profiles
    Créer ou rechercher des profiles au format json (métadata). Le header X-Http-Method-Override pilote la décision entre la recherche et la création.

| PUT https://vitam/admin-external/v1/profiles
    Importer le profile au format rng ou xsd

| GET https://vitam/admin-external/v1/profiles
    Télécharger le profile au format rng ou xsd si le accept est un octet-stream sinon c'est une recherche de profiles au format json (métadata)

| GET https://vitam/admin-external/v1/profiles/profile_id
    Rechercher un profile avec son id (profile_id)

| POST https://vitam/admin-external/v1/profiles/profile_id
    Si X-Http-Method-Override égale à GET alors rechercher un profile avec son id (profile_id)

