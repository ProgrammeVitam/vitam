==============
Common-storage
==============

Le common storage est un module commun pour plusieurs modules qui consiste à gérer des objets stockés dans un container et/ou dans dans un repertoire, ce module propose plusieurs offres de stockage (Jclouds), par exemple file Systeme et swift (open stack et ceph) configurabe par code (java) ou par fichier de configuration. Dans les chapitres suivants, on présentra les 2 modes du configuration

1- Présentation des APIs Java:
------------------------------------------------
1.1 - Introduction :

Le Module common storage expose un ensemble des methodes qui gèrent la creation, la mise à jour , la supprission des contenaire, des repertoires et des objets, Vous trouverez ci-dessous la liste des methodes avec leur fonctions attendus.

L'API principale est l'interface ContentAddressableStorage. Celle-ci a la hiérarchie de classe suivante :

- ContentAddressableStorageAbstract : classe abstraite implémentant quelques méthodes communes

  * HashFileSystem : implémentation d'un CAS sur FileSystem (via java.nio.*) avec une répertoire par sous-répertoire permettant un stockage d'un grand nombre d'objets (jusqu'à 500e6 objets )
  * ContentAddressableStorageJcloudsAbstract : classe abstraite implémentant la plupart des méthodes pour une implémentation jclouds sous-jacente

    + FileSystem : implémentation d'un CAS sur FileSystem (via jclouds) avec une répertoire à plat sous les container
    + OpenstackSwift : classe d'implémentation permettant le stockage sur Swift (via jclouds)

1.2 - Liste des méthodes :

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

- getObjectMetadatas: lire et récupérer les métadonnées d'un objet (le fichier ou le répertoire)

	- Paramètres :

    	- containerName::String (le nom de contenaire dans lequel qu'on stock l'object)
    	- objectId::String (Id de l'object. S'il est null, c'est-à-dire, il est un repertoire)

    - Retourner :

    	- MetadatasObject: La classe qui contient les informations de metadata

    		- objectName: l'ID du fichier
    		- type: le type (dossier comme Units, Binary, ObjectGroup, Reports, ...)
    		- digest: l'empreinte
    		- fileOwner: propriétaire
    		- fileSize: taille du fichier
    		- lastAccessDate: date de dernier accès
    		- lastModifiedDate: date de modification des données


Dans le cas echéant la method retourne une immuatable empty list.

	- uncompressObject : cette méthode extracte des fichiers compressés toute en indiquant le type de l'archive, pour cette version (v0.14.0) supporte 4 types : zip, tar, tar.gz et tar.bz2.

		-Paramètres :

			- containerName::String : c'est le nom de container dans lequel on stocke les objets
			- folderName::String : c'est le repertoire racine .

            - archiveType :: String : c'est le nom ou le type de l'archive (exemple: application/zip , application/x-tar)

			- compressedInputStream::InputStream c'est le stream des objets compressés

    - retourner :

Dans le cas echéant (uncompress KO) la methode génère une exception avec un message internal server.

2 - Configuration
------------------

La première chose que nous devons faire est d'ajouter la dépendance maven dans le pom.xml du projet. Après il faut configurer le contexte de stockage souhaités (filesystem/swift ceph/ swift openStack), (on traitera cette problématique au chapitre 2.1 et 2.2)

.. code-block:: xml

  <dependency>
       <groupId>fr.gouv.vitam</groupId>
       <artifactId>common-storage<artifactId>
       <version>x.x.x</version>
  </dependency>

La configuration de l'offre de stockage est basé sur plusieurs paramètres:

  - provider :: String : le type de l'offre de stockage (valeur par defaut: filesystem, valeur possibles : openstack-swift , filesystem ou chaîne vide)
  - swiftKeystoneAuthUrl* :: String : URL d'authentification keystone
  - swiftUser* :: String : le nom de l'utilisateur (sur rados, il prend la forme <tenant>$<user>)
  - storagePath :: String : path de stockage pour l'offre FileSystem

2.1 - Configuration par code:

2.1.a Exemple file systeme:

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

Il y a deux classes qui héritent les APIs. l'une utilise SWIFT et l'autre utilise FileSystem.

3.2 - Liste des méthodes :

3.2.1 getObjectInformation :

- SWIFT: Obtenir l'objet par les APIs swift

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
