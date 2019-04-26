Troubleshooting
###############

Cette section a pour but de recenser les problèmes déjà rencontrés et apporter une solution associée.

Erreur au chargement des tableaux de bord Kibana
================================================

Dans le cas de machines petitement taillées, il peut arriver que, durant le déploiement, la tâche ``Wait for the kibana port port to be opened`` prenne plus de temps que le `timeout` défini (``vitam_defaults.services.start_timeout``).
Pour fixer cela, il suffit de relancer le déploiement.



.. include:: ../../exploitation/FAQ/kb.rst
