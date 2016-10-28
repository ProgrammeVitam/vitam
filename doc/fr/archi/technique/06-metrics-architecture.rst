Métriques applicatives
######################


Besoins
=======

À des fins de monitoring des composants logiciel Java VITAM et de l'utilisation des ressources système par ceux-ci, VITAM intègre un reporting et une gestion de métriques applicatives.


Modèle générique
================

On peut noter les composants suivants :

* Enregistreur de métriques : il s'agit de la librairie en charge de l'enregistrement d'une métrique.
* Reporters de métriques: il s'agit de librairies en charge de collecter les métriques enregistrées et d'en faire un reporting.
* Stockage des métriques : il s'agit du composant stockant les métriques (de manière plus ou moins requêtable).
* Visualisation des métriques : il s'agit du composant (souvent IHM) qui permet la recherche et la visualisation des métriques. 

L'architecture générique peut être vue de la manière suivante : 

.. figure:: images/gestion_logs.png
    :align: center

    Architecture générique d'un système de gestion de logs.

    VITAM n'implémente qu'une sous partie de cette architecture générique (la centralisation / stockage / visualisation), mais permet l'intégration d'un composant externe de gestion de logs.


Choix des implémentations
=========================

De manière générale, l'implémentation s'appuie fortement sur une architecture syslog.

.. figure:: images/technical-architecture-exploitation.*
    :align: center
    :height: 15 cm

    Architecture du sous-système de centralisation des logs



Enregistreur de métriques
-------------------------

Dans le système VITAM, l'enregistrement de métriques s'effectue uniquement dans les composants logiciel Java à l'aide de la librairie `Dropwizard metrics <http://metrics.dropwizard.io/3.1.0/>`_.

Les plugins suivants sont utilisés pour leur métriques respectives :

* `Dropwizard Jersey integration <http://metrics.dropwizard.io/3.1.0/manual/jersey/#instrumenting-jersey-2-x>`_ pour les métriques Jersey.
* `Dropwizard JVM integration <http://metrics.dropwizard.io/3.1.0/manual/jvm/>`_ pour les métriques JVM.

L'enregistreur de métriques possède un registre interne qui peut stocker différentes métriques : **Gauges**, **Timer**, **Meter**, **Counter** ou **Histograms**. Ces métriques seront collectées dans le temps par le/les reporter(s) de métriques.

Les métriques Jersey sont automatiquement générées par application VITAM. Elles représentent un jeu de 3 métriques, **Meter**, **Timer** et **ExceptionMeter** pour chaque end-point des ressources de l'application.

Les métriques JVM sont aussi uniques par application. Elles représentent plusieurs types de métriques sur la consommation de ressources système.

.. note::
        Une description fonctionelle des métriques est disponible dans le `manuel utilisateur dropwizard metrics <http://metrics.dropwizard.io/3.1.0/manual/core/>`_


Reporters de métriques
----------------------

Dans le système VITAM, un ou plusieurs reporters de métriques peuvent être utilisés. A ce jour, il existe deux reporters différents :

* Un reporter Console Java (sortie standard de l'application).
* Un reporter ElasticSearch issue de la librairie `metrics elasticsearch reporter <https://github.com/elastic/elasticsearch-metrics-reporter-java>`_.

Les reporters sont utilisés dans les composants logiciel Java. Ils sont en charge de récupérer les valeurs de toutes les métriques enregistrées et de les transmettre sur différents canaux; ici la Console Java ou une base de donnée ElasticSearch. 


Stockage des métriques
----------------------

Si un reporter de métriques ElasticSearch est utilisé, celles-ci seront stockées dans le moteur d'indexation ElasticSearch, dans un cluster dédié au stockage des logs/métriques (pour séparer les données de logs/métriques et les données métier d'archives).

 Ce cluster est configuré de la manière suivante :

* Taille du cluster (pour les déploiements VITAM de taille importante, ce nombre pourra être amené à évoluer (Cf. les abbaques :doc:`fournies plus loin <20-resources>`)) :

    - Nombre nominal de noeuds : 2 ; 
	- Nombre nominal de shards primaires par index : 4 ;
	- Nombre nominal de replica : 1 ;
	
.. note::
	Ces paramètres ne permettent pas de se parer contre la perte d'un noeud elasticsearch, et correspondent à un compromis en terme d'usage des resources VS résilience du système.
	Ces paramètres peuvent être changés si un besoin plus fort de résilience était identifié. Dans ce cas, on peut augmenter le nombre de noeuds ainsi que le nombre de replica, en veillant à ce que le nombre de shards primaires ne soit jamais inférieur au nombre de noeuds du cluster, et que le nombre de replica ne soit jamais supérieur au nombre de noeuds du cluster - 1.

.. caution:: Une modification du nombre de shards primaires d'un index est une opération coûteuse à réaliser sur un cluster en cours de fonctionnement et qui doit dans la mesure du possible être évitée (indisponibilité du cluster et/ou risque de corruption et de perte de données en cas de problème au cours de l'opération) ; le bon dimensionnement de cette valeur doit être réalisé dès l'installation du cluster.

* Index : chaque index stockant des données de métriques correspond à 1 jour de métriques (déterminé à partir du timestamp de la métrique). Les index définis sont les suivants :

    - ``metrics-vitam-jersey-YYYY.MM.dd`` pour les métriques de Jersey, avec un champ *name* automatiquement généré sous la forme :

        **uri:http_method:consumed_types:produced_types:metric_type**

    - ``metrics-vitam-jvm-YYYY.MM.dd`` pour les métriques JVM.

    - ``metrics-vitam-business-YYYY.MM.dd`` pour les métriques métier.

    - ``.kibana`` pour le stockage des paramètres (et notamment des dashboards) Kibana.


.. Gestion des index
.. +++++++++++++++++

.. La création des templates d'index et des index doit être réalisée par l'application à l'origine de l'écriture dans Elasticsearch (kibana pour l'index ``.kibana``, logstash pour les autres index). La gestion des index est réalisée par l'application `Curator <https://www.elastic.co/guide/en/elasticsearch/client/curator/4.0/index.html>`_. Par défaut, l'outil est livré avec la configuration suivante :

.. * Durée de maintien des index "online" : 30 jours ; cela signifie qu'au bout de 30 jours, les index seront fermés, et n'apparaîtront donc plus dans l'IHM de suivi des logs. Cependant, ils sont conservés, et pourront donc être réouverts en cas de besoin.
.. * Durée de conservation des index : 365 jours ; au bout de cette durée, les index seront supprimés.


Visualisation des métriques
---------------------------

La visalisation des métriques se fait par le composant Kibana. Il est instancié de manière unique, et persiste sa configuration dans ElasticSearch (dans l'index ``.kibana``).

Aucun mécanisme d'authentification n'est mis en place pour sécuriser l'accès à Kibana.

.. hint:: La version opensource de Kibana, utilisée dans VITAM, ne supporte pas nativement l'authentification des clients ; d'autres solutions peuvent être mises en place (ex: l'utilisation du composant `shield <https://www.elastic.co/products/shield>`_ ), sous réserve d'une étude de compatibilité de la solution choisie.


Limites
=======

La solution implémentée dans Vitam possède les limites connues suivantes :

* Du fait que la librairie Dropwizard Metrics fait une aggregation des métriques et que le système de visualisation Kibana fonctionne lui aussi à l'aide d'aggrégations, les résultats visualisés sont corrects dans la limite d'une certaine précision (certaines données deviennent non-représentatives de la réalité). 
* Il n'existe à ce jour que 3 types de métriques, **Meter**, **Timer** et **ExceptionMeter** supportés par le plugin Jersey Dropwizard Metrics.
