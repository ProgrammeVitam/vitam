Introduction
************

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.storage**

Itération 6
-----------
3 sous-modules dans Storage (parent).

| - storage-driver : module décrivant l'interface du driver
| - storage-engine : module embarquant la partie core du storage (client et server)
| - storage-offers : module embarquant les différentes offres de stockage Vitam


Modules - packages Storage
--------------------------

|  storage
|     /storage-driver
|        fr.gouv.vitam.storage.driver

|     /storage-engine
|        /storage-engine-client
|           fr.gouv.vitam.storage.engine.client
|        /storage-engine-server
|           fr.gouv.vitam.storage.engine.server.spi
|           fr.gouv.vitam.storage.engine.server.rest
|           fr.gouv.vitam.storage.engine.server.distribution
|        /storage-engine-common
|           fr.gouv.vitam.storage.engine.common

|     /storage-offers
|        /storage-drivers
|           /workspace-driver
|              fr.gouv.vitam.storage.offers.workspace.driver
|        /workspace-offer
|           fr.gouv.vitam.storage.offers.workspace.core
|           fr.gouv.vitam.storage.offers.workspace.rest

