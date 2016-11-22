Déploiement Nightly
#####################

Scripts
============================

Un déploiement automatique est réalisé chaque nuit. A la date de rédaction de ce document, ce build se déclenche à 03h00 via la crontab de ``vitam-iaas-bastion-01``.
Les scripts sont sotckés ici ``<GIT_REPO>/pf-dev-conf/nightly_deploy/``

.. code-block:: bash

   [centos@vitam-iaas-bastion-01 ~]$ crontab -l
   00 03 * * * /home/centos/nightly/pf-dev-conf/nightly_deploy/launch_nightly_deploy.sh /home/centos/nightly/pf-dev-conf /home/centos/nightly/vitam int master_iteration_11

1. launch_nightly_deploy.sh

	1.1. Paramètres

* PATH du repo git pf-dev-conf
* PATH du repo git vitam
* Inventaire exemple : ``int``
* Branche à partir de laquelle le déploiement se réalisera

	1.2. Rôle

* Script est en charge de lancer deux script

* ``clean_repo.sh`` et ``nightly_deploy.sh``

2. clean_repo.sh

	1.1. Paramètres

* PATH du repo git vitam
* Branche à partir de laquelle le déploiement se réalisera

	1.2. Rôle

* Clean le repo vitam et se positionne sur la branche à partir de laquelle il faut déployer

3. nightly_deploy.sh

	1.1. Paramètres

* PATH du repo git pf-dev-conf
* PATH du repo git vitam
* Inventaire exemple : ``int``

	1.2. Rôle

* Génére les CA, certificats, store et copie les fichiers
* Lance les playbooks ansible-vitam-rpm,  ansible-vitam-rpm-extra, ansible-vitam-exploitation
* Copie le fichier de log sur ``vitam-iaas-reverse-01.vitam``

Token git
=========

Le script de nightly deploy utilise le token gitlab CI/CD pipelines.
Il donne un accès lecture uniquement sur le repository Vitam.
Ce token est éditable sur gitlab, dans le projet Vitam: options->CI/CD Pipelines

Logs
============================

Après le déploiement, le script envoie les logs sur le reverse proxy dans ``/var/www/html-<env>/logs-nightly-deploy/``
Un lien vers les logs est présent sur la page d'accueil de l'environnement sur lequel le déploiement a été effectué.
