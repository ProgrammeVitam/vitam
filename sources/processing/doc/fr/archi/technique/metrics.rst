Métriques spécifiques
#####################


Besoins
=======
A des fins de monitoring du composant processing un certain nombre de métriques sont intégrées.
    - La possibilité d'avoir une vue instantané des opérations gérées par le composant processing
    - La capacité de détecter des opérations dans un état et un statut particulier
    - La possibilité de pouvoir filter ces métrique par type d'opération, par état et par statut
    - Calculer la durée d'exécution des steps, des tâches
    - Tracer le cycle de vie des tâches crées par le distributeur, création, attente d'entrée dans une queue, temps passé dans la queue et durée d'exécution par un worker.
    - Le nombre de worker abonnés au distributeur

Un outil de monitoring, à ce jour, prometheus, permet de faire des requêtes sur ces métriques et surtout de lancer des alertes dans les cas suspects nécessitant une intervention rapide.

Liste des métriques
===================
* vitam_processing_workflow_operation_total : Récupère un snapshot de l'ensemble des opérations visible par le composant processing
* vitam_processing_worker_task_in_queue_total: Total des tâches dans la queue en attendre d'exécution
* vitam_processing_worker_current_task_total: Total des tâches crées par le distributeur et qui sont pas encore terminées. C'est la somme des tâches en attente d'entrer dans la queue + Tâches dans la queue + Tâches en cours d'exécution pour les workers.
* vitam_processing_worker_registered_total: Total des worker enregistré dans le distributeur
* vitam_processing_worker_task_execution_duration_seconds: C'est une métrique de type Histogram, elle calcule la durée d'exécution d'une tâche du point de vu Distributeur/Worker
* vitam_processing_worker_task_idle_duration_in_queue_seconds: C'est une métrique de type Histogram, elle calcule la durée d'attente d'exécution d'une tâche depuis sa création jusqu'a sa prise en charge par un worker.
* vitam_processing_workflow_step_execution_duration_seconds: C'est une métrique de type Histogram, elle calcule la durée d'exécution d'une step du point de vu ProcessEngine

Exploitation des métriques
==========================
L'exploitation de ces métriques à des fins de visualisation ou d'alerting est de la responsabilité d'un collection externe de métriques.
A ce jour, le serveur prometheus avec une bonne configuration permet d'exploiter ces métriques.

.. note::
    Veuillez vous référer au manuel de développement pour avoir plus d'information et de détail sur chacune de ces métriques
    Veuillez vous référer à la documentation d'exploitation pour savoir comment exploiter ces métriques, exemple d'utilisation, alerting, et visualisation