Access-rest
***********

Présentation
************

API REST appelées par le client access interne. Il y a un controle des paramètres (SanityChecker.checkJsonAll) transmis
avec ESAPI.

Packages:
**********

fr.gouv.vitam.access.external.config : contient les paramètres de configurations du service web d'application.
fr.gouv.vitam.access.external.model : classes métiers, classes implémentant le pattern DTO... .
fr.gouv.vitam.access.external.rest : classes de lancement du serveur d'application et du controlleur REST.

fr.gouv.vitam.access.external.rest
*************************

Rest API
--------

| https://vitam/access-external/v1/units
| https://vitam/access-external/v1/units/unit_id
| https://vitam/access-external/v1/objects
| https://vitam/access-external/v1/units/unit_id/object
| https://vitam/access-external/v1/accession-register
| https://vitam/access-external/v1/accession-register/document_id
| https://vitam/access-external/v1/accession-register/document_id/accession-register-detail
| https://vitam/access-external/v1/operations
| https://vitam/access-external/v1/operations/operation_id
| https://vitam/access-external/v1/unitlifecycles/lifecycle_id
| https://vitam/access-external/v1/objectgrouplifecycles/lifecycle_id
| https://vitam/admin-external/v1/collection_id
| https://vitam/admin-external/v1/collection_id/document_id

-AccessApplication.java
#######################
classe de démarrage du serveur d'application.

.. code-block:: java

    // démarrage
    public static void main(String[] args) {
        try {
            startApplication(args);
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    Dans le startApplication on effectue le start de VitamServer.
    Le join permet de lancer les tests unitaires et d'arreter le serveur.
    Dans le fichier de configuration, le paramètre jettyConfig est à
    paramétrer avec le nom du fichier de configuration de jetty.



-AccessResourceImpl.java
########################
classe controlleur REST

la classe contient actuellement 9 méthodes :

1. getUnits()
	 NB : the post X-Http-Method-Override header
 .. code-block:: java
 	@POST
    @Path("/units")
    public Response getUnits(String requestDsl,
        @HeaderParam("X-Http-Method-Override") String xhttpOverride) {

        ...

        try {
            if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
                queryJson = JsonHandler.getFromString(requestDsl);
                result = accessModule.selectUnit(queryJson.toString());

            } else {
                throw new AccessExecutionException("There is no 'X-Http-Method-Override:GET' as a header");
            }
            ....

2. createOrSelectUnits()
	récupère la liste des units avec la filtre
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode createOrSelectUnits() va appeler méthode getUnits()


 .. code-block:: java
 	@POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnits(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride)
     ...

3. getUnitById()
    récupère un unit avec son id
	NB : the post X-Http-Method-Override header
 .. code-block:: java
    @POST
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("id_unit") String id_unit) {
    ...

4. createOrSelectUnitById()
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode createOrSelectUnitById() va appeler méthode getUnitById()
 .. code-block:: java
 	@POST
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnitById(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("idu") String idUnit) {
     ...

5. updateUnitById()
    mise à jour d'un unit par son id avec une requête json

 .. code-block:: java
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(String queryDsl,
                                   @PathParam("id_unit") String id_unit) {
    ...

6. getObjectGroup()
	récupérer une groupe d'objet avec la filtre
    NB : the post X-Http-Method-Override header
 .. code-block:: java
 	@GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@PathParam("ido") String idObjectGroup, JsonNode queryJson)
     ...

7. getObjectGroupPost()
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode getObjectGroupPost() va appeler méthode getObjectGroup()
 .. code-block:: java
 	@POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupPost(@Context HttpHeaders headers,
        @PathParam("ido") String idObjectGroup, JsonNode queryJson)
     ...


8. getObject()
	récupérer le group d'objet par un unit
	NB : the post X-Http-Method-Override header
 .. code-block:: java
 	@GET
    @Path("/units/{ido}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObject(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
     ...


9. getObjectPost()
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode getObjectPost() va appeler méthode getObject()
 .. code-block:: java
 	@POST
    @Path("/units/{ido}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectPost(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
     ...

-LogbookExternalResourceImpl.java
#########################################
classe controlleur REST

la classe contient actuellement 6 méthodes :

1. getOperationById()
	récupère l'opération avec son id
	NB : the post X-Http-Method-Override header

 .. code-block:: java
 	@GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationById(@PathParam("id_op") String operationId) {
     ...

2. selectOperationByPost()
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode selectOperationByPost() va appeler méthode getOperationById()
 .. code-block:: java
 	@POST
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationByPost(@PathParam("id_op") String operationId,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
     ...

3. selectOperation()
     récupérer tous les journaux de l'opéraion
     NB : the post X-Http-Method-Override header

 .. code-block:: java
 	@GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperation(JsonNode query)
     ...

4. selectOperationWithPostOverride()
	NB : La méthode HTTP GET n'est pas compatible,
		 on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET"
	méthode selectOperationWithPostOverride() va appeler méthode selectOperation()

 .. code-block:: java
 	@POST
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationWithPostOverride(JsonNode query,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
     ...

5. getUnitLifeCycle()
	récupère le journal sur le cycle de vie d'un unit avec son id

 .. code-block:: java
 	@GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId)
     ...

6. getObjectGroupLifeCycle()
     récupère le journal sur le cycle de vie d'un groupe d'objet avec son id

 .. code-block:: java
 	@GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId)
     ...


-AdminManagementExternalResourceImpl.java
##########################################
classe controlleur REST

la classe contient actuellement 6 méthodes :
1. checkDocument()
	vérifier le format ou la règle

 .. code-block:: java
 	@Path("/{collection}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDocument(@PathParam("collection") String collection, InputStream document) {
     ...

2. importDocument()
	importer le fichier du format ou de la règle

 .. code-block:: java
	@Path("/{collection}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importDocument(@PathParam("collection") String collection, InputStream document) {
     ...

3. findDocuments()
     récupérer le format ou la règle

 .. code-block:: java
 	@Path("/{collection}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocuments(@PathParam("collection") String collection, JsonNode select) {
     ...

4. findDocumentByID()
     récupérer le format ou la règle avec la filtre avec son id

 .. code-block:: java
 	@POST
    @Path("/{collection}/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentByID(@PathParam("collection") String collection,
        @PathParam("id_document") String documentId) {
     ...

5. updateAccessContract()
   Mise à jour du contrat d'accès
   .. code-block:: java
    @PUT
      @Path("/accesscontract")
      @Consumes(MediaType.APPLICATION_JSON)
      @Produces(MediaType.APPLICATION_JSON)
       public Response updateAccessContract(JsonNode queryDsl) {
       ...

6. updateIngestContract()
     Mise à jour du contrat d'entrée
     .. code-block:: java
      @PUT
        @Path("/contract")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
         public Response updateIngestContract(JsonNode queryDsl) {
         ...
