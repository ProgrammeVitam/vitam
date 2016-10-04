Architecture technique de l'application Back
############################################

But de cette documentation
==========================
Cette documentation décrit l'architecture technique de la partie Back de l'application IHM de VITAM.

Organisation du module ihm-demo
===============================
L'application IHM de VITAM est assurée par le module ihm-demo composé de deux sous-modules:

**1. Module ihm-demo-web-application**
--------------------------------------
Ce module encapsule à la fois le serveur d'application et l'application Front (sous le répertoire main/resources/webapp). Vous pouvez vous référer à la documentation de l'application Front pour plus de détails.

**package fr.gouv.vitam.ihmdemo.appserver**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- ServerApplication : cette classe configure et lance le serveur d'application Jetty.
- WebApplicationConfig : cette classe définit les paramètres de configuration du serveur d'application
    - Paramètres de configuration du serveur IHM:
        - port : port du serveur
        - serverHost : adresse du serveur
        - baseUrl : URL de base
        - staticContent : emplacement des fichiers statiques
- WebApplicationResource : cette classe définit les services REST assurés par l'application IHM:
    - POST /ihm-demo/v1/api/archivesearch/units
    - GET /ihm-demo/v1/api/archivesearch/unit/{id}
    - POST /ihm-demo/v1/api/logbook/operations
    - POST /ihm-demo/v1/api/logbook/operations/{idOperation}
    - GET /ihm-demo/v1/api/status
    - POST /ihm-demo/v1/api/ingest/upload
    - PUT /ihm-demo/v1/api/archiveupdate/units/{id}
    - POST /ihm-demo/v1/api/admin/formats
    - POST /ihm-demo/v1/api/admin/formats/{idFormat}
    - POST /ihm-demo/v1/api/format/check
    - POST /ihm-demo/v1/api/format/upload
    - DELETE /ihm-demo/v1/api/format/delete


**2. Module ihm-core**
--------------------------------------
Ce module gère la couche fonctionnelle de l'IHM ainsi que l’interaction avec les autres modules de VITAM.

**package fr.gouv.vitam.ihmdemo.core**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- DslQueryHelper : cette classe fournit les méthodes de construction des requêtes DSL requises par les services de l'application IHM telles que les requêtes de sélection et de mise à jour.
- UiConstants (Enumeration) : définit les constantes partagées
- UserInterfaceTransactionManager : cette classe assure l'appel des autres modules VITAM; en l'occurrence elle gère l'appel au module Access.