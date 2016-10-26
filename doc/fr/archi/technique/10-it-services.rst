Dépendances aux services d'infrastructures
##########################################


Ordonnanceurs techniques / batchs
=================================

Sans objet pour cette version de VITAM.

.. note:: Curator permet d'effectuer des opérations périodiques de maintenance sur les index elasticsearch ; cependant, il gère lui-même le déclenchement de ses actions, et ne nécessite donc pas la configuration d'un ordonnanceur externe.

.. note:: Des batchs d'exploitation seront disponibles dans les versions ultérieures de la solution VITAM (ex : validation périodique de la validité des certificats clients)

Cas de la sauvegarde
--------------------
.. todo:: rédiger mieux. Cette procédure s'insère entre 20h et 8h.

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

A l'issue, sauvegarde à froid des bases (procédure en cours de mise en place).

.. todo:: finaliser cette partie quand les scripts seront écrits.

A 8h du matin, redémarrage de tous les services.

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
