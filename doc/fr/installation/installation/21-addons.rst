

.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``


Paramétrages supplémentaires
============================

.. _update_jvm:

Tuning JVM
-----------

.. note:: Cette section est en cours de développement.

.. caution:: en cas de colocalisation, bien prendre en compte la taille JVM de chaque composant (VITAM : -Xmx512m par défaut) pour éviter de swapper.


Un tuning fin des paramètres JVM de chaque composant VITAM est possible
Pour cela, il faut modifier le fichier ``group_vars/all/jvm_opts.yml``

Pour chaque composant, il est possible de modifier ces 3 variables:

* memory: paramètres Xms et Xmx
* gc: parmètres gc
* java: autres paramètres java


Paramétrage de l'antivirus (ingest-externe)
-------------------------------------------

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
-------------------------------------------------

Se reporter au chapitre dédié à la gestion des certificats: :doc:`20-certificats`


Paramétrage de la centralisation des logs Vitam
-----------------------------------------------

2 cas sont possibles :

* Utiliser le sous-système de gestion des logs fournis par la solution logicielle VITAM ;
* Utiliser un SIEM tiers.

Gestion par Vitam
^^^^^^^^^^^^^^^^^

Pour une gestion des logs par Vitam, il est nécessaire de déclarer les serveurs ad-hoc dans le fichier d'inventaire pour les 3 groupes suivants:
    - hosts-logstash
    - hosts-kibana-log
    - hosts-elasticsearch-log


Redirection des logs sur un SIEM tiers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

En configuration par défaut, les logs Vitam sont tout d'abord routés vers un serveur rsyslog installé sur chaque machine.
Il est possible d'en modifier le routage, qui par défaut redirige vers le serveur logstash via le protocole syslog en TCP.

Pour cela, il est nécessaire de placer un fichier de configuration dédié dans le dossier ``/etc/rsyslog.d/`` ; ce fichier sera automatiquement pris en compte par rsyslog. Pour la syntaxe de ce fichier de configuration rsyslog, se référer à la `documentation rsyslog <http://www.rsyslog.com/doc/v7-stable/>`_.

.. tip:: Pour cela, il peut être utile de s'inspirer du fichier de référence VITAM ``deployment/ansible-vitam/roles/rsyslog/templates/vitam_transport.conf.j2`` (attention, il s'agit d'un fichier template ansible, non directement convertible en fichier de configuration sans en ôter les directives jinja2).


Fichiers complémentaires
------------------------

A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées dans les fichiers suivants :

* |repertoire_inventory| ``/group_vars/all/vitam_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/vitam_vars.yml
     :language: yaml
     :linenos:

** |repertoire_inventory| ``/group_vars/all/cots_vars.yml``, comme suit :

  .. literalinclude:: ../../../../deployment/environments/group_vars/all/cots_vars.yml
     :language: yaml
     :linenos:
