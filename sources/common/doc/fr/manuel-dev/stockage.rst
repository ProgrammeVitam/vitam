==============
Common-storage
==============

Le common storage est un module commun pour plusieurs modules qui consiste à gérer des objets stockés dans un container et/ou dans un répertoire, ce module propose plusieurs offres de stockage (Jclouds), par exemple filesystem, Swift (open stack et ceph) et s3 configurables par code (java) ou par fichier de configuration. Dans les chapitres suivants, on présentera les 3 modes de configuration.

1- Présentation des APIs Java:
------------------------------------------------
1.1 - Introduction :

Le Module common storage expose un ensemble des méthodes qui gèrent la création, la mise à jour , la suppression des conteneurs, des répertoires et des objets. Vous trouverez ci-dessous la liste des méthodes avec leurs fonctions attendues.

L'API principale est l'interface ContentAddressableStorage. Celle-ci a la hiérarchie de classe suivante :

- ContentAddressableStorageAbstract : classe abstraite implémentant quelques méthodes communes

  * HashFileSystem : implémentation d'un CAS sur FileSystem (via java.nio.*) avec un répertoire par sous-répertoire permettant un stockage d'un grand nombre d'objets (jusqu'à 500e6 objets )
  * ContentAddressableStorageJcloudsAbstract : classe abstraite implémentant la plupart des méthodes pour une implémentation jclouds sous-jacente

    + FileSystem : implémentation d'un CAS sur FileSystem (via jclouds) avec un répertoire à plat sous les containers
    + OpenstackSwift : classe d'implémentation permettant le stockage sur Swift (via jclouds)
    + AmazonS3V1 : classe d'implémentation permettant le stockage sur S3 (via le sdk amazon s3 v1)

1.2 - Liste des méthodes :

- getContainerInformation : consulter les informations d'un conteneur (pour la version 0.14.0-SNAPSHOT)

    - Paramètres :
    - containerName::String
    - Retourner : (pour la version 0.14.0-SNAPSHOT) l'espace utilisé et l'espace disponible par région

- CreateContainer : créer un conteneur

    - Paramètres :
    - containerName::String
    - Retourner :

- getUriListDigitalObjectFromFolder :

    - Paramètres :

        - containerName::String (le nom de conteneur à consulter)
        - folderName::String (le nom de répertoire à consulter pour lister les URIs des objets )

    - Retourner :

        - List<URI>: La liste des URIs des objets dans le répertoire cité ci-dessus.

- getObjectMetadatas: lire et récupérer les métadonnées d'un objet (le fichier ou le répertoire)

	- Paramètres :

    	- containerName::String (le nom de conteneur dans lequel qu'on stock l'object)
    	- objectId::String (Id de l'object. S'il est null, c'est-à-dire, il est un répertoire)

    - Retourner :

    	- MetadatasObject: La classe qui contient les informations de metadata

    		- objectName: l'ID du fichier
    		- type: le type (dossier comme Units, Binary, ObjectGroup, Reports, ...)
    		- digest: l'empreinte
    		- fileOwner: propriétaire
    		- fileSize: taille du fichier
    		- lastAccessDate: date de dernier accès
    		- lastModifiedDate: date de modification des données


Dans le cas échéant, la méthode retourne une immutable empty list.

	- uncompressObject : cette méthode extrait des fichiers compressés toute en indiquant le type de l'archive, pour cette version (v0.14.0) supporte 4 types : zip, tar, tar.gz et tar.bz2.

		-Paramètres :

			- containerName::String : c'est le nom de container dans lequel on stocke les objets
			- folderName::String : c'est le répertoire racine .

            - archiveType :: String : c'est le nom ou le type de l'archive (exemple: application/zip , application/x-tar)

			- compressedInputStream::InputStream c'est le stream des objets compressés

    - retourner :

Dans le cas échéant (uncompress KO) la méthode génère une exception avec un message internal server.

2 - Configuration
------------------

La première chose que nous devons faire est d'ajouter la dépendance maven dans le pom.xml du projet. Après il faut configurer le contexte de stockage souhaité (filesystem/swift ceph/ swift openStack), (on traitera cette problématique au chapitre 2.1 et 2.2)

.. code-block:: xml

  <dependency>
       <groupId>fr.gouv.vitam</groupId>
       <artifactId>common-storage<artifactId>
       <version>x.x.x</version>
  </dependency>

La configuration de l'offre de stockage est basée sur plusieurs paramètres.

Les paramètres communs aux types d'offres sont:
  - provider :: String : le type de l'offre de stockage (valeur par défaut si chaîne vide: filesystem) Les valeurs possibles sont:
    - filesystem
    - openstack-swift
    - amazon-s3-v1

Pour une offre Filesystem, les paramètres de configuration sont :
  - storagePath :: String : path de stockage pour l'offre FileSystem

Pour une offre Swift les paramètres de configuration sont :
  - swiftKeystoneAuthUrl* :: String : URL d'authentification keystone
  - swiftUser* :: String : le nom de l'utilisateur (sur rados, il prend la forme <tenant>$<user>)

Pour une offre S3 les paramètres de configuration sont :
  - s3AccessKey :: String : Access Key ID
  - s3SecretKey :: String : Secret Access key
  - s3RegionName :: String : region (pour les requêtes signées en algorithme V4)
  - s3Endpoint :: String : URL du stockage
  - s3SignerType :: String : type de signature utilisé (cf documentation officielle Amazon sur la `signature des requêtes <https://docs.aws.amazon.com/fr_fr/AmazonS3/latest/dev/UsingAWSSDK.html#specify-signature-version>`_). Valeurs possibles :
     - 'AWSS3V4SignerType' : signature V4 (valeur par défaut si chaîne vide)
     - 'S3SignerType' : signature V2
  - s3TrustStore :: String : chemin vers le fichier TrustStore contenant le certificat racine de l'autorité du certificat du stockage (obligatoire en cas de SSL)
  - s3PathStyleEnabled :: Boolean : type d'accès aux buckets S3 (cf documentation officielle Amazon sur l'`hébergement virtuel de compartiments <https://docs.aws.amazon.com/fr_fr/AmazonS3/latest/dev/VirtualHosting.html>`_). Valeurs possibles :
     - 'true' : l'accès en mode "path-style" (exemple d'URI : ``http://mys3domain/mybucket/``)
     - 'false' : l'accès en "virtual-hosted-style" (exemple d'URI : ``http://mybucket.mys3domain/``)
  - s3MaxConnections :: Integer : nombre maximum de connexions HTTP ouvertes
  - s3ConnectionTimeout :: Integer : temps maximum pour l'établissement d'une connexion avant d'abandonner (en millisecondes)
  - s3SocketTimeout :: Integer : temps maximum pour le transfert de la donnée avant d'abandonner (en millisecondes)
  - s3RequestTimeout :: Integer : temps maximum pour l'exécution de la requête avant d'abandonner (en millisecondes)
  - s3ClientExecutionTimeout :: Integer : temps maximum pour l'exécution de la requête par le client java avant d'abandonner (en millisecondes)


2.1 - Configuration par code:

2.1.a Exemple file système:

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.FILESYSTEM.getValue())
    .setStoragePath("/");



2.1.b Exemple SWIFT CEPH

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.SWIFT.getValue())
       .setSwiftKeystoneAuthUrl("http://10.10.10.10:5000/auth/v1.0)
       .setSwiftDomain(domain)
       .setSwiftUser(user)
       .setSwiftPassword(passwd);

2.1.c Exemple SWIFT OpenStack

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.SWIFT.getValue())
       .setKeystoneEndPoint("http://10.10.10.10:5000/auth/v1.0)
       .setSwiftUid(swift)
       .setSwiftSubUser(user)
       .setCredential(passwd);

2.1.d Exemple S3

Cet exemple correspond aux valeurs d'une image docker Openio.

.. code-block:: java

  StorageConfiguration storeConfiguration = new StorageConfiguration().setProvider(StorageProvider.AMAZON_S3_V1.getValue())
		.setS3RegionName(Regions.US_WEST_1.getName());
		.setS3Endpoint("http://127.0.0.1:6007");
		.setS3AccessKey("demo:demo");
		.setS3SecretKey("DEMO_PASS");
		.setS3PathStyleAccessEnabled(true);


2.2 - Configuration par fichier


Exemple d'un fichier de configuration :

.. code-block:: yaml

  provider: openstack-swift
  swiftKeystoneAuthUrl : http://10.10.10.10:5000/auth/v1.0
  swiftDomain : vitam
  swiftUser : swift
  swiftPassword : password

Dans ce cas, on peut utiliser un Builder qui permet de fournir le context associé au provider.

 .. code-block:: java

	ContentAddressableStorage storage=StoreContextBuilder.newStoreContext(configuration)



3- Présentation des méthodes dans SWIFT & FileSystem:
------------------------------------------------------

3.1 - Introduction :

Il y a deux classes qui héritent les APIs. L'une utilise SWIFT et l'autre utilise FileSystem.

3.2 - Liste des méthodes :

3.2.1 getObjectInformation :

- SWIFT: Obtenir l'objet par les APIs Swift

.. code-block:: java

		result.setFileOwner("Vitam_" + containerName.split("_")[0]);
        result.setType(containerName.split("_")[1]);
        result.setLastAccessDate(null);
        if (objectId != null) {
            SwiftObject swiftobject = getSwiftAPi()
                .getObjectApi(swiftApi.getConfiguredRegions().iterator().next(), containerName).get(objectId);

            result.setObjectName(objectId);
            result.setDigest(computeObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType()));
            result.setFileSize(swiftobject.getPayload().getContentMetadata().getContentLength());
            result.setLastModifiedDate(swiftobject.getLastModified().toString());
        } else {
            Container container = getContainerApi().get(containerName);
            result.setObjectName(containerName);
            result.setDigest(null);
            result.setFileSize(container.getBytesUsed());
            result.setLastModifiedDate(null);
        }

- FileSystem: Obtenir le fichier de jclouds par le nom du conteneur et le nom du dossier

.. code-block:: java

		File file = getFileFromJClouds(containerName, objectId);
        BasicFileAttributes basicAttribs = getFileAttributes(file);
        long size = Files.size(Paths.get(file.getPath()));
        if (null != file) {
            if (objectId != null) {
                result.setObjectName(objectId);
                result.setDigest(computeObjectDigest(containerName, objectId, VitamConfiguration.getDefaultDigestType()));
                result.setFileSize(size);
            } else {
                result.setObjectName(containerName);
                result.setDigest(null);
                result.setFileSize(getFolderUsedSize(file));
            }
            result.setType(containerName.split("_")[1]);
            result.setFileOwner("Vitam_" + containerName.split("_")[0]);
            result.setLastAccessDate(basicAttribs.lastAccessTime().toString());
            result.setLastModifiedDate(basicAttribs.lastModifiedTime().toString());
        }

4- Détail de l'implémentation HashFileSystem
--------------------------------------------

Logique d'implémentation

- /<storage-path> : défini par configuration

  * /container-name : sur les offres de stockage, cela est construit dans le CAS Manager par concaténation du type d'objet et du tenant . Cette configuration n'est pas la configuration cible (notamment par rapport à l'offre froide)

    + /0/a/b/c/<fichier> : avec 0abc les 4 premiers hexdigits du SHA-256 du nom du fichier stocké
