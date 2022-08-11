collect-rest
#############

Présentation
************

A faire

Packages:
*********

**fr.gouv.vitam.collect.dto** : contient les objets entrants et sortants du service web d'application.

**fr.gouv.vitam.collect.exception** : classes d'exceptions gérées par le module collecte.

**fr.gouv.vitam.collect.helpers** : classes gérant la structure des objets en interne du module collecte.

**fr.gouv.vitam.collect.model** : classes interne du module collecte.

**fr.gouv.vitam.collect.repository** : classes pour mapper les objets de la base de données.

**fr.gouv.vitam.collect.resource** : classe du controlleur REST qui contient les endpoints.

**fr.gouv.vitam.collect.server** : classes de lancement du serveur d'application.

**fr.gouv.vitam.collect.service** : classes fonctionnelles.

fr.gouv.vitam.collect.resource
**********************************

Rest API
--------

| https://vitam/collect/v1/transactions
| https://vitam/collect/v1/units
| https://vitam/collect/v1/objects/{usage}/{version}
| https://vitam/collect/v1/objects/{usage}/{version}/binary
| https://vitam/collect/v1/transactions/close
| https://vitam/collect/v1/transactions/send

-TransactionResource.java
##########################

classe controlleur REST
La classe contient actuellement 6 méthodes :

1. initTransaction()

.. code-block:: java

   @Path("/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_CREATE, description = "Créer une transaction")
    public Response initTransaction(TransactionDto transactionDto) {

  ....

2. uploadArchiveUnit()

	Ajouter une unité archivistique

 .. code-block:: java

   	@Path("/transactions/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_UNIT_CREATE, description = "Créer une unité archivistique")
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {

     ...

3. uploadObjectGroup()

    ajouter un object group a une unité archivistique
	NB : the post X-Http-Method-Override header

.. code-block:: java

  @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_OBJECT_UPSERT, description = "ajouter ou modifier un objet group")
    public Response uploadObjectGroup(@PathParam("unitId") String unitId,
                                      @PathParam("usage") String usageString,
                                      @PathParam("version") Integer version,
                                      ObjectGroupDto objectGroupDto) {
  ...

4. upload()

	méthode pour uploader un binaire

.. code-block:: java

 	@Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_BINARY_UPSERT, description = "ajouter ou modifier un binaire")
    public Response upload(@PathParam("unitId") String unitId,
                           @PathParam("usage") String usageString,
                           @PathParam("version") Integer version,
                           InputStream uploadedInputStream) throws CollectException {
   ...

5. closeTransaction()

  fermeture de la transaction

.. code-block:: java

  @Path("/transactions/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_CLOSE, description = "Fermer une transaction")
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
    ...

6. generateAndSendSip()

	génerer un SIP et l'envoyer a Vitam


.. code-block:: java

 	@Path("/transactions/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission =  TRANSACTION_SEND, description = "Envoyer une transaction")
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {
     ...