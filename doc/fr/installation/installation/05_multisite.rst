Multi-sites
###########

Procédure
===========

Dans le cadre d'une installation multi-sites, il est nécessaire de déployer la solution logicielle :term:`VITAM` sur le site secondaire dans un premier temps, puis déployer le site "production".

.. caution:: noter de bien prendre en compte, selon le site, les variables ansible ``vitam_site_name``. La variable ``secret_plateforme`` doit être commune sur les différents sites.