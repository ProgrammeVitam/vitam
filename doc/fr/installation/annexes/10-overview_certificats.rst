Vue d'ensemble de la gestion des certificats
############################################

Liste des suites cryptographiques & protocoles supportés par Vitam
==================================================================

Il est possible de consulter les *ciphers* supportés par Vitam dans deux fichiers disponibles sur ce chemin: `ansible-vitam/roles/vitam/templates/`

* Le fichier ``jetty-config.xml.j2``
    - La balise contenant l'attribut name="IncludeCipherSuites" référence les ciphers supportés
    - La balise contenant l'attribut name="ExcludeCipherSuites" référence les ciphers non supportés
* Le fichier ``java.security.j2``
    - La ligne jdk.tls.disabledAlgorithms renseigne les *ciphers* désactivés au niveau java

.. warning:: Les 2 balises concernant les *ciphers* sur le fichier jetty-config.xml.j2 sont complémentaires car elles comportent des wildcards (*) ; en cas de conflit, l'exclusion est prioritaire.

.. seealso:: Ces fichiers correspondent à la configuration recommandée ; celle-ci est décrite plus en détail dans le :term:`DAT` (chapitre sécurité).

Vue d'ensemble de la gestion des certificats
============================================

.. _pki-certificats:

.. only:: html

    .. figure:: images/pki-certificats.svg
        :align: center

        Vue d'ensemble de la gestion des certificats au déploiement

.. only:: latex

    .. figure:: images/pki-certificats.png
        :align: center

        Vue d'ensemble de la gestion des certificats au déploiement

Description de l'arborescence de la :term:`PKI`
===============================================

Tous les fichiers de gestion de la :term:`PKI` se trouvent dans le répertoire ``deployment`` de l'arborescence Vitam:

* Le sous répertoire ``pki`` contient les scripts de génération des :term:`CA` & des certificats, les :term:`CA` générées par les scripts, et les fichiers de configuration d'openssl
* Le sous répertoire ``environments`` contient tous les certificats nécessaires au bon déploiement de Vitam:

    - certificats publics des :term:`CA`
    - Certificats clients, serveurs, de timestamping, et coffre fort contenant les mots de passe des clés privées des certificats (sous-répertoire ``certs``)
    - Magasins de certificats (keystores / truststores / grantedstores), et coffre fort contenant les mots de passe des magasins de certificats (sous-répertoire ``keystores``)

* Le script ``generate_stores.sh`` génère les magasins de certificats (keystores), cf la section :ref:`fonctionnementScriptsPki`

.. _arborescence_pki:

.. only:: html

    .. figure:: images/arborescence_pki.svg
        :align: center

        Vue l'arborescence de la :term:`PKI` Vitam

.. only:: latex

    .. figure:: images/arborescence_pki.png
        :align: center

        Vue l'arborescence de la :term:`PKI` Vitam


.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_pki.xmind <binary/arborescence_pki.xmind>`



Description de l'arborescence du répertoire deployment/environments/certs
=========================================================================

.. _arborescence_certs:

.. only:: html

    .. figure:: images/arborescence_certs.svg
        :align: center

        Vue détaillée de l'arborescence des certificats

.. only:: latex

    .. figure:: images/arborescence_certs.png
        :align: center

        Vue détaillée de l'arborescence des certificats

.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_certs.xmind <binary/arborescence_certs.xmind>`


Description de l'arborescence du répertoire deployment/environments/keystores
=============================================================================

.. _arborescence_keystores:

.. only:: html

    .. figure:: images/arborescence_keystores.svg
        :align: center

        Vue détaillée de l'arborescence des keystores

.. only:: latex

    .. figure:: images/arborescence_keystores.png
        :align: center

        Vue détaillée de l'arborescence des keystores

.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_keystores.xmind <binary/arborescence_keystores.xmind>`


.. _fonctionnementScriptsPki:


Fonctionnement des scripts de la :term:`PKI`
============================================

La gestion de la :term:`PKI` se fait avec 3 scripts dans le répertoire deployment de l'arborescence Vitam:

* ``pki/scripts/generate_ca.sh`` : génère des autorités de certifications (si besoin)
* ``pki/scripts/generate_certs.sh`` : génère des certificats à partir des autorités de certifications présentes (si besoin)

    - Récupère le mot de passe des clés privées à générer dans le vault ``environments/certs/vault-certs.yml``
    - Génère les certificats & les clés privées

* ``generate_stores.sh`` : génère les magasins de certificats nécessaires au bon fonctionnement de Vitam

    - Récupère le mot de passe du magasin indiqué dans ``environments/group_vars/all/vault-keystore.yml``
    - Insère les bon certificats dans les magasins qui en ont besoin

Si les certificats sont créés par la :term:`PKI` externe, il faut donc les positionner dans l'arborescence attendue avec le nom attendu pour certains (cf :ref:`l'image ci-dessus<arborescence_certs>`).
