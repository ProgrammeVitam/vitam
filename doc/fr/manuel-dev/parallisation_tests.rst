#########################
Parallélisation des tests
#########################

Ce document présente la procédure pour réduire le temps de traitement des tests en 
les parallélisant. Ce travail réfère au US#714 et au techDesign IT01.


Il y a des tests TDD et des tests d'intégration dans les modules existants de la plate-forme, 
nous voulons faire paralléliser des classes de tests utilisant JUnit pour avoir la performance. 
Pour ce but, nous effectuons les étapes suivantes :

- Séparation des tests : tests unitaires et test d'intégration
- Parallélisation des tests unitaires 
- Configuration de build avec les options de tests     

Séparation des tests TDD et tests d'intégration
===============================================

- Il y a plusieurs tests d'intégration présents dans le module *integration-test* :

*ProcessingIT* : test d'intégration pour différents services : workspace, functional-administration, worker, metadata, logbook, processing

	StorageClientIT : test d'intégration pour le client du service de storage. Cela concerne deux modules:
	storage (client & rest) et le client de workspace 
	  
	WorkerIT : test d'intégration pour les services : workspace, worker, metadata, logbook, processing
   
	FunctionalAdminIT : test d'intégration pour le service FunctionalAdministration.
   
	IngestInternalIT : test d'intégration pour le service IngestInternal.
   
	LogbookCheckConsistencyIT : test d'intégration pour le service de vérification de cohérence des journaux.
   
	\*.Reconstruction\*.IT : test d'intégration pour les services de reconstruction et de backup.
   
	SecurityInternalIT : test d'intégration pour le service de sécurité interne.
   
	
	Ces tests d'intégration sont en mode séquentiel. Pour cela, nous indiquons dans le pom.xml de ce module de test-integration 
   
.. code-block:: xml

	<build>
		<pluginManagement>
			<plugins>
				<plugin>
					<!-- Run the Junit unit tests in an isolated classloader and not Parallel. -->
					<artifactId>maven-surefire-plugin</artifactId>
					<configuration>
						<parallel>classes</parallel>
						<threadCount>1</threadCount>
						<perCoreThreadCount>false</perCoreThreadCount>
						<forkCount>1</forkCount>
						<reuseForks>false</reuseForks>
						<systemPropertyVariables>
							<org.owasp.esapi.opsteam>AC001</org.owasp.esapi.opsteam>
							<org.owasp.esapi.devteam>AC001</org.owasp.esapi.devteam>
							<org.owasp.esapi.resources>../common/common-private/src/main/resources/esapi</org.owasp.esapi.resources>
						</systemPropertyVariables>
					</configuration>
				</plugin>
			</plugins>
		</pluginManagement>
	</build>	

Parallélisation de tests unitaires
==================================

Les tests unitaires de chaque module sont configurés pour être lancés en mode parallèle.
Pour cela, nous indiquons dans le pom.xml parent pour la phrase de build  

.. code-block:: xml

	<build>
		<plugins>
			<plugin>
				<!-- Run the Junit unit tests in an isolated classloader. -->
				<artifactId>maven-surefire-plugin</artifactId>
				<version>2.19.1</version>
				<configuration>
					<argLine>-Xmx2048m -Dvitam.tmp.folder=/tmp ${coverageAgent}</argLine>
					<parallel>classes</parallel>
					<threadCount>3</threadCount>
					<perCoreThreadCount>true</perCoreThreadCount>
					<forkCount>3C</forkCount>
					<reuseForks>false</reuseForks>
					<trimStackTrace>false</trimStackTrace>
				</configuration>
			</plugin>		
		</plugins>
		</build>
 

Configuration de build avec les options de tests
================================================

- ``mvn install`` : lancer le build normal avec tous les tests	
- ``mvn clean install -DskipTests`` : pour ignorer tous les tests:
- ``mvn clean test`` ou ``mvn clean install -DskipITs`` : pour ignorer les tests d'intégration
- ``mvn integration-test`` : pour lancer les tests d'intégration

Pour cela, nous ajoutons le code suivant dans le pom parent.

.. code-block:: xml

	<plugin>
		<executions>
			<execution>
				<id>integration-test</id>
				<goals>
					<goal>test</goal>
				</goals>
				<phase>integration-test</phase>
				<configuration>
					<skip>${skipITs}</skip>
					<excludes>
						<exclude>none</exclude>
					</excludes>
					<includes>
						<include>**/*IT.java</include>
					</includes>
				</configuration>
			</execution>
		</executions>
	</plugin>


- mvn clean test-compile failsafe:integration-test: pour exécuter uniquement les tests d'intégration.

Pour cela, nous ajoutons le code suivant dans le pom parent.

.. code-block:: xml

	<build>
		<plugin>
			<!-- Run the Junit integration tests in an isolated classloader. -->
			<artifactId>maven-failsafe-plugin</artifactId>
			<version>2.19.1</version>
			<executions>
				<execution>
					<id>integration-test</id>
					<goals>
						<goal>integration-test</goal>
						<goal>verify</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
	</build>

				
