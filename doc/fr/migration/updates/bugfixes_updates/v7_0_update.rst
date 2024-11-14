Notes et procédures spécifiques V7.0
####################################

Procédures à exécuter AVANT la montée de version
================================================

Duplication des packages lors de la mise à jour
-----------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 7.0.1- (v7.0.1 ou inférieur) vers une version 7.0.2+ (v7.0.2 ou supérieure).

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

Procédures à exécuter APRÈS la montée de version
================================================

Migration unités archivistiques avec demande de transfert non complétée
-----------------------------------------------------------------------

Cette migration permet de corriger des erreurs de sécurisation ou d'audit sur des unités archivistiques pour lesquelles une demande de transfert a été initiée, mais dont le transfert effectif n'a pas été finalisée.

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration mineure depuis une version 7.0.1- (v7.0.1 ou inférieure) vers une version 7.0.2+ (v7.0.2 ou supérieure).

.. caution:: Cette migration est à effectuer **APRES** l'installation et uniquement sur le **site primaire**.

.. caution:: Dans le cas où un tenant comporte un très grand nombre d'unités archivistiques dont la demande de transfert a été initiée pour un tenant donné (plus de 100 000 unités archivistiques), la migration ne pourra alors s'opérer. Le support Vitam devra alors être contacté.

Pour lancer la migration, le playbook suivant doit être exécuté :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_metadata_transfer_request_traceability.yml --ask-vault-pass
