Cas 2: Configuration production
===============================


Procédure générale
------------------

La procédure suivante s'applique lorsqu'une :term:`PKI` est déjà disponible pour fournir les certificats nécessaires.

Les étapes d'intégration des certificats à la solution :term:`Vitam` sont les suivantes :

* Générer les certificats avec les bons `key usage` par type de certificat
* Déposer les certificats et les autorités de certifications correspondantes dans les bons répertoires.
* Renseigner les mots de passe des clés privées des certificats dans le vault ansible ``environments/certs/vault-certs.yml``
* Utiliser le script VITAM permettant de générer les différents *keystores*.

.. note:: Rappel pré-requis : vous devez disposer d'une ou plusieurs :term:`PKI` pour tout déploiement en production de la solution logicielle :term:`VITAM`.

Génération des certificats
--------------------------

En conformité avec le document RGSV2 de l'ANSSI, il est recommandé de générer des certificats avec les caractéristiques suivantes:

Certificats serveurs
^^^^^^^^^^^^^^^^^^^^

* Key Usage
    * digitalSignature, keyEncipherment
* Extended Key Usage
    * TLS Web Server Authentication

Les certificats serveurs générés doivent prendre en compte des alias "web" ( ``subjectAltName`` ).

Le *subjectAltName* des certificats serveurs ( ``deployment/environments/certs/server/hosts/*`` ) doit contenir le nom DNS du service sur consul associé.

Exemple avec un cas standard: <composant_vitam>.service.<consul_domain>.
Ce qui donne pour le certificat serveur de access-external par exemple:

.. code-block:: text

    X509v3 Subject Alternative Name:
        DNS:access-external.service.consul, DNS:localhost

Il faudra alors mettre le même nom de domaine pour la configuration de Consul (fichier ``deployment/environments/group_vars/all/vitam_vars.yml``, variable ``consul_domain`` )

Cas particulier pour ihm-demo et ihm-recette: il faut ajouter le nom :term:`DNS` qui sera utilisé pour requêter ces deux applications, si celles-ci sont appelées directement en frontal https.

Certificat clients
^^^^^^^^^^^^^^^^^^

* Key Usage
    * digitalSignature
* Extended Key Usage
    * TLS Web Client Authentication

Certificats d'horodatage
^^^^^^^^^^^^^^^^^^^^^^^^

Ces certificats sont à générer pour les composants ``logbook`` et ``storage``.

* Key Usage
    * digitalSignature, nonRepudiation
* Extended Key Usage
    * Time Stamping

Intégration de certificats existants
------------------------------------

Une fois les certificats et :term:`CA` mis à disposition par votre :term:`PKI`, il convient de les positionner sous ``environments/certs/....`` en respectant la structure indiquée ci-dessous.

.. only:: html

    .. figure:: ../../annexes/images/arborescence_certs.svg
        :align: center

        Vue détaillée de l'arborescence des certificats

.. only:: latex

    .. figure:: ../../annexes/images/arborescence_certs.png
        :align: center

        Vue détaillée de l'arborescence des certificats


.. tip::

    Dans le doute, n'hésitez pas à utiliser la :term:`PKI` de test (étapes de génération de :term:`CA` et de certificats) pour générer les fichiers requis au bon endroit et ainsi observer la structure exacte attendue ;
    il vous suffira ensuite de remplacer ces certificats "placeholders" par les certificats définitifs avant de lancer le déploiement.

Ne pas oublier de renseigner le vault contenant les *passphrases* des clés des certificats: ``environments/certs/vault-certs.yml``

Pour modifier/créer un vault ansible, se référer à la documentation Ansible sur `cette url <http://docs.ansible.com/ansible/playbooks_vault.html>`_.

.. caution:: Durant l'installation de VITAM, il est nécessaire de créer un certificat "vitam-admin-int" (à placer sous ``deployment/environments/certs/client-external/clients/vitam-admin-int``).

.. caution:: Durant l'installation des extra de VITAM, il est nécessaire de créer un certificat "gatling" (à placer sous ``deployment/environments/certs/client-external/clients/gatling``).


Intégration de certificats clients de :term:`VITAM`
---------------------------------------------------

.. _external_sia_certs_integration:

Intégration d'une application externe (cliente)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dans le cas d'ajout de certificats :term:`SIA` externes au déploiement de la solution logicielle :term:`VITAM` :

    * Déposer le certificat (``.crt``) de l'application client dans ``environments/certs/client-external/clients/external/``
    * Déposer les :term:`CA` du certificat de l'application (``.crt``) dans ``environments/certs/client-external/ca/``
    * Editer le fichier ``environments/group_vars/all/vitam_security.yml`` et ajouter le(s) entrée(s) supplémentaire(s) (sous forme répertoire/fichier.crt, exemple: ``external/mon_sia.crt``) dans la directive ``admin_context_certs`` pour que celles-ci soient associés aux contextes de sécurité durant le déploiement de la solution logicielle :term:`VITAM`.

.. note:: Les certificats :term:`SIA` externes ajoutés par le mécanisme de déploiement sont, par défaut, rattachés au contexte applicatif d'administration ``admin_context_name`` lui même associé au profil de sécurité ``admin_security_profile`` et à la liste de tenants ``vitam_tenant_ids`` (voir le fichier ``environments/group_vars/all/vitam_security.yml``). Pour l'ajout de certificats applicatifs associés à des contextes applicatifs autres, se référer à la procédure du document d'exploitation (:term:`DEX`) décrivant l'intégration d’une application externe dans Vitam.

.. _personal_certs_integration:

Intégration d'un certificat personnel (*personae*)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dans le cas d'ajout de certificats personnels au déploiement de la solution logicielle :term:`VITAM` :

    * Déposer le certificat personnel (``.crt``) dans ``environments/certs/client-external/clients/external/``
    * Editer le fichier ``environments/group_vars/all/vitam_security.yml`` et ajouter le(s) entrée(s) supplémentaire(s) (sous forme répertoire/fichier.crt, exemple: ``external/mon_personae.crt``) dans la directive ``admin_personal_certs`` pour que ceux-ci soient ajoutés à la base de donées du composant `security-internal` durant le déploiement de la solution logicielle :term:`VITAM`.

Cas des offres objet
-----------------------

Placer le ``.crt`` de la :term:`CA` dans ``deployment/environments/certs/server/ca``.


Absence d'usage d'un *reverse*
---------------------------------

Dans ce cas, il convient de :

- supprimer le répertoire ``deployment/environments/certs/client-external/clients/reverse``
- supprimer les entrées **reverse** dans le fichier ``vault_keystore.yml``
