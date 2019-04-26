
.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``


Paramétrages supplémentaires
#############################

.. _update_jvm:

Tuning JVM
==========

.. caution:: en cas de colocalisation, bien prendre en compte la taille :term:`JVM` de chaque composant (VITAM : ``-Xmx512m`` par défaut) pour éviter de `swapper`.


Un `tuning` fin des paramètres JVM de chaque composant :term:`VITAM` est possible.
Pour cela, il faut modifier le contenu du fichier ``environments/group_vars/all/jvm_opts.yml``

Pour chaque composant, il est possible de modifier ces 3 variables:

* memory: paramètres Xms et Xmx
* gc: parmètres gc
* java: autres paramètres java

Installation des *griffins*
============================

.. note:: Fonctionnalité apparue en R9.

.. caution:: Cette version de :term:`VITAM` ne mettant pas encore en oeuvre de mesure d'isolation particulière des *griffins*, il est recommandé de veiller à ce que l'usage de chaque *griffin* soit en conformité avec la politique de sécurité de l'entité. Il est en particulier déconseillé d'utiliser un griffon qui utiliserait un outil externe qui n'est plus maintenu.

Il est possible de choisir les *griffins* installables sur la plate-forme. Pour cela, il faut éditer le contenu du fichier ``environments/group_vars/all/vitam-vars.yml`` au niveau de la directive ``vitam_griffins``. Cette action est à rapprocher de l'incorporation des binaires d'installation : les binaires d'installation des greffons doivent être accessibles par les machines hébergeant le composant **worker**.

Exemple::

   vitam_griffins: ["vitam-imagemagick-griffin", "vitam-jhove-griffin"]

Voici la liste des greffons disponibles au moment de la présente publication :


.. literalinclude:: liste_griffins.txt
   :language: text

.. warning:: Ne pas oublier d'avoir déclaré au préalable sur les machines cibles le dépôt de binaires associé aux *griffins*.

Paramétrage de l'antivirus (ingest-externe)
===========================================

L'antivirus utilisé par ingest-externe est modifiable (par défaut, ClamAV) ; pour cela :

* Modifier le fichier ``environments/group_vars/all/vitam_vars.yml`` pour indiquer le nom de l'antivirus qui sera utilisé (norme : scan-<nom indiqué dans vitam-vars.yml>.sh)
* Créer un shell (dont l'extension doit être ``.sh``) sous ``environments/antivirus/`` (norme : scan-<nom indiqué dans vitam-vars.yml>.sh) ; prendre comme modèle le fichier ``scan-clamav.sh``. Ce script shell doit respecter le contrat suivant :

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

.. warning:: Sur plate-forme Debian, ClamAV est installé sans base de données. Pour que l'antivirus soit fonctionnel, il est nécessaire, durant l'installation, de la télécharger ; il est donc nécessaire de renseigner dans l'inventaire la directive ``http_proxy_environnement``.

Paramétrage des certificats externes (\*-externe)
=================================================

Se reporter au chapitre dédié à la gestion des certificats: :doc:`20-certificats`

Placer "hors Vitam" le composant ihm-demo
=========================================

Sous ``deployment/environments/host_vars``, créer ou éditer un fichier nommé par le nom de machine hébergeant le composant ihm-demo et ajouter le contenu ci-dessous ::

   consul_disabled: true

A l'issue, le déploiement n'installera pas l'agent Consul. Le composant ihm-demo appellera, alors, par l'adresse IP de services les composants "access-external" et "ingest-external".

Il est également fortement recommandé de positionner la valeur de la directive ``vitam.ihm_demo.metrics_enabled`` à  ``false`` dans le fichier ``deployment/environments/group_vars/all/vitam_vars.yml``, afin que ce composant ne tente pas d'envoyer de données sur "elasticsearch-log".

Paramétrer le ``secure_cookie`` pour ihm-demo
=============================================

Le composant ihm-demo (ainsi qu'ihm-recette) dispose d'une option suplémentaire par rapport aux autres composants vitam: le ``secure_cookie`` qui permet de renforcer ces deux ihm contre certaines attaques assez répandues comme les CSRF.

Il faut savoir que si cette variable est à true (valeur par défaut), le client doit obligatoirement sec connecter en https sur l'ihm, et ce même si un reverse proxy se trouve entre le serveur web et le client.

Cela peut donc obliger le reverse proxy frontal de la chaine d'accès à écouter en https.


Paramétrage de la centralisation des logs Vitam
================================================

2 cas sont possibles :

* Utiliser le sous-système de gestion des logs fournis par la solution logicielle VITAM ;
* Utiliser un SIEM tiers.

Gestion par Vitam
-------------------

Pour une gestion des logs par Vitam, il est nécessaire de déclarer les serveurs ad-hoc dans le fichier d'inventaire pour les 3 groupes suivants:
    - hosts-logstash
    - hosts-kibana-log
    - hosts-elasticsearch-log


Redirection des logs sur un SIEM tiers
--------------------------------------

En configuration par défaut, les logs Vitam sont tout d'abord routés vers un serveur rsyslog installé sur chaque machine.
Il est possible d'en modifier le routage, qui par défaut redirige vers le serveur logstash via le protocole syslog en TCP.

Pour cela, il est nécessaire de placer un fichier de configuration dédié dans le dossier ``/etc/rsyslog.d/`` ; ce fichier sera automatiquement pris en compte par rsyslog. Pour la syntaxe de ce fichier de configuration rsyslog, se référer à la `documentation rsyslog <http://www.rsyslog.com/doc/v7-stable/>`_.

.. tip:: Pour cela, il peut être utile de s'inspirer du fichier de référence VITAM ``deployment/ansible-vitam/roles/rsyslog/templates/vitam_transport.conf.j2`` (attention, il s'agit d'un fichier template ansible, non directement convertible en fichier de configuration sans en ôter les directives jinja2).

Passage des identifiants des référentiels en mode esclave
==============================================================

La génération des identifiants des référentiels est géré par Vitam quand il fonctionne en mode maître.

Par exemple :

- Préfixé par PR- pour les profils
- Préfixé par IC- pour les contrats d'entrée
- Préfixé par AC- pour les contrats d'accès

Si vous souhaitez gérer vous-même les identifiants sur un service référentiel, il faut qu'il soit en mode esclave.
Par défaut tous les services référentiels de Vitam fonctionnent en mode maître. Pour désactiver le mode maître de :term:`VITAM`, il faut modifier le fichier ansible ``deployment/ansible-vitam/roles/vitam/templates/functional-administration/functional-administration.conf.j2``.

Exemple du fichier par défaut :

.. literalinclude:: ../../../../deployment/ansible-vitam/roles/vitam/templates/functional-administration/functional-administration.conf.j2
     :language: yaml
     :linenos:


Un exemple de ce fichier se trouve dans la Documentation d’exploitation au chapitre "Exploitation des composants de la solution logicielle VITAM".

.. code-block:: text

 # ExternalId configuration

 listEnableExternalIdentifiers:
  0:
    - INGEST_CONTRACT
    - ACCESS_CONTRACT
  1:
    - INGEST_CONTRACT
    - ACCESS_CONTRACT
    - PROFILE
    - SECURITY_PROFILE
    - CONTEXT

Depuis la version 1.0.4, la configuration par défaut de Vitam autorise des identifiants externes (ceux qui sont dans le fichier json importé).

 - pour le tenant 0 pour les référentiels : contrat d'entrée et contrat d'accès.
 - pour le tenant 1 pour les référentiels : contrat d'entrée, contrat d'accès, profil, profil de sécurité et contexte.

La liste des choix possibles, pour chaque tenant, est :

  - INGEST_CONTRACT : contrats d'entrée
  - ACCESS_CONTRACT : contrats d'accès
  - PROFILE : profils SEDA
  - SECURITY_PROFILE : profils de sécurité (utile seulement sur le tenant d'administration)
  - CONTEXT : contextes applicatifs (utile seulement sur le tenant d'administration)
  - ARCHIVEUNITPROFILE : profils d'unités archivistiques

Durées minimales permettant de contrôler les valeurs saisies
==============================================================

Afin de se prémunir contre une alimentation du référentiel des règles de gestion avec des durées trop courtes susceptibles de déclencher des actions indésirables sur la plate-forme (ex. éliminations) – que cette tentative soit intentionnelle ou non –, la solution logicielle :term:`VITAM` vérifie que l’association de la durée et de l’unité de mesure saisies pour chaque champ est supérieure ou égale à une durée minimale définie lors du paramétrage de la plate-forme, dans un fichier de configuration.

Pour mettre en place le comportement attendu par le métier, il faut modifier le contenu de la directive ``listMinimumRuleDuration`` dans le fichier ansible ``deployment/ansible-vitam/roles/vitam/templates/functional-administration/functional-administration.conf.j2``.

Exemple::

  listMinimumRuleDuration:
    2:
      AppraisalRule : 1 year
      DisseminationRule : 10 year

    3:
      AppraisaleRule : 5 year
      StorageRule : 5 year
      ReuseRule : 2 year


Par tenant, les directives possibles sont :

- AppraisalRule
- DisseminationRule
- StorageRule
- ReuseRule
- AccessRule (valeur par défaut : 0 year)
- ClassificationRule

Les valeurs associées sont une durée au format <nombre> <unité en angais, au singulier>

Exemples::

   6 month
   1 year
   5 year

Pour plus de détails, se rapporter à la documentation métier "Règles de gestion".



Fichiers complémentaires
==========================

A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées dans les fichiers suivants :

* |repertoire_inventory| ``/group_vars/all/vitam_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vitam_vars.yml
     :language: yaml
     :linenos:

.. note:: Cas du composant ingest-external. Les directives ``upload_dir``, ``success_dir``, ``fail_dir`` et ``upload_final_action`` permettent de prendre en charge (ingest) des fichiers déposés dans ``upload_dir`` et appliquer une règle ``upload_final_action`` à l'issue du traitement (NONE, DELETE ou MOVE dans ``success_dir`` ou ``fail_dir`` selon le cas). Se référer au :term:`DEX` pour de plus amples détails. Se référer au manuel de développement pour plus de détails sur l'appel à ce cas.

.. warning:: Selon les informations apportées par le métier, redéfinir les valeurs associées dans les directives ``classificationList`` et ``classificationLevelOptional``. Cela permet de définir quels niveaux de protection du secret de la défense nationale supporte l'instance. Attention : une instance de niveau supérieur doit toujours supporter les niveaux inférieurs

* |repertoire_inventory| ``/group_vars/all/cots_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/cots_vars.yml
     :language: yaml
     :linenos:

.. note:: installation multi-sites. Déclarer dans ``consul_remote_sites`` les datacenters Consul des autres site ; se référer à l'exemple fourni pour renseigner les informations.

.. note:: Concernant Curator, en environnement de production, il est recommandé de procéder à la fermeture des index au bout d'une semaine pour les index de type "logstash" ( 3 jours pour les index "metrics"), qui sont le reflet des traces applicatives des composants de la solution logicielle :term:`VITAM`. Il est alors recommandé de lancer le *delete* de ces index au bout de la durée minimale de rétention : 1 an (il n'y a pas de durée de rétention minimale légale sur les index "metrics", qui ont plus une vocation technique et, éventuellement, d'investigations).

* |repertoire_inventory| ``/group_vars/all/jvm_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/jvm_opts.yml
     :language: yaml
     :linenos:

.. note:: Cette configuration est appliquée à la solution logicielle :term:`VITAM`  ; il est possible de créer un tuning par "groupe" défini dans ansible.
