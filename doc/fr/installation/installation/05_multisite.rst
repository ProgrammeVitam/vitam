Cas particulier d'une installation multi-sites
##############################################

Procédure d'installation
========================

Dans le cadre d'une installation multi-sites, il est nécessaire de déployer la solution logicielle :term:`VITAM` sur le site secondaire dans un premier temps, puis déployer le site `production`.

Il faut paramétrer correctement un certain nombre de variables ansible pour chaque site:

vitam_site_name
---------------

Fichier: ``deployment/environments/hosts.<environnement>``

Cette variable sert à définir le nom du site.
Elle doit être différente sur chaque site.

primary_site
------------

Fichier: ``deployment/environments/hosts.<environnement>``

Cette variable sert à définir si le site est primaire ou non.
Sur VITAM installé en mode multi site, un seul des sites doit avoir la valeur `primary_site` à true.
Sur les sites secondaires (primary_site: false), certains composants ne seront pas démarrés et apparaitront donc en orange sur l':term:`IHM` de consul.
Certains timers systemd seront en revanche démarrés pour mettre en place la reconstruction au fil de l'eau, par exemple.

consul_remote_sites
-------------------

Fichier: ``deployment/environments/group_vars/all/cots_vars.yml``

Cette variable sert à référencer la liste des `Consul Server` des sites distants, à celui que l'on configure.

Exemple de configuration pour une installation avec 3 sites.

Site 1:

.. code-block:: yaml

    consul_remote_sites:
        - dc2:
          wan: ["dc2-host-1","dc2-host-2","dc2-host-3"]
        - dc3:
          wan: ["dc3-host-1","dc3-host-2","dc3-host-3"]


Site 2:

.. code-block:: yaml

    consul_remote_sites:
        - dc1:
          wan: ["dc1-host-1","dc1-host-2","dc1-host-3"]
        - dc3:
          wan: ["dc3-host-1","dc3-host-2","dc3-host-3"]


Site 3:

.. code-block:: yaml

    consul_remote_sites:
        - dc1:
          wan: ["dc1-host-1","dc1-host-2","dc1-host-3"]
        - dc2:
          wan: ["dc2-host-1","dc2-host-2","dc2-host-3"]


Il faut également prévoir de déclarer, lors de l'installation de chaque site distant, la variable ``ip_wan`` pour les partitions hébergeant les serveurs Consul (groupe ansible ``hosts_consul_server``) et les offres de stockage (groupe ansible ``hosts_storage_offer_default``, considérées distantes par le site primaire).
Ces ajouts sont à faire dans ``environments/host_vars/<nom partition>``.

Exemple:

  ip_service: 172.17.0.10
  ip_admin: 172.19.0.10
  ip_wan: 10.2.64.3

Ainsi, à l'usage, le composant ``storage`` va appeler les services ``offer``. Si le service est "hors domaine" (déclaration explicite ``<service>.<datacenterdistant>.service.<domaineconsul>``), un échange d'information entre "datacenters" Consul est réalisé et la valeur de ``ip_wan`` est fournie pour l'appel au service distant.

vitam_offers
------------

Fichier: ``deployment/environments/group_vars/all/offer_opts.yml``

Cette variable référence toutes les offres disponibles sur la totalité des sites VITAM. Sur les sites secondaires, il suffit de référencer les offres disponible localement.

Exemple:

.. code-block:: yaml

    vitam_offers:
        offer-fs-1:
            provider: filesystem-hash
        offer-fs-2:
            provider: filesystem-hash
        offer-fs-3:
            provider: filesystem-hash


vitam_strategy
--------------

Fichier: ``deployment/environments/group_vars/all/offer_opts.yml``

Cette variable référence la stratégie de stockage de plateforme *default* sur le site courant.

Si l'offre se situe sur un site distant, il est nécessaire de préciser le nom du site, via la variable `vitam_site_name`, sur lequel elle se trouve comme dans l'exemple ci-dessous.

Il est fortement conseillé de prendre comme offre référente une des offres locale au site. Les sites secondaires doivent uniquement écrire sur leur(s) offre(s) locale(s).

Exemple pour le site 1 (site primaire):

.. code-block:: yaml

    vitam_strategy:
        - name: offer-fs-1
          referent: true
        - name: offer-fs-2
          referent: false
          distant: true
          vitam_site_name: site2
        - name: offer-fs-3
          referent: false
          distant: true
          vitam_site_name: site3
    # Optional params for each offers in vitam_strategy. If not set, the default values are applied.
    #    referent: false              # true / false (default), only one per site must be referent
    #    status: ACTIVE               # ACTIVE (default) / INACTIVE
    #    vitam_site_name: distant-dc2 # default is the value of vitam_site_name defined in your local inventory file, should be specified with the vitam_site_name defined for the distant offer
    #    distant: false               # true / false (default). If set to true, it will not check if the provider for this offer is correctly set
    #    id: idoffre                  # OPTIONAL, but IF ACTIVATED, MUST BE UNIQUE & SAME if on another site
    #    asyncRead: false             # true / false (default). Should be set to true for tape offer only


Exemple pour le site 2 (site secondaire):

.. code-block:: yaml

    vitam_strategy:
        - name: offer-fs-2
          referent: true


Exemple pour le site 3 (site secondaire):

.. code-block:: yaml

    vitam_strategy:
        - name: offer-fs-3
          referent: true


other_strategies
----------------

Fichier: ``deployment/environments/group_vars/all/offer_opts.yml``

Cette variable référence les stratégies de stockage additionnelles sur le site courant. **Elles ne sont déclarées et utilisées que dans le cas du multi-stratégies.**
Si l'offre se situe sur un site distant, il est nécessaire de préciser le nom du site sur lequel elle se trouve comme dans l'exemple ci-dessous.
Les sites secondaires doivent uniquement écrire sur leur(s) offre(s) locale(s).


Les offres correspondant à l'exemple ``other_strategies`` sont les suivantes:

.. code-block:: yaml

    vitam_offers:
        offer-fs-1:
            provider: filesystem-hash
        offer-fs-2:
            provider: filesystem-hash
        offer-fs-3:
            provider: filesystem-hash
        offer-s3-1:
            provider: amazon-s3-v1
        offer-s3-2:
            provider: amazon-s3-v1
        offer-s3-3:
            provider: amazon-s3-v1


Exemple pour le site 1 (site primaire):

.. code-block:: yaml

    other_strategies:
        metadata:
            - name: offer-fs-1
              referent: true
            - name: offer-fs-2
              referent: false
              distant: true
              vitam_site_name: site2
            - name: offer-fs-3
              referent: false
              distant: true
              vitam_site_name: site3
            - name: offer-s3-1
              referent: false
            - name: offer-s3-2
              referent: false
              distant: true
              vitam_site_name: site2
            - name: offer-s3-3
              referent: false
              distant: true
              vitam_site_name: site3
        binary:
            - name: offer-s3-1
              referent: false
            - name: offer-s3-2
              referent: false
              distant: true
              vitam_site_name: site2
            - name: offer-s3-3
              referent: false
              distant: true
              vitam_site_name: site3


Exemple pour le site 2 (site secondaire):

.. code-block:: yaml

    other_strategies:
        metadata:
            - name: offer-fs-2
              referent: true
            - name: offer-s3-2
              referent: false
        binary:
            - name: offer-s3-2
              referent: false


Exemple pour le site 3 (site secondaire):

.. code-block:: yaml

    other_strategies:
        metadata:
            - name: offer-fs-3
              referent: true
            - name: offer-s3-3
              referent: false
        binary:
            - name: offer-s3-3
              referent: false


plateforme_secret
-----------------

Fichier: ``deployment/environments/group_vars/all/vault-vitam.yml``

Cette variable stocke le `secret de plateforme` qui doit être commun à tous les composants de la solution logicielle :term:`VITAM` de tous les sites.
La valeur doit donc être identique pour chaque site.

consul_encrypt
--------------

Fichier: ``deployment/environments/group_vars/all/vault-vitam.yml``

Cette variable stocke le `secret de plateforme` qui doit être commun à tous les `Consul` de tous les sites.
La valeur doit donc être identique pour chaque site.

Procédure de réinstallation
===========================

En prérequis, il est nécessaire d'attendre que tous les `workflows` et reconstructions (sites secondaires) en cours soient terminés.

Ensuite:

* Arrêter vitam sur le site primaire.
* Arrêter les sites secondaires.
* Redéployer vitam sur les sites secondaires.
* Redéployer vitam sur le site primaire

Flux entre Storage et Offer
===========================

Dans le cas **d'appel en https entre les composants Storage et Offer**, il faut modifier ``deployment/environments/group_vars/all/vitam_vars.yml`` et indiquer ``https_enabled: true`` dans ``storageofferdefault``.

Il convient également également d'ajouter:

* Sur le site primaire
    * Dans le truststore de Storage: la :term:`CA` ayant signé le certificat de l'Offer du site secondaire
* Sur le site secondaire
    * Dans le truststore de Offer: la :term:`CA` ayant signé le certificat du Storage du site primaire
    * Dans le grantedstore de Offer: le certificat du storage du site primaire

.. only:: html

    .. figure:: ../annexes/images/certificats-multisite.png
        :align: center

        Vue détaillée des certificats entre le storage et l'offre en multi-site

.. only:: latex

    .. figure:: ../annexes/images/certificats-multisite.png
        :align: center

        Vue détaillée des certificats entre le storage et l'offre en multi-site

Il est possible de procéder de 2 manières différentes:

Avant la génération des keystores
---------------------------------

.. warning:: Pour toutes les copies de certificats indiquées ci-dessous, il est important de ne jamais les écraser, il faut donc renommer les fichiers si nécessaire.

Déposer les :term:`CA` du client storage du site 1 ``environments/certs/client-storage/ca/*`` dans le client storage du site 2 ``environments/certs/client-storage/ca/``.

Déposer le certificat du client storage du site 1 ``environments/certs/client-storage/clients/storage/*.crt`` dans le client storage du site 2 ``environments/certs/client-storage/clients/storage/``.

Déposer les :term:`CA` du serveur offer du site 2 ``environments/certs/server/ca/*`` dans le répertoire des :term:`CA` serveur du site 1 ``environments/certs/server/ca/*``

Après la génération des keystores
---------------------------------

Via le script ``deployment/generate_stores.sh``, il convient donc d'ajouter les :term:`CA` et certificats indiqués sur le schéma ci-dessus.

Ajout d'un certificat :
``keytool -import -keystore -file <certificat.crt> -alias <alias_certificat>``

Ajout d'une :term:`CA`:
``keytool -import -trustcacerts -keystore -file <ca.crt> -alias <alias_certificat>``
