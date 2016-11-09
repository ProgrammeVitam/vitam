Métriques dans VITAM
####################

Fonctionnement
**************
Les métriques dans VITAM sont stockées dans le package :

**fr.gouv.common.metrics**

Les registres de métriques et les reporters de métriques sont tous les deux contenus dans une classe *VitamMetrics*. Cette classe doit être instanciée avec un *VitamMetricsType* qui peut être **JERSEY**, **JVM** ou **BUSINESS**. Le type définira les métriques enregistrées dans le registre interne de la classe.

La classe **AbstractVitamApplication** contient une *Map* statique de *VitamMetrics* qui est initialisée à chaque configuration d'une application VITAM. Cette *Map* contient obligatoirement un *VitamMetrics* de type BUSINESS et peut accessoirement contenir les *VitamMetrics* de types JVM et JERSEY. La fonction en question est :

.. code-block:: java

   protected static final void clearAndconfigureMetrics()

Cette fonction vide et recharge les métriques à chaque création d'une application VITAM. Les reporters de métriques quant à eux sont démarrés lors d'un appel à la fonction :

.. code-block:: java

   public final void run() throws VitamApplicationServerException

qui elle même appelle la fonction :

.. code-block:: java

   public final void startMetrics()

de la classe *AbstractVitamApplication*.

.. note::
   Les *VitamMetrics* de type JERSEY ou JVM n'ont pas à être modifiés épendant l'execution d'une application VITAM.

Configuration
*************
Les métriques sont configurées dans le fichier ``/vitam/conf/<service_id>/vitam.metrics.conf``. Ce fichier contient la documentation nécessaire pour configurer correctement les métriques.

Métriques métier
****************
Les métriques métiers permettent aux développeurs d'enregistrer des métriques n'importe où dans le code, pour par exemple suivre une variable ou bien chronométrer une fonction. Pour cela il suffit d'appeler la fonction statique *getBusinessMetricRegistry* dans la classe *AbstractVitamApplication*, puis d'enregistrer une métrique.

.. code-block:: java

   AbstractVitamApplication.getBusinessMetricsRegistry().register("Running workflows",
       new Gauge<Long>() {
           @Override
           public Long getValue() {
               return runningWorkflows.get();
           }
       });

Reporters
*********
2 reporters sont disponibles, un reporter Logback (toutes les métriques sont dumpées dans Logback) ou bien un reporter ElasticSearch (toutes les métriques sont dumpées dans la base ElasticSearch Log). Le reporter est configurable avec un interval de temps entre chaque reporting.

.. warning::
   Les index ElasticSearch ne sont pas configurables pour les métriques. Ils se nomment respectivement :
   
   * ``metrics-vitam-jersey-YYYY.MM.dd`` pour les métriques JERSEY
   * ``metrics-vitam-jvm-YYYY.MM.dd`` pour les métriques JVM
   * ``metrics-vitam-business-YYYY.MM.dd`` pour les métriques métier

   

