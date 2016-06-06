workspace

le workspace est un module qui consiste à stocker le sip dans un container lors de traitement.

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

			- unzipSipObject :
				-Paramètres :
					-containerName::String
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


	File zippedFile=new File("/vitam/data/sip.zip");

	InputStream inputStream=new InputStream(zippedFile);

	WorkSpaceClient client= new WorkSapaceClient(http://143.126.93.166:8080/workspace/v1)


	client.unzipSipObject("myContainer",inputStream);









