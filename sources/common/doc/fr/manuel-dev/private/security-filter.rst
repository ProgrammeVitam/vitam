Présentation
************
Nous proposons le filtre de sécurité qui permet de contrôler les requêtes vers vitam pour 
éviter les vulnaribilités et faille de sécurité. Le fitre sera contrôler : 
- le Header de la requête 
- le Parameter URI
- le Body   
 
 
Classes de filtres
******************
Nous proposons trois filtres différents dans les classe comme suits :  
 
- SanityCheckerCommonFilter.class : le filtre commmun pour contrôler le header, parametre URI 
et ceux de la requête. Ce filtre intègre aussi le contrôle XSS au niveau des header. 
   
- SanityCheckerInputStreamFilter.class : filter body de type Inputstream

- SanityCheckerJsonFilter.class : filtre body de type Json

 La logique est fait une contrôle et si c'est KO, une réponse de status 412 (PRECONDITION_FAILED)
 sera retourné. 
 
Implémenter des filters
***********************
Le filtre sera ajouté dans registerInResourceConfig de chaque serveur application sous le syntaxe par example 
        serviceRegistry.register(AccessInternalClientFactory.getInstance())
            .register(SanityCheckerCommonFilter.class)

Appliquer le filtre pour Vitam
******************************
- le filtre commun SanityCheckerCommonFilter sera appliqué pour les modules suivants : 
AccessExternal, IngestExternal, Workspace, Metadata

- le filtre body Json SanityCheckerJsonFilter  et body Inputstream SanityCheckerInputStreamFilter seront appliqué pour les modules AccessExternal,
IngestExternal, Metadata


            