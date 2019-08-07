
Cas 1: Configuration développement / tests
==========================================

Pour des usages de développement ou de tests hors production, il est possible d'utiliser la :term:`PKI` fournie avec la solution logicielle :term:`VITAM`.

Procédure générale
------------------

.. danger:: La :term:`PKI` fournie avec la solution logicielle :term:`VITAM` doit être utilisée UNIQUEMENT pour faire des tests, et ne doit par conséquent surtout pas être utilisée en environnement de production ! De plus il n'est pas possible de l'utiliser pour générer les certificats d'une autre application qui serait cliente de VITAM.

La :term:`PKI` de la solution logicielle :term:`VITAM` est une suite de scripts qui vont générer dans l'ordre ci-dessous:

- Les autorités de certifcation (:term:`CA`)
- Les certificats (clients, serveurs, de *timestamping*) à partir des :term:`CA`
- Les *keystores*, en important les certificats et :term:`CA` nécessaires pour chacun des *keystores*


Génération des CA par les scripts Vitam
---------------------------------------

Il faut faire la génération des autorités de certification (:term:`CA`) par le script décrit ci-dessous.


Dans le répertoire de déploiement, lancer le script :

.. code-block:: console

   pki/scripts/generate_ca.sh


Ce script génère sous ``pki/ca`` les autorités de certification `root` et intermédiaires pour générer des certificats clients, serveurs, et de timestamping.
Les mots de passe des clés privées des autorités de certification sont stockés dans le vault ansible environments/certs/vault-ca.yml

.. warning:: Il est impératif de noter les dates de création et de fin de validité des CA. En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.


Génération des certificats par les scripts Vitam
------------------------------------------------

Le fichier d'inventaire de déploiement ``environments/<fichier d'inventaire>`` (cf. :ref:`inventaire`) doit être correctement renseigné pour indiquer les serveurs associés à chaque service. En prérequis les :term:`CA` doivent être présentes.

Puis, dans le répertoire de déploiement, lancer le script :


.. code-block:: console

    pki/scripts/generate_certs.sh <fichier d'inventaire>


Ce script génère sous ``environments/certs`` les certificats (format ``crt`` & ``key``) nécessaires pour un bon fonctionnement dans VITAM.
Les mots de passe des clés privées des certificats sont stockés dans le vault ansible ``environments/certs/vault-certs.yml``.

.. caution::  Les certificats générés à l'issue ont une durée de validité de 3 ans.
