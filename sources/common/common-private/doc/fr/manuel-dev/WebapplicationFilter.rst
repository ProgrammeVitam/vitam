Implémentation du WAF dans jetty
################################################

But de cette documentation
==========================
On présente dans ce document l'implémentation de WAF dans jetty serveur

Fonctionnement général du filtre
================================
La class WafFilter utilise XSSWrapper pour valider les en-têtes et les paramètres dans les requêtes http. Si on détecte une faille XSS, on retourne directement la réponse 406(NOT ACCEPTABLE) au client

Implémentation du WAF
---------------------------------------------------------------
.. code-block:: java
	final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(sh, "/*");
        
        context.addFilter(WafFilter.class, "/*", EnumSet.of(
            DispatcherType.INCLUDE, DispatcherType.REQUEST,
            DispatcherType.FORWARD, DispatcherType.ERROR));
