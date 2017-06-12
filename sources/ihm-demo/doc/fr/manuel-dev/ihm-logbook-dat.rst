DAT : module IHM logbook operations 
###################################

Ce document présente l'ensemble de manuel développement concernant le développment du module 
ihm-demo qui représente le story #90, qui contient :

- modules & packages
- classes métiers

--------------------------


1. Modules et packages

Au présent : nous proposons le schéma ci-dessous représentant le module principal
et ses sous modules.

ihm-demo 
    |
    |---ihm-demo-core : le traitement essentiel pour construire des requêtes DSL depuis des données saisies dans l'interface web
    |---ihm-demo-web-application  : le serveur d'application web qui fournit des services au client pour les traitements sur la recherche de logbook des opérations

Depuis ces deux modules, nous proposons deux packages correspondants : 

ihm-demo-core --> fr.gouv.vitam.ihmdemo.core
ihm-demo-web-application --> fr.gouv.vitam.ihmdemo.appserver		      	
ihm-demo-web-application --> webapp (resources)

2. Classes de métiers 
  
Cette section, nous présentons les classes/fonctions principales dans chaque module/package qui 
permettent de réaliser l'ensemble de tâches requis par User Story #90.

2.1. Partie Backend
 
ihm-demo-core : CreateDSLClient.java  
La classe a pour l'objecttif de création d'une requête  DSL à partir de l'ensemble de données de 
critères saisies depuis l'interface web du client. Les données en paramètres sont représentées dans 
un Map de type de String.

ihm-demo-web-application

- ServerApplication.java : créer & lancer le serveur d'application avec un configuration en paramètre
- WebApplicationConfig.java : 

créer la configuration pour le serveur d'application en utilisant différents paramètres : host, port, context

- WebApplicatationResource.java : 

définir des ressources différentes pour être utilisé par les contrôles de la partie Fontend. Le détail sur la resource de l'application serveur sera présenté dans le document RAML associé ihm-logbook-rest.rst.  

2.1. Partie Frontend

La partie Fontend web se trouve dans le sous module ihm-demo-web-application. Ce fontend web est développé 
en AngularJS. Dans cette partie, nous trouvons des composants différents

- views 
- modules
- controller js 

