.. _extrainstall:

.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

Elements extras de l'installation
#################################

.. caution:: Les élements décrits dans cette section sont des élements "extras" ; il ne sont pas officiellement supportés, et ne sont par conséquence pas inclus dans l'installation de base. Cependant, ils peuvent s'avérer utile, notamment pour les installation sur des environnements hors production.

.. caution:: Dans le cas où l'installateur souhaite utiliser un `repository` de binaires qu'il gère par lui-même, il est fortement recommandé de rajouter ``--skip-tags "enable_vitam_repo"`` à la commande ``ansible-playbook`` ; dans ce cas, le comportement de ``yum`` n'est pas impacté par la solution de déploiement.

Configuration des extra
=======================

Le fichier |repertoire_inventory| ``/group_vars/all/extra_vars.yml`` contient la configuration des extra :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/extra_vars.yml
     :language: yaml
     :linenos:

.. bug #113

.. warning:: A modifier selon le besoin avant de lancer le playbook ! Le composant ihm-recette, s'il est déployé en "https", doit avoir un paramétrage différent dans ``environments/group_vars/all/extra_vars.yml`` sur le paramètre ``secure_cookie`` selon qu'il est attaqué en direct ou derrière un proxy https (``secure_cookie: true``) ou un proxy http (``secure_cookie: false``). Par défaut, la variable est à ``true``. Le symptôme observé, en cas de problème, est une bonne authentification, mais l'impossibilité de sélectionner un tenant.

.. note:: La section ``metricbeat`` permet de configurer la périodicité d'envoi des informations collectées. Selon l'espace disponible sur le `cluster` Elasticsearch de log et la taille de l'environnement :term:`VITAM` (en particulier, le nombre de machines), il peut être nécessaire d'allonger cette périodicité (en secondes).


Le fichier |repertoire_inventory| ``/group_vars/all/all/vault-extra.yml`` contient les secrets supplémentaires des extra ; ce fichier est encrypté par ``ansible-vault`` et doit être paramétré avant le lancement de l'orchestration de déploiement, si le composant ihm-recette est déployé avec récupération des TNR.

.. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-extra.example
   :language: ini
   :linenos:

.. note:: Pour ce fichier, l'encrypter avec le même mot de passe que ``vault-vitam.yml``.


Déploiement des extra
=====================

Plusieurs *playbook* d'extra sont fournis pour usage "tel quel".

ihm-recette
-----------

Ce *playbook* permet d'installer également le composant :term:`VITAM` ihm-recette.

.. code-block:: console

   ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<ficher d'inventaire> --ask-vault-pass

.. caution:: avant de jouer le `playbook`, ne pas oublier, selon le contexte d'usage, de positionner correctement la variable ``secure_cookie`` décrite plus haut.



extra complet
-------------

Ce *playbook* permet d'installer :
  - des éléments de monitoring système
  - un serveur Apache pour naviguer sur le ``/vitam``  des différentes machines hébergeant :term:`VITAM`
  - mongo-express (en docker ; une connexion internet est alors nécessaire)
  - le composant :term:`VITAM` library, hébergeant la documentation du projet
  - le composant :term:`VITAM` ihm-recette (utilise si configuré des dépôts de jeux de tests)
  - un reverse proxy, afin de fournir une page d'accueil pour les environnements de test
  - l'outillage de tests de performance


.. code-block:: console

   ansible-playbook ansible-vitam-extra/extra.yml -i environments/<ficher d'inventaire> --ask-vault-pass



