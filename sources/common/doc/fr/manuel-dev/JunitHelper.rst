JunitHelper
###########

MongoDb or Web Server Junit Support
***********************************

Si dans un Web Server Junit, il est nécessaire d'activer un service utilisant un port, et ceci afin de favoriser un parallélisme maximal des tests unitaires, il est demandé de procéder comme suit :

.. code-block:: java
  
  
      private static JunitHelper junitHelper;
      private static int databasePort;
      private static int serverPort;
      
      // dans le @BeforeClass
      // Créer un objet JunitHelper
      junitHelper = new JunitHelper();
      
      // Pour MongoDB (exemple)
      databasePort = junitHelper.findAvailablePort();
      final MongodStarter starter = MongodStarter.getDefaultInstance();
      // On utilise le port
      mongodExecutable = starter.prepare(new MongodConfigBuilder()
          .version(Version.Main.PRODUCTION)
          .net(new Net(databasePort, Network.localhostIsIPv6()))
          .build());
      mongod = mongodExecutable.start();
  
      // Pour le serveur web (ici Logbook)
      // On initialise le mongoDbAccess pour le service
      mongoDbAccess =
          MongoDbAccessFactory.create(
              new DbConfigurationImpl(DATABASE_HOST, databasePort,
                  "vitam-test"));
      // On alloue un port pour le serveur Web
      serverPort = junitHelper.findAvailablePort();
          
      // On lit le fichier de configuration par défaut présent dans le src/test/resources
      File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
      // On extraie la configuration
      LogbookConfiguration realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
      // On change le port
      realLogbook.setDbPort(databasePort);
      // On sauvegarde le fichier (dans un nouveau fichier différent) (static File)
      newLogbookConf = File.createTempFile("test", LOGBOOK_CONF, logbook.getParentFile());
      PropertiesUtils.writeYaml(newLogbookConf, realLogbook);
      
      // On utilise le port pour RestAssured
      RestAssured.port = serverPort;
      RestAssured.basePath = REST_URI;
  
      // On démarre le serveur
      try {
         vitamServer = LogbookApplication.startApplication(new String[] {
            // On utilise le fichier de configuration ainsi créé
             newLogbookConf.getAbsolutePath(),
             Integer.toString(serverPort)});
         ((BasicVitamServer) vitamServer).start();
      } catch (FileNotFoundException | VitamApplicationServerException e) {
         LOGGER.error(e);
         throw new IllegalStateException(
             "Cannot start the Logbook Application Server", e);
     }

     // Dans le @AfterClass
     // On arrête le serveur
     try {
         ((BasicVitamServer) vitamServer).stop();
     } catch (final VitamApplicationServerException e) {
         LOGGER.error(e);
     }
     mongoDbAccess.close();
     junitHelper.releasePort(serverPort);
     // On arrête MongoDb
     mongod.stop();
     mongodExecutable.stop();
     junitHelper.releasePort(databasePort);
     // On efface le fichier temporaire
     newLogbookConf.delete();
     
