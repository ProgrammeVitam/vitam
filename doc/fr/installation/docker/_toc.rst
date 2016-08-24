Procédures d'installation / mise à jour : packages Docker
#########################################################

.. caution:: Les packages Docker doivent uniquement être utilisés dans le cadre des activités de développement logiciel ; en particulier, ils ne sont pas prévus pour une installation sur un environnement de production !  

Pré-requis supplémentaire
=========================

Tous les serveurs cible doivent avoir accès au registry docker vitam (docker.programmevitam.fr). Les éléments d'installation (playbook ansible, ...) doivent être disponibles sur la machine ansible orchestrant le déploiement de la solution.


Procédures
==========

.. toctree::
   :maxdepth: 2

   fresh_install
   update
