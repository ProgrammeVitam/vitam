Configuration jetty
-------------------
Le besoin est de pouvoir fournir la capacité de configurer de manière programmatique Jetty. On peut penser aux besoins suivants :

1. Choisir le port de connection
2. Choisir le connecteur HTTP/HTTPS que l’on désire utiliser

    * Paramètres communs aux connecteurs HTTP et HTTPS
        - Taille des pools de thread (min,max nombre de threads)
        - Taille du backlog (nombre de connections en attente d’un thread disponible)
        - Différents timeout
    * Paramétres spécifiques à la couche TLS
        - Paramétres liés aux keystore (emplacement, mot de passe keystore, mot de passe des clés privées)
        - Paramétres liés aux trustore (idem keystore)
        - Paramétres liés à TLS (protocoles autorisés, ciphers autorisés, options TLS)

Gestion des Handlers :
----------------------
Pour la gestion de ces différents paramètres, on utilise le système de configuration en “Inversion of Control” de Jetty.
Un exemple de configuration est disponible à l’adresse suivante : https://gist.github.com/gustavosoares/1438086

Cette solution présente les avantages suivants :
    une gestion relativement souple de la configuration (la prise en compte du binding d’un paramètre ne nécessite pas de coder le binding)
    un exploitant qui connaît déjà Jetty sera en terrain connu de configuration

Parmi les choix à faire, il faut décider si on limite la configuration par fichier xml à la configuration “serveur d’application” ou si on pousse à la configuration des servlet .

L'important est d'utiliser la classe XMLConfiguration (package maven jetty-xml) dont la javadoc est disponible à l'adresse : http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/xml/XmlConfiguration.html
Pour la mise en oeuvre de ce composant, voici le pseudo code :

.. code-block:: java
   URL jettyConfigFileURL = PropertiesUtils.findFile(<fichier>).toURI().toURL();
   Server jettyServer = (Server) new XmlConfiguration(jettyConfigFileURL).configure();
   <Ajout RequestHandler>
   <Ajout ContextHandler>
   jettyServer.start();
   ...

avec fichier qui est défini de la manière suivante :

    - Si Le fichier passé en 1er argument du module (ex: access.conf) contient une variable nommé "jettyConfig" alors le serveur cherche dans son répertoire un configuration un fichier du nom de la valeur de jettyConfig .
    - Si la variable jettyConfig n'existe ou s'il n'existe pas de fichier correspond à la valeur de la variable 'jettyConfig' , le serveur cherche un fichier "jetty-vitam.xml" dans le répertoire de configuration
    - Si les 2 premiers cas échoue, le serveur s'arrête en erreur

A noter : pour les tests Unitaires, comme il n'y a pas de besoins de tuning particulier (pour l'instant) et qu'il y a un besoin d'avoir le port variable, on conserve la méthode actuelle pour démarrer les serveur (la méthode actuelle est de faire un (new Server (port) de la classe org.eclipse.jetty.server.Server).

Les modules concernées sont :
	* access-rest
	* ihm-demo-web-application
	* ingest-external-rest
	* ingest-internal-rest
	* metadata-rest

	TODO :
	* functional-administration-rest
	* logbook-rest
	* processing-management
	* storage-engine-server
	* storage-offer-default
	* workspace-rest

	
	
	
	
	
	
	
	
	
	
	
	
	
	
