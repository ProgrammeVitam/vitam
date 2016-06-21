Introduction
############

But de cette documentation
**************************

L'objectif de cette documentation est de compléter la Javadoc pour ce module.

Utilitaires Commons
*******************


FileUtil
========

Cet utilitaire propose quelques méthodes pour manipuler des fichiers.

Attention : les méthodes "readFile" doivent être limitées en termes d'usage au strict minimum et pour des fichiers de petites tailles.


LocalDateUtil
=============

Cet utilitaire propose quelques méthodes pour manipuler des dates avec la nouvelle classe LocalDateTime.

ServerIdentity
==============

Cet utilitaire propose une implémentation de la carte d'identité de chaque service/serveur.

ServerIdentity contient le ServerName, le ServerRole, et le Global PlatformId.

Pour une JVM, un seul ServerIdentity existe.

C'est un Common Private.

Par défaut cette classe est initialisée avec les valeurs suivantes :
* ServerName: hostname ou UnknownHostname si introuvable
* ServerRole: UnknownRole
* PlatformId: MAC adresse partielle comme entier

**Il est important que chaque server à son démarrage initialise les valeurs correctement.**

.. code-block:: java

    ServerIdentity serverIdentity = ServerIdentity.getInstance();
    serverIdentity.setName(name).setRole(role).setPlatformId(platformId);
    // or
    ServerIdentity.getInstance().setFromMap(map);
    // or
    ServerIdentity.getInstance().setFromPropertyFile(file);

Où name, role et platformID viennent d'un fichier de configuration par exemple.

Usage
-----

.. code-block:: java

    ServerIdentity serverIdentity = ServerIdentity.getInstance();
    String name = serverIdentity.getName();
    String role = serverIdentity.getRole();
    int platformId = serverIdentity.getPlatformId();

Les usages principaux
---------------------

* GUID pour PlatformId
* Logger and Logbook pour tous les champs

SyetemPropertyUtil
==================

Cet utilitaire propose quelques méthodes pour manipuler les Propritétés héritées du Système, notamment celle déduites de "-Dxxxx" dans la ligne de commande Java.

Il intègre notamment :
- String getVitamConfigFolder()
- String getVitamDataFolder()
- String getVitamLogFolder()
- String getVitamTmpFolder()

Les répertoires sont par défaut :
- Config = /vitam/conf
- Data = /vitam/data
- Log = /vitam/log
- Tmp = /vitam/data/tmp

Ils peuvent être dynamiquement surchargés par une option au lancement du programme Java :
- -Dvitam.config.folder=/path
- -Dvitam.data.folder=/path
- -Dvitam.log.folder=/path
- -Dvitam.tmp.folder=/path

PropertiesUtils
===============

Cet utilitaire propose quelques méthodes pour manipuler des fichiers de propriétés et notamment dans le répertoire Resources.

Il intègre notamment :
- File getResourcesFile(String resourcesFile) qui retourne un File se situant dans "resources (classpath) /resourcesFile"
- File findFile(String filename) qui retourne un File se situant dans l'ordre

  - Chemin complet donné par resourcesFile
  - Chemin complet donné par ConfigFolder + resourcesFile
  - Chemin complet dans resources (classpath) /resourcesFile
  
- File fileFromConfigFolder(String subpath) qui retourne un File se situant dans "ConfigFolder + subpath" (non checké)
- File fileFromDataFolder(String subpath) qui retourne un File se situant dans "DataFolder + subpath" (non checké)
- File fileFromLogFolder(String subpath) qui retourne un File se situant dans "LogFolder + subpath" (non checké)
- File fileFromTmpFolder(String subpath) qui retourne un File se situant dans "TmpFolder + subpath" (non checké)

BaseXXX
=======

Cet utilitaire propose quelques méthodes pour maipuler des Base16, Base32 et Base64.

CharsetUtils
============

Cet utilitaire propose quelques méthodes pour la gestion des Charset.

ParametersChecker
=================

Cet utilitaire propose quelques méthodes pour gérer la validité des arguments dans les méthodes.

SingletonUtil
=============

Cet utilitaire propose quelques méthodes pour obtenir des Singletons.

StringUtils
===========

Cet utilitaire propose quelques méthodes pour manipuler des String.


GUID
****

Cf chapitre dédié.

Logging
*******

Cf chapitre dédié.


LRU
***

Cet utilitaire propose une implémentation en mémoire de Cache Last Recent Used.

Il est notamment utilisé dans la partie Metadata.

Son usage doit positionner une dimension maximale et un délai avant retrait :

* Les plus anciens sont supprimés lorsque la place manque
* Les plus anciens sont supprimés lorsque la méthode **forceClearOldest()** est appelé

Digest
******

Cet utilitaire propose les fonctionnalités de calculs d'empreintes selon différents formats.

Cf chapitre dédié.

Json
****

Cet utilitaire propose les fonctionnalités de manipulation de Json en utilisant Jackson.

Ce module propose une configuration par défaut pour Vitam.

Exception
*********

L'exception parente Vitam VitamException s'y trouve. Toutes les exceptions Vitam en héritent.

 
