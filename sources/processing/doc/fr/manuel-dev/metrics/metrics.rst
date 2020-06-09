Métriques
##########

Introduction
************
Dans ce qui suit la liste de métriques développées pour ce composant.

.. note::
    Pour avoir plus d'informations sur la partie développement des métriques prometheus, veuillez vous référer à la documentation du composant **Common Cf. vitam-mertics.rst**


Liste des métriques
*******************
* vitam_processing_workflow_operation_total :
    > Récupère un snapshot de l'ensemble des opérations visible par le composant processing
    > Cette métrique dispose de trois labels ("workflow", "state", "status")
        - "workflow": C'est le nom du LogbookTypeProcess d'une opération
        - "state" : L'état de l'opération (PAUSE, RUNNING, COMPLETED)
        - "status": Le statut de l'opération (UNKNOWN, OK, WARNING, KO, FATAL)
    > Total des opérations tout type confondu:
        sum (vitam_processing_workflow_operation_total)

    > Total des opérations en état `PAUSE` statut `FATAL`. Cette requête peut être utilisée pour lancer des alertes:
        sum (vitam_processing_workflow_operation_total{state="PAUSE", status="FATAL"})

    > Total des opérations tout d'ingest:
        sum (vitam_processing_workflow_operation_total{workflow = "ingest"})

    > Par type de workflow, donne la somme des moyennes du nombre d'opérations par seconde sur un interval de 5 minutes:
        sum by(workflow) (rate(vitam_processing_workflow_operation_total[5m]))


* vitam_processing_worker_task_in_queue_total:
    > Total des tâches dans la queue en attendre d'exécution
    > Cette métrique dispose des labels ("worker_family")
    > Total des tâches dans la queue
        sum(vitam_processing_worker_task_in_queue_total)

* vitam_processing_worker_current_task_total:
    > Total des tâches crées par le distributeur et qui sont pas encore terminées. C'est la somme des tâches en attente d'entrer dans la queue + Tâches dans la queue + Tâches en cours d'exécution pour les workers.
    > Cette métrique dispose des labels ("worker_family", "workflow", "step_name")
    > C'est un type Gauge qui s'incrémente à la création de la tâche et qui se décrémente à la fin de l'execution de la tâche

* vitam_processing_worker_registered_total:
    > Total des worker enregistré dans le distributeur
    > Cette métrique dispose des labels ("worker_family")
    > Pour avoir tous les workers :
        sum (vitam_processing_worker_registered_total)

* vitam_processing_worker_task_execution_duration_seconds:
    > C'est une métrique de type Histogram, elle calcule la durée d'exécution d'une tâche du point de vu Distributeur/Worker
    > Cette métrique dispose des labels ("worker_family", "worker_name", "workflow", "step_name")
    > Dans une step distribuée, on peut avoir plusieurs tâches (selon la distribution)
    > La somme regroupé par worker des moyennes de durées par seconde pendant les dernières 5 minutes
        sum by(worker_name)(rate(vitam_processing_worker_task_execution_duration_seconds_sum[5m]))
    > La somme regroupé par worker des moyennes de nombre de tâches exécutées par seconde pendant les dernières 5 minutes
        sum by(worker_name)(rate(vitam_processing_worker_task_execution_duration_seconds_count[5m]))

* vitam_processing_worker_task_idle_duration_in_queue_seconds:
    > C'est une métrique de type Histogram, elle calcule la durée d'attente d'exécution d'une tâche depuis sa création jusqu'a sa prise en charge par un worker.
    > Cette métrique dispose des labels ("worker_family", "workflow", "step_name")
    > On peut analyser la distribution statistique des durées d'exécution et du nombre de tâches des steps

* vitam_processing_workflow_step_execution_duration_seconds:
    > C'est une métrique de type Histogram, elle calcule la durée d'exécution d'une step du point de vu ProcessEngine
    > Cette métrique dispose des labels ("workflow", "step_name")
    > C'est, presque, la somme des durées vitam_processing_worker_task_execution_duration_seconds pour une step donnée. Si ce n'est pas la même valeur, ça veut dire qu'entre une tâche et une autre d'une même step, on peut avoir des temps d'attente causés par la concurrence entre opérations.
    > Durée d'exécution moyenne par seconde par step name durant les 5 dernières minutes regroupées par step name
        sum by (step_name) (rate(vitam_processing_workflow_step_execution_duration_seconds_sum[5m]) / rate(vitam_processing_workflow_step_execution_duration_seconds_count[5m]))
    > Exemple de 95 percentile sur la somme des moyennes par seconde sur les 5 dernières minutes regroupées par bucket
        histogram_quantile(0.95, sum(rate(vitam_processing_workflow_step_execution_duration_seconds_bucket[5m])) by (le))

