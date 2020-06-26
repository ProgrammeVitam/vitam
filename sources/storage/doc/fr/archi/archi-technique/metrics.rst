Métriques spécifiques
#####################


Besoins
=======
A des fins de monitoring du composant storage-engine pour estimer les données qui traverse ce composant depuis et vers les offres de stockage, un certain nombre de métriques sont intégrées.
    - La de calculer le taux moyen des données téléversées dans les offres de stockage
    - La de calculer le taux moyen des données téléchargées depuis les offres de stockage

Il est important d'avoir des filtres sur les critères suivant:
    - tenant
    - strategy de stockage
    - la carégorie de la donnée
    - l'identifiant de l'offre de stockage.
    - optionnellement, l'origin de la demande et aussi le numéro d'essai pour mesurer s'il y a des erreurs.

Un outil de monitoring, à ce jour, prometheus, permet de faire des requêtes sur ces métriques et surtout de lancer des alertes dans les cas suspects nécessitant une intervention rapide.

Liste des métriques
===================
* vitam_storage_download_size_bytes : Données en octets téléchargées par le composant `vitam-storage-engine` depuis les offres de stockages.
* vitam_storage_upload_size_bytes: Données en octets téléversées par le composant `vitam-storage-engine` vers les offres de stockages.


Exploitation des métriques
==========================
L'exploitation de ces métriques à des fins de visualisation ou d'alerting est de la responsabilité d'un collecteur externe de métriques.
A ce jour, le serveur prometheus avec une bonne configuration permet d'exploiter ces métriques.

.. note::
    Veuillez vous référer au manuel de développement pour avoir plus d'information et de détails sur chacune de ces métriques
    Veuillez vous référer à la documentation d'exploitation pour savoir comment exploiter ces métriques, exemple d'utilisation, alerting, et visualisation