.. _extrainstall:

Elements extras de l'installation
#################################

.. Les élements décrits dans cette section sont des élements "extras" non inclus dans l'installation de base mais pouvant être utile

Plusieurs playbook d'extra sont fournis pour usage "tel quel".

1. ihm-recette

Ce playbook permet d'installer également le composant :term:`VITAM` ihm-recette.

.. code-block:: bash

   ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<ficher d'inventaire> --ask-vault-pass


2. extra complet

Ce playbook permet d'installer :
  - topbeat
  - un serveur Apache pour naviguer sur le ``/vitam``  des différentes machines hébergeant :term:`VITAM`
  - mongo-express (en docker  ; une connexion internet est alors nécessaire)
  - le composant :term:`VITAM` library, hébergeant les documentations du projet
  - le composant :term:`VITAM` ihm-recette (nécessite un accès à un répertoire "partagé" pour récupérer les jeux de tests)
  - un reverse proxy, afin de simplifier les appels aux composants


.. code-block:: bash

   ansible-playbook ansible-vitam-extra/extra.yml -i environments/<ficher d'inventaire> --ask-vault-pass



