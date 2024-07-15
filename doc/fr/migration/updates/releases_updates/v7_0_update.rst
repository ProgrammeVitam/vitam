Notes et procédures spécifiques V7
##################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: R16 -> V5RC -> V5 -> V6RC -> V6.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Duplication des packages lors de la mise à jour
-----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7.0

.. caution:: Cette opération doit être effectuée uniquement pour les systèmes d'exploitation à base de RPM (CentOS 7 & AlmaLinux 9) sur un vitam éteint.

Un bug a été introduit dans la construction des packages RPM à partir de la V6RC. Si vous effectuez une montée de version depuis la V6RC (v6.rc.1 ou inférieure) ou de la V6 (v6.2 ou inférieure), vous devrez appliquer la commande suivante pour supprimer les précédents packages. Sinon lors de de la mise à jour, le package précédent ne sera pas désinstallé conduisant à la présence de plusieurs versions du même package.

Exemple d'erreur:

.. code-block:: bash

    /var/tmp/rpm-tmp.fGwZMk: ligne 1 : fg: pas de contrôle de tâche
    erreur : %preun(vitam-offer-6.2-1.noarch) scriptlet échoué, état de sortie 1
    Error in PREUN scriptlet in rpm package vitam-offer
    erreur : vitam-offer-6.2-1.noarch: effacer échoué

..

Voici la commande à exécuter pour permettre la désinstallation des packages de la version précédente:

.. code-block:: bash

    ansible vitam --ask-vault-pass -v -a "yum --setopt=tsflags=noscripts remove -y vitam-*.noarch" -i environments/<inventaire>

..

Application de la montée de version
===================================

.. caution:: L'application de la montée de version s'effectue d'abord sur les sites secondaires puis sur le site primaire.

Lancement du master playbook vitam
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam/vitam.yml --ask-vault-pass

..

Lancement du master playbook extra
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/extra.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

Arrêt des jobs Vitam et des accès externes à Vitam
--------------------------------------------------

.. caution:: Cette opération doit être effectuée IMMÉDIATEMENT APRÈS la montée de version vers la V7

Les jobs Vitam et les services externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..

Réindexation des référentiels sur elasticsearch
-----------------------------------------------

Cette migration de données consiste à mettre à jour le modèle d'indexation des référentiels sur elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass --tags "securityprofile, context, ontology, ingestcontract, agencies, accessionregisterdetail, archiveunitprofile, accessionregistersummary, accesscontract, fileformat, filerules, profile, griffin, preservationscenario, managementcontract"

..

Migration des mappings elasticsearch pour les métadonnées
---------------------------------------------------------

Cette migration de données consiste à mettre à jour le modèle d'indexation des métadonnées sur elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..

Redémarrage des Jobs Vitam et des accès externes à Vitam
--------------------------------------------------------

La montée de version est maintenant terminée, vous pouvez réactiver les services externals ainsi que les jobs Vitam sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduler.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduling.yml --ask-vault-pass

..
