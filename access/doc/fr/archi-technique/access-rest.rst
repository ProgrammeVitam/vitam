Access-rest
***********

Présentation
************

API REST appelées par le client access interne 

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
            final VitamServer vitamServer = startApplication(args);
            vitamServer.run();
        } catch (final VitamApplicationServerException exc) {
            LOGGER.error(exc);
            throw new IllegalStateException("Cannot start the Access Application Server", exc);
        }
    }


-AccessResourceImpl

classe controlleur REST

la classe contient actuellement une méthode : 
-getUnits()
//TODO (future itération , ajouter la méthode de modification des métadonnées ?)
 
 
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
 
 La méthode HTTP GET n'est pas compatible, on utilisera une méthode HTTP POST dont l'entête contiendra "X-HTTP-Method-GET" 
 
le controlleur REST appelle une api qui communique avec le moteur de données (accessModule, cf access-module.rst)