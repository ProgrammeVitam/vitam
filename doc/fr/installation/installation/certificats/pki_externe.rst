
Cas 2: Configuration production
===============================


Procédure générale
------------------

La procédure suivante s'applique lorsqu'une :term:`PKI` est déjà disponible pour fournir les certificats nécessaires.

Les étapes d'intégration des certificats à la solution Vitam sont les suivantes :

* déposer les certificats et les autorités de certifications correspondantes dans les bons répertoires.
* renseigner les mots de passe des clés privées des certificats dans le vault ansible environmements/certs/vault-certs.yml
* utiliser le script Vitam permettant de générer les différents keystores.

.. note:: Rappel pré-requis : vous devez disposer d'une ou plusieurs :term:`PKI` pour tout déploiement en production de la solution VITAM.

Génération des certificats
--------------------------

Les certificats générés doivent prendre en compte des alias "web" (subjectAltName).

Le subjectAltName des certificats serveurs (deployment/environments/certs/server/hosts/*) doit contenir le nom dns du service sur consul.
Exemple avec un cas standard: <composant_vitam>.service.consul.
Ce qui donne pour le certificat serveur de access-external par exemple:

.. code-block:: text

    X509v3 Subject Alternative Name:
        DNS:access-external.service.consul, DNS:localhost

Intégration de certificats existants
------------------------------------

Une fois les certificats et CA mis à disposition par votre PKI, il convient de les positionner sous ``environmements/certs/....`` en respectant la structure indiquée ci-dessous.

.. only:: html

    .. figure:: ../../annexes/images/arborescence_certs.svg
        :align: center

        Vue détaillée de l'arborescence des certificats

.. only:: latex

    .. figure:: ../../annexes/images/arborescence_certs.png
        :align: center

        Vue détaillée de l'arborescence des certificats


.. tip::

    Dans le doute, n'hésitez pas à utiliser la PKI de test (étapes de génération de CA et de certificats) pour générer les fichiers requis au bon endroit et ainsi voir la structure exacte attendue ;
    il vous suffira ensuite de remplacer ces certificats "placeholders" par les certificats définitifs avant de lancer le déploiement.

Ne pas oublier de renseigner le vault contenant les passphrases des clés des certificats: ``environmements/certs/vault-certs.yml``

Dans le cas d'ajout de certificats :term:`SIA` externes, éditer le fichier ``environments/group_vars/all/vitam_security.yml`` et ajouter le(s) entrée(s) supplémentaire(s)  (sous forme répertoire/fichier.crt)
dans  la directive ``admin_context_certs`` pour que ceux-ci soient ajoutés aux profils de sécurité durant le déploiement de la solution logicielle :term:`VITAM`.

Pour modifier/créer un vault ansible, se référer à la documentation sur `cette url <http://docs.ansible.com/ansible/playbooks_vault.html>`_.

.. include:: swift.rst

.. include:: keystores.rst
