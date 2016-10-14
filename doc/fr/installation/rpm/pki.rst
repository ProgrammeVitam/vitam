PKI
###

Génération des autorités de certification
=========================================

Dans le répertoire de déploiement, lancer le script : ``./pki-generate-ca.sh``

Ce script génère sous ``PKI/CA`` les certificats CA et intermédiaires pour client et server.



.. note::  bien noter les dates de création et de fin de validité des CA.

.. caution:: En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.


Génération des certificats & stores
===================================

.. warning:: cette étape n'est à effectuer que pour les clients ne possédant pas de certificats.

Dans le répertoire de déploiement, lancer le script : ``./generate_certsandstores.sh``

Ce script utilise le fichier ``environnements-rpm/group_vars/all/vault.yml`` qui est un fichier protégé. Le mot de passe de ce fichier sera demandé 3 fois et génèrera des certificats et stores adéquats au contenu du fichier yml.

Ce script génère sous ``PKI/certificats`` les certificats (format p12), ainsi que les stores (jks) associés pour un bon fonctionnemebnt dans VITAM.

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

Recopie des bons fichiers dans l'ansiblerie
============================================

Dans le répertoire de déploiement, lancer le script : ``./copie_fichiers_vitam.sh``

Ce script recopie les fichiers nécessaires (certificats, stores) au bons endrits de l'ansiblerie (sous ``ansible-vitam-rpm/roles/vitam/files/<composant>``).

