=========
workspace
=========

le workspace est un module qui consiste à stocker le sip dans un container lors de traitement. Il y a un controle des
paramètres (SanityChecker.checkJsonAll) transmis avec ESAPI.

1- Consommer les services exposés par le module:
------------------------------------------------

1.1 - Introduction :

on peut consommer les services via le sous module workspaceClient notament via la classe WorkspaceClient: 
		Cette classe contient la liste des methodes suivantes :

- CreateContainer :

    - Paramètres :
    - containerName::String
    - Retourner : 

- getUriListDigitalObjectFromFolder :

    - Paramètres :

        - containerName::String
        - folderName::String

    - Retourner :

        - List<URI>

Dans le cas echéant la method return une immuatable empty list.

	- uncompressObject : cette méthode capable d'extracter des fichiers compressés toute en indiquant le type de l'archive, pour cette version (v0.9.0) supporte 3 types : zip, tar, tar.gz. Elle sauvgarde directement les fichiers extractés dans le workspace, notamment dans le container précisé lors de l'appel (containerName).

		- Paramètres :

            - containerName::String : c'est le nom de container dans lequel on stocke les objets
            - folderName::String : c'est le répertoire central (pour cette methode, c'est le sip)
            - archiveType::String : c'est le nom ou le type de l'archive (exemple: application/zip , application/x-tar)
            - compressedInputStream::InputStream : c'est le stream des objets compressés

    - retourner :    

Dans le cas echéant (uncompress KO) la methode génère une exception avec un message internal server.


    - getObjectInformation :

        - Paramètres :
        - containerName::String
        - objectName::String
        - Retourner :
        - JsonNode
               
            La méthode retourne un Json contenant des informations sur un objet présent sur le workspace (et des exceptions en cas d'erreur : objet non existant, erreur server).

    - purgeOldFilesInContainer : Cette méthode permet de purger les  anciens fichiers dans un conteneur (date de dernière modification date d'au moins une durée donnée)

        - Paramètres :
            - containerName::String
            - timeToLive::TimeToLive

2.2 - Exemple d'utilisation
---------------------------

	D'abord il faut ajouter la dependence sur la pom.xml du projet.

.. code-block:: xml

    <dependencies>
        <groupId>fr.gouv.vitam</groupId>
        <artifactId>workspace-client<artifactId>
        <version>x.x.x</version>
    </dependencies>


Supposons que nous avons besoins d'extraire un SIP de format zip dans le workspace.

.. code-block:: java
    
    InputStream inputStream=new InputStream(zippedFile);
    WorkspaceClientFactory.changeMode(WORKSPACE_URL);
    WorkspaceClientFactory.changeMode(FileConfiguration);	
    WorkspaceClient workspaceClient = WorkspaceClientFactory().getInstance().getClient();
    workspaceClient.createContainer(containerName);
    workspaceClient.uncompressObject(containerName,"SIP","application/zip" inputStream);


2- Configuration du pom
-------------------------

Configuration du pom avec maven-surefire-plugin permet le build sous jenkins. Il permet de configurer le chemin des resources de esapi dans le common private.

