Métadata
########

Utilisation
###########

Paramètres
**********


Le client
*********

Le client propose actuellement différentes méthodes : insert et selection des units, select des objectGroups.

Il faut ajouter la dependance au niveau de pom.xml

 		<dependency>
			<groupId>fr.gouv.vitam</groupId>
			<artifactId>metadata-client</artifactId>
			<version>${project.version}</version>
		</dependency>
		

.. code-block exemple :: java

	1. Créer le client métadata
	En deux étapes
	- chargement de la configuration en utilisant 
		MetaDataClientFactory.changeMode(new ClientConfigurationImpl(server, port));
		ou 
		MetaDataClientFactory.changeMode(ConfigurationFilePath);)
	- création du client
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        
	2. Accéder aux fonctionnalités différents
	le client métadata fournit les foncitonnalités suivantes : insérer un ArchiveUnit, 
	insérer un ObjectGroup et sélectionner un métadata (archiveUnit). Le détail de l'utilisation 
	de chaque fonctionnalité est ci-dessous. 

	2.1 Insérer des ArchiveUnits 
	 	try {
			JsonNode result= metadataClient.insertUnit(JsonNode insertQuery) 
		} catch (InvalidParseOperationException e) {
	            LOG.error("parsing error", e);
        	    throw e;
        	} catch (MetaDataExecutionException e) {
	            LOG.error("execution error", e);
        	    throw e;
        	} catch (MetaDataDocumentSizeException e) {
	            LOG.error("document size input error", e);
        	    throw e;
        	} catch (MetaDataAlreadyExistException e) {
	            LOG.error("data already exists error", e);
        	    throw e;
        	} catch (MetaDataNotFoundException e) {
	            LOG.error("not found parent/path error", e);
        	    throw e;
        	}
 
	Paramètre d'entrée est une requête DSL de type JsonNode, indiquant la requête sur la collection Unit. 
	Un exemple de la requête paramètrée est le suivant : 

		{
		  "$root" : [],	
		  "$queries": [{ "$path": "aaaaa" }],
		  "$filter": { },
		  "$data": { "_id": "value" }
		}

	Cette fonction retourne une réponse de type JsonNode contenant les informations : code de retour en cas d'erreur, 
	la requête effectuée sur la collection ... 

	2.1 Insérer des ObjectGroups
	try {
			JsonNode result= metadataClient.insertObjectGroup(JsonNode insertQuery) 
		} catch (InvalidParseOperationException e) {
	            LOG.error("parsing error", e);
        	    throw e;
        	} catch (MetaDataExecutionException e) {
	            LOG.error("execution error", e);
        	    throw e;
        	} catch (MetaDataDocumentSizeException e) {
	            LOG.error("document size input error", e);
        	    throw e;
        	} catch (MetaDataAlreadyExistException e) {
	            LOG.error("data already exists error", e);
        	    throw e;
        	} catch (MetaDataNotFoundException e) {
	            LOG.error("not found parent/path error", e);
        	    throw e;
        	}
 
	Paramètre d'entrée est une requête DSL de type JsonNode, indiquant la requête sur la collection ObjectGroup. 
	Un exemple de la requête paramètrée est le suivant : 
		{
		  "$root" : [],	
		  "$queries": [{ "$exists": "value" }],
		  "$filter": { },
		  "$data": { "_id": "objectgroupValue" }
		}
	Cette fonction retourne une réponse de type JsonNode contenant les informations : code de retour en cas d'erreur, 
	la requête effectuée sur la collection ... 

	2.3 Sélection des ArchiveUnits 

        try {
        
        // return JsonNode
            jsonNode = metaDataClient.selectUnits(
                accessModuleBean != null ? accessModuleBean.getRequestDsl() : "");

        } catch (InvalidParseOperationException e) {
            LOG.error("parsing error", e);
            throw e;
        } catch (MetadataInvalidSelectException e) {
            LOG.error("invalid select", e);
            throw e;
        } catch (MetaDataDocumentSizeException e) {
            LOG.error("document size problem", e);
            throw e;
        } catch (MetaDataExecutionException e) {
            LOG.error("metadata execution problem", e);
            throw e;
        } catch (IllegalArgumentException e) {
            LOG.error("illegal argument", e);
            throw new AccessExecutionException();
        } catch (Exception e) {
            LOG.error("exeption thrown", e);
            throw e;
        }

   2.4 Sélection d'un ObjectGroup 

        try { 
            JsonNode selectQuery;
            String objectGroupId;
        // return JsonNode
            jsonNode = metaDataClient.selectObjectGrouptbyId(selectQuery, objectGroupId);

        } catch (InvalidParseOperationException e) {
            LOG.error("parsing error", e);
            throw e;
        } catch (MetadataInvalidSelectException e) {
            LOG.error("invalid select", e);
            throw e;
        } catch (MetaDataDocumentSizeException e) {
            LOG.error("document size problem", e);
            throw e;
        } catch (MetaDataExecutionException e) {
            LOG.error("metadata execution problem", e);
            throw e;
        } catch (IllegalArgumentException e) {
            LOG.error("illegal argument", e);
            throw new AccessExecutionException();
        } catch (MetadataInvalidSelectException e) {
            LOG.error("invalid selection", e);
            throw new AccessExecutionException();
        } catch (Exception e) {
            LOG.error("exeption thrown", e);
            throw e;
        }   


