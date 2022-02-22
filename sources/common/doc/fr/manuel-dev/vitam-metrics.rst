Métriques dans VITAM
####################

Introduction
************
Les métriques dans :term:`VITAM` sont développées en utilisant les libraries **dropwizard**.
Depuis la release R14, la solution logicielle :term:`VITAM` intègre `Prometheus` et permet d'exposer les métriques déjà existantes via une API d'administration ``/admin/v1/metrics``. De nouvelles métriques techniques et métiers sont aussi développées et exposées via cette API.


Fonctionnement des métriques dropwizard
***************************************
Les métriques historiques dans :term:`VITAM` sont développées en utilisant les libraries **dropwizard** et sont stockées dans le package :

**fr.gouv.common.metrics**

Les `registres` de métriques et les `reporters` de métriques sont tous les deux contenus dans une classe *VitamMetrics*. Cette classe doit être instanciée avec un *VitamMetricsType* qui peut être **REST**, **JVM** ou **BUSINESS**. Le type définira les métriques enregistrées dans le registre interne de la classe.

La classe **CommonBusinessApplication** contient une *Map* statique de *VitamMetrics* qui est vide et initialisée à chaque démarrage d'une application VITAM.
Cette *Map* contient obligatoirement un *VitamMetrics* de type BUSINESS et peut accessoirement contenir les *VitamMetrics* de types JVM et/ou REST.
Les métriques de types JVM et REST peuvent être activées/désactivées depuis le fichier de conf (Cf. Configuration). 

.. code-block:: java

   protected static final void clearAndconfigureMetrics()


Cette fonction permet de vider et de recharger les métriques à chaque création d'une application VITAM.
Les `reporters` de métriques (elasticsearch ou logback) sont démarrés lors du démarrage d'un serveur VITAM.

La fonction suivante de la classe *CommonBusinessApplication* quant à elle s'occupe du démarrage des reporters:

.. code-block:: java

   public final void startMetrics()


.. note::
   Les *VitamMetrics* de type REST ou JVM n'ont pas à être modifiés pendant l'execution d'une application VITAM.


Métriques métier
================
Les métriques métiers permettent aux développeurs d'enregistrer des métriques n'importe où dans le code, pour par exemple suivre une variable ou bien chronométrer une fonction. Pour cela il suffit d'appeler la fonction statique *getBusinessMetricRegistry* dans la classe *CommonBusinessApplication*, puis d'enregistrer une métrique.

.. code-block:: java

   CommonBusinessApplication.getBusinessMetricsRegistry().register("Running workflows",
       new Gauge<Long>() {
           @Override
           public Long getValue() {
               return runningWorkflows.get();
           }
       });

.. warning::
   Avec la fonction *register*, si une métrique avec un nom identique est déjà enregistrée, alors l'ancienne métrique sera ecrasée par la nouvelle avec un avertissement dans les logs. En revanche, avec les fonctions de création de métrique comme *timer*, *meter*..., une exception sera soulevée.

Reporters
=========
2 reporters sont disponibles, un reporter Logback (toutes les métriques sont dumpées dans Logback) ou bien un reporter ElasticSearch (toutes les métriques sont dumpées dans la base ElasticSearch Log). Le reporter est configurable avec un interval de temps entre chaque reporting.

.. warning::
   Les index ElasticSearch ne sont pas configurables pour les métriques. Ils se nomment respectivement :
   * ``metrics-vitam-rest-YYYY.MM.dd`` pour les métriques REST
   * ``metrics-vitam-jvm-YYYY.MM.dd`` pour les métriques JVM
   * ``metrics-vitam-business-YYYY.MM.dd`` pour les métriques métier


Legacy
======
Pour celui ou celle qui souhaiterais continuer le développement du système de métriques au sein de VITAM, voici quelques points qui peuvent être intéressants à développer :

* Pour un reporter ElasticSearch, vérifier l'état de la connexion à chaque reporting et augmenter progressivement le temps de reporting si la base de données n'est pas accessible.
* Permettre le chargement de reporters de manière générique, en se passant du *switch* dans *VitamMetrics* et abstraire tout ce qui concerne le reporting.


Prometeus
*********
Depuis la release R14, la solution logicielle :term:`VITAM` intègre `Prometheus`.
A la différence des reporters ci-dessus qui diffuse par un modèle `push`, prometheus serveur a besoin d'une API pour récupérer les métriques depuis les applications.
L'avatange du fonctionnement du `Promtheus` avec un modèle `pull` est multiple :

    - Faciliter de lancer la supervision sur un post de dev lors du développement de nouvelles métriques
    - L'inaccessiblé de l'API est une information important pour la supervision des composants VITAM (Composant tombé).
    - L'API peut être appelée depuis un navigateur.

Prometheus fonctionne aussi en mode `push` pour les traitement de type batch (Pour plus d'information voir Pushgateway).


API
===
La classe qui expose l'API est AdminStatusResource:

.. code-block:: java

    @Path("/metrics")
    @GET
    @Produces(TextFormat.CONTENT_TYPE_004)
    public Response prometheusMetrics() {

        return Response
            .ok()
            .type(TextFormat.CONTENT_TYPE_004)
            .entity((StreamingOutput)
                output -> {
                    try (final Writer writer = new OutputStreamWriter(output)) {
                        TextFormat.write004(writer,
                            CollectorRegistry.defaultRegistry.metricFamilySamples());
                    }
                })
            .build();
    }

.. warning::
   L'api ci-dessus est exposée sur l'interface d'admin uniquement (Ip admin et Port admin).

Configuration du serveur promtheus
==================================
Pour que le serveur prometheus récupère les métriques, il suffit de déclarer l'API ci-dessus dans sa configuration.
L'URL complète de cette API est ``http(s)://ip-admin-composant-vitam:port-admin/admin/v1/metrics``.

Il est possible de configurer promtheus pour utiliser `Consul`.
Veillez-vous référer à la documentation officielle pour plus de détails sur la configuration d'un serveur `Prometheus`

Implémentation des métriques
============================
La solution prometheus met à disposition des libraries clientes, implémentés en différents langages, pour faciliter le développement de nouvelles métriques.

.. code-block:: xml

     <!-- Prometheus common -->
    <dependency>
        <groupId>io.prometheus</groupId>
        <artifactId>simpleclient_common</artifactId>
        <version>${prometheus-version}</version>
    </dependency>
    <!-- Prometheus the client -->
    <dependency>
        <groupId>io.prometheus</groupId>
        <artifactId>simpleclient</artifactId>
        <version>${prometheus-version}</version>
    </dependency>
    <!-- Prometheus hotspot JVM metrics-->
    <dependency>
        <groupId>io.prometheus</groupId>
        <artifactId>simpleclient_hotspot</artifactId>
        <version>${prometheus-version}</version>
    </dependency>
    <!-- Prometheus get dropwizard metrics-->
    <dependency>
        <groupId>io.prometheus</groupId>
        <artifactId>simpleclient_dropwizard</artifactId>
        <version>${prometheus-version}</version>
    </dependency>


Récupération des métriques déjà existante
-----------------------------------------

Dans la classe **CommonBusinessApplication**, les *VitamMetrics* sont enveloppées par des clients prometheus pour les exposer à son format.
    - La dépendance `simpleclient_dropwizard` permet facilement d'envelopper les métriques dropwizard déjà existantes et de les exposer au format prometheus.
    - La dépendance `simpleclient_hotspot` vient avec des métriques `jvm` prêtes à utiliser

.. code-block:: java

    /* Wrap up dropwizard metrics */
    new DropwizardExports(vitamMetrics.getRegistry()).register();

    /* Initialize JVM prometheus metrics */
    DefaultExports.initialize();


Développement de nouvelles métriques prometheus
________________________________________________
Prometheus dispose d'une *CollectorRegistry* instanciée par défaut au démarrage d'une application. Il suffit par la suite de développer des métriques et de les enregistrer dans cette *CollectorRegistry.defaultRegistry*
Quatre type de métriques sont possible:

    - Counter : Les métriques dont la valeur s'incrémente uniquement dans le temps (Exemple: Nombre de requêtes sur une API donnée)
    - Gauge : Les métriques dont la valeur s'incrémente ou se décrémente dans le temps (Exemple: L'utilisation de la RAM)
    - Histogram: Permet de compter le nombre d'événements et la somme de la durée d'execution de ces événements. Des fonctions sont à appliquer sur ces métriques du côté prometheus serveur pour faire des aggregations, moyenne, quantile, ...
    - Summary: A la différence de l'Histogramme, c'est l'application qui doit calculer des aggregation, moyenne, quantiles, ...

Ce qu'il faut retenir :
    - Pour chacune des types de métriques, on peut définir des `label`. Une métrique avec deux labels par exemple génère deux `séries temporelles`
    - La métrique de type histogram, peut définir des buckets.
    - Le nom de toutes les nouvelles métriques prometheus ajoutées sont listées dans la classe: **VitamMetricsNames**,

.. warning::
   Veuillez renseigner les nouvelles métriques dans la classe **VitamMetricsNames**
   Afin de mieux développer des métriques et de respecter les bonnes pratiques, veuillez vous référer à la documentation officielle de prometheus ``https://prometheus.io/docs/practices/``

La classe liste sous forme de constantes toutes les métriques prometheus ajoutées. Voici le contenu de cette classe:

.. code-block:: java

    public class VitamMetricsNames {

        private VitamMetricsNames() {
            // This class is only for constants
        }

        /*
         * =================================
         *            Common
         * ==================================
         */

        /**
         * Vitam requests size in bytes per tenant and method
         * Type: Summary
         * Labels: "tenant", "method"
         */
        public static final String VITAM_REQUESTS_SIZE_BYTES = "vitam_requests_size_bytes";

        /**
         * Vitam responses size in bytes per tenant and method
         * Type: Summary
         * Labels: "tenant", "method
         */
        public static final String VITAM_RESPONSES_SIZE_BYTES = "vitam_responses_size_bytes";


        /**
         * Vitam storage upload objects to offers size in bytes per tenant, strategy, offer_id, data_category, origin (normal, bulk, offer_sync), and per attempt
         * Type: Summary
         * Labels: "tenant", "strategy", "offer_id", "data_category", "origin", "attempt"
         */
        public static final String VITAM_STORAGE_UPLOAD_SIZE_BYTES = "vitam_storage_upload_size_bytes";

        /**
         * Vitam storage download objects from offers size in bytes per tenant, strategy, offer_id, origin of request  (normal, traceability, offer_sync) and data_category
         * Type: Summary
         * Labels: "tenant", "strategy", "offer_id", "origin", "data_category"
         */
        public static final String VITAM_STORAGE_DOWNLOAD_SIZE_BYTES = "vitam_storage_download_size_bytes";


        /**
         * Vitam alert service counter per log_level
         * Type: Counter
         * Labels: "log_level"
         */
        public static final String VITAM_ALERT_COUNTER = "vitam_alert_count";


        /**
         * Vitam consistency errors counter
         * Type: Counter
         * Labels: "tenant", "service"
         */
        public static final String VITAM_CONSISTENCY_ERRORS_COUNT = "vitam_consistency_errors_count";

        /*
         * =================================
         *            Processing
         * ==================================
         */

        /**
         * Vitam operation count per state and status
         * Type: Gauge
         * Labels: "workflow", "state", "status"
         */
        public static final String VITAM_PROCESSING_WORKFLOW_OPERATION_TOTAL = "vitam_processing_workflow_operation_total";

        /**
         * Current number of worker tasks in the queue
         * Type: Gauge
         * Labels: "worker_family"
         */
        public static final String VITAM_PROCESSING_WORKER_TASK_IN_QUEUE_TOTAL =
            "vitam_processing_worker_task_in_queue_total";


        /**
         * Current number of worker tasks instantiated by the distributor. In queue or waiting to be added to the queue
         * Type: Gauge
         * Labels: "worker_family", "workflow", "step_name"
         */
        public static final String VITAM_PROCESSING_WORKER_CURRENT_TASK_TOTAL =
            "vitam_processing_worker_current_task_total";

        /**
         * Current number of workers
         * Type : Gauge
         * Labels: "worker_family"
         */
        public static final String VITAM_PROCESSING_WORKER_REGISTERED_TOTAL = "vitam_processing_worker_registered_total";

        /**
         * Worker tasks execution duration. From call of worker until receiving the response. Task contains one or collection of elements to send to workers
         * Type: Histogram
         * Labels: "worker_family", "worker_name", "workflow", "step_name"
         */
        public static final String VITAM_PROCESSING_WORKER_TASK_EXECUTION_DURATION_SECONDS =
            "vitam_processing_worker_task_execution_duration_seconds";

        /**
         * Worker tasks waiting time since task creation until task dequeue from the queue. Task contains one or collection of elements to send to workers
         * Type: Histogram
         * Labels: "worker_family", "workflow", "step_name"
         */
        public static final String VITAM_PROCESSING_WORKER_TASK_IDLE_DURATION_IN_QUEUE_SECONDS =
            "vitam_processing_worker_task_idle_duration_in_queue_seconds";


        /**
         * ProcessWorkflow step execution duration. From call of distributor until receiving the response
         * Type: Histogram
         * Labels: "workflow", "step_name"
         */
        public static final String VITAM_PROCESSING_WORKFLOW_STEP_EXECUTION_DURATION_SECONDS =
            "vitam_processing_workflow_step_execution_duration_seconds";


        /*
         * =================================
         *            Metadata
         * ==================================
         */

        /**
         * Vitam metadata effective log shipping histogram duration metric
         * Type: Histogram
         * Labels: "collection"
         */
        public static final String VITAM_METADATA_LOG_SHIPPING_DURATION = "vitam_metadata_log_shipping_duration";

        /**
         * Vitam metadata log shipping events counter for all events. Even for those with response already running
         * Type: Counter
         */
        public static final String VITAM_METADATA_LOG_SHIPPING_TOTAL = "vitam_metadata_log_shipping_total";

        /**
         * Vitam metadata reconstruction histogram metric
         * Type: Histogram
         * Labels: "tenant", "container"
         */
        public static final String VITAM_RECONSTRUCTION_DURATION = "vitam_reconstruction_duration";
    }


Deux façons d'implémenter les métriques prometheus:

    - Soit en utilisant les classes déjà disponible. Ci-dessous des exemples de métriques développées pour le composant `processing`

    .. code-block:: java

        // Exemple d'une gauge
        // Il suffit partout dans le code d'appeler WORKER_TASKS_IN_QUEUE.inc() et WORKER_TASKS_IN_QUEUE.dec()
        public static final Gauge WORKER_TASKS_IN_QUEUE = Gauge.build()
        .name("vitam_processing_worker_task_in_queue_total")
        .labelNames("worker_family")
        .help("Current number of worker tasks in the queue")
        .register();

        // Exemple d'un Histogram
        // Pour l'histogramme on peut utiliser des buckets
        // Pour chaque événement, si sa durée d'execution est inférieure la valeur de la bucket, le compteur du nombre d'événements pour cette bucket est incrémenté
        public static final Histogram PROCESS_WORKFLOW_STEP_EXECUTION_DURATION_HISTOGRAM = Histogram.build()
        .name("vitam_processing_workflow_step_execution_duration_seconds")
        .help("ProcessWorkflow step execution duration. From call of distributor until receiving the response")
        .labelNames("workflow", "step_name")
        .buckets(.1, .25, .5, .75, 1, 2.5, 5, 7.5, 10, 30, 60, 120, 180, 300, 600, 1800, 3600)
        .register();

        // Exemple d'utilisation d'Histogram
        Histogram.Timer timer =
                CommonProcessingMetrics.PROCESS_WORKFLOW_STEP_EXECUTION_DURATION_HISTOGRAM
                .labels(workParams.getLogbookTypeProcess().name(), step.getStepName())
                .startTimer();
        try {
            // Process any action that we want to compute duration
        } finally {
            timer.observeDuration();
        }



    - Soit en étend la classe Collector


    .. code-block:: java

        // Exemple d'une métrique du composant `processing` ProcessWorkflowMetricsCollector.
        public class ProcessWorkflowMetricsCollector extends Collector {
            private static final ProcessWorkflowMetricsCollector instance = new ProcessWorkflowMetricsCollector();
            private ProcessWorkflowMetricsCollector() {
                // Private constructor for singleton
                register();
            }
            public static ProcessWorkflowMetricsCollector getInstance() {
                return instance;
            }
            @Override
            public List<MetricFamilySamples> collect() {
                // Collect
                return xxxx.collect();
            }
        }
