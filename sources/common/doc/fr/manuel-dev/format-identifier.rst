Format Identifiers
##################

But de cette documentation
==========================

Cette documentation indique comment utiliser les services d'identification de format et comment créer sa propre implémentation.

Format Identifier
=================

L'interface commune du service d'identification des formats est : *fr.gouv.vitam.common.format.identification.FormatIdentifier*.

Elle mets à disposition les méthodes suivantes :

- la récupération du status du logiciel auquel le service se connecte
- l'identification du format d'un fichier par le logiciel auquel le service se connecte

Les implémentations de l'interface sont :

- pour l'implémentation Mock : *fr.gouv.vitam.common.format.identification.FormatIdentifierMock*
- pour l'implémentation du logiciel Siegfried : *fr.gouv.vitam.common.format.identification.FormatIdentifierSiegfried*

Il sera possible d'en implémenter d'autres.

Implémentation Mock
*******************

Implémentation simple renvoyant des réponses statiques.

Implémentation Siegried
***********************

Implémentation basique utilisant un client HTTP.

Format Identifier Factory
=========================

Afin de récupérer l'implémentation configurée une factory a été mise en place.

Configuration
*************

Cette factory charge un fichier de configuration "format-identifiers.json". Ce fichier contient les configurations des services d'identificaton de format identifiées par un id :

.. code-block:: json

	{
		"siegfried-local": {
			"type":"SIEGFRIED",
         "client":"http",
         "host":"localhost",
         "port":"55800",
         "rootPath":"/root/path",
         "versionPath":"/root/path/version/folder",
         "createVersionPath":"true"
		},
		"mock": {
			"type":"MOCK"
		}
	}

Le type est obligatoire et doit correspondre à l'enum *fr.gouv.vitam.common.format.identification.model.FormatIdentifierType*.

Les autres données sont spécifiques à chaque implémentation du service d'identification de format.

Si le fichier n'est pas présent au démarrage du serveur, aucune configuration n'est chargée par la factory.

Méthodes
********

Pour récupérer un service d'identification de formats :

.. code-block:: java

	FormatIdentifier siegfried = FormatIdentifierFactory.getInstance().getFormatIdentifierFor("siegfried-local");

Pour ajouter une configuration mock :

.. code-block:: java

	FormatIdentifierConfiguration mock = new FormatIdentifierConfiguration();
        siegfried.setType(FormatIdentifierType.MOCK);
        FormatIdentifierFactory.getInstance().addFormatIdentifier("mock", mock);

Pour ajouter une configuration siegfried :

.. code-block:: json

   "siegfried-local": {
      "type":"SIEGFRIED",
      "client":"http",
      "host":"localhost",
      "port":"55800",
      "rootPath":"/root/path",
      "versionPath":"/root/path/version/folder",
      "createVersionPath":false
   }

*client*: *http* correspond au client HTTP à lancer (ce dernier effectue des requêtes HTTP pour analyser les fichiers)
*host*/*port* correspond au le serveur sur lequel Siegfried est installé.
*rootPath* correspond au chemin vers les fichiers analysables par Siegfried.
*versionPath* correspond au chemin vers un dossier vide utilisé pour requêter la version de Siegfried.
*createVersionPath* : Si *false* le dossier doit pré-existant sur le server sur lequel tourne Siegfried. Sinon, le client siegfried tente de créer automatiquement le dossier en local.

.. code-block:: java

	FormatIdentifierConfiguration siegfried = new FormatIdentifierConfiguration();
        siegfried.setType(FormatIdentifierType.SIEGFRIED);
        FormatIdentifierFactory.getInstance().addFormatIdentifier("siegfried-local", siegfried);


Pour supprimer une configuration :

.. code-block:: java

        FormatIdentifierFactory.getInstance().removeFormatIdentifier("siegfried-local");
