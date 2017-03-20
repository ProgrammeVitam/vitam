Dépendances aux services d'infrastructures
##########################################


Ordonnanceurs techniques / batchs
=================================

.. note:: Curator permet d'effectuer des opérations périodiques de maintenance sur les index elasticsearch. Les jobs Curator sont initiés automatiquement au déploiement de VITAM et sont lancés via crontab.

.. note:: Des batchs d'exploitation seront disponibles dans les versions ultérieures de la solution VITAM (ex : validation périodique de la validité des certificats clients)

Job de sécurisation du logbook : lancé toutes les nuits peu après minuit sur une des machines (la dernière) hébergeant le composant vitam-logbook.

.. cas de la sécurisation des journaux ?

Cas de la sauvegarde
--------------------

Se référer à la section dédiée du :term:`DAT`.

Socles d'exécution
==================

OS
--	

.. caution:: SELinux doit être configuré en mode ``permissive`` ou ``disabled``

.. Sujets à adresser : préciser la version minimale ; donner une matrice de compatibilité


Middlewares
-----------

* Java : JRE 8 ; les versions suivantes ont été testées :

    - OpenJDK 1.8.0, dans la version présente dans les dépôts officiels au moment de la parution cette release de Vitam (Centos et Debian en 1.8.0_121)
  
.. Sujets à adresser : Préciser la version minimale ; donner une matrice de compatibilité
