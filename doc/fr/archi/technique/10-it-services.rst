Dépendances aux services d'infrastructures
##########################################


Ordonnanceurs techniques / batchs
=================================

.. note:: Curator permet d'effectuer des opérations périodiques de maintenance sur les index elasticsearch ; cependant, il gère lui-même le déclenchement de ses actions, et ne nécessite donc pas la configuration d'un ordonnanceur externe.

.. note:: Des batchs d'exploitation seront disponibles dans les versions ultérieures de la solution VITAM (ex : validation périodique de la validité des certificats clients)

.. cas de la sécurisation des journaux ?

Cas de la sauvegarde
--------------------

.. todo:: rédiger mieux. Cette procédure s'insère entre 20h et 8h.

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

.. todo:: finaliser cette partie quand les scripts seront écrits.

A 8h du matin, redémarrage de tous les services.

.. figure:: /technique/images/backup-vitam-full-process.*
    :align: center
    :height: 20 cm

    Procédure de sauvegarde


Ci-dessous un script shell reprenant la procédure de sauvegarde décrite ci-dessus.
Ce dernier peut servir de démonstration dans un environnement Vitam "tout-en-un".

Ce script est disponible sous ``deployment/demo_backup_vitam.sh``

.. literalinclude:: ../../../../deployment/demo_backup_vitam.sh
  :language: shell
  :linenos:

Socles d'exécution
==================

OS
--	

* CentOS 7

.. caution:: SELinux doit être configuré en mode ``permissive`` ou ``disabled``

.. Sujets à adresser : préciser la version minimale ; donner une matrice de compatibilité


Middlewares
-----------

* Java : JRE 8 ; les versions suivantes ont été testées :

    - OpenJDK 1.8.0, dans la version présente dans les dépôts officiels CentOS 7 au moment de la parution de la version VITAM (actuellement : 1.8.0.101)
  
.. Sujets à adresser : Préciser la version minimale ; donner une matrice de compatibilité
