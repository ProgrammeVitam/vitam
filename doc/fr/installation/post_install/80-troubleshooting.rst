Troubleshooting
###############

Cette section a pour but de recenser les problèmes déjà rencontrés et apporter une solution associée.

Erreur au chargement des *index template* kibana
=================================================

Cette erreur ne se produit qu'en cas de *filesystem* plein sur les partitions hébergeant un cluster elasticsearch. Par sécurité, kibana passe alors ses *index* en ``READ ONLY``.

Pour fixer cela, il est d'abord nécessaire de déterminer la cause du *filesystem* plein,puis libérer ou agrandir l'espace disque.

Ensuite, comme indiqué sur `ce fil de discussion <https://discuss.elastic.co/t/forbidden-12-index-read-only-allow-delete-api/110282/2>`_, vous devez désactiver le mode ``READ ONLY`` dans les *settings* de l'index ``.kibana`` du cluster elasticsearch.

Exemple::

    PUT .kibana/_settings
    {
        "index": {
            "blocks": {
                "read_only_allow_delete": "false"
            }
        }
    }


.. hint:: Il est également possible de lancer cet appel via l'IHM du kibana associé, dans l'onglet ``Dev Tools``.

A l'issue, vous pouvez relancer l'installation de la solution logicielle :term:`VITAM`.

Erreur au chargement des tableaux de bord Kibana
================================================

Dans le cas de machines petitement taillées, il peut arriver que, durant le déploiement, la tâche ``Wait for the kibana port port to be opened`` prenne plus de temps que le `timeout` défini (``vitam_defaults.services.start_timeout``).
Pour fixer cela, il suffit de relancer le déploiement.



.. include:: ../../exploitation/FAQ/kb.rst
