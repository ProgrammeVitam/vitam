.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

Configuration du déploiement
############################

.. seealso:: L'architecture de la solution logicielle, les éléments de dimensionnement ainsi que les principes de déploiement sont définis dans le :term:`DAT`.


Fichiers de déploiement
=======================

Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le sous-répertoire |repertoire_deploiement| . Concernant l'installation, ils consistent en 2 parties :

 * les playbook ansible de déploiement, présents dans le sous-répertoire |repertoire_playbook ansible|, qui est indépendant de l'environnement à déployer ; ces fichiers ne sont normalement pas à modifier pour réaliser une installation.
 * l'arborescence d'inventaire ; des fichiers d'exemple sont disponibles dans le sous-répertoire |repertoire_inventory|. Cette arborescence est valable pour le déploiement d'un environnement, et est à dupliquer lors de l'installation d'environnements ultérieurs. Les fichiers qui y sont contenus doivent être adaptés avant le déploiement, comme il est expliqué dans les paragraphes suivants.


.. _inventaire:

Informations "plate-forme"
==========================

Pour configurer le déploiement, il est nécessaire de créer dans le répertoire |repertoire_inventory| un nouveau fichier d'inventaire (dans la suite, ce fichier sera communément appelé  ``hosts.<environnement>``). Ce fichier doit être basé sur la structure présente dans le fichier ``hosts.example`` (et notamment respecter scrupuleusement l'arborescence des groupes ansible) ; les commentaires dans ce fichier donnent les explications permettant l'adaptation à l'environnement cible :

.. literalinclude:: ../../../../deployment/environments/hosts.example
   :language: ini
   :linenos:

Pour chaque type de "host", indiquer le(s) serveur(s) défini(s) pour chaque fonction. Une colocalisation de composants est possible (Cf. le paragraphe idoine du :term:`DAT`)

.. note:: Concernant le groupe "hosts-consul-server", il est nécessaire de déclarer un minimum de 3 machines.


La configuration des droits d'accès à VITAM est ralisée dans le fichier |repertoire_inventory| ``/group_vars/all/vitam_security.yml``, comme suit :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/vitam_security.yml
     :language: yaml
     :linenos:


Enfin, la déclaration des configuration des offres de stockage est réalisée dans le fichier |repertoire_inventory| ``/group_vars/all/offers_opts.yml`` :

 .. literalinclude:: ../../../../deployment/environments/group_vars/all/offers_opts.yml
     :language: yaml
     :linenos:

Se référer aux commentaires dans le fichier pour le renseigner correctement.

.. warning:: Ne pas oublier, en cas de connexion à un keystone en https, de répercuter dans la :term:`PKI` la clé publique de la CA du keystone.


Déclaration des secrets
=======================
.. warning:: Cette section décrit des fichiers contenant des données sensibles ; il convient de sécuriser ces fichiers avec un mot de passe "fort". En cas d'usage d'un fichier de mot de passe ("vault-password-file"), il faut renseigner ce mot de passe comme contenu du fichier et ne pas oublier de sécuriser ou supprimer ce fichier à l'issue de l'installation.


Les secrets utilisés par la solution logicielle (en-dehors des certificats qui sont abordés dans une section ultérieure) sont définis dans des fichiers chiffrés par ansible-vault.

.. important:: Tous les vault présents dans l'arborescence d'inventaire doivent être tous protégés par le même mot de passe !



La première étape consiste à changer les mots de passe de tous les vault présents dans l'arborescence de déploiement (le mot de passe par défaut est contenu dans le fichier ``vault_pass.txt``) à l'aide de la commande ``ansible-vault rekey <fichier vault>``.

2 vaults sont principalement utilisés dans le déploiement d'une version ; leur contenu est donc à modifier avant tout déploiement :

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-vitam.yml`` contient les secrets généraux :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-vitam.example
     :language: ini
     :linenos:

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-keystores.yml`` contient les mots de passe des magasins de certificats utilisés dans VITAM :
  
  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-keystores.example
     :language: ini
     :linenos:

.. warning:: il convient de sécuriser votre environnement en définissant des mots de passe "forts".

Cas des extra
--------------

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-extra.yml`` contient les mot de passe des magasins de certificats utilisés dans VITAM :
  
  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-extra.example
     :language: ini
     :linenos:



.. note:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.
