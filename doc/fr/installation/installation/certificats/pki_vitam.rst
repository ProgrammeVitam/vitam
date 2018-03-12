
Cas 1: Je ne dispose pas de PKI, je souhaite utiliser celle de Vitam
====================================================================

Dans ce cas, il est nécessaire d'utiliser la :term:`PKI` fournie avec la solution logicielle VITAM.

Procédure générale
------------------

.. danger:: La :term:`PKI` fournie avec la solution logicielle Vitam ne doit être utilisée que pour faire des tests, et ne doit par conséquent surtout pas être utilisée en environnement de production !

La :term:`PKI` de la solution logicielle VITAM est une suite de scripts qui vont générer dans l'ordre ci-dessous:

- Les autorités de certifcation (CA)
- Les certificats (clients, serveurs, de timestamping) à partir des CA
- Les keystores, en important les certificats et CA nécessaires pour chacun des keystores


Génération des CA par les scripts Vitam
---------------------------------------

Il faut faire générer les autorités de certification par le script décrit ci-dessous.


Dans le répertoire de déploiement, lancer le script :

.. code-block:: console

   pki/scripts/generate_ca.sh


Ce script génère sous ``pki/ca`` les autorités de certification root et intermédiaires pour générer des certificats clients, serveurs, et de timestamping.

.. warning:: Bien noter les dates de création et de fin de validité des CA. En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.


Génération des certificats par les scripts Vitam
------------------------------------------------

Le fichier d'inventaire de déploiement ``environments/<fichier d'inventaire>`` (cf. :ref:`inventaire`) doit être correctement renseigné pour indiquer les serveurs associés à chaque service. En prérequis les CA doivent être présentes.

Puis, dans le répertoire de déploiement, lancer le script :


.. code-block:: console

    pki/scripts/generate_certs.sh <fichier d'inventaire>


Ce script génère sous ``environmements/certs`` les certificats (format crt & key) nécessaires pour un bon fonctionnement dans VITAM.
Les mots de passe des clés privées des certificats sont stockés dans le vault ansible environmements/certs/vault-certs.yml

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

.. include:: swift.rst

.. include:: keystores.rst
