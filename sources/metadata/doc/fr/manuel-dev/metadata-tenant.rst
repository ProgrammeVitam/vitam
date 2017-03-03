Métadata-tenant
################

Les indices elasticsearch des unités archives et les groupes d'objets technique doivent être séparées par 
les tenants. Ces indices doivent être créées lors de démarrage du serveur grâce au fichier de configuration.  
Par exemple, pour metadata.conf on ajourte d'une ligne suivante :
 - tenants : [0, 1, 2]
 indiqué que le serveur va travailler sur différents tenants 0, 1 et 2. 


1. Valeur du tenant

La valeur de tenant est sauvegardé dans VitamSession et cette valeur sera récupérée par la fonction suivante.

.. code-block exemple :: java

VitamThreadUtils.getVitamSession().getTenantId()


Les indices sont créées basé sur les tenant pour chaque collection correspondante. 
- Pour collection des unités archives, les indices sont : unit_0, unit_1, ... pour la liste de tenant 0, 1 ... 
- Pour la collection des groupes d'objets technique, les indices sont objectgroups_0, objectgroups_1... 

2. Refactor

Pour permettre de réaliser les opérations sur les collections de métadata via l'elastichsearch par le tenant, 
nous faisons un refactor sur les classe DbRequest et ElasticsearchAccessMetadata.


2.1. ElasticsearchAccessMetadata
Les fonctions d'ajout des indices pour la collection, mise à jour des indices ou delete des indices sont fait par le paramètre tenantId. 

.. code-block exemple :: java

deleteIndex(final MetadataCollections collection, Integer tenantId)

addIndex(final MetadataCollections collection, Integer tenantId)

refreshIndex(final MetadataCollections collection, Integer tenantId)

addEntryIndexesBlocking(final MetadataCollections collection, final Integer tenantId, final Map<String, String> mapIdJson)
        
addEntryIndex(final MetadataDocument<?> document, Integer tenantId)

...


2.2. DbRequest

- Le tenantId est récupéré dans la session par VitamThreadUtils.getVitamSession().getTenantId() pour appliquer au executeQuery() 
pour exécuter une requête. 

.. code-block exemple :: java

	Result executeQuery(final RequestToAbstract requestToMongodb, final int rank, final Result previous) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        
        ...
     }
  
  
        
        