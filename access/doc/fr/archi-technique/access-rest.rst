Access-rest
***********

Présentation
************

API REST appelées par le client access interne. Il y a un controle des paramètres (SanityChecker.checkJsonAll) transmis
avec ESAPI.

Packages:
**********

fr.gouv.vitam.access.config : contient les paramètres de configurations du service web d'application.
fr.gouv.vitam.access.model : classes métiers, classes implémentant le pattern DTO... .
fr.gouv.vitam.access.rest : classes de lancement du serveur d'application et du controlleur REST.

fr.gouv.vitam.access.rest
*************************

Rest API
--------

| https://vitam/access/v1/units

-AccessApplication.java

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



-AccessResourceImpl

classe controlleur REST

la classe contient actuellement 4 méthodes :
-getStatus()
    récupère le status du controlleur REST

 .. code-block:: java
 @GET
 @Path("/status")
 public Response getStatus() {
     return Response.status(200).entity("OK_status").build();
 }

-getUnits()

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
            
 NB : the post X-Http-Method-Override header

-getUnitById()
    récupère un unit avec son id


 .. code-block:: java
    @POST
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(String queryDsl,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("id_unit") String id_unit) {
    ...



-updateUnitById()
    mise à jour d'un unit par son id avec une requête json

 .. code-block:: java
    @PUT
    @Path("/units/{id_unit}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(String queryDsl,
                                   @PathParam("id_unit") String id_unit) {
    ...

 La méthode HTTP GET n'est pas compatible, on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET" 
 
le controlleur REST appelle une api qui communique avec le moteur de données (accessModule, cf access-module.rst)

