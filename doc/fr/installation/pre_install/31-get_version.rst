Récupération de la version
##########################


Utilisation des dépôts open-source
==================================

Les scripts de déploiement de VITAM sont disponibles dans le dépôt github `VITAM <https://github.com/ProgrammeVitam/vitam>`_ , dans le répertoire ``deployment``.

Les binaires de VITAM sont disponibles sur un dépôt vitam public indiqué ci-dessous par type de package; ces dépôts doivent être correctement configurés sur la plate-forme cible avant toute installation.


Repository pour environnement CentOS
-------------------------------------

Sur les partitions cibles, configurer le fichier ``/etc/yum.repos.d/vitam-repositories.repo`` (remplacer <branche_vitam> par le nom de la branche de support à installer) comme suit ::

   [programmevitam-vitam-rpm-release-product]
   name=programmevitam-vitam-rpm-release-product
   baseurl=http://download.programmevitam.fr/vitam_repository/<vitam_version>/rpm/vitam-product/
   gpgcheck=0
   repo_gpgcheck=0
   enabled=1

   [programmevitam-vitam-rpm-release-external]
   name=programmevitam-vitam-rpm-release-external
   baseurl=http://download.programmevitam.fr/vitam_repository/<vitam_version>/rpm/vitam-external/
   gpgcheck=0
   repo_gpgcheck=0
   enabled=1

.. note:: remplacer <vitam_version> par la version à déployer.

Repository pour environnement Debian
-------------------------------------

Sur les partitions cibles, configurer le fichier ``/etc/apt/sources.list.d/vitam-repositories.list`` (remplacer <branche_vitam> par le nom de la branche de support à installer) comme suit ::

   deb [trusted=yes] http://download.programmevitam.fr/vitam_repository/<vitam_version>/deb jessie vitam-product vitam-external


.. note:: remplacer <vitam_version> par la version à déployer.

Utilisation du package global d'installation
============================================

.. note:: Le package global d'installation n'est pas présent dans les dépôts publics.

Le package global d'installation contient :

* le package proprement dit
* la release notes
* les empreintes de contrôle

Sur la machine "ansible" dévouée au déploiement de :term:`VITAM`, décompacter le package (au format ``tar.gz``).

Sur le repository "VITAM", récupérer également depuis le fichier d'extension tar.gz les binaires d'installation (rpm pour CentOS ; deb pour Debian) et les faire prendre en compte par le repository.


