Implémentation du secret de la plateforme
################################################

Présentation
------------
Le secret de plateforme permet de se protéger contre des erreurs de manipulation et de configuration 
en séparant les environnements de manière logique (secret partagé par l'ensemble de la plateforme mais différent entre plateforme).

Implémentation
--------------
* Un Header X-Request-Timestamp contenant le timestamp de la requête sous forme epoch (secondes depuis 1970)
* Un Header X-Platform-ID qui est SHA256("<methode>;<URL>;<Valeur du header X-Request-Timestamp>;<Secret partagé de plateforme>").
	Par contre, mettre le secret de plateforme à la fin permet de limite les attaques par extension.
	
.. code-block:: java

    // add Authorization Headers (X_TIMESTAMP, X_PLATFORM_ID)
    Map<String,String> authorizationHeaders = AuthorizationFilterHelper.getAuthorizationHeaders(httpMethod,baseUri);
    if(authorizationHeaders.size()==2){
    	builder.header(GlobalDataRest.X_TIMESTAMP,authorizationHeaders.get(GlobalDataRest.X_TIMESTAMP));
    	builder.header(GlobalDataRest.X_PLATFORM_ID,authorizationHeaders.get(GlobalDataRest.X_PLATFORM_ID));
    }
    .....................

Si on veut assurer une sécurité additionnelle, il est possible de transmettre un hash des valeurs suivantes :
- URI + paramètres de l'URI
- Header Timestamp
- Secret de plateforme en clair non transmis (connus par les participants de la plateforme)
=> Hash (URI + paramètres (dans l'ordre alphabétique) + Header Timestamp + secret non transmis)
Ce Hash est transmis dans le Header : X-Platform-Id

.. code-block:: java

    //encode URL using secret
    public static String encodeURL(String httpMethod, String url, String timestamp, String secret,
    	DigestType digestType) {
    	ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, httpMethod, url, timestamp, secret, digestType);
    	Digest digest = new Digest(digestType);
    	return digest.update(httpMethod + DELEMITER_SEPARATED_VALUES + url + DELEMITER_SEPARATED_VALUES + timestamp +
    		DELEMITER_SEPARATED_VALUES + secret).toString();
    }
    .....................

Le contrôle est alors le suivant :
1) Existance de X-Platform-Id et Timestamp
2) Vérification que Timestamp est distant de l'heure actuelle sur le serveur requêté de moins de 10 secondes 
	( |Timestamp - temps local| < 10 s )
3) Calcul d'un Hash2 = Hash(URI+paramètres (dans l'ordre alphabétique) + Header Timestamp + secret non transmis) 
	et vérification avec la valeur Hash transmise
	
.. code-block:: java

    if ((Strings.isNullOrEmpty(platformId)) || (Strings.isNullOrEmpty(timestamp))) {
    	return false;
    } else {
    	return (checkTimestamp(timestamp) && (checkPlatformId(platformId, timestamp)));
    }
    private boolean checkTimestamp(String timestamp) {
         ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, timestamp);
         long currentEpoch = System.currentTimeMillis() / 1000;
         long requestEpoch = Long.valueOf(timestamp).longValue();
         if (Math.abs(currentEpoch - requestEpoch) <= VitamConfiguration.getAcceptableRequestTime()) {
             return true;
         }

         LOGGER.error("Timestamp check failed");
         return false;
    }
    private boolean checkPlatformId(String platformId, String timestamp) {
         ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, platformId, timestamp);
         String uri = getRequestURI();
         String httpMethod = getMethod();
         String code = URLCodec.encodeURL(httpMethod, uri, timestamp, VitamConfiguration.getSecret(),
             VitamConfiguration.getSecurityDigestType());
         if (code.equals(platformId)) {
             return true;
         }
         LOGGER.error("PlatformId check failed");
         return false;
    }

