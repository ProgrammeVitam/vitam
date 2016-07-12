Vérifier SEDA
############################

Algorithme
*******************

1. Vérifier la validation du seda (SedaUtils->checkSedaValidation)
	
	1. Vérifier l'existance de manifest.xml (SedaUtils->checkExistenceManifest)
	
	2. Valider manifest.xml en utilisant XSD (ValidationXsdUtils->checkWithXSD et getSchema)
	
2. Vérifier le nombre de BinaryDataObject (CheckObjectsNumberActionHandler)
    * Si le nombre de BinaryDataObject dans manifest.xml n'est pas égal à le nombre dans workspace
    * Lister toutes les objets numériques non référencés (CheckObjectsNumberActionHandler->foundUnreferencedDigitalObject)
	
3. Récupérer toutes les informations des BinaryDataObject (SedaUtils->getBinaryObjectInfo)
   
   * En parcourant manifest.xml, récupère les informations des BinaryDataObject
   * En type map(ID de BinaryDataObject, BinaryObjectInfo)
   * BinaryObjectInfo inclut id, uri, version, empreint, type d'empreint ...

4. Vérifier les versions de BinaryDataObject
	
	1. Créer la liste de version de manifest.xml (SedaUtils->manifestVersionList)
	
	2. Comparer la liste avec le fichier version.conf (SedaUtils->compareVersionList)
		* S'il y a la version invalide, stocker dans une liste de version invalide.
		* Si la liste de version invalide n'est pas vide, handler retourne la réponse avec statut "Warning".
	
	3. Journalisation de l'action CheckVersion

5. Vérifier les empreintes de BinaryDataObject

	1. Récupération d'empreinte du GUID/objects/SIP/content/<uri_correspondent> (WorkspaceClient->computeObjectDigest)
	
	2. Créer la liste d'empreinte de manifest.xml 
	
	3. Comparer les empreintes (SedaUtils->compareDigestMessage)
		* S'il y a la version invalide, stocker dans une liste de version invalide.
		* Si la liste de version invalide n'est pas vide, handler retourne la réponse avec status "Warning".
		
	4. Journalisation de l'action CheckConformity