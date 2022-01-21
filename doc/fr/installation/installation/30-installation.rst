Procédure de première installation
##################################


.. |repertoire_deploiement| replace:: ``deployment/``
.. |repertoire_inventory| replace:: ``environments/``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam/``

Déploiement
===========

Cas particulier : utilisation de ClamAv en environnement Debian
---------------------------------------------------------------

Dans le cas de l'installation en environnement Debian, la base de données n'est pas intégrée avec l'installation de ClamAv, C'est la commande ``freshclam`` qui en assure la charge. Si vous n'êtes pas connecté à internet, la base de données doit être installée manuellement. Les liens suivants indiquent la procédure à suivre: `Installation ClamAv <https://www.clamav.net/documents/installing-clamav>`_ et `Section Virus Database <https://www.clamav.net/downloads>`_

Fichier de mot de passe des vaults ansible
-------------------------------------------

Par défaut, le mot de passe des `vault` sera demandé à chaque exécution d'ansible avec l'utilisation de l'option ``--ask-vault-pass`` de la commande ``ansible-playbook``.

Pour simplifier l'exécution des commandes ``ansible-playbook``, vous pouvez utiliser un fichier |repertoire_deploiement|``vault_pass.txt`` contenant le mot de passe des fichiers vault. Ainsi, vous pouvez utiliser l'option ``--vault-password-file=vault_pass.txt`` à la place de l'option ``--ask-vault-pass`` dans les différentes commandes de cette documentation.

.. Warning:: Il est déconseillé de conserver le fichier ``vault_pass.txt`` sur la machine de déploiement ansible car ce fichier permet d'avoir accès à l'ensemble des secrets de :term:`VITAM`.


Mise en place des repositories VITAM (optionnel)
------------------------------------------------

:term:`VITAM` fournit un playbook permettant de définir sur les partitions cible la configuration d'appel aux repositories spécifiques à :term:`VITAM` :


Editer le fichier |repertoire_inventory|``group_vars/all/repositories.yml`` à partir des modèles suivants (décommenter également les lignes) :

Pour une cible de déploiement CentOS :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/repositories_centos.yml.example
   :language: yaml
   :linenos:


Pour une cible de déploiement Debian :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/repositories_debian.yml.example
   :language: yaml
   :linenos:

Ce fichier permet de définir une liste de repositories. Décommenter et adapter à votre cas.

Pour mettre en place ces repositories sur les machines cibles, lancer la commande :

.. code-block:: console

  ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/hosts.<environnement> --ask-vault-pass

.. note:: En environnement CentOS, il est recommandé de créer des noms de *repository* commençant par `vitam-` .

Génération des *hostvars*
-------------------------

Une fois l'étape de :term:`PKI` effectuée avec succès, il convient de procéder à la génération des *hostvars*, qui permettent de définir quelles interfaces réseau utiliser.
Actuellement la solution logicielle :term:`VITAM` est capable de gérer 2 interfaces réseau :

    - Une d'administration
    - Une de service

Cas 1: Machines avec une seule interface réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles :term:`VITAM` sera déployé ne disposent que d'une interface réseau, ou si vous ne souhaitez en utiliser qu'une seule, il convient d'utiliser le playbook |repertoire_playbook ansible|``generate_hostvars_for_1_network_interface.yml``

Cette définition des host_vars se base sur la directive ansible ``ansible_default_ipv4.address``, qui se base sur l'adresse :term:`IP` associée à la route réseau définie par défaut.

.. Warning:: Les communications d'administration et de service transiteront donc toutes les deux via l'unique interface réseau disponible.

Cas 2: Machines avec plusieurs interfaces réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles :term:`VITAM` sera déployé disposent de plusieurs interfaces et si celles-ci respectent cette règle:

    - Interface nommée eth0 = ip_service
    - Interface nommée eth1 = ip_admin

Alors il est possible d'utiliser le playbook ``ansible-vitam-extra/generate_hostvars_for_2_network_interfaces.yml``

.. Note:: Pour les autres cas de figure, il sera nécessaire de générer ces hostvars à la main ou de créer un script pour automatiser cela.

Vérification de la génération des hostvars
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A l'issue, vérifier le contenu des fichiers générés sous |repertoire_inventory|``host_vars/`` et les adapter au besoin.

.. caution:: Cas d'une installation multi-sites. Sur site secondaire, s'assurer que, pour les machines hébergeant les offres, la directive ``ip_wan`` a bien été déclarée (l'ajouter manuellement, le cas échéant), pour que site le site `primaire` sache les contacter via une IP particulière. Par défaut, c'est l'IP de service qui sera utilisée.



Déploiement
-----------

Une fois les étapes précédentes correctement effectuées (en particulier, la section :ref:`pkistores`), le déploiement s'effectue depuis la machine `ansible` et va distribuer la solution :term:`VITAM` selon l'inventaire correctement renseigné.

Une fois l'étape de la génération des hosts effectuée avec succès, le déploiement est à réaliser avec la commande suivante :

.. code-block:: console

   ansible-playbook ansible-vitam/vitam.yml -i environments/hosts.<environnement> --ask-vault-pass


.. note:: Une confirmation est demandée pour lancer ce script. Il est possible de rajouter le paramètre ``-e confirmation=yes`` pour bypasser cette demande de confirmation (cas d'un déploiement automatisé).

.. note:: Il est également possible de forcer la suppression de profils de sécurité et de leurs données associées (contextes applicatifs et certificats) en ajoutant le paramètre ``-e delete_security_profiles=yes``. Cela peut éventuellement être utile dans le cas d'un nouveau lancement de l'installation suite à un échec.

.. caution:: Dans le cas où l'installateur souhaite utiliser un `repository` de binaires qu'il gère par lui-même, il est fortement recommandé de rajouter ``--skip-tags "enable_vitam_repo"`` à la commande ``ansible-playbook`` ; dans ce cas, le comportement de ``yum`` n'est pas impacté par la solution de déploiement.
