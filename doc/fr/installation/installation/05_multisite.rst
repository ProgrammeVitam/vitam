Cas particulier d'une installation multi-sites
###############################################

Procédure d'installation
========================

Dans le cadre d'une installation multi-sites, il est nécessaire de déployer la solution logicielle :term:`VITAM` sur le site secondaire dans un premier temps, puis déployer le site "production".

Il est nécessaire de paramétrer correctement un certain de variables ansible pour chaque site:

vitam_site_name
---------------

Fichier: ``deployment/environments/hosts.my_env``

Cette variable sert à définir le nom du site.
Elle doit être différente sur chaque site.

primary_site
------------

Fichier: ``deployment/environments/hosts.my_env``

Cette variable sert à définir si le site est primaire ou non.
Sur un vitam installé en mode multi site, un seul des sites doit avoir la valeur à true.
Sur les sites secondaires (primary_site: false), certains composants ne seront pas démarrés et apparaitront donc en orange sur consul.
Certains timers systemd seront en revanche démarrés pour mettre en place la reconstruction au fil de l'eau par exemple.

consul_remote_sites
-------------------

Fichier: ``deployment/environments/group_vars/all/cots_vars.yml``

Cette variable sert à référencer la liste des consul server des sites distants à celui qu'on est en train de configurer.

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

vitam_offers
------------

Fichier: ``deployment/environments/group_vars/all/offer_opts.yml``

Cette variable référence toutes les offres disponibles sur la totalité des sites vitam.

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

Cette variable référence la stratégie de stockage sur le site courant.
Si l'offre se situe sur un site distant, il est nécessaire de préciser le nom du site sur lequel elle se trouve comme dans l'exemple ci-dessous.
Il est fortement conseiller de prendre comme offre référente une des offres locale au site.
Les sites secondaires doivent uniquement écrire sur leur(s) offre(s) locale(s).

Exemple pour le site 1 (site primaire):

.. code-block:: yaml

    vitam_strategy:
        - name: offer-fs-1
          referent: true
        - name: offer-fs-2
          referent: false
          vitam_site_name: site2
        - name: offer-fs-3
          referent: false
          vitam_site_name: site3

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

plateforme_secret
-----------------

Fichier: ``deployment/environments/group_vars/all/vault-vitam.yml``

Cette variable stocke le secret de plateforme qui doit être commun entre tous les composants vitam de tous les sites.
La valeur doit donc être la même entre chaque site.

consul_encrypt
--------------

Fichier: ``deployment/environments/group_vars/all/vault-vitam.yml``

Cette variable stocke le secret de plateforme qui doit être commun entre tous les consul de tous les sites.
La valeur doit donc être la même entre chaque site.

Procédure de réinstallation
===========================

En prérequis, il est nécessaire d'attendre que tous les workflow et reconstructions (sites secondaires) en cours soient terminés.

Ensuite:

* Arrêter vitam sur le site primaire.
* Arrêter les sites sites secondaires.
* Redéployer vitam sur les sites secondaires.
* Redéployer vitam sur le site primaire

Flux entre Storage et offer
===========================

Dans le cas **d'appel en https entre les composants Storage et Offer**, il convient également de rajouter:

* Sur le site primaire
    * Dans le truststore de Storage: la CA ayant signé le certificat de l'Offer du site secondaire
* Sur le site secondaire
    * Dans le truststore de Offer: la CA ayant signé le certificat du Storage du site primaire
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

.. warning:: Pour toutes les copies de certificats indiquées ci-dessous, il est important de ne jamais en écraser, il faut donc renommer les fichiers si-nécessaire.

Déposer les CA du client storage du site 1 ``environments/certs/client-storage/ca/*`` dans le client storage du site 2 ``environments/certs/client-storage/ca/``.

Déposer le certificat du client storage du site 1 ``environments/certs/client-storage/clients/storage/*`` dans le client storage du site 2 ``environments/certs/client-storage/clients/storage/``.

Déposer les CA du serveur offer du site 2 ``environments/certs/server/ca/*`` dans le répertoire des CA serveur du site 1 ``environments/certs/server/ca/*``

Après la génération des keystores
---------------------------------

Via le script deployment/generate_stores.sh, il convient donc de rajouter les CA et certificats indiqués sur le schéma ci-dessus.

Ajout d'un certificat:
``keytool -import -keystore -file <certificat.crt> -alias <alias_certificat>``

Ajout d'une CA:
``keytool -import -trustcacerts -keystore -file <ca.crt> -alias <alias_certificat>``
