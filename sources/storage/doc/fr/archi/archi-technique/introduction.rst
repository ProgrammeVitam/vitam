Introduction
#############

Présentation
------------

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.storage**

Itération 16
------------
4 sous-modules dans Storage (parent).

| - storage-driver-api : module décrivant l'interface du driver
| - storage-engine : module embarquant la partie core du storage (client et server)
| - cas-manager : module embarquant l'offre Vitam (module vitam-offer) ainsi que l'implémentation du driver pour cette offre (cas-manager-drivers) et de son mock pour les tests
| - cas-container : module embraquant les implémentations spécifiques de l'offre de stockage, actuellement que l'implémntation swift.


Modules - packages Storage
--------------------------

|  storage
|     /storage-driver-api
|        fr.gouv.vitam.storage.driver

|     /storage-engine
|        /storage-engine-client
|           fr.gouv.vitam.storage.engine.client
|        /storage-engine-server
|           fr.gouv.vitam.storage.engine.server.spi
|           fr.gouv.vitam.storage.engine.server.logbook
|           fr.gouv.vitam.storage.engine.server.rest
|           fr.gouv.vitam.storage.engine.server.distribution
|        /storage-engine-common
|           fr.gouv.vitam.storage.engine.common

|     /cas-manager
|        /cas-manager-driver
|           /mock-driver
|              fr.gouv.vitam.driver.fake
|           /vitam-driver
|              fr.gouv.vitam.storage.offers.workspace.driver
|     /cas-container
|        /cas-container-filesystem
|           fr.gouv.vitam.cas.container.filesystem
|        /cas-container-swift
|           fr.gouv.vitam.cas.container.swift
|        /cas-container-utils
|           fr.gouv.vitam.cas.container.utils

