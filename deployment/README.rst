Déploiement de VITAM
====================

Déploiement complet rpm/deb
---------------------------

Procédure de déploiement de Vitam:

Tips: pour tous les cas d'appel de ansible-playbook, il est possible de remplacer l'argument "--vault-password-file vault_pass.txt" par "--ask-vault-pass" si vous ne voulez pas stocker le mot de passe de chiffrement des vault ansible dans un fichier.


1.  Créer un fichier d'inventaire des repositories des composants VITAM en se basant sur le fichier d'exemple:
    ``environments/group_vars/all/example_bootstrap_repo_centos.yml`` pour un environnement CentOS
    ``environments/group_vars/all/example_bootstrap_repo_debian.yml`` pour un environnement Debian


2.  Générer les autorités de certification des certificats (si vous n'avez pas vos propres autorités de certification et que vous ne souhaitez pas vous en occuper)
    ``pki/scripts/generate_ca.sh``


3.  Certificats
    A.  Si vous ne les avez pas déjà généré vous même, lancer la génération avec le script generate_certs.sh
        a.  Configurer le vault référençant les mots de passe de chiffrement des clés privées des certificats
            ``ansible-vault edit environments/certs/vault-certs.yml --vault-password-file vault_pass.txt``
        b.  Générer les certificats
            ``pki/scripts/generate_certs.sh environments/<fichier d'inventaire>``
    B.  Si vous avez vos propres certificats & CA, les déposer dans la bonne arborescence (se servir des certificats par défaut fournis comme exemple pour les nommages):
        - environments
            - cert
                - client-external
                    - ca: CA(s) des certificats clients external
                    - clients
                        - external: Certificats des SIAs
                        - ihm-demo: Certificat de ihm-demo
                        - ihm-recette: Certificat de ihm-recette
                        - reverse: Certificat du reverse
                - client-storage
                    - ca: CA(s) des certificats clients storage
                    - clients
                        - storage-engine: Certificat de storage-engine
                - server
                    - ca: CA(s) des certificats côté serveurs
                    - hosts
                        - [nom_serveur]: certificats des composants installés sur le serveur donné, [nom_serveur] doit être identique à ce qui est référencé dans le fichier d'inventaire
                - timestamping
                    - ca: CA (intermediate & root) des certificats de timestamping
                    - vitam: Certificats de timestamping
                        - logbook.key
                        - logbook.crt
                        - worker.key
                        - worker.crt


4.  Générer les keystores
    a.  Configurer le vault référençant les mots de passe des keystores
        ``ansible-vault edit environments/group_vars/all/vault-keystores.yml --vault-password-file vault_pass.txt``
    a.  Générer les keystores
        ``./generate_stores.sh``


5.  Facultatif: ajouter les repositories des packages rpm/deb sur les machines cibles
    a.  Configurer les repositories en se basant sur les fichiers d'exemple dans environment/group_vars/all/example_boostrap_repo_*.yml
    b.  Lancer le boostrap pour ajouter les repositories sur les machines cibles
        ``ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


6. Générer les host_vars "réseau"
    ``ansible-playbook ansible-vitam/generate_hostvars_for_1_network_interface.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``
   A l'issue, vérifier les fichiers sous ``environments/host_vars`` et les adapter au besoin.


7.  Déployer Vitam
    ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


8.  Déployer les extras de Vitam
    a.  Extras complets
        ``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``
    b.  Ihm-recette seulement
        ``ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


Mettre à jour uniquement les packages rpm/deb des composants Vitam
------------------------------------------------------------------

Pour les composants Vitam standards
    ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_package_vitam``
Pour les composants de recette / documentation
    ``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_package_vitam``


Redéployer uniquement les keystores / truststores / grantedstores
-----------------------------------------------------------------

Pour les composants Vitam standards
    ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_vitam_certificates``
Pour les composants de recette / documentation
    ``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_vitam_certificates``


Modifier uniquement la configuration JVM des composants VITAM
-------------------------------------------------------------

Modifier dans environments/<fichier d'inventaire> la directive memory_opts
Exemple: memory_opts="-Xms384m -Xmx384m"
Puis lancer le playbook ansible
Pour les composants Vitam standards
    ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_jvmoptions_vitam``
Pour les composants de recette / documentation
    ``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_jvmoptions_vitam``


Modifier uniquement la configuration des clusters mongodb
---------------------------------------------------------

Modifier les options associées puis lancer le playbook de cette manière:
    ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_mongodb_configuration``


Automatisation du chargement de PRONOM
--------------------------------------

Lancer le playbook suivant pour charger le référentiel pronom par ligne de commande plutôt que par l'IHM
    ``ansible-playbook ansible-vitam-extra/init_pronom.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``
.. caution:: le playbook ne se termine pas correctement (code HTTP 403) si un référentiel PRONOM a déjà été chargé.


Tests TNR automatisés
---------------------

Lancer les tests de non régression
    ``ansible-playbook -i environments/hosts.local ansible-vitam-extra/load_tnr.yml --vault-password-file vault_pass.txt``
