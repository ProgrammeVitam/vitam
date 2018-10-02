Multi-sites
###########

Procédure
===========

Dans le cadre d'une installation multi-sites, il est nécessaire de déployer la solution logicielle :term:`VITAM` sur le site secondaire dans un premier temps, puis déployer le site "production".

.. caution:: La variable ``secret_plateforme`` doit être commune sur les différents sites.


Flux entre Storage et offer
===========================

Dans le cas d'appel en https entre les composants Storage et Offer, il convient également de rajouter:

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

Après la génération des keystore via le script deployment/generate_stores.sh, il convient donc de rajouter les CA et certificats indiqués ci-dessus.

Ajout d'un certificat:
``keytool -import -keystore -file <certificat.crt> -alias <alias_certificat>``

Ajout d'une CA:
``keytool -import -trustcacerts -keystore -file <ca.crt> -alias <alias_certificat>``

Il est également possible de placer les fichiers CA et certificats directement dans les bons répertoires avant de lancer generate_stores.sh.