workspace

le workspace est un module qui consiste à stocker le sip dans un container lors de traitement. Il y a un controle des
paramètres (SanityChecker.checkJsonAll) transmis avec ESAPI.

1- Consommer les services exposés par le module:
	1.1 - Introduction
	on peut consommer les services via le sous module workspaceClient notament via la class WorkspaceClient: 
		Cette classe contient la liste des methodes suivantes :

			- CreateContainer :
				-Paramètres :
					-containerName::String
				-Retourner :

			- getUriListDigitalObjectFromFolder :
				-Paramètres :
					-containerName::String
					-folderName::String
				-Retourner :
					-List<URI>

				Dans le cas echéant la method return une immuatable empty list.

			- unzipObject :
				-Paramètres :
					-containerName::String
					- folderName::String
					- zippedInputStream::InputStream
				-Retourner :

			Dans le cas echéant (unzip KO) la methode génère une exception avec un message internal server.

	2.2 - Exemple d'utilisation

	D'abord il faut ajouter la dependence sur la pom.xml du projet.

		<dependencies>	
		....
			<groupId>fr.gouv.vitam</groupId>
			<artifactId>workspace-Client<artifactId>
			<version>1.2.3</version>
			...
		</dependencies>

	Supposans que nous ayons besoin d'extracter le SIP compressé dans le workspace.

    
     InputStream inputStream=new InputStream(zippedFile);
	
	 WorkspaceClient workspaceClient =
                    new WorkspaceClientFactory().create("localhost:8082");

      workspaceClient.createContainer(containerName);
      workspaceClient.unzipObject(containerName,"SIP", inputStream);

2- Configuration du pom
Configuration du pom avec maven-surefire-plugin permet le build sous jenkins. Il permet de configurer le chemin des resources de esapi dans le common private.








