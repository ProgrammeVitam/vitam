Global Unique Identifier (GUID) pour Vitam
##########################################

Spécifier ProcessId
*******************

Pour surcharger/spécifier le processId, qui par défaut prend la valeur du PID du processus Java, il faut utiliser la property suivante:
 
.. code-block:: bash
 
    -Dfr.gouv.vitam.processId=nnnnn
 
Où nnnnn est un nombre entre 0 et 2^22 (4194304).

GUID Factory
************

Usage:

Il faut utiliser le helper approprié en fonction du type d'objet pour lequel on souhaite créer un GUID.

Pour la partie interne Vitam
============================

* Obligatoire en **Interne Vitam**: le ServerIdentity doit être initialisé (inutile en mode client Vitam)

.. code-block:: java

    ServerIdentity.getInstance.setFromMap(Map);
    ServerIdentity.getInstance.setFromPropertyFile(File);
    ServerIdentity.getInstance.setName(String).setRole(String).setPlatformId(int);


* Pour un Unit et son Unit Logbook associé : 

.. code-block:: java

    GUID unitGuid = GUIDFactory.newUnitGUID(tenantId);
 
 
* Pour un ObjectGroup et son ObjectGroup Logbook associé : 

.. code-block:: java

    GUID objectGroupGuid = GUIDFactory.newObjectGroupGUID(tenantId);
    // or
    GUID objectGroupGuid = GUIDFactory.newObjectGroupGUID(unitParentGUID);

* Pour un Object et son Binary object associé : 

.. code-block:: java

    GUID objectGuid = GUIDFactory.newObjectGUID(tenantId);
    // or
    GUID objectGuid = GUIDFactory.newObjectGUID(objectGroupParentGUID);

* Pour une Opération (process): 

.. code-block:: java

    GUID operationGuid = GUIDFactory.newOperationIdGUID(tenantId);

* Pour un Request Id (X-Request-Id) : 

.. code-block:: java

    GUID requestIdGuid = GUIDFactory.newRequestIdGUID(tenantId);

* Pour un SIP / Manifest / Seda like informations Id: 

.. code-block:: java

    GUID manifestGuid = GUIDFactory.newManifestGUID(tenantId);

* Pour un Logbook daily Id (Operation, Write): 

.. code-block:: java

    GUID writeLogbookGuid = GUIDFactory.newWriteLogbookGUID(tenantId);

* Pour un storage operation Id: 

.. code-block:: java

    GUID storageOperationGuid = GUIDFactory.newStorageOperationGUID(tenantId);
    
* Pour savoir si un GUID est par défaut associé à une Règle WORM :

.. code-block:: java

    GUID storageOperationGuid.isWorm();


Pour la partie interne et public Vitam
======================================

* Pour récupérer un GUID depuis sa réprésentation :
  
.. code-block:: java

    GUID guid = GUIDReader.getGUID(stringGuid);
    GUID guid = GUIDReader.getGUID(byteArrayGuid);
    
Où le "stringGuid" peut être dans sa forme BASE16 / BASE32 / BASE64 ou ARK.
  


Attention
*********

* Personne ne devrait utiliser les helpers constructeurs directs (newUuid).
  * Ces méthodes sont réservées à des usages spéciaux futurs non encore définis.
