Introduction sur les certificats dans Vitam
===========================================

Liste des suites cryptographiques & protocoles supportés par Vitam
------------------------------------------------------------------

Il est possible de consulter les ciphers supportés par Vitam dans deux fichiers disponibles sur ce chemin: `ansible-vitam/roles/vitam/templates/`

* Le fichier jetty-config.xml.j2
    - La balise contenant l'attribut name="IncludeCipherSuites" référence les ciphers supportés
    - La balise contenant l'attribut name="ExcludeCipherSuites" référence les ciphers non supportés
* Le fichier java.security.j2
    - La ligne jdk.tls.disabledAlgorithms renseigne les ciphers désactivés au niveau java

.. warning:: Les 2 balises concernant les ciphers sur le fichier jetty-config.xml.j2 sont complémentaires car elles comportent des wildcards (*) ; en cas de conflit, l'exclusion est prioritaire.

.. seealso:: Le DAT (chapitre sécurité) comporte des indications sur les ciphers autorisés.

Vue d'ensemble de la gestion des certificats
--------------------------------------------

.. _pki-certificats:
.. figure:: images/pki-certificats.*
    :align: center

    Vue d'ensemble de la gestion des certificats au déploiement


Description de l'arborescence de la PKI
---------------------------------------

Tous les fichiers de gestion de la PKI se trouvent dans le répertoire deployment de l'arborescence Vitam:

* Le sous répertoire pki contient les scripts de génération des CA & des certificats, les CA générées par les scripts, et les fichiers de configuration d'openssl
* Le sous répertoire environments contient tout tous les certificats nécessaires au bon déploiement de Vitam:

    - certificats publics des CA
    - Certificats clients, serveurs, de timestamping, et coffre fort contenant les mots de passe des clés privées des certificats (sous-répertoire ``certs``)
    - Magasins de certificats (keystores / truststores / grantedstores), et coffre fort contenant les mots de passe des magasins de certificats (sous-répertoire ``keystores``)

* Le script generate_stores.sh génère les magasins de certificats (keystores), cf la section :ref:`fonctionnementScriptsPki`

.. _arborescence_pki:
.. figure:: images/arborescence_pki.*
    :align: center
    :target: ../_images/arborescence_pki.svg

    Vue l'arborescence de la PKI Vitam

.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_pki.xmind <binary/arborescence_pki.xmind>`



Description de l'arborescence du répertoire deployment/environments/certs
-------------------------------------------------------------------------

.. _arborescence_certs:
.. figure:: images/arborescence_certs.*
    :align: center
    :target: ../_images/arborescence_certs.svg

    Vue détaillée de l'arborescence des certificats


.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_certs.xmind <binary/arborescence_certs.xmind>`


Description de l'arborescence du répertoire deployment/environments/keystores
-----------------------------------------------------------------------------

.. _arborescence_keystores:
.. figure:: images/arborescence_keystores.*
    :align: center
    :target: ../_images/arborescence_keystores.svg

    Vue détaillée de l'arborescence des keystores


.. only:: html

    Le fichier ci-dessus est aussi disponible au format xmind :download:`arborescence_keystores.xmind <binary/arborescence_keystores.xmind>`


.. _fonctionnementScriptsPki:


Fonctionnement des scripts de la PKI
------------------------------------

La gestion de la PKI se fait avec 3 scripts dans le répertoire deployment de l'arborescence Vitam:

* pki/scripts/generate_ca.sh : génère des autorités de certifications (si besoin)
* pki/scripts/generate_certs.sh : génère des certificats à partir des autorités de certifications présentes (si besoin)

    - Récupère le mot de passe des clés privées à générer dans le vault environments/certs/vault-certs.yml
    - Génère les certificats & les clés privées

* generate_stores.sh : génère les magasins de certificats nécessaires au bon fonctionnement de Vitam

    - Récupère le mot de passe du magasin indiqué dans environments/group_vars/all/vault-keystore.yml
    - Insère les bon certificats dans les magasins qui en ont besoin

Si les certificats sont créés par la PKI externe, il faut donc les positionner dans l'arborescence attendue avec le nom attendu pour certains (cf :ref:`arborescence_certs`)
