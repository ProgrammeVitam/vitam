PKI
###

Les commandes sont à passer dans le sous-répertoire ``deployment`` de la livraison.

Action préalable
================

Le fichier ``environnements-rpm/group_vars/all/vault.yml`` a été généré avec un mot de passe ; le changer par la commande :

``ansible-vault rekey environnements-rpm/group_vars/all/vault.yml``

Puis éditer le fichier ``environnements-rpm/<votre fichier d'inventaire>`` et le mettre en conformité de l'environnement souhaité.

Génération des autorités de certification
=========================================

Cas d'une PKI inexistante
--------------------------

Dans le répertoire de déploiement, lancer le script : ``./pki-generate-ca.sh``

Ce script génère sous ``PKI/CA`` les certificats CA et intermédiaires pour client et server.

.. note::  bien noter les dates de création et de fin de validité des CA.

.. caution:: En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.

Cas d'une CA déjà existante
----------------------------

Si le client possède déjà une :term:`PKI`, ou ne compte pas utiliser la :term:`PKI` fournie par VITAM, il convient de positionner les fichiers ``ca.crt`` et ``ca.key`` sous ``PKI/CA/<usage>``, où usage est :

- server
- server_intermediate
- client
- client_intermediate

.. todo:: droits Unix à vérifier

Génération des certificats
==========================

Cas de certificats inexistants
-------------------------------

.. warning:: cette étape n'est à effectuer que pour les clients ne possédant pas de certificats.

Editer le fichier ``environnements-rpm/group_vars/all/vault.yml`` ( qui est un fichier protégé par mot de passe ) pour indiquer les mots de passe nécessaires.

.. note:: Pour éditer le fichier, lancer la commande ``ansible-vault edit environnements-rpm/group_vars/all/vault.yml``


Editer le fichier ``environnements-rpm/<inventaire>``  pour indiquer les serveurs associés à chaque service.

Puis, dans le répertoire de déploiement, lancer le script : ``./generate_certs <environnement>``

.. note:: Ce script utilise le fichier ``environnements-rpm/group_vars/all/vault.yml``. Le mot de passe de ce fichier sera demandé plusieurs fois et génèrera des certificats et stores adéquats au contenu du fichier yml.

Ce script génère sous ``PKI/certificats`` les certificats (format p12) nécessaires pour un bon fonctionnement dans VITAM.

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

Cas de certificats déjà créés par le client
--------------------------------------------

.. todo:: procédure à écrire

Génération des stores
=====================

Editer le fichier ``environnements-rpm/group_vars/all/vault.yml`` ( qui est un fichier protégé par mot de passe ) pour indiquer les mots de passe nécessaires.

.. info:: Pour éditer le fichier, lancer la commande sous le répertoire deployment : ``ansible-vault edit environnements-rpm/group_vars/all/vault.yml``


Editer le fichier ``environnements-rpm/<inventaire>``  pour indiquer les serveurs associé à chaque service.

Puis, dans le répertoire de déploiement, lancer le script : ``./generate_stores.sh <environnement>``

.. note:: Ce script utilise le fichier ``environnements-rpm/group_vars/all/vault.yml``. Le mot de passe de ce fichier sera demandé plusieurs fois et génèrera des certificats et stores adéquats au contenu du fichier yml.

Ce script génère sous ``PKI/certificats`` les  les stores (jks) associés pour un bon fonctionnement dans VITAM.


Recopie des bons fichiers dans l'ansiblerie
============================================

Dans le répertoire de déploiement, lancer le script : ``./copie_fichiers_vitam.sh <environnement>``

Ce script recopie les fichiers nécessaires (certificats, stores) aux bons endroits de l'ansiblerie (sous ``ansible-vitam-rpm/roles/vitam/files/<composant>``).

Cas des SIA
-----------

Pour le moment, la prise en charge des certificats des SIA n'est pas effective.

