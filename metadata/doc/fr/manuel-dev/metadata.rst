Logbook
#######

Utilisation
###########

Paramètres
**********


Le client
*********

Le client propose actuellement deux méthode : insert et la selection des units.

Il faut ajouter la dependance au niveau de pom.xml

 		<dependency>
			<groupId>fr.gouv.vitam</groupId>
			<artifactId>metadata-client</artifactId>
			<version>${project.version}</version>
		</dependency>
		

.. code-block exemple :: java

     metaDataClientFactory = new MetaDataClientFactory();
     
        metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());
        
        
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
   


