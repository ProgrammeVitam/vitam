==============
Common-storage
==============

Le common storage est un module commun pour plusieurs modules qui consiste à gérer des objets stockés dans un container et/ou dans dans un repertoire, ce module propose plusieurs offres de stockage (Jclouds), par exemple file Systeme et swift (open stack et ceph) configurabe par code (java) ou par fichier de configuration. Dans les chapitres suivants, on présentra les 2 modes du configuration 

1- Présentation des APIs Java:
------------------------------------------------
1.1 - Introduction :

Le Module common storage expose un ensemble des methodes qui gèrent la creation, la mise à jour , la supprission des contenaire, des repertoires et des objets, Vous trouverez ci-dessous la liste des methodes avec leur fonctions attendus.

2.2 - Liste des méthodes :

- getContainerInformation : consulter les information d'un contenaire (pour la version 0.14.0-SNAPSHOT)
    - Paramètres :
    - containerName::String 
    - Retourner : (pour la version 0.14.0-SNAPSHOT) l'espace utilisés et l'espace disponible par région

- CreateContainer : creer un contenaire 
    - Paramètres :
    - containerName::String 
    - Retourner : 

- getUriListDigitalObjectFromFolder :
    - Paramètres :
        - containerName::String (le nom de contenaire à consulter)
        - folderName::String (le nom de repertoire à consulter pour lister les URIs des objets )
    - Retourner :
        - List<URI>: La liste des URIs des objets dans le repertoire cité ci-dessus.

Dans le cas echéant la method retourne une immuatable empty list.

	- uncompressObject : cette méthode extracte des fichiers compressés toute en indiquant le type de l'archive, pour cette version (v0.14.0) supporte 4 types : zip, tar, tar.gz et tar.bz2. 
		-Paramètres :
			- containerName::String : c'est le nom de container dans lequel on stocke les objets
			- folderName::String : c'est le repertoire racine .
            - archiveType :: String : c'est le nom ou le type de l'archive (exemple: application/zip , application/x-tar)
			- compressedInputStream::InputStream c'est le stream des objets compressés
    - retourner :    

Dans le cas echéant (uncompress KO) la methode génère une exception avec un message internal server.


    - getObjectInformation :cette méthode retourne des informations utils sur un objet
        - Paramètres :
        - containerName::String (le nom de contenaire à consulter)
        - objectName::String 
        - Retourner :
        - JsonNode
               
     La méthode retourne un Json contenant des informations sur un objet présent dans un contenaire prédéfinit (et des exceptions en cas d'erreur : objet non existant, erreur server).

2 - Configuration
------------------

La première chose que nous devons faire est d'ajouter la dépendance maven dans le pom.xml du projet. Après il faut configurer le contexte de stockage souhaités (filesystem/swift ceph/ swift openStack), (on traitera cette problématique au chapitre 2.1 et 2.2)

<dependency>	
     <groupId>fr.gouv.vitam</groupId>

     <artifactId>common-storage<artifactId>

     <version>x.x.x</version>

</dependency>

La configuration de l'offre de stockage est basé sur plusieurs paramètres:

  - provider :: String : le type de l'offre de stockage (valeur par defaut: filesystem, valeur possibles : openstack-swift , filesystem ou chaîne vide)
  - keystoneEndPoint* :: String : URL d'authentification keystone
  - swiftSubUser* :: String : le nom de l'utilisateur (sur rados, il prend la forme <tenant>$<user>) 
  - cephMode* :: boolean : l'implementation swift (true pour ceph, false pour openstack)
  - storagePath :: String : path de stockage pour l'offre FileSystem
 
2.1 - Configuration par code:

2.1.a Exemple file systeme:

.. code-block:: java

 StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.FILESYSTEM.getValue())  
       .setStoragePath("/");      
      


2.1.b Exemple SWIFT CEPH

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.SWIFT.getValue())       
       .setKeystoneEndPoint("http://10.10.10.10:5000/auth/v1.0)      
       .setSwiftUid(swiftUID) 
       .setSwiftSubUser(user)  
       .setCredential(passwd) 
       .setCephMode(true);  

2.1.c Exemple SWIFT OpenStack

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.SWIFT.getValue())       
       .setKeystoneEndPoint("http://10.10.10.10:5000/auth/v1.0)      
       .setSwiftUid(swift) 
       .setSwiftSubUser(user)  
       .setCredential(passwd) 
       .setCephMode(false);  


2.2 - Configuration par fichier 


Exemple d'un fichier de configuration :

provider: openstack-swift

keystoneEndPoint : http://10.10.10.10:5000/auth/v1.0

swiftUid : vitam

swiftSubUser : swift

credential : password

cephMode : true

Dans ce cas, on peut utiliser un Builder qui permet de fournir le context associé au provider.

 .. code-block:: java
 
	ContentAddressableStorage storage=StoreContextBuilder.newStoreContext(configuration)

		