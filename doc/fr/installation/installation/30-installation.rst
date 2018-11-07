Procédure de première installation
##################################


.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

Déploiement
===========

Cas particulier : utilisation de ClamAv en environnement Debian
---------------------------------------------------------------

Dans le cas de l'installation en environnement Debian, la base de donnée n'est pas intégrée avec l'installation de ClamAv, C'est la commande ``freshclam`` qui en assure la charge. Si vous n'êtes pas connecté à internet, la base de données doit s'installer manuellement. Les liens suivants indiquent la procédure à suivre: `Installation ClamAv <https://www.clamav.net/documents/installing-clamav>`_ et `Section Virus Database <https://www.clamav.net/downloads>`_

Fichier de mot de passe
-----------------------

Par défaut, le mot de passe des "vault" sera demandé à chaque exécution d'ansible. Si le fichier ``deployment/vault_pass.txt`` est renseigné avec le mot de passe du fichier ``environments/group_vars/all/vault-vitam.yml``, le mot de passe ne sera pas demandé (dans ce cas, changez l'option ``--ask-vault-pass`` des invocations ansible par l'option ``--vault-password-file=VAULT_PASSWORD_FILES``.


Mise en place des repositories VITAM (optionnel)
-------------------------------------------------

VITAM fournit un playbook permettant de définir sur les partitions cible la configuration d'appel aux repositories spécifiques à VITAM :


Editer le fichier ``environments/group_vars/all/repositories.yml`` à partir des modèles suivants (décommenter également les lignes) :

Pour une cible de déploiement CentOS :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/repositories_centos_example.yml
   :language: yaml
   :linenos:


Pour une cible de déploiement Debian :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/repositories_debian_example.yml
   :language: yaml
   :linenos:

Ce fichier permet de définir une liste de repositories. Décommenter et adapter à votre cas.

Pour mettre en place ces repositories sur les machines cibles, lancer la commande :

.. code-block:: console

  ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/<fichier d'inventaire>  --ask-vault-pass

.. note:: En environnement CentOS, il est recommandé de créer des noms de repository commençant par  "vitam-".

Génération des hostvars
-----------------------

Une fois l'étape de PKI effectuée avec succès, il convient de procéder à la génération des hostvars, qui permettent de définir quelles interfaces réseau utiliser.
Actuellement la solution logicielle Vitam est capable de gérer 2 interfaces réseau:

    - Une d'administration
    - Une de service

Cas 1: Machines avec une seule interface réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles Vitam sera déployé ne disposent que d'une interface réseau, ou si vous ne souhaitez en utiliser qu'une seule, il convient d'utiliser le playbook ``ansible-vitam/generate_hostvars_for_1_network_interface.yml``

Cette définition des host_vars se base sur la directive ansible ``ansible_default_ipv4.address``, qui se base sur l'adresse IP associée à la route réseau définie par défaut.

.. Warning:: Les communication d'administration et de service transiteront donc toutes les deux via l'unique interface réseau disponible.

Cas 2: Machines avec plusieurs interfaces réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles Vitam sera déployé disposent de plusieurs interfaces, si celles-ci respectent cette règle:

    - Interface nommée eth0 = ip_service
    - Interface nommée eth1 = ip_admin

Alors il est possible d'utiliser le playbook ``ansible-vitam-extra/generate_hostvars_for_2_network_interfaces.yml``

.. Note:: Pour les autres cas de figure, il sera nécessaire de générer ces hostvars à la main ou de créer un script pour automatiser cela.

Vérification de la génération des hostvars
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A l'issue, vérifier le contenu des fichiers générés sous ``environments/host_vars/`` et les adapter au besoin.

.. caution:: Cas d'une installation multi-sites. Sur site secondaire, s'assurer que, pour les machines hébergeant les offres, la directive ``ip_wan`` a bien été déclarée (l'ajouter manuellement, le cas échéant), pour que site le site "primaire" sache les contacter via une IP particulière. Par défaut, c'est l'IP de service qui sera utilisée.


Passage des identifiants des référentiels en mode esclave
----------------------------------------------------------

La génération des identifiants des référentiels est géré par Vitam quand il fonctionne en mode maître.

Par exemple :

- Préfixé par PR- pour les profils
- Préfixé par IC- pour les contrats d'entrée
- Préfixé par AC- pour les contrats d'accès

Si vous souhaitez gérer vous-même les identifiants sur un service référentiel, il faut qu'il soit en mode esclave.
Par défaut tous les services référentiels de Vitam fonctionnent en mode maître. Pour désactiver le mode maître de Vitam, il faut modifier le fichier ansible ``deployment/ansible-vitam/roles/vitam/templates/functional-administration/functional-administration.conf.j2``.
Un exemple de ce fichier se trouve dans la Documentation d’exploitation au chapitre "Exploitation des composants de la solution logicielle VITAM".

.. code-block:: text

 # ExternalId configuration

 listEnableExternalIdentifiers:
  0:
    - INGEST_CONTRACT
    - ACCESS_CONTRACT
  1:
    - INGEST_CONTRACT
    - ACCESS_CONTRACT
    - PROFILE
    - SECURITY_PROFILE
    - CONTEXT

Depuis la version 1.0.4, la configuration par défaut de Vitam autorise des identifiants externes (ceux qui sont dans le fichier json importé).

 - pour le tenant 0 pour les référentiels : contrat d'entrée et contrat d'accès.
 - pour le tenant 1 pour les référentiels : contrat d'entrée, contrat d'accès, profil, profil de sécurité et contexte.

La liste des choix possibles, pour chaque tenant, est :

  - INGEST_CONTRACT : contrats d'entrée
  - ACCESS_CONTRACT : contrats d'accès
  - PROFILE : profils SEDA
  - SECURITY_PROFILE : profils de sécurité
  - CONTEXT : contextes applicatifs
  - ARCHIVEUNITPROFILE : profils d'unités archivistiques

Déploiement
-------------

Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

Une fois l'étape de la génération des hosts a été effectuée avec succès, le déploiement est à réaliser avec la commande suivante :

.. code-block:: console

   ansible-playbook ansible-vitam/vitam.yml -i environments/<ficher d'inventaire> --ask-vault-pass


.. note:: Une confirmation est demandée pour lancer ce script. Il est possible de rajouter le paramètre ``-e confirmation=yes`` pour bypasser cette demande de confirmation (cas d'un déploiement automatisé).

.. caution:: Dans le cas où l'installateur souhaite utiliser un `repository` de binaires qu'il gère par lui-même, il est fortement recommandé de rajouter ``--skip-tags "enable_vitam_repo"`` à la commande ``ansible-playbook`` ; dans ce cas, le comportement de ``yum`` n'est pas impacté par la solution de déploiement.

