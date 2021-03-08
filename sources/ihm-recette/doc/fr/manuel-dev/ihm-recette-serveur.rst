IHM Recette serveur
####################

IhmRecette
===========
L'application web IHM Recette est utilisée pour lancer le serveur.

.. code-block:: java

		VitamStarter.createVitamStarterForIHM(WebApplicationConfig.class, configurationFile,
            BusinessApplication.class, AdminApplication.class, Lists.newArrayList());

Classe BusinessApplication
============================

La classe BusinessApplication possède les singletons qui contiennent les ressources de l'application web IHM recette(WebApplicationResource) pour:

* Supprimer des collections vitam (WebApplicationResourceDelete)
* Gérer les tests système(ApplicativeTestResource)
* Définir les performances(PerformanceResource)

.. code-block:: java

    commonBusinessApplication = new CommonBusinessApplication();
        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());

        final WebApplicationResourceDelete deleteResource = new WebApplicationResourceDelete(configuration);
        final WebApplicationResource resource = new WebApplicationResource(configuration.getTenants(), configuration.getSecureMode());
        singletons.add(deleteResource);
        singletons.add(resource);

        Path sipDirectory = Paths.get(configuration.getSipDirectory());
        Path reportDirectory = Paths.get(configuration.getPerformanceReportDirectory());

        if (!Files.exists(sipDirectory)) {
            Exception sipNotFound =
                new FileNotFoundException(String.format("directory %s does not exist", sipDirectory));
            throw Throwables.propagate(sipNotFound);
        }

        if (!Files.exists(reportDirectory)) {
            Exception reportNotFound =
                new FileNotFoundException(format("directory %s does not exist", reportDirectory));
            throw Throwables.propagate(reportNotFound);
        }

        PerformanceService performanceService = new PerformanceService(sipDirectory, reportDirectory);
        singletons.add(new PerformanceResource(performanceService));

        String testSystemSipDirectory = configuration.getTestSystemSipDirectory();
        String testSystemReportDirectory = configuration.getTestSystemReportDirectory();
        ApplicativeTestService applicativeTestService =
            new ApplicativeTestService(Paths.get(testSystemReportDirectory));

        singletons.add(new ApplicativeTestResource(applicativeTestService,
            testSystemSipDirectory));

Configuration
=============

Le fichier de configuration se nomme ihm-recette.conf:

Fichier ``ihm-recette.conf``
----------------------------

.. literalinclude:: ../../../../../deployment/ansible-vitam/roles/vitam/templates/ihm-recette/ihm-recette.conf.j2
   :language: text


* port, serverHost, jettyConfig, tenants, secureMode
* baseUrl, staticContent, baseUri (qui configure jetty)
* authentication (ajoute le filtre shiro si le booléen est à "true")
* dbName, masterdataDbName, logbookDbName, metadataDbName, mongoDbNodes, clusterName, elasticsearchNodes
* testSystemSipDirectory, testSystemReportDirectory
* sipDirectory, performanceReportDirectory
* elasticsearchExternalMetadataMappings (liste des collections respectivement Unit et ObjectGroup et les fichiers de mappings associés d'elasticsearch)
