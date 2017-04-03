Sauvegarde
##########


.. caution:: actuellement, la procédure de sauvegarde s'applique "à froid" durant la phase de bêta.


Cette procédure devrait être effectuée durant la nuit.
Les horaires indicatifs de cette procédure sont compris entre 20h et 8h.

.. KWA : la procédure aussi détaillé n'irait-elle pas au final dans le DEX ? A voir également, elle est décrite rapidement dans la partie "data management" de l'architecture d'exploitation

20h : arrêt des services, dans l'ordre suivant :

- ingest-internal
- ingest-external
- access-internal
- access-external

Minuit: cron logbook (sécrurisation des ...)
En parallèle, check du nombre de workflow sur le processing.
Quand il n'y a plus de workflow actif, arrêt dans l'ordre des composants suivants :

- worker
- workspace

Quand le cron logbook est terminé ET quand les services (worker et workspace) sont arrêtés, arrêt des services :

- functional-administration
- logbook
- metadata
- storage
- storage-default-offer

Ensuite, arêt des clusters ElasticSearch et MongoDB.

Sauvegarde MongoDB de base dite "Shardée"
-----------------------------------------

#. Désactiver la répartition de charge **mongos**.
#. S'assurer que les répartiteurs ont terminé leurs transactions.
#. Pour chaque nœud ("Shard") constitué d'un lot de réplicats **mongod** DB.
#. #. Déterminer le service de donnée **mongod** *élu principal* du lot.
   #. Stopper ce service.
#. **Continuer lorsque tous les serveurs principaux de tous les nœuds ont été arrêtés.**
#. Déterminer le service de configuration **mongoc** *élu principal* du lot.
#. Stopper ce service.
#. Lancer les sauvegardes les données sur chaque service **mongod** précédemment arrêtés.
#. Lancer la sauvegarde des données du service **mongoc** arrêté.
#. **Continuer lorsque toutes les sauvegardes se sont terminées.**
#. Relancer chaque service de donnée **mongod**.
#. Relancer le service de configuration **mongoc**.
#. Ré-activer la répartition de charge **mongos**.
#. S'assurer que la répartition de charge est bien active.


A 8h du matin, redémarrage de tous les services.

.. figure:: /technique/images/backup-vitam-full-process.*
    :align: center
    :height: 20 cm

    Procédure de sauvegarde complète


Ci-dessous un script shell reprenant la procédure de sauvegarde décrite ci-dessus.
Ce dernier peut servir de démonstration dans un environnement Vitam "tout-en-un".

Ce script est disponible sous ``deployment/demo_backup_vitam.sh``

.. literalinclude:: ../../../../deployment/demo_backup_vitam.sh
  :language: shell
  :linenos:
