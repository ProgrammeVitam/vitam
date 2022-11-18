SCHEDULER
#########


Création d'un nouveau job
*******************************************
Pour créer un nouveau job, il faudrait tout d'abord créer une classe qui implémente l'interface Job dans le package fr.gouv.vitam.scheduler.server.job. Par la suite, implémenter la méthode "execute", dans laquelle on trouve la logique métier du job
Création et configuration de trigger
**************************
Pour configurer le job, il faut créer un nouveau fichier XML dans vitam/vitam-conf-dev/conf/scheduler/jobs, le fichier sera pris automatiquement par une config java .

Au sein de ce fichier de configuration, et suite à la création du job, il faudrait créer la configuration du trigger et le relier au job concerné.

Exemple:

Fichier ``jobs-logbook.xml``

.. literalinclude:: ../../../../../../deployment/ansible-vitam/roles/vitam/templates/scheduler/jobs-logbook.xml.j2
   :language: xml

