Notes et procédures spécifiques V6RC
####################################

Procédures à exécuter AVANT la montée de version
================================================

Duplication des packages lors de la mise à jour
-----------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 6.rc.1- (v6.rc.1 ou inférieur) vers une version 6.rc.2+ (v6.rc.2 ou supérieure).

.. caution:: Cette opération doit être effectuée uniquement pour les systèmes d'exploitation à base de RPM (CentOS 7 & AlmaLinux 9) sur un vitam éteint.

Un bug a été introduit dans la construction des packages RPM à partir de la V6RC. Si vous effectuez une montée de version depuis la V6RC ou de la V6, vous devrez appliquer la commande suivante pour supprimer les précédents packages. Sinon lors de de la mise à jour, le package précédent ne sera pas désinstallé conduisant à la présence de plusieurs versions du même package.

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

Procédures à exécuter APRÈS la montée de version
================================================

Migration des mappings elasticsearch
------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 6.rc.3- (v6.rc.3 ou inférieur) vers une version 6.rc.4+ (6.rc.4 ou supérieure).

Cette migration de données consiste à mettre à jour le modèle d'indexation elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

- Les jobs Vitam et les services externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..


- Réindexation des référentiels sur elasticsearch :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass --tags "securityprofile, context, ontology, ingestcontract, agencies, accessionregisterdetail, archiveunitprofile, accessionregistersummary, accesscontract, fileformat, filerules, profile, griffin, preservationscenario, managementcontract"

..

- Lancement de la migration du modèles d'indexation des métadonnées sur elasticsearch-data :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..

- Réactivation des services externals ainsi que les timers sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduler.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduling.yml --ask-vault-pass

..
