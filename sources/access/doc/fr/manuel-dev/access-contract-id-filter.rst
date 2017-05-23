Présentation
************
Un filter passée dans les headers, a été ajouté pour pouvoir interdire toute requête n'indiquant pas de headerAccessContratId
ou son contrat est inconnu sur ce tenant ou son contrat est invalide.

Classe de filtre
****************
Une classe de filtre a été ajoutée :  
 
AccessContratIdContainerFilter : 
On vérifie la présence du header X_ACCESS_CONTRAT_ID dans la requête, sinon, une réponse UNAUTHORIZED (code 401) sera retournée.
Ensuite, on vérifie l'existence et la validité du contrat avec id de X_ACCESS_CONTRAT_ID, 
sinon, une réponse une réponse UNAUTHORIZED (code 401) sera retournée.

Implémenter des filters
***********************
Le filtre sera ajouté dans registerInResourceConfig de serveur application de access internal

.. code-block:: java

resourceConfig.register(new AccessInternalResourceImpl(getConfiguration()))
                .register(new LogbookInternalResourceImpl())
                .register(AccessContratIdContainerFilter.class);