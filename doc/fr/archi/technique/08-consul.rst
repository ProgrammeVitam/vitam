Service registry
################


Architecture
============

Un déploiement Consul est composé de 2 types de noeuds différents :

* Les noeuds serveurs : ils persistent l'état des données stockées dans Consul ; les données sont répliquées entre eux, et eux seuls participent à l'élection du maître (ils forment un cluster Raft). Un quorum de ces noeuds doit toujours être déclarés ; dans le cas contraire, on entre dans un cas de désastre de cluster (Cf. `la documentation sur l'"outage recovery" <https://www.consul.io/docs/guides/outage.html>`_ ) ; le nombre de serveurs doit être impair, avec un minimum conseillé de 3 noeuds (pour des problématiques de maintien de quorum).
* Les noeuds client : ils exposent les API d'accès aux structures de données Consul, et réalisent les healthchecks des services dont ils ont la définition. Ils communiquent avec les serveurs.

Un noeud Consul est également appelé un agent.

.. note:: Les noeuds serveurs sont en fait des noeuds clients réifiés, i.e. ils ont également les capacités des clients.

Dans le cadre de VITAM, le déploiement des noeuds consul doit correspondre aux principes suivants :

* Un cluster de serveurs consul sur un nombre impair de noeuds dédiés, chacun d'entre eux étant configurés pour exposer l'IHM de suivi ;
* 1 client par serveur hébergeant un service VITAM.

Préconisation
=============

Le fonctionnement de consul via trois noeuds master nous prémunissent de la perte d'un de ces noeuds sans perturbation du service.

Résolution DNS
==============

Les résolutions de noms de service se font via l'API dns de Consul ; un resolver externe doit être configuré pour les requêtes externes. 

Chaque client agit comme serveur DNS local ; il écoute sur le port udp 53 (sur la boucle locale - ``127.0.0.1``), et est configuré comme serveur DNS de l'OS (typiquement dans le fichier ``/etc/resolv.conf``).

.. caution:: Cela rend consul incompatible avec d'autres implémentations de serveur DNS qui seraient lancés sur l'OS, et en particulier les cache DNS installés par défaut dans certaines distributions linux (ex: dnsmasq).

.. note:: Pour pouvoir écouter sur le port 53, consul nécessite la capacité ``CAP_NET_BIND_SERVICE`` (Cf. la section suivante).

Lorsque le système fait une requête DNS, cette dernière arrive à l'agent consul local et la séquence suivante est exécutée :

* Si le nom à résoudre appartient au domaine réservé pour consul (par défaut ``consul``), il est résolu en tant que nom de service ou de noeud (Cf. `la documentation officielle concernant l'interface DNS <https://www.consul.io/docs/agent/dns.html>`_) ;
* Dans le cas contraire, la requête est transmise aux serveurs DNS configurés dans la liste des `recursors <https://www.consul.io/docs/agent/options.html#recursors>`_).

.. note:: Consul a pour l'instant été configuré en mode ``allow_stale = false`` (Cf. `la directive de configuration <https://www.consul.io/docs/agent/options.html#allow_stale>`_) , ce qui signifie que chaque requête DNS se traduit par un appel RPC au noeud leader des serveurs Consul. Cela permet d'assurer la consistance des réponses DNS, mais peut potentiellement poser des problèmes de performance sur des larges déploiements. Il est possible de changer ce comportement (clés de configuration ``allow_stale`` et ``max_stale`` - qui permettent de préciser la durée maximum pendant laquelle le noeud répond aux requêtes DNS sans interroger le leader), et également de changer le TTL des réponses DNS (qui est par défaut gardé à 0). 

Packaging
=========

Vitam intègre un package RPM dédié pour consul ; ce package permet essentiellement :

* De configurer consul en tant que service systemd ;
* De permettre le lancement de consul sous l'utilisateur ``vitam`` ;
* Enfin, il intègre une directive ``setcap`` de post-install pour attribuer la capacité ``CAP_NET_BIND_SERVICE`` au binaire ``/vitam/vitam-consul/bin/consul`` afin de permettre à ce dernier d'exposer une interface DNS sur le port 53 sans pour autant nécessiter les droits root.


Monitoring
==========

Chaque instance de service doit être déclaré dans Consul ; cette déclaration se fait en déposant un fichier de configuration dans le répertoire de configuration de consul. Ce fichier contient notamment l'identifiant du service ainsi que son port d'écoute, ainsi qu'une liste de healthchecks qui permettent à Consul de connaître l'état du service. Pour les services VITAM, ces healthchecks s'appuient sur les API de supervision qui ont été décrites dans :doc:`la section dédiée </fonctionnelle-exploitation/40-principles-monitoring>`.

Consul permet d'exposer une IHM Web permettant d'accéder à la topologie des services dépoyés (i.e. quel service sur quel noeud) et à leur état instantané.
