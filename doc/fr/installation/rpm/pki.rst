Explications relatives à la PKI
###############################

Les commandes sont à passer dans le sous-répertoire ``deployment`` de la livraison.

Valorisation des variables propres à l'environnement
====================================================

.. note:: Afin de réaliser l'étape ci-dessous, le mot de passe par défaut du fichier ``vault.yml`` se situe dans le fichier ``vault_pass.txt``. Après avoir changé ce mot de passe, ne pas oublier de le mettre à jour dans le fichier ``vault_pass.txt``.


Le fichier ``environments-rpm/group_vars/all/vault.yml`` a été généré avec un mot de passe (change_it) ; le changer par la commande :

.. code-block:: bash

   ansible-vault rekey environments-rpm/group_vars/all/vault.yml

Pour modifier et adapter au besoin le "vault" (qui, pour rappel, contient les mots de passe sensibles de la plate-forme), éditer le fichier avec la commande :

.. code-block:: bash

   ansible-vault edit environments-rpm/group_vars/all/vault.yml


Puis, éditer le fichier ``environments-rpm/hosts.<environnement>`` et le mettre en conformité de l'environnement souhaité. Ce fichier est l'inventaire associé au playbook de déploiement VITAM et il est décrit dans le paragraphe :ref:`inventaire`


.. note:: les scripts des étapes suivantes utilisent ``environments-rpm/group_vars/all/vault.yml`` et, s'il existe, le fichier ``vault_pass.txt`` qui contient le mot de passe du fichier ``vault``. Si ``vault_pass.txt`` n'existe pas, le mot de passe de ``environments-rpm/group_vars/all/vault.yml`` sera demandé.

.. caution:: par la suite, le terme <environnement> correspond à l'extension du nom de fichier d'inventaire.


Génération des autorités de certification
=========================================

Cas d'une PKI inexistante
--------------------------

Dans le répertoire de déploiement, lancer le script :

.. code-block:: bash

   ./pki-generate-ca.sh


Ce script génère sous ``PKI/CA`` les certificats CA et intermédiaires pour client et server.

Voici ci-dessous un exemple de rendu du script :

.. code-block:: bash

	Lancement de la procédure de création d'une CA
	==============================================
	Répertoire ./PKI/CA absent ; création...
	Création du répertoire de travail temporaire newcerts sous ./PKI/newcerts...
		Création de CA root pour server...
		Create CA request...
	Generating a 512 bit RSA private key
	...............++++++++++++
	...++++++++++++
	writing new private key to './PKI/CA/server/ca.key'
	-----
		Create CA certificate...
	Using configuration from ./PKI/config/server/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :T61STRING:'CA_server'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 14 15:44:32 2026 GMT (3650 days)

	Write out database with 1 new entries
	Data Base Updated
		CA root pour server créée sous ./PKI/CA/server !
		Création de la CA intermediate pour server...
		Generate intermediate request...
	Generating a 4096 bit RSA private key
	..............................................................................................................................................................................................++
	.................................................++
	writing new private key to './PKI/CA/server_intermediate/ca.key'
	-----
		Sign...
	Using configuration from ./PKI/config/server/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :T61STRING:'CA_server_intermediate'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 14 15:44:33 2026 GMT (3650 days)

	Write out database with 1 new entries
	Data Base Updated
		CA intemédiaire server créée sous ./PKI/CA/server_intermediate !
	----------------------------------------------------------------------
		Création de CA root pour client...
		Create CA request...
	Generating a 512 bit RSA private key
	.....++++++++++++
	..................++++++++++++
	writing new private key to './PKI/CA/client/ca.key'
	-----
		Create CA certificate...
	Using configuration from ./PKI/config/client/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :T61STRING:'CA_client'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 14 15:44:33 2026 GMT (3650 days)

	Write out database with 1 new entries
	Data Base Updated
		CA root pour client créée sous ./PKI/CA/client !
		Création de la CA intermediate pour client...
		Generate intermediate request...
	Generating a 4096 bit RSA private key
	....................++
	............................................................................................................++
	writing new private key to './PKI/CA/client_intermediate/ca.key'
	-----
		Sign...
	Using configuration from ./PKI/config/client/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :T61STRING:'CA_client_intermediate'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 14 15:44:34 2026 GMT (3650 days)

	Write out database with 1 new entries
	Data Base Updated
		CA intemédiaire client créée sous ./PKI/CA/client_intermediate !
	----------------------------------------------------------------------
	==========================================================================
	Fin du shell

.. note::  bien noter les dates de création et de fin de validité des CA. En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.

Cas d'une CA déjà existante
----------------------------

Si le client possède déjà une :term:`PKI`, ou ne compte pas utiliser la :term:`PKI` fournie par VITAM, il convient de positionner les fichiers ``ca.crt`` et ``ca.key`` sous ``PKI/CA/<usage>``, où usage est :

- server
- server_intermediate
- client
- client_intermediate


Génération des certificats
==========================

Cas de certificats inexistants
-------------------------------

.. warning:: cette étape n'est à effectuer que pour les clients ne possédant pas de certificats.

Editer complètement le fichier ``environments-rpm/<inventaire>``  pour indiquer les serveurs associés à chaque service.

Puis, dans le répertoire de déploiement, lancer le script :


.. code-block:: bash

   ./generate_certs.sh <environnement>

Ci-dessous un exemple de sortie du script :

.. code-block:: bash

	Sourcer les informations nécessaires dans vault.yml
	Generation du certificat client de ihm-demo
		Création du certificat  pour ihm-demo hébergé sur localhost.localdomain...
		Generation de la clé...
	Generating a 4096 bit RSA private key
	..............................................................................................++
	.....................................................................................................................................................................................................++
	writing new private key to './PKI/certificats/client/ihm-demo/ihm-demo.key'
	-----
		Generation du certificat signé avec client...
	Using configuration from ./PKI/config/client/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :PRINTABLE:'ihm-demo'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 16 15:48:11 2019 GMT (1095 days)

	Write out database with 1 new entries
	Data Base Updated
		Conversion en p12...
		Fin de conversion sous ./PKI/certificats/client/ihm-demo/ !
	Fin de génération du certificat client de ihm-demo
	--------------------------------------------------
	Generation du certificat client de ihm-recette
		Création du certificat  pour ihm-recette hébergé sur localhost.localdomain...
		Generation de la clé...
	Generating a 4096 bit RSA private key
	................................++
	..........................................................++
	writing new private key to './PKI/certificats/client/ihm-recette/ihm-recette.key'
	-----
		Generation du certificat signé avec client...
	Using configuration from ./PKI/config/client/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :PRINTABLE:'ihm-recette'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 16 15:48:11 2019 GMT (1095 days)

	Write out database with 1 new entries
	Data Base Updated
		Conversion en p12...
		Fin de conversion sous ./PKI/certificats/client/ihm-recette/ !
	Fin de génération du certificat client de ihm-recette
	--------------------------------------------------
	Generation du certificat server de ingest-external
		Génération pour vitam-iaas-app-01.int...
		Création du certificat server pour ingest-external hébergé sur vitam-iaas-app-01.int...
		Generation de la clé...
	Generating a 4096 bit RSA private key
	..................................................++
	..........................................................++
	writing new private key to './PKI/certificats/server/hosts/vitam-iaas-app-01.int/ingest-external.key'
	-----
		Generation du certificat signé avec CA server...
	Using configuration from ./PKI/config/server/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :PRINTABLE:'ingest-external.service.consul'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 16 15:48:12 2019 GMT (1095 days)

	Write out database with 1 new entries
	Data Base Updated
		Conversion en p12...
		Fin de conversion sous ./PKI/certificats/server/hosts/vitam-iaas-app-01.int/ !
	Fin de génération du certificat server de ingest-external
	---------------------------------------------------------
	Generation du certificat server de access-external
		Génération pour vitam-iaas-app-01.int...
		Création du certificat server pour access-external hébergé sur vitam-iaas-app-01.int...
		Generation de la clé...
	Generating a 4096 bit RSA private key
	.............++
	.......................................................................................................................................................................................................++
	writing new private key to './PKI/certificats/server/hosts/vitam-iaas-app-01.int/access-external.key'
	-----
		Generation du certificat signé avec CA server...
	Using configuration from ./PKI/config/server/ca-config
	Check that the request matches the signature
	Signature ok
	The Subject's Distinguished Name is as follows
	commonName            :PRINTABLE:'access-external.service.consul'
	organizationName      :PRINTABLE:'Vitam.'
	countryName           :PRINTABLE:'FR'
	stateOrProvinceName   :PRINTABLE:'idf'
	localityName          :PRINTABLE:'paris'
	Certificate is to be certified until Nov 16 15:48:14 2019 GMT (1095 days)

	Write out database with 1 new entries
	Data Base Updated
		Conversion en p12...
		Fin de conversion sous ./PKI/certificats/server/hosts/vitam-iaas-app-01.int/ !
	Fin de génération du certificat server de access-external
	---------------------------------------------------------
	=============================================================================================
	Fin de script.


Ce script génère sous ``PKI/certificats`` les certificats (format p12) nécessaires pour un bon fonctionnement dans VITAM.

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

Cas de certificats déjà créés par le client
--------------------------------------------

Si le client possède déjà une :term:`PKI`, ou ne compte pas utiliser la :term:`PKI` fournie par VITAM, il convient de positionner les certificats sous ``PKI/certificats/<usage>``, où usage est :

- client/ihm-recette/ihm-recette.p12
- client/ihm-demo/ihm-recette.crt
- client/ihm-demo/ihm-demo.p12
- client/ihm-demo/ihm-demo.crt
- server/hosts/<hostname défini dans l'inventaire>/<nom composant vitam>.p12 pour
	- ingest-external
	- access-external


Génération des stores
=====================

Lancer le script :

.. code-block:: bash

   ./generate_stores.sh <environnement>

Ci-dessous un exemple de sortie du script :

.. code-block:: bash

	Sourcer les informations nécessaires dans vault.yml
	Génération du keystore de ihm-demo
		Génération pour vitam-iaas-ext-01.int...
	Génération du truststore de ihm-demo...
		Import des CA server dans truststore de ihm-demo...
			... import CA server root...
	Certificat ajouté au fichier de clés
			... import CA server intermediate...
	Certificat ajouté au fichier de clés
			... import CA client root...
	Certificat ajouté au fichier de clés
			... import CA client intermediate...
	Certificat ajouté au fichier de clés
	Fin de génération du trustore de ihm-demo
	------------------------------------------------
	Génération du keystore de ihm-recette
		Génération pour vitam-iaas-ext-01.int...
	Génération du truststore de ihm-recette...
		Import des CA server dans truststore de ihm-recette...
			... import CA server root...
	Certificat ajouté au fichier de clés
			... import CA server intermediate...
	Certificat ajouté au fichier de clés
			... import CA client root...
	Certificat ajouté au fichier de clés
			... import CA client intermediate...
	Certificat ajouté au fichier de clés
	Fin de génération du trustore de ihm-recette
	------------------------------------------------
	Génération du keystore de access-external
		Génération pour vitam-iaas-app-01.int...
		Import du p12 de ingest-external dans le keystore
	L'entrée de l'alias vitam-iaas-app-01.int a été importée.
	Commande d'import exécutée : 1 entrées importées, échec ou annulation de 0 entrées
	Fin de génération du keystore ingest-external
	---------------------------------------------
	Génération du truststore de ingest-external...
		Import des CA server dans truststore de ingest-external...
			... import CA server root...
	Certificat ajouté au fichier de clés
			... import CA server intermediate...
	Certificat ajouté au fichier de clés
			... import CA client root...
	Certificat ajouté au fichier de clés
			... import CA client intermediate...
	Certificat ajouté au fichier de clés
	Fin de génération du trustore de ingest-external
	------------------------------------------------
	Génération du grantedstore de ingest-external...
		Import certificat IHM-demo & ihm-recette du grantedstore de ingest-external...
	Certificat ajouté au fichier de clés
	Certificat ajouté au fichier de clés
	------------------------------------------------
	Génération du keystore de access-external
		Génération pour vitam-iaas-app-01.int...
		Import du p12 de access-external dans le keystore
	L'entrée de l'alias vitam-iaas-app-01.int a été importée.
	Commande d'import exécutée : 1 entrées importées, échec ou annulation de 0 entrées
	Fin de génération du keystore access-external
	---------------------------------------------
	Génération du truststore de access-external...
		Import des CA server dans truststore de access-external...
			... import CA server root...
	Certificat ajouté au fichier de clés
			... import CA server intermediate...
	Certificat ajouté au fichier de clés
			... import CA client root...
	Certificat ajouté au fichier de clés
			... import CA client intermediate...
	Certificat ajouté au fichier de clés
	Fin de génération du trustore de access-external
	------------------------------------------------
	Génération du grantedstore de access-external...
		Import certificat IHM-demo & ihm-recette du grantedstore de access-external...
	Certificat ajouté au fichier de clés
	Certificat ajouté au fichier de clés
	------------------------------------------------
	=============================================================================================
	Fin de script.



Ce script génère sous ``PKI/certificats`` les stores (jks) associés pour un bon fonctionnement dans VITAM.

Recopie des bons fichiers dans l'ansiblerie
============================================

Lancer le script :

.. code-block:: bash

   ./copie_fichiers_vitam.sh <environnement>


Ci-dessous un exemple de sortie du script :

.. code-block:: bash

	Recopie des stores dans VITAM
		Recopie pour access-external...
		Fichiers recopiés
	------------------------
		Recopie pour ingest-external...
		Fichiers recopiés
	------------------------
		Recopie pour ihm-demo...
		Fichiers recopiés
	------------------------
		Recopie pour ihm-recette...
		Fichiers recopiés
	------------------------
	=============================================================================================
	Fin de procédure ; vous pouvez déployer l'ansiblerie.


Ce script recopie les fichiers nécessaires (certificats, stores) aux bons endroits de l'ansiblerie (sous ``ansible-vitam-rpm/roles/vitam/files/<composant>``).

Cas des SIA
-----------

Pour le moment, la prise en charge des certificats des SIA n'est pas effective ; seuls les certificats d'ihm-demo et ihm-recette sont aujourd'hui intégrés dans l'installation.

.. hint:: Pour connecter un client externe à une instance de test Vitam, utiliser donc l'un des certificats cités (ihm-demo ou ihm-recette).
