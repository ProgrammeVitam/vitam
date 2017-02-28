Présentation
************
Un filtre sur la valeur du tenant, passée dans les headers, a été ajouté pour pouvoir interdire toute requête n'indiquant pas de tenant, ou indiquant un tenant invalide. 

 
Classe de filtre
****************
Une classe de filtre a été ajoutée :  
 
TenantFilter : on vérifie la présence du header X-Tenant-Id dans la requête. Ensuite, on s'assure que la valeur transmise est bien un Integer. 
Le contrôle est effectué, s'il est KO (tenant non valide), une réponse PRECONDITION_FAILED (code 412) sera retournée. 
 
On vérifie ensuite la cohérence du X-Tenant-Id dans la requête, par rapport à la liste des tenants disponibles dans VITAM.
Le contrôle est effectué, s'il est KO (tenant non présent dans la liste des tenants), une réponse UNAUTHORIZED (code 401) sera retournée.  
 
Ajout du filtre
***************
Le filtre est ajouté dans setFilter(ServletContextHandler context) de chaque serveur d'application :
.. code-block:: java


        // chargemenet de la liste des tenants de l'application
        JsonNode node = JsonHandler.toJsonNode(getConfiguration().getTenants());
        context.setInitParameter(GlobalDataRest.TENANT_LIST, JsonHandler.unprettyPrint(node));
        context.addFilter(TenantFilter.class, "/*", EnumSet.of(
            DispatcherType.INCLUDE, DispatcherType.REQUEST,
            DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));

Modules Vitam impactés
**********************
Le filtre sera appliqué pour les modules AccessExternal et IngestExternal.


            