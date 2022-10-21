Notes et procédures spécifiques V5RC
####################################

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

En cas de migrations depuis une version 5.rc.2- (v5.rc.2 ou inférieur) vers une version 5.rc.3+ (5.rc.3 ou supérieure), un contrôle / purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires est nécessaire.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass
