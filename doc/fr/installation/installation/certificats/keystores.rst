
Génération des magasins de certificats
--------------------------------------

En prérequis, les certificats et les autorités de certification (:term:`CA`) doivent être présents dans les répertoires attendus.

.. caution:: Avant de lancer le script de génération des *stores*, il est nécessaire de modifier le vault contenant les mots de passe des *stores* : ``environments/group_vars/all/vault-keystores.yml``, décrit dans la section :ref:`pkiconfsection`.

Lancer le script : ``./generate_stores.sh``

Ce script génère sous ``environments/keystores`` les `stores` ( aux formats ``jks`` / ``p12``) associés pour un bon fonctionnement dans la solution logicielle :term:`VITAM`.

Il est aussi possible de déposer directement les `keystores` au bon format en remplaçant ceux fournis par défaut et en indiquant les mots de passe d'accès dans le vault: ``environments/group_vars/all/vault-keystores.yml``

.. note:: Le mot de passe du fichier ``vault-keystores.yml`` est identique à celui des autres `vaults` ansible.
