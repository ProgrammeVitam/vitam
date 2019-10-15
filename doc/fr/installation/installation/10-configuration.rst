.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

Configuration du déploiement
############################

.. seealso:: L'architecture de la solution logicielle, les éléments de dimensionnement ainsi que les principes de déploiement sont définis dans le :term:`DAT`.


Fichiers de déploiement
=======================

Les fichiers de déploiement sont disponibles dans la version :term:`VITAM` livrée, dans le sous-répertoire |repertoire_deploiement| . Concernant l'installation, ils se déclinent en 2 parties :

 * les *playbook* ansible de déploiement, présents dans le sous-répertoire |repertoire_playbook ansible|, qui est indépendant de l'environnement à déployer ; ces fichiers ne sont normalement pas à modifier pour réaliser une installation.
 * l'arborescence d'inventaire ; des fichiers d'exemples sont disponibles dans le sous-répertoire |repertoire_inventory|. Cette arborescence est valable pour le déploiement d'un environnement, et doit être dupliquée lors de l'installation d'environnements ultérieurs. Les fichiers contenus dans cette arborescence doivent être adaptés avant le déploiement, comme expliqué dans les paragraphes suivants.


.. _inventaire:

Informations `plate-forme`
==========================

Inventaire
-----------

Pour configurer le déploiement, il est nécessaire de créer, dans le répertoire |repertoire_inventory|, un nouveau fichier d'inventaire (par la suite, ce fichier sera communément appelé  ``hosts.<environnement>``). Ce fichier devra se conformer à la structure présente dans le fichier ``hosts.example`` (et notamment respecter scrupuleusement l'arborescence des groupes `ansible`). Les commentaires dans ce fichier fournissent les explications permettant l'adaptation à l'environnement cible :

.. literalinclude:: ../../../../deployment/environments/hosts.example
   :language: ini
   :linenos:

Pour chaque type de `host`, indiquer le(s) serveur(s) défini(s), pour chaque fonction. Une colocalisation de composants est possible (Cf. le paragraphe idoine du :term:`DAT`)

.. note:: Concernant le groupe `hosts_consul_server`, il est nécessaire de déclarer au minimum 3 machines.

.. warning:: Il n'est pas possible de colocaliser les clusters MongoDB `data` et `offer`.

.. bug 114

.. warning:: Il n'est pas possible de colocaliser `kibana-data` et `kibana-log`.

.. note:: Pour les composants considérés par l'exploitant comme étant "hors :term:`VITAM`" (typiquement, le composant ``ihm-demo``), il est possible de désactiver la création du servcie Consul associé. Pour cela, après chaque hostname impliqué, il faut rajouter la directive suivante : ``consul_disabled=true``.

.. caution:: Concernant la valeur de ``vitam_site_name``, seuls les caractères alphanumériques et le tiret ("-") sont autorisés.

Fichier ``vitam_security.yml``
-------------------------------

La configuration des droits d'accès à VITAM est réalisée dans le fichier |repertoire_inventory| ``/group_vars/all/vitam_security.yml``, comme suit :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/vitam_security.yml
     :language: yaml
     :linenos:

.. note:: Pour la directive ``admin_context_certs`` concernant l'intégration de certificats :term:`SIA` au déploiement, se reporter à la section :ref:`external_sia_certs_integration`. 

.. note:: Pour la directive ``admin_personal_certs`` concernant l'intégration de certificats personnels (*personae*) au déploiement, se reporter à la section :ref:`personal_certs_integration`. 

Fichier ``offers_opts.yml``
----------------------------

.. hint:: Fichier à créer depuis ``offers_opts.yml.example`` et à paramétrer selon le besoin.

La déclaration de configuration des offres de stockage associées se fait dans le fichier |repertoire_inventory| ``/group_vars/all/offers_opts.yml`` :

 .. literalinclude:: ../../../../deployment/environments/group_vars/all/offers_opts.yml.example
     :language: yaml
     :linenos:

Se référer aux commentaires dans le fichier pour le renseigner correctement.

.. note:: Dans le cas d'un déploiement multi-sites, dans la section ``vitam_strategy``, la directive ``vitam_site_name`` définit pour l'offre associée le nom du datacenter Consul. Par défaut, si non définie, c'est la valeur de la variable ``vitam_site_name`` définie dans l'inventaire qui est prise en compte.

.. warning:: La cohérence entre l'inventaire et la section ``vitam_strategy`` est critique pour le bon déploiement et fonctionnement de la solution logicielle VITAM. En particulier, la liste d'offres de ``vitam_strategy`` doit correspondre *exactement* aux noms d'offres déclarés dans l'inventaire (ou les inventaires de chaque datacenter, en cas de fonctionnement multi-site).

.. warning:: Ne pas oublier, en cas de connexion à un keystone en https, de répercuter dans la :term:`PKI` la clé publique de la :term:`CA` du keystone.


Fichier ``cots_vars.yml``
----------------------------

Fichier le fichier |repertoire_inventory| ``/group_vars/all/cots_vars.yml`` :

 .. literalinclude:: ../../../../deployment/environments/group_vars/all/cots_vars.yml
     :language: yaml
     :linenos:

Dans le cas du choix du :term:`COTS` d'envoi des messages syslog dans logastsh, il est possible de choisir entre ``syslog-ng`` et ``rsyslog``. Il faut alors modifier la valeur de la directive ``syslog.name`` ; la valeur par défaut est ``rsyslog``.

.. note:: si vous  décommentez et renseignez les valeurs dans le bloc ``external_siem``, les messages seront envoyés (par ``syslog`` ou ``syslog-ng``, selon votre choix de déploiement) dans un :term:`SIEM` externe à la solution logicielle :term:`VITAM`, aux valeurs indiquées dans le bloc ; il n'est alors pas nécessaire de renseigner de partitions pour les groupes ansible ``[hosts_logstash]`` et ``[hosts_elasticsearch_log]``.

.. _pkiconfsection:

Déclaration des secrets
=======================

.. warning:: L'ensemble des mots de passe fournis ci-après le sont par défaut et doivent être changés !

.. warning:: Cette section décrit des fichiers contenant des données sensibles. Il est important d'implémenter une politique de mot de passe robuste conforme à ce que l'ANSSI préconise. Par exemple: ne pas utiliser le même mot de passe pour chaque service, renouveler régulièrement son mot de passe, utiliser des majuscules, minuscules, chiffres et caractères spéciaux (Se référer à la documentation ANSSI https://www.ssi.gouv.fr/guide/mot-de-passe). En cas d'usage d'un fichier de mot de passe (`vault-password-file`), il faut renseigner ce mot de passe comme contenu du fichier et ne pas oublier de sécuriser ou supprimer ce fichier à l'issue de l'installation.


Les secrets utilisés par la solution logicielle (en-dehors des certificats qui sont abordés dans une section ultérieure) sont définis dans des fichiers chiffrés par ``ansible-vault``.

.. important:: Tous les vault présents dans l'arborescence d'inventaire doivent être tous protégés par le même mot de passe !

La première étape consiste à changer les mots de passe de tous les vaults présents dans l'arborescence de déploiement (le mot de passe par défaut est contenu dans le fichier ``vault_pass.txt``) à l'aide de la commande ``ansible-vault rekey <fichier vault>``.

Voici la liste des vaults pour lesquels il est nécessaire de modifier le mot de passe:

* ``environments/group_vars/all/vault-vitam.yml``
* ``environments/group_vars/all/vault-keystores.yml``
* ``environments/group_vars/all/vault-extra.yml``
* ``environments/certs/vault-certs.yml``

2 vaults sont principalement utilisés dans le déploiement d'une version :

.. warning:: Leur contenu est donc à modifier avant tout déploiement.

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-vitam.yml`` contient les secrets généraux :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-vitam.yml.example
     :language: ini
     :linenos:

.. warning:: Le paramétrage du mode d'authentifications des utilisateurs à l':term:`IHM` démo est géré au niveau du fichier ``deployment/environments/group_vars/all/vitam_vars.yml``. Plusieurs modes d'authentifications sont proposés au niveau de la section ``authentication_realms``. Dans le cas d'une authentification se basant sur le mécanisme ``iniRealm`` (configuration ``shiro`` par défaut), les mots de passe déclarés dans la section ``vitam_users`` devront s'appuyer sur une politique de mot de passe robuste, comme indiqué en début de chapitre. Il est par ailleurs possible de  choisir un mode d'authentification s'appuyant sur un annuaire LDAP externe (``ldapRealm`` dans la section ``authentication_realms``).

.. note:: Dans le cadre d'une installation avec au moins une offre `swift`, il faut déclarer, dans la section ``vitam_offers``, le nom de chaque offre et le mot de passe de connexion `swift` associé, défini dans le fichier ``offers_opts.yml``. L'exemple ci-dessus présente la déclaration du mot de passe pour l'offre swift `offer-swift-1`.

.. note:: Dans le cadre d'une installation avec au moins une offre `s3`, il faut déclarer, dans la section ``vitam_offers``, le nom de chaque offre et l'access key secret `s3` associé, défini dans le fichier ``offers_opts.yml``. L'exemple ci-dessus présente la déclaration du mot de passe pour l'offre s3 `offer-s3-1`.

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-keystores.yml`` contient les mots de passe des magasins de certificats utilisés dans VITAM :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-keystores.yml.example
     :language: ini
     :linenos:

.. warning:: il convient de sécuriser votre environnement en définissant des mots de passe `forts`.

Cas des extras
--------------

* Le fichier |repertoire_inventory| ``/group_vars/all/vault-extra.yml`` contient les mots de passe des magasins de certificats utilisés dans VITAM :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-extra.yml.example
     :language: ini
     :linenos:



.. note:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.
