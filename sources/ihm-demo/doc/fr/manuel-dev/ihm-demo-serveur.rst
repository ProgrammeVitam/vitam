IHM Demo serveur
#################

IhmMain
=======
L'application web IHM Demo est utilisée pour lancer le serveur

.. code-block:: java

		VitamStarter.createVitamStarterForIHM(WebApplicationConfig.class, configurationFile,
            BusinessApplication.class, AdminApplication.class, Lists.newArrayList());

Classe BusinessApplication
====================
La classe BusinessApplication possède les singletons qui contiennent les ressources de l'application web IHM Demo.

.. code-block:: java

			final WebApplicationConfig configuration =
                PropertiesUtils.readYaml(yamlIS, WebApplicationConfig.class);
            Set<String> permissions =
                PermissionReader.getMethodsAnnotatedWith(WebApplicationResource.class, RequiresPermissions.class);
            commonBusinessApplication = new CommonBusinessApplication();
            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(new WebApplicationResource(configuration, permissions));

Configuration
=============

Le fichier de configuration se nomme ihm-demo.conf et contient les paramètres suivants :

* port, serverHost, jettyConfig, tenants
* baseUrl, staticContent, baseUri, staticContentV2, baseUriV2 (qui configure jetty pour l'IHM-V1 et l'IHM-V2)
* authentication (ajoute le filtre shiro si le booléen est à "true")