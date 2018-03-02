Filtre Contrat d'accès
######################

Un filtre passé dans les headers, a été ajouté pour pouvoir interdire toute requête n'indiquant pas de header AccessContratId
ou son contrat est inconnu sur ce tenant ou son contrat est invalide.

L'exécution du filtre (vérification de la présence du Header + validation) est effectué dans le module Access Internal.

Classe de filtre
****************
Une classe de filtre a été ajoutée :  
 
AccessContractIdContainerFilter : 
On vérifie la présence du header X_ACCESS_CONTRAT_ID dans la requête, sinon, une réponse UNAUTHORIZED (code 401) sera retournée.
Ensuite, on vérifie l'existence et la validité du contrat avec id de X_ACCESS_CONTRAT_ID, 
sinon, une réponse une réponse UNAUTHORIZED (code 401) sera retournée.

Implémenter des filters
***********************
Le filtre sera ajouté dans la construction du serveur application (BusinessApplication) de access internal.

.. code-block:: java

   singletons = new HashSet<>();
   // some code
   singletons.addAll(commonBusinessApplication.getResources());
   // some code
	singletons.add(new AccessContractIdContainerFilter());

