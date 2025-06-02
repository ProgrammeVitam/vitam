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
