Notes et procédures spécifiques V9.0
####################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: V7.1 -> V8.0 -> V8.1 -> V9.0

Adaptation des sources de déploiement ansible
=============================================

Migration de la configuration d'offres S3
-----------------------------------------

Dans le cas de l'utilisation d'une offre de stockage S3, le provider ``amazon-s3-v1`` n'est plus disponible. Il est remplacé par le provider ``amazon-s3-v2``.

La clé de configuration ``vitam_offers.<offre>.s3SignerType`` n'est plus paramétrable à présent et doit être supprimée. Seul le mode de signature ``AWSS3V4SignerType`` (par défaut dans les versions précédentes de Vitam) est supportée à présent. Le mode de signature ``S3SignerType`` (dangereux / déprécié par Amazon AWS) n'est plus supporté.

De même, la validation des hostnames des certificats HTTPS est dorénavant activée par défault dans le nouveau provider ``amazon-s3-v2``. La validation peut être désactivée via la clé ``vitam_offers.<offre>.s3IgnoreCertificateHostnameValidation`` (uniquement pour environnements de test).

Il convient de mettre à jour la configuration des offres de stockage en conséquent (``vitam_offers.<offre>.provider``) dans les sources de déploiement.

Déploiement du nouveau composant vitam-antivirus
------------------------------------------------

L'appel a l'antivirus été externalisé dans un nouveau composant permettant son déploiement autonome. Actuellement, ce nouveau composant est colocalisé avec les groupes ``hosts_ingest_external`` & ``hosts_worker``. Cependant, il n'est utile sur les workers que lors de l'utilisation du module de collecte.

Ainsi, vous pouvez désactiver son déploiement sur les workers si vous n'utilisez pas le module de collecte en appliquant la procédure suivante.

Dans le playbook ``ansible-vitam/services/vitam/antivirus.yml``, supprimez la chaine de caractère ``:hosts_worker``.

Résultat attendu après modification:

.. code-block:: yaml

  - hosts: hosts_ingest_external
    any_errors_fatal: true
    roles:
      - antivirus
      - vitam
    vars:
      vitam_struct: "{{ vitam.antivirus }}"
    tags: antivirus

..

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.0

.. caution:: Cette opération doit être effectuée avec les sources de déploiement de l'ancienne version.

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

Pour le dépôt vitam-external, vous devez renseigner la version adaptée à votre système d'exploitation (par exemple pour la version 9.0.0):

* AlmaLinux 9: https://download.programmevitam.fr/vitam_repository/9.0.0/rpm/vitam-external/9/
* Debian 12: https://download.programmevitam.fr/vitam_repository/9.0.0/deb/vitam-external/12/

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..

Mise à jour de MongoDB 8.0.23
-----------------------------

.. caution:: **Attention**
    Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V9.1.
    Cette opération est à effectuer si vous venez des versions de Vitam suivante: V8.1.2-.
    Il est recommandé d'effectuer un backup des bases de données à l'aide de mongodump avant de poursuivre.

Exécutez le playbook suivant à partir de l'ansiblerie de la V9.0 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_80.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.0

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

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

N/A
