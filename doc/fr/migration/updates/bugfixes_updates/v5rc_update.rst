Notes et procédures spécifiques V5RC
####################################

Procédures à exécuter AVANT la montée de version
================================================

Réinitialisation de la reconstruction des registres de fond des sites secondaires
---------------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 5.rc.3- (v5.rc.3 ou inférieur) vers une version 5.rc.4+ (5.rc.4 ou supérieure). Elle permet la réinitialisation de la reconstruction des registre de fonds sur les sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que les timers de Vitam aient bien été préalablement arrêtés (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_accession_register_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 5.rc.3- (v5.rc.3 ou inférieur) vers une version 5.rc.4+ (5.rc.4 ou supérieure). Elle permet le contrôle et la purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

  ..
