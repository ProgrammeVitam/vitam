
Gestion des logs Vitam
######################

Gestion par Vitam
=================

Pour une gestion des logs par Vitam, il est nécessaire de déclarer des serveurs dans le fichier d'inventaire pour les 3 groupes suivants:
    - hosts-logstash
    - hosts-kibana
    - hosts-elasticsearch-log


Redirection des logs sur un SIEM tiers
======================================

En configuration par défaut, les logs Vitam sont tout d'abord routés vers un serveur rsyslog installé sur chaque machine.
Il est possible d'en modifier le routage, qui par défaut redirige vers le serveur logstash via le protocole syslog en TCP.

Pour cela, il est nécessaire de placer un fichier de configuration dédié dans le dossier ``/etc/rsyslog.d/`` ; ce fichier sera automatiquement pris en compte par rsyslog. Pour la syntaxe de ce fichier de configuration rsyslog, se référer à la `documentation rsyslog<http://www.rsyslog.com/doc/v7-stable/>`_.

.. tip:: Pour cela, il peut être utile de s'inspirer du fichier de référence VITAM ``deployment/ansible-vitam/roles/rsyslog/templates/vitam_transport.conf.j2`` (attention, il s'agit d'un fichier template ansible, non directement convertible en fichier de configuration sans en ôter les directives jinja2).

