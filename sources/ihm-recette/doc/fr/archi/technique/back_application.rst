Architecture technique de l'application Back
############################################

But de cette documentation
==========================
Cette documentation décrit l'architecture technique de la partie Back de l'application IHM de VITAM.

Organisation du module ihm-recette
==================================
L'application IHM de recette de VITAM est assurée par le module ihm-recette composé de trois sous-modules:

**1. Module ihm-demo-web-application**
--------------------------------------
Ce module encapsule à la fois le serveur d'application.

**package fr.gouv.vitam.ihmdemo.appserver**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- ServerApplication : cette classe configure et lance le serveur d'application Jetty.
- WebApplicationConfig : cette classe définit les paramètres de configuration du serveur d'application
    - Paramètres de configuration du serveur IHM:
        - port : port du serveur
        - serverHost : adresse du serveur
        - baseUrl : URL de base
        - staticContent : emplacement des fichiers statiques

**package fr.gouv.vitam.ihmdemo.appserver.performance.**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
- PerformanceResource : cette classe définit les services REST assurés par l'application IHM:
    - POST /ihm-recette/v1/api/performance : permet de lancer un test de performance
    - HEAD /ihm-recette/v1/api/performance : permet de connaître l'état du test (en cours ou fini)
    - GET /ihm-recette/v1/api/performance/reports : liste les rapports de tests
    - GET /ihm-recette/v1/api/performance/reports/{secureLogbookFileName} : télécharge un rapport de test
    - GET /ihm-recette/v1/api/performance/sips : liste les fichiers pouvant servir de pour le test de performance

**2. Module ihm-recette-web-front**
-----------------------------------

Ce module contient la partie front de l'IHM de recette. Il s'agit d'une application classique angular 1.5.3 dont les dépendances
de build sont gérés par le fichier `package.json` et les dépendances applicatives par le fichier `bower.json`.

**3. Module ihm-core**
----------------------
Ce module gère la couche fonctionnelle de l'IHM ainsi que l’interaction avec les autres modules de VITAM.
