Métadata
########

Utilisation
=============

Paramètres
-----------


Le client
-----------

Le client propose actuellement différentes méthodes : insert et selection des units, select des objectGroups.

Il faut ajouter la dependance au niveau de pom.xml

.. sourcecode:: xml

    <dependency>
        <groupId>fr.gouv.vitam</groupId>
        <artifactId>metadata-client</artifactId>
        <version>${project.version}</version>
    </dependency>



Créer le client metadata
^^^^^^^^^^^^^^^^^^^^^^^^^

En deux étapes :

- chargement de la configuration en utilisant  une des méthodes suivantes :
	- MetaDataClientFactory.changeMode(new ClientConfigurationImpl(server, port));
	- MetaDataClientFactory.changeMode(ConfigurationFilePath);)``

- création du client
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        
Accéder aux fonctionnalités
^^^^^^^^^^^^^^^^^^^^^^^^^^^
	le client métadata fournit les foncitonnalités suivantes : insérer un ArchiveUnit, 
	insérer un ObjectGroup et sélectionner un métadata (archiveUnit). Le détail de l'utilisation 
	de chaque fonctionnalité est ci-dessous. 

Insérer des ArchiveUnits 
~~~~~~~~~~~~~~~~~~~~~~~~~


.. sourcecode:: java

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
	}
 
Paramètre d'entrée est une requête DSL de type JsonNode, indiquant la requête sur la collection Unit. 

Un exemple de la requête paramètrée est le suivant : 

.. sourcecode:: json

	{
		"$root" : [],	
		"$queries": [{ "$path": "aaaaa" }],
		"$filter": { },
		"$data": { "_id": "value" }
	}


Cette fonction retourne une réponse de type JsonNode contenant les informations : code de retour en cas d'erreur, 
la requête effectuée sur la collection ... 

Insérer des ObjectGroups
~~~~~~~~~~~~~~~~~~~~~~~~~~

	.. sourcecode:: java

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
 
Paramètre d'entrée est une requête :term:`DSL` de type JsonNode, indiquant la requête sur la collection ObjectGroup. 

Un exemple de la requête paramètrée est le suivant : 

.. sourcecode:: json

    {
        "$root" : [],	
        "$queries": [{ "$exists": "value" }],
        "$filter": { },
        "$data": { "_id": "objectgroupValue" }
    }


Cette fonction retourne une réponse de type JsonNode contenant les informations : code de retour en cas d'erreur,  la requête effectuée sur la collection ... 

Sélection des ArchiveUnits 
------------------------------

.. sourcecode:: java

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

Sélection d'un ObjectGroup
---------------------------

.. sourcecode:: java

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

