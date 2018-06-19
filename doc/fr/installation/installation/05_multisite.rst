Multi-sites
###########

Procédure
===========

Dans le cadre d'une installation multi-sites, il est nécessaire de déployer la solution logicielle :term:`VITAM` sur le site secondaire dans un premier temps, puis déployer le site "production".

.. caution:: La variable ``secret_plateforme`` doit être commune sur les différents sites.

.. note:: Cas d'appel https au composant offer sur site secondaire. Dans ce cas, il convient également de rajouter, sur le site "primaire", les certificats relatifs ( CA du site secondaire ) à l'offre secondaire. Il faut également rapatrier sur site secondaire la CA et le certificat client du site primaire.
