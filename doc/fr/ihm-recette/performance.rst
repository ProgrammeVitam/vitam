Tests de performance
####################

Principe
========

Les tests de performance consistent à réaliser plusieurs fois l'entrée d'un SIP et d'en mesurer le temps. Ces entrées peuvent être réalisées par une ou plusieurs tâches parallèles. 

Champs disponibles
==================

L'IHM est constituée de trois champs :

* Liste des SIP : liste des SIP disponibles pour réaliser le test. Ces SIP sont ceux déposés dans le dépôt vitam-itest. Il n'est possible de sélectionner qu'un SIP à la fois.
* Nb Thread : Permet de définir le nombre de tâches parallèles qui exécuteront les entrées.
* Nb total : Permet de définir le nombre total d'entrées à réaliser.

Un bouton "lancer" permet d’exécuter le test de performance.

.. image:: images/performance_champs_disponibles.png

Résultats
=========

Les résultats sont disponibles dans la section en bas de la page.

Chaque ligne représente un test de performance. Le nom du test est formaté de la façon suivante : report_AAAAMMJJ_HHmmSS.csv. Le bouton de téléchargement permet de récupérer le fichier csv contenant les données du test.

.. image:: images/performance_resultats.png

Chaque ligne du csv représente une entrée. Les colonnes sont :

* OperationID
* PROCESS_SIP_UNITARY
* SANITY_CHECK_SIP	
* CHECK_CONTAINER	
* STP_SANITY_CHECK_SIP	
* STP_UPLOAD_SIP	
* STP_INGEST_CONTROL_SIP
 
La première contient le GUID de l'opération d'entrée. Les autres colonnes indique le temps en millisecondes qui a été nécessaire pour passer l'étape.