Paramétrages supplémentaires
#############################

.. _update_jvm:

`Tuning` JVM
============

.. caution:: En cas de colocalisation, bien prendre en compte la taille :term:`JVM` de chaque composant (VITAM : ``-Xmx512m`` par défaut) pour éviter de `swapper`.


Un `tuning` fin des paramètres :term:`JVM` de chaque composant :term:`VITAM` est possible.
Pour cela, il faut modifier le contenu du fichier ``deployment/environments/group_vars/all/main/jvm_opts.yml``

Pour chaque composant, il est possible de modifier ces 3 variables:

* memory: paramètres Xms et Xmx
* gc: paramètres gc
* java: autres paramètres java

Installation des *griffins* (greffons de préservation)
======================================================

.. note:: Fonctionnalité disponible partir de la R9 (2.1.1) .

.. caution:: Cette version de :term:`VITAM` ne mettant pas encore en oeuvre de mesure d'isolation particulière des *griffins*, il est recommandé de veiller à ce que l'usage de chaque *griffin* soit en conformité avec la politique de sécurité de l'entité. Il est en particulier déconseillé d'utiliser un griffon qui utiliserait un outil externe qui n'est plus maintenu.

Il est possible de choisir les *griffins* installables sur la plate-forme. Pour cela, il faut éditer le contenu du fichier ``deployment/environments/group_vars/all/main/main.yml`` au niveau de la directive ``vitam_griffins``. Cette action est à rapprocher de l'incorporation des binaires d'installation : les binaires d'installation des greffons doivent être accessibles par les machines hébergeant le composant **worker**.

Exemple::

   vitam_griffins: ["vitam-imagemagick-griffin", "vitam-jhove-griffin"]

Voici la liste des greffons disponibles au moment de la présente publication :


.. literalinclude:: liste_griffins.txt
   :language: text

.. warning:: Ne pas oublier d'avoir déclaré au préalable sur les machines cibles le dépôt de binaires associé aux *griffins*.


Rétention liée aux logback
===========================

La solution logicielle :term:`VITAM` utilise logback pour la rotation des log, ainsi que leur rétention.

Il est possible d'appliquer un paramétrage spécifique pour chaque composant VITAM.

Éditer le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml`` (et ``extra_vars.yml``, dans le cas des extra) et appliquer le paramétrage dans le bloc ``logback_total_size_cap`` de chaque composant sur lequel appliquer la modification de paramétrage.
Pour chaque **APPENDER**, la valeur associée doit être exprimée en taille et unité (exemple : 14GB ; représente 14 gigabytes).

.. note :: des *appenders* supplémentaires existent pour le composant storage-engine (appender offersync) et offer (offer_tape et offer_tape_backup).


Cas des accesslog
------------------

Il est également possible d'appliquer un paramétrage différent par composant VITAM sur le logback *access*.

Éditer le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml`` (et ``extra_vars.yml``, dans le cas des extra) et appliquer le paramétrage dans les directives ``access_retention_days`` et ``access_total_size_GB`` de chaque composant sur lequel appliquer la modification de paramétrage.

.. _confantivirus:

Paramétrage de l'antivirus (ingest-external)
============================================

L'antivirus utilisé par ingest-external est modifiable (par défaut, ClamAV) ; pour cela :

* Éditer la variable ``vitam.ingestexternal.antivirus`` dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml`` pour indiquer le nom de l'antivirus à utiliser.
* Créer un script shell (dont l'extension doit être ``.sh``) sous ``environments/antivirus/`` (norme : scan-<vitam.ingestexternal.antivirus>.sh) ; prendre comme modèle le fichier ``scan-clamav.sh``. Ce script shell doit respecter le contrat suivant :

    * Argument : chemin absolu du fichier à analyser
    * Sémantique des codes de retour
        - 0 : Analyse OK - pas de virus
        - 1 : Analyse OK - virus trouvé et corrigé
        - 2 : Analyse OK - virus trouvé mais non corrigé
        - 3 : Analyse NOK
    * Contenu à écrire dans stdout / stderr
        - stdout : Nom des virus trouvés, un par ligne ; Si échec (code 3) : raison de l’échec
        - stderr : Log « brut » de l’antivirus

.. caution:: En cas de remplacement de clamAV par un autre antivirus, l'installation de celui-ci devient dès lors un prérequis de l'installation et le script doit être testé.

.. warning:: Il subsiste une limitation avec l'antivirus ClamAV qui n'est actuellement pas capable de scanner des fichiers > 4Go. Ainsi, il n'est pas recommandé de conserver cet antivirus en environnement de production.

.. warning:: Sur plate-forme Debian, ClamAV est installé sans base de données. Pour que l'antivirus soit fonctionnel, il est nécessaire, durant l'installation, de le télécharger ; il est donc nécessaire de renseigner dans l'inventaire la directive ``http_proxy_environnement`` ou de renseigner un `miroir local privé <https://www.clamav.net/documents/private-local-mirrors>`_).


Extra: Avast Business Antivirus for Linux
-----------------------------------------

.. note:: Avast étant un logiciel soumis à licence, Vitam ne fournit pas de support ni de licence nécessaire à l'utilisation de Avast Antivirus for Linux.

  Vous trouverez plus d'informations sur le site officiel : `Avast Business Antivirus for Linux <https://www.avast.com/fr-fr/business/products/linux-antivirus>`_

..

À la place de clamAV, il est possible de déployer l'antivirus **Avast Business Antivirus for Linux**.

Pour se faire, il suffit d'éditer la variable ``vitam.ingestexternal.antivirus: avast`` dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``.

Il sera nécessaire de fournir le fichier de licence sous ``deployment/environments/antivirus/license.avastlic`` pour pouvoir déployer et utiliser l'antivirus Avast.

De plus, il est possible de paramétrer l'accès aux repositories (Packages & Virus definitions database) dans le fichier ``deployment/environments/group_vars/all/advanced/cots_vars.yml``.

Si les paramètres ne sont pas définis, les valeurs suivantes sont appliquées par défaut.

.. code-block:: yaml

  ## Avast Business Antivirus for Linux
  ## if undefined, the following default values are applied.
  avast:
      # logs configuration
      logrotate: enabled # or disabled
      history_days: 30 # How many days to store logs if logrotate is set to 'enabled'
      manage_repository: true
      repository:
          state: present
          # For CentOS
          baseurl: http://rpm.avast.com/lin/repo/dists/rhel/release
          gpgcheck: no
          proxy: _none_
          # For Debian
          baseurl: 'deb http://deb.avast.com/lin/repo debian-buster release'
      vps_repository: http://linux-av.u.avcdn.net/linux-av/avast/x86_64
      ## List of sha256 hash of excluded files from antivirus. Useful for test environments.
      whitelist:
          - <EMPTY>

..

.. warning:: Vitam gère en entrée les SIPs aux formats: ZIP ou TAR (tar, tar.gz ou tar.bz2); cependant et d'après les tests effectués, il est fortement recommandé d'utiliser le format .zip pour bénéficier des meilleures performances d'analyses avec le scan-avast.sh.

De plus, il faudra prendre en compte un dimensionnement supplémentaire sur les ingest-external afin de pouvoir traiter le scan des fichiers >500Mo.

Dans le cas d'un SIP au format .zip ou .tar, les fichiers >500Mo contenus dans le SIP seront décompressés et scannés unitairement. Ainsi la taille utilisée ne dépassera pas la taille d'un fichier.

Dans le cas d'un SIP au format .tar.gz ou .tar.bz2, les SIPs >500Mo seront intégralement décompressés et scannés. Ainsi, la taille utilisée correspondra à la taille du SIP décompressé.

..

Paramétrage des certificats externes (\*-externe)
=================================================

Se reporter au chapitre dédié à la gestion des certificats: :doc:`20-certificats`

Placer "hors Vitam" le composant ihm-demo
=========================================

Sous ``deployment/environments/host_vars``, créer ou éditer un fichier nommé par le nom de machine qui héberge le composant ihm-demo et ajouter le contenu ci-dessous :

   consul_disabled: true

Il faut également modifier le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml`` en remplaçant :

* dans le bloc ``accessexternal``, la directive ``host: "access-external.service.{{ consul_domain }}"`` par ``host: "<adresse IP de access-external>"`` (l'adresse IP peut être une :term:`FIP`)
* dans le bloc ``ingestexternal``, la directive ``host: "ingest-external.service.{{ consul_domain }}"`` par ``host: "<adresse IP de ingest-external>"`` (l'adresse IP peut être une :term:`FIP`)


A l'issue, le déploiement n'installera pas l'agent Consul. Le composant ihm-demo appellera, alors, par l'adresse :term:`IP` de service les composants "access-external" et "ingest-external".

Il est également fortement recommandé de positionner la valeur de la directive ``vitam.ihm_demo.metrics_enabled`` à ``false`` dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``, afin que ce composant ne tente pas d'envoyer des données sur "elasticsearch-log".

Paramétrer le ``secure_cookie`` pour ihm-demo
=============================================

Le composant ihm-demo (ainsi qu'ihm-recette) dispose d'une option supplémentaire, par rapport aux autres composants VITAM, dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``: le ``secure_cookie`` qui permet de renforcer ces deux :term:`IHM` contre certaines attaques assez répandues comme les CSRF (Cross-Site Request Forgery).

Il faut savoir que si cette variable est à *true* (valeur par défaut), le client doit obligatoirement se connecter en https sur l':term:`IHM`, et ce même si un reverse proxy se trouve entre le serveur web et le client.

Cela peut donc obliger le reverse proxy frontal de la chaîne d'accès à écouter en https.


Paramétrage de la centralisation des logs VITAM
================================================

2 cas sont possibles :

* Utiliser le sous-système de gestion des logs fourni par la solution logicielle :term:`VITAM` ;
* Utiliser un :term:`SIEM` tiers.

Gestion par VITAM
-------------------

Pour une gestion des logs par :term:`VITAM`, il est nécessaire de déclarer les serveurs ad-hoc dans le fichier d'inventaire pour les 3 groupes suivants :
    - hosts_logstash
    - hosts_kibana_log
    - hosts_elasticsearch_log


Redirection des logs sur un :term:`SIEM` tiers
----------------------------------------------

En configuration par défaut, les logs VITAM sont tout d'abord routés vers un serveur rsyslog installé sur chaque machine.
Il est possible d'en modifier le routage, qui par défaut redirige vers le serveur logstash, via le protocole syslog en TCP.

Pour cela, il est nécessaire de placer un fichier de configuration dédié dans le dossier ``/etc/rsyslog.d/`` ; ce fichier sera automatiquement pris en compte par rsyslog. Pour la syntaxe de ce fichier de configuration rsyslog, se référer à la `documentation rsyslog <http://www.rsyslog.com/doc/v7-stable/>`_.

.. tip:: Pour cela, il peut être utile de s'inspirer du fichier de référence :term:`VITAM` ``deployment/ansible-vitam/roles/rsyslog/templates/vitam_transport.conf.j2`` (attention, il s'agit d'un fichier template ansible, non directement convertible en fichier de configuration sans en ôter les directives jinja2).

Passage des identifiants des référentiels en mode `esclave`
===========================================================

La génération des identifiants des référentiels est géré par :term:`VITAM` lorsqu'il fonctionne en mode maître.

Par exemple :

- Préfixé par ``PR-`` pour les profils
- Préfixé par ``IC-`` pour les contrats d'entrée
- Préfixé par ``AC-`` pour les contrats d'accès

Depuis la version 1.0.4, la configuration par défaut de :term:`VITAM` autorise des identifiants externes (ceux qui sont dans le fichier json importé).

 - pour le tenant 0 pour les référentiels : contrat d'entrée et contrat d'accès.
 - pour le tenant 1 pour les référentiels : contrat d'entrée, contrat d'accès, profil, profil de sécurité et contexte.

La liste des choix possibles, pour chaque tenant, est :

.. csv-table:: Description des identifiants de référentiels
   :file: external_identifiers.csv
   :delim: ;
   :class: longtable
   :widths: 1, 2
   :header-rows: 1


Si vous souhaitez gérer vous-même les identifiants sur un service référentiel, il faut qu'il soit en mode esclave.

Par défaut tous les services référentiels de Vitam fonctionnent en mode maître. Pour désactiver le mode maître de :term:`VITAM`, il faut modifier le fichier ansible ``deployment/environments/group_vars/all/main/main.yml`` dans les sections ``vitam_tenants_usage_external`` (pour gérer, par tenant, les collections en mode esclave).

Paramétrage du batch de calcul pour l'indexation des règles héritées
====================================================================

La paramétrage du batch de calcul pour l'indexation des règles héritées peut être réalisé dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``.

La section suivante du fichier ``vitam_vars.yml`` permet de paramétrer la fréquence de passage du batch :

.. code-block:: yaml

    vitam_timers:
        metadata:
            - name: vitam-metadata-computed-inherited-rules
              frequency: "*-*-* 02:30:00"

La section suivante du fichier ``vitam_vars.yml`` permet de paramétrer la liste des tenants sur lequels s'exécute le batch :

.. code-block:: yaml

    vitam:
      worker:
            # api_output_index_tenants : permet d'indexer les règles de gestion, les chemins des règles et les services producteurs
            api_output_index_tenants: [0,1,2,3,4,5,6,7,8,9]
            # rules_index_tenants : permet d'indexer les règles de gestion
            rules_index_tenants: [0,1,2,3,4,5,6,7,8,9]

Durées minimales permettant de contrôler les valeurs saisies
==============================================================

Afin de se prémunir contre une alimentation du référentiel des règles de gestion avec des durées trop courtes susceptibles de déclencher des actions indésirables sur la plate-forme (ex. éliminations) – que cette tentative soit intentionnelle ou non –, la solution logicielle :term:`VITAM` vérifie que l’association de la durée et de l’unité de mesure saisies pour chaque champ est supérieure ou égale à une durée minimale définie lors du paramétrage de la plate-forme, dans un fichier de configuration.

Pour mettre en place le comportement attendu par le métier, il faut modifier le contenu de la directive ``vitam_tenant_rule_duration`` dans le fichier ansible ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``.

Exemple:

.. code-block:: yaml

  vitam_tenant_rule_duration:
    - name: 2 # applied tenant
      rules:
        - AppraisalRule : "1 year" # rule name : rule value
    - name: 3
      rules:
        AppraisaleRule : "5 year"
        StorageRule : "5 year"
        ReuseRule : "2 year"


Par `tenant`, les directives possibles sont :

.. csv-table:: Description des règles
   :file: rules.csv
   :delim: ;
   :class: longtable
   :widths: 1, 1
   :header-rows: 1

Les valeurs associées sont une durée au format <nombre> <unité en anglais, au singulier>

Exemples:

   6 month
   1 year
   5 year

.. seealso:: Pour plus de détails, se rapporter à la documentation métier "Règles de gestion".

Augmenter la précision sur le nombre de résultats retournés dépassant 10000
===========================================================================

Suite à une évolution d'ElasticSearch (à partir de la version 7.6), le nombre maximum de résultats retournés est limité à 10000. Ceci afin de limiter la consommation de ressources sur le cluster elasticsearch.

Pour permettre de retourner le nombre exact de résultats, il est possible d'éditer le paramètre ``vitam.accessexternal.authorizeTrackTotalHits`` dans le fichier de configuration ``environments/group_vars/all/vitam_vars.yml``

Il sera nécessaire de réappliquer la configuration sur le groupe hosts_access_external:

.. code-block:: bash

  ansible-playbook ansible-vitam/vitam.yml --limit hosts_access_external --tags update_vitam_configuration -i environments/hosts.<environnement> --ask-vault-pass

..

Ensuite, si l'API de recherche utilise le type d'entrée de DSL "SELECT_MULTIPLE", il faut ajouter ``$track_total_hits : true`` au niveau de la partie "filter" de la requête d'entrée.

Ci-dessous, un exemple de requête d'entrée :

.. code-block:: json

    {
      "$roots": [],
      "$query": [
       {
         "$match": {
            "Title": "héritage"
         }
       }
      ],
      "$filter": {
        "$offset": 0,
        "$limit": 100,
        "$track_total_hits": true
      },
      "$projection": {}
    }

..

Fichiers complémentaires
========================

A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées dans les fichiers suivants :

* ``deployment/environments/group_vars/all/main/main.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/main/main.yml
     :language: yaml
     :linenos:

.. note:: Installation multi-sites. Déclarer dans ``consul_remote_sites`` les datacenters Consul des autres site ; se référer à l'exemple fourni pour renseigner les informations.

* ``deployment/environments/group_vars/all/advanced/vitam_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/advanced/vitam_vars.yml
     :language: yaml
     :linenos:

.. note:: Cas du composant ingest-external. Les directives ``upload_dir``, ``success_dir``, ``fail_dir`` et ``upload_final_action`` permettent de prendre en charge (ingest) des fichiers déposés dans ``upload_dir`` et appliquer une règle ``upload_final_action`` à l'issue du traitement (NONE, DELETE ou MOVE dans ``success_dir`` ou ``fail_dir`` selon le cas). Se référer au :term:`DEX` pour de plus amples détails. Se référer au manuel de développement pour plus de détails sur l'appel à ce cas.

.. warning:: Selon les informations apportées par le métier, redéfinir les valeurs associées dans les directives ``classificationList`` et ``classificationLevelOptional``. Cela permet de définir quels niveaux de protection du secret de la défense nationale, supporte l'instance. Attention : une instance de niveau supérieur doit toujours supporter les niveaux inférieurs.

* ``deployment/environments/group_vars/all/advanced/cots_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/advanced/cots_vars.yml
     :language: yaml
     :linenos:

.. note:: Concernant Curator, en environnement de production, il est recommandé de procéder à la fermeture des index au bout d'une semaine pour les index de type "logstash" (3 jours pour les index "metrics"), qui sont le reflet des traces applicatives des composants de la solution logicielle :term:`VITAM`. Il est alors recommandé de lancer le *delete* de ces index au bout de la durée minimale de rétention : 1 an (il n'y a pas de durée de rétention minimale légale sur les index "metrics", qui ont plus une vocation technique et, éventuellement, d'investigations).

* ``deployment/environments/group_vars/all/advanced/jvm_opts.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/advanced/jvm_opts.yml
     :language: yaml
     :linenos:

.. note:: Cette configuration est appliquée à la solution logicielle :term:`VITAM` ; il est possible de créer un tuning par "groupe" défini dans ansible.


Paramétrage de l'Offre Froide ( librairies de cartouches )
==========================================================

.. seealso:: Les principes de fonctionnement de l'offre froide sont décrits dans la documentation externe dédiée ("Archivage sur Offre Froide").

La librairie et les lecteurs doivent déjà être configurés sur la machine devant supporter une instance de ce composant (avec login automatique en cas de reboot).

La commande ``lsscsi -g`` peut permettre de vérifier si des périphériques sont détectés.

.. note:: Une offre froide est mono-instantiable uniquement. Elle ne peut être déployée en haut-disponibilité.

Le paramétrage de l'offre froide se fait via la configuration du fichier ``deployment/environments/group_vars/all/offer_opts.yml``. L'ensemble des clés disponibles est listé dans le fichier ``deployment/environments/group_vars/all/offer_opts.yml.example``

L'offre froide doit être configurée avec le flag ``AsyncRead`` défini à `True` dans la stratégie par défaut de Vitam via ``vitam_strategy`` ou dans une stratégie additionnelle ``other_strategies``.

Exemple:

.. code-block:: yaml

        vitam_strategy:
          - name: offer-tape-1
            referent: false
            asyncRead: true
          - name: offer-fs-2
            referent: true
            asyncRead: false
..

Une offre froide doit être définie dans la rubrique ``vitam_offers`` avec un provider de type *tape-library*

Exemple:

.. code-block:: yaml

        vitam_offers:
          offer-tape-1:
            provider: tape-library
            tapeLibraryConfiguration:
              ...
..

La section ``tapeLibraryConfiguration`` décrit le paramétrage général de l'offre froide.

* **maxTarEntrySize** Taille maximale (en octets) au-delà de la laquelle les fichiers entrants seront découpés en segments. Typiquement 1 Go, maximum 8 Go.
* **maxTarFileSize** Taille maximale (en octets) des `tars` à constituer. Typiquement 10 Go.
* **forceOverrideNonEmptyCartridges** Permet de passer outre le contrôle vérifiant que les bandes nouvellement introduites sont vides. Par défaut à *false*. Ne doit être défini à *true* que sur un environnement de recette où l'écrasement d'une bande de test est sans risque.

* **cachedTarMaxStorageSpaceInMB** Permet de définir la taille maximale du cache disque (en Mo) (Ex. 10 To pour un env de production)
* **cachedTarEvictionStorageSpaceThresholdInMB** Permet de définir la taille critique du cache disque (en Mo). Une fois ce seuil atteint, les archives non utilisées sont purgées (selon la date de dernier accès). Doit être plus petit que la taille maximale **cachedTarMaxStorageSpaceInMB**. (Ex. 8 To pour un env de production)
* **cachedTarSafeStorageSpaceThresholdInMB** Seuil "confortable" d'utilisation du cache (en Mo). Le processus d'éviction des archives du cache s'arrête lorsque ce seuil est atteint. Doit être plus petit que la taille critique **cachedTarEvictionStorageSpaceThresholdInMB**. (Ex. 6 To pour un env de production)

* **maxAccessRequestSize** Définit un seuil technique du nombre d'objets que peut cibler une demande d'accès. Par défaut de 10000. À ne pas modifier.

* **readyAccessRequestExpirationDelay** Valeur du délais d'expiration des demandes d'accès. Une fois une demande d'accès à des objets est prête, l'accès immédiat aux objets est garantie durant cette période.
* **readyAccessRequestExpirationUnit** Unité du délais d'expiration des demandes d'accès (une valeur parmi "SECONDS" / "MINUTES" / "HOURS" / "DAYS" / "MONTHS").

* **readyAccessRequestPurgeDelay** Valeur du délais de purge complète des demandes d'accès.
* **readyAccessRequestPurgeUnit** Unité du délais de purge complète des demandes d'accès (une valeur parmi "SECONDS" / "MINUTES" / "HOURS" / "DAYS" / "MONTHS").

* **accessRequestCleanupTaskIntervalDelay** Valeur de la fréquence de nettoyage des demandes d'accès.
* **accessRequestCleanupTaskIntervalUnit** Unité de la fréquence de nettoyage des demandes d'accès (une valeur parmi "SECONDS" / "MINUTES" / "HOURS" / "DAYS" / "MONTHS").

.. note:: maxTarEntrySize doit être strictement inférieur à maxTarFileSize
.. note:: cachedTarEvictionStorageSpaceThresholdInMB doit être strictement inférieur à cachedTarMaxStorageSpaceInMB
.. note:: cachedTarSafeStorageSpaceThresholdInMB doit être strictement inférieur à cachedTarEvictionStorageSpaceThresholdInMB
.. note:: Se référer à la documentation :term:`DAT` pour les éléments de dimensionnement du cache.
.. note:: La durée de purge des demandes d'accès doit être strictement supérieure à leur durée d'expiration.
.. note:: Le monitoring de l'offre froide est for est **fortement recommandé** afin de s'assurer du bon fonctionnement de l'offre, et du dimensionnement du disque local. Un dashboard dédié à l'offre froide de Vitam est déployé avec les composants "extra" ``prometheus`` et ``grafana``.

Exemple:

.. code-block:: yaml

        inputFileStorageFolder: "/vitam/data/offer/offer/inputFiles"
        inputTarStorageFolder: "/vitam/data/offer/offer/inputTars"
        tmpTarOutputStorageFolder: "/vitam/data/offer/offer/tmpTarOutput"
        cachedTarStorageFolder: "/vitam/data/offer/offer/cachedTars"
        maxTarEntrySize: 10000000
        maxTarFileSize: 10000000000
        ForceOverrideNonEmptyCartridge: false
        cachedTarMaxStorageSpaceInMB: 1_000_000
        cachedTarEvictionStorageSpaceThresholdInMB: 800_000
        cachedTarSafeStorageSpaceThresholdInMB: 700_000
        maxAccessRequestSize: 10_000
        readyAccessRequestExpirationDelay: 30
        readyAccessRequestExpirationUnit: DAYS
        readyAccessRequestPurgeDelay: 60
        readyAccessRequestPurgeUnit: DAYS
        accessRequestCleanupTaskIntervalDelay: 15
        accessRequestCleanupTaskIntervalUnit: MINUTES

        topology:
          ...
        tapeLibraries:
          ...
..

Le paragraphe ``topology`` décrit la topologie de l'offre doit être renseigné. L'objectif de cet élément est de pouvoir définir une segmentation de l'usage des bandes pour répondre à un besoin fonctionnel. Il convient ainsi de définir des *buckets*, qu'on peut voir comme un ensemble logique de bandes, et de les associer à un ou plusieurs tenants.

* **tenants** tableau de 1 à n identifiants de tenants au format [1,...,n]
* **tarBufferingTimeoutInMinutes** Valeur en minutes durant laquelle une archive TAR peut rester ouverte (durée maximale d'accumulation des objets dans un TAR) avant que le TAR soit finalisé / planifié pour écriture sur bande.

Exemple:

.. code-block:: yaml

        topology:
          buckets:
            test:
              tenants: [0]
              tarBufferingTimeoutInMinutes: 60
            admin:
              tenants: [1]
              tarBufferingTimeoutInMinutes: 60
            prod:
              tenants: [2,3,4,5,6,7,8,9]
              tarBufferingTimeoutInMinutes: 60
..

.. note:: Tous les tenants doivent être affectés à un et un seul bucket.
.. caution:: L’affectation d’un tenant à un bucket est définitive. i.e. Il est impossible de modifier le bucket auquel un tenant a été déjà affecté car les données ont déjà été écrites sur bandes. Il est possible cependant, lors de l’ajout d’un tout nouveau tenant à Vitam, d’affecter ce nouveau tenant à un bucket existant.

La section ``tapeLibraries`` permet de définir le paramétrage des bibliothèques de bandes pilotées par l'offre froide.

.. note:: Une offre de stockage Vitam ne peut manipuler qu’une seule bibliothèque de bandes. Afin de piloter plusieurs bibliothèques de bandes, il convient d’utiliser des offres Vitam différentes.

Une bibliothèque de bandes est composée d'un robot (bras articulé), et d'un ensemble de lecteurs.

.. note:: Seul un robot doit être configuré pour piloter une librairie de bandes. La configuration de plusieurs robots pour une même librairie de bandes n'est actuellement PAS supportée.

La commande ``ls -l /dev/tape/by-id/`` permet de lister les chemins des périphériques (lecteurs et bras articulés) à configurer.

Exemple:

.. code-block:: bash

  $ ls -l /dev/tape/by-id/
  total 0
  lrwxrwxrwx 1 root root  9 Mar  7 11:07 scsi-1HP_EML_E-Series_B4B0AC0000 -> ../../sg1
  lrwxrwxrwx 1 root root  9 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00001 -> ../../st0
  lrwxrwxrwx 1 root root 10 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00001-nst -> ../../nst0
  lrwxrwxrwx 1 root root  9 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00002 -> ../../st1
  lrwxrwxrwx 1 root root 10 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00002-nst -> ../../nst1
  lrwxrwxrwx 1 root root  9 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00003 -> ../../st2
  lrwxrwxrwx 1 root root 10 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00003-nst -> ../../nst2
  lrwxrwxrwx 1 root root  9 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00004 -> ../../st3
  lrwxrwxrwx 1 root root 10 Mar  7 11:07 scsi-SHP_DLT_VS80_B4B0A00004-nst -> ../../nst3

.. caution:: Ne pas utiliser les chemins ``/dev/*`` dont l'index peut changer en cas de redémarrage. Utiliser les chemins ``/dev/tape/by-id/*`` (qui utilisent le numéro de série du device cible).

.. caution:: Seuls les devices de lecteurs de type ``/dev/nstX`` (par exemple : ``/dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00001-nst -> /dev/nst0``) peuvent être utilisés dans Vitam. Les devices de lecteurs de type ``/dev/stX`` (par exemple : ``/dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00001 -> /dev/st0``) ne doivent PAS être utilisés (car ils causent à rebobinage automatique de la bande après chaque opération).

* **robots:** Définition du bras robotique de la librairie.

  *   **device:** Chemin du fichier de périphérique scsi générique associé au bras. (ex. ``/dev/tape/by-id/scsi-1HP_EML_E-Series_B4B0AC0000``)
  *   **mtxPath:** Chemin vers la commande Linux de manipulation du bras.
  *   **timeoutInMilliseconds:** timeout en millisecondes à appliquer aux ordres du bras.

* **drives:** Définition du/ou des lecteurs de cartouches de la librairie.

  *   **index:** Numéro de lecteur, valeur débutant à 0.
  *   **device:** Chemin du fichier de périphérique scsi SANS REMBOBINAGE associé au lecteur. (ex. ``/dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00001-nst``)
  *   **mtPath:** Chemin vers la commande Linux de manipulation des lecteurs.
  *   **ddPath:** Chemin vers la commande Linux de copie de bloc de données.
  *   **timeoutInMilliseconds:** timeout en millisecondes à appliquer aux ordres du lecteur.

* **fullCartridgeDetectionThresholdInMB** Seuil de détection de bande pleine (en Mo)
  Permet pour détecter en cas d'erreur d'écriture sur bande, la cause probable de l'erreur :

  - Si le volume des données écrites sur bande > seuil : La bande est considérée comme pleine
  - Si le volume des données écrites sur bande < seuil : La bande est considérée comme corrompue

  Typiquement, 90% de la capacité théorique de stockage des cartouches (hors compression).

Exemple:

.. code-block:: yaml

        tapeLibraries:
          TAPE_LIB_1:
            robots:
              -
                device: /dev/tape/by-id/scsi-1HP_EML_E-Series_B4B0AC0000
                mtxPath: "/usr/sbin/mtx"
                timeoutInMilliseconds: 3600000
            drives:
              -
                index: 0
                device: /dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00001-nst
                mtPath: "/bin/mt"
                ddPath: "/bin/dd"
                timeoutInMilliseconds: 3600000
              -
                index: 1
                device: /dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00002-nst
                mtPath: "/bin/mt"
                ddPath: "/bin/dd"
                timeoutInMilliseconds: 3600000
              -
                index: 2
                device: /dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00003-nst
                mtPath: "/bin/mt"
                ddPath: "/bin/dd"
                timeoutInMilliseconds: 3600000
              -
                index: 3
                device: /dev/tape/by-id/scsi-SHP_DLT_VS80_B4B0A00004-nst
                mtPath: "/bin/mt"
                ddPath: "/bin/dd"
                timeoutInMilliseconds: 3600000

            fullCartridgeDetectionThresholdInMB : 2_000_000
..

Sécurisation SELinux
====================

Depuis la release R13, la solution logicielle :term:`VITAM` prend désormais en charge l'activation de SELinux sur le périmètre du composant worker et des processus associés aux *griffins* (greffons de préservation).

SELinux (Security-Enhanced Linux) permet de définir des politiques de contrôle d'accès à différents éléments du système d'exploitation en répondant essentiellement à la question "May <subject> do <action> to <object>", par exemple "May a web server access files in user's home directories".

Chaque processus est ainsi confiné à un (voire plusieurs) domaine(s), et les fichiers sont étiquetés en conséquence. Un processus ne peut ainsi accéder qu'aux fichiers étiquetés pour le domaine auquel il est confiné.

.. note:: La solution logicielle :term:`VITAM` ne gère actuellement que le mode *targeted* (« only *targeted* processes are protected »)

Les enjeux de la sécurisation SELinux dans le cadre de la solution logicielle :term:`VITAM` sont de garantir que les processus associés aux *griffins* (greffons de préservation) n'auront accès qu'au ressources système strictement requises pour leur fonctionnement et leurs échanges avec les composants *worker*.

.. note:: La solution logicielle :term:`VITAM` ne gère actuellement SELinux que pour le système d'exploitation Centos

.. warning:: SELinux n'a pas vocation à remplacer quelque système de sécurité existant, mais vise plutôt à les compléter. Aussi, la mise en place de politiques de sécurité reste de mise et à la charge de l'exploitant. Par ailleurs, l'implémentation SELinux proposée avec la solution logicielle :term:`VITAM` est minimale et limitée au greffon de préservation Siegfried. Cette implémentation pourra si nécessaire être complétée ou améliorée par le projet d'implémentation.

SELinux propose trois modes différents :

* *Enforcing* : dans ce mode, les accès sont restreints en fonction des règles SELinux en vigueur sur la machine ;
* *Permissive* : ce mode est généralement à considérer comme un mode de débogage. En mode permissif, les règles SELinux seront interrogées, les erreurs d'accès logguées, mais l'accès ne sera pas bloqué.
* *Disabled* : SELinux est désactivé. Rien ne sera restreint, rien ne sera loggué.

La mise en oeuvre de SELinux est prise en charge par le processus de déploiement et s'effectue de la sorte :

* Isoler dans l'inventaire de déploiement les composants worker sur des hosts dédiés (ne contenant aucun autre composant :term:`VITAM`)
* Positionner pour ces hosts un fichier *hostvars* sous ``environments/host_vars/`` contenant la déclaration suivante ::

   selinux_state: "enforcing"

* Procéder à l'installation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d’installation classique décrite dans le DIN


Installation de la stack Prometheus
===================================

.. note:: Si vous disposez d'un serveur Prometheus et alertmanager, vous pouvez installer uniquement ``node_exporter``.

Prometheus server et alertmanager sont des addons dans la solution :term:`VITAM`.

Voici à quoi correspond une configuration qui permettra d'installer toute la stack prometheus.

.. code-block:: yaml

    prometheus:
        metrics_path: /admin/v1/metrics
        check_consul: 10 # in seconds
        prometheus_config_file_target_directory: # Set path where "prometheus.yml" file will be generated. Example: /tmp/
        server:
            port: 9090
        node_exporter:
            enabled: true
            port: 9101
            metrics_path: /metrics
        alertmanager:
            api_port: 9093
            cluster_port: 9094
..

- L'adresse d'écoute de ces composants est celle de la patte d'administration.
- Vous pouvez surcharger la valeur de certaines de ces variables (Par exemple le port d'écoute, le path de l'API).
- Pour générer uniquement le fichier de configuration prometheus.yml à partir du fichier d'inventaire de l'environnement en question, il suffit de spécifier le répertoire destination dans la variable ``prometheus_config_file_target_directory``

Playbooks ansible
-----------------

Veuillez vous référer à la documentation d'exploitation pour plus d'information.

* Installer prometheus et alertmanager

.. code-block:: bash

    ansible-playbook ansible-vitam-extra/prometheus.yml -i environments/hosts.<environnement> --ask-vault-pass
..

* Générer le fichier de conf ``prometheus.yml`` dans le dossier ``prometheus_config_file_target_directory``

.. code-block:: bash

    ansible-playbook ansible-vitam-extra/prometheus.yml -i environments/hosts.<environnement> --ask-vault-pass
--tags gen_prometheus_config
..


Installation de Grafana
=======================

.. note:: Si vous disposez déjà d'un Grafana, vous pouvez l'utiliser pour l'interconnecter au serveur Prometheus.

Grafana est un addon dans la solution :term:`VITAM`.

Grafana sera déployé sur l'ensemble des machines renseignées dans le groupe ``[hosts_grafana]`` de votre fichier d'inventaire.

Pour se faire, il suffit d'exécuter le playbook associée :

.. code-block:: bash

    ansible-playbook ansible-vitam-extra/grafana.yml -i environments/hosts.<environnement> --ask-vault-pass
..

Configuration
-------------

Les paramètres de configuration de ce composant se trouvent dans le fichier ``environments/group_vars/all/advanced/cots_vars.yml``. Vous pouvez adapter la configuration en fonction de vos besoins.

Configuration spécifique derrière un proxy
------------------------------------------

Si Grafana est déployé derrière un proxy, vous devez apporter des modification au fichier de configuration ``ansible-vitam-extra/roles/grafana/templates/grafana.ini.j2``

Voici les variables modifiées par la solution :term:`VITAM` pour permettre le fonctionnement de Grafana derrière un proxy apache.

.. code-block:: text

    [server]
    root_url = http://{{ ip_admin }}:{{ grafana.http_port | default(3000) }}/grafana
    serve_from_sub_path = true

    [auth.basic]
    enabled = false

..

.. warning:: Lors de la première connexion, vous devrez changer le mot de passe par défaut (login: admin; password: aadmin1234), configurer le datasource et créer/importer les dashboards manuellement.


Installation de restic
======================

restic est un addon (beta) de la solution :term:`VITAM`.

restic sera déployé sur l'ensemble des machines du groupe ``[hosts_storage_offer_default]`` qui possèdent le paramètre ``restic_enabled=true``. Attention à ne renseigner qu'une seule fois ce paramètre par ``offer_conf``.

Pour se faire, il suffit d'exécuter le playbook associé :

.. code-block:: bash

    ansible-playbook --vault-password-file vault_pass.txt ansible-vitam-extra/restic.yml -i environments/hosts.<environnement>
..

Configuration
-------------

Les paramères de configuration de ce composant se trouvent dans les fichiers ``environments/group_vars/all/advanced/cots_vars.yml`` et ``environments/group_vars/all/main/vault-cots.yml``. Vous pouvez adapter la configuration en fonction de vos besoins.

Limitations actuelles
---------------------
	
restic est fourni en tant que fonctionnalité beta. À ce titre, il ne peut se substituer à des vérifications régulières de l'état des sauvegardes de vos bases.

restic ne fonctionne pas avec les providers `openstack-swift`, `openstack-swift-v2` et `tape-library`.


restic ne fonctionne pas avec un cluster mongo multi-shardé. Ainsi, mongo-data ne peut être sauvegardé via restic que dans de petites instances de Vitam.