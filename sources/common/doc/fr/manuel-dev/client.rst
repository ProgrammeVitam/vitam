Client
######

But de cette documentation
==========================

Cette documentation indique comment utiliser le code commun du client Vitam pour créer son propre client Vitam.

Client Vitam
============

L'interface commune du client Vitam est : *fr.gouv.vitam.common.client.BasicClient*.

Elle mets à disposition les méthodes suivantes :

- la récupération du status du serveur distant auquel le client se connecte
- la récupération du chemin du serveur distant auquel le client se connecte
- l'arrêt du client

Une implémentation par défaut de ces méthodes est fournie dans la classe abstraite associée *fr.gouv.vitam.common.AbstractClient*.

Chaque client Vitam doit créer sa propre interface qui hérite de l'interface BasicClient


.. code-block:: java

	public interface MyModuleClient extends BasicClient {
		....
	}

Chaque client Vitam doit créer au moins deux implémentations :

- le client production

.. code-block:: java

	class MyModuleClientRest extends AbstractClient implements MyModuleClient {
		....
	}

- le client bouchonné (Mock)

.. code-block:: java

	class MyModuleClientMock extends AbstractClient implements MyModuleClient {
		....
	}

Une factory doit être mise en place pour récupérer l'instance du client adaptée.
Par défaut, le client attends un fichier de configuration mymodule-client.conf. S'il n'est pas présent, le client bouchonnée est renvoyé.


.. code-block:: java

	public class MyModuleClientFactory {
		....
	}

Elle doit pouvoir être utilisée de la manière suivante :

.. code-block:: java

		// Retrieve the default mymodule client
		MyModuleClient client = MyModuleClientFactory.getInstance().getMyModuleClient();


Configuration
=============

Une classe de configuration par défaut est fournie : *fr.gouv.vitam.common.clientSSLClientConfiguration* .
Elle contient les propriétés suivantes :

- **serverHost** : le nom d'hôte du serveur distant auquel le client va se connecter (Exemple : localhost)
- **serverPort** : le port du serveur distant auquel le client va se connecter (Exemple : 8082)
- **serverContextPath** : le context  sur lequel est exposé le serveur distant auquel le client va se connecter (Exemple : / )
- **useSSL** : booléen permettant de spécifier si le client doit utiliser le protocole HTTP (false) ou HTTPS (true)

Un fichier de configuration nommé **mymodule-client.conf** doit être présent dans le classpath de l'application utilisant le client.
Ce fichier de configuration est au format YAML et il doit contenir les propriétés définies par la classe de configuration.

*Note :* Actuellement le mode HTTPS n'est pas encore implémenté. Ainsi une runtime exception est lancée si le client
est instancié avec une configuration dont le **useSSL** vaut true.
