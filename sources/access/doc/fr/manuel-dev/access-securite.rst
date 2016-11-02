contrôle des flux d'accèss
##########################

Le module access-external a besoin de disposer d'une brique frontale effectuant 
les contrôles de sécurité pour les flux d'accès à la plateforme.

	-- Fournissant la terminaison TLS
	-- Fournissant l'authentification par certificat
	-- Un WAF applicatif permettant le filtrage d'entrée filtrant les entrées être une menace pour le système (ESAPI)
	
.. code-block:: java

    protected void setFilter(ServletContextHandler context) throws VitamApplicationServerException {
        if (getConfiguration().isAuthentication()) {
            File shiroFile = null;
            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new VitamApplicationServerException(e.getMessage());
            }
            context.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
            context.addEventListener(new EnvironmentLoaderListener());
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }
        context.addFilter(WafFilter.class, "/*", EnumSet.of(
            DispatcherType.INCLUDE, DispatcherType.REQUEST,
            DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
    }