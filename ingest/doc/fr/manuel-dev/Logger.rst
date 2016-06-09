Logging
#######

Tous les logiciels Vitam utilisent le logger VitamLogger instantié via VitamLoggerFactory.


Initialisation
**************

Dans la Classe contenant la méthode **main** : si Logback n'est pas l'implémentation choisie, il faut changer le Factory.

.. code-block:: java

    // Out of **main** method
    private static VitamLogger logger;

    
    // In the **main** method
    VitamLoggerFactory.setDefaultFactory(another VitamLoggerFactory);
      // Could be JdkLoggerFactory, Log4JLoggerFactory, LogbackLoggerFactory
    logger = VitamLoggerFactory.getInstance(Class);
    // or
    logger = VitamLoggerFactory.getInstance(String);
    
Si l'implémentation est bien celle de Logback, cette initialisation peut être ignorée.

    
Usage
*****

.. code-block:: java

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Class);
    
    LOGGER.debug(String messageFormat, args...);
    // Note messageFormat supports argument replacement using '{}'
    LOGGER.debug("Valeurs: {}, {}, {}", "value", 10, true);
    // => "Valeur: value, 10, true" 

Il est possible de changer le niveau de log :

.. code-block:: java

    VitamLoggerFactory.setLogLevel(VitamLogLevel);

5 niveaux de logs existent :

* TRACE : le plus bas niveau, ne devrait pas être activé en général
* DEBUG : le plus bas niveau usuel
* INFO : pour des informations explicatives ou contextuelles
* WARN : pour les points d'attentions (warning)
* ERROR : pour les erreurs


Pour l'usage interne Vitam
**************************


.. code-block:: java

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Class);
    static final VitamLoggerHelper LOGGER_HELPER = VitamLoggerHelper.newInstance();
     
    LOGGER.debug(LOGGER_HELPER.format(message), args...);
    // Allow special formatting and extra information to be set
    
