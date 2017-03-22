Déploiement de VITAM
====================

Déploiement docker
------------------
Le fichier d'inventaire est différent selon l'environnement :

* hosts.local : pour le déploiement sur un poste de développement
* hosts.int : pour le déploiement sur l'environnement pic d'intégration
* hosts.rec : pour le déploiement sur l'environnement pic de recette


Pour tester le déploiement de VITAM : ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --check``

Pour le déployer : ``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire>``

Pour une remise à zéro (encore en cours de développement) : ``ansible-playbook ansible-vitam/vitam-raz.yml  -i environments/<fichier d'inventaire>``

Pour tester en local, ne pas oublier qu'il y a un mappage utilisateur de l'hôte (la machine qui lance les docker) et le containeur docker (utilisateur vitam avec uid : 2000). Vérfier ce point en cas de souci de droits d'écriture !

Pour ouvrir et éditer le fichier de secrets : ``ansible-vault edit <filename>``


Déploiement rpm
----------------

Pour tester le déploiement de VITAM :
``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --check``

.. note:: ce mode n'est pas recommandé

Pour le déployer :

1. générer les certificats nécessaire en lançant le script :
``pki/scripts/generate_ca.sh``, si pas de CA fournie
``pki/scripts/generate_certs.sh <fichier environnement>``, si pas de certificats fournis
``./generate_stores.sh``


2. Si gestion par VITAM des dépots de binaires:
Editer le fichier ``environments/group_vars/all/example_repo.yml`` (sert de modèle)
Puis lancer :
``ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/<fichier d'inventaire>  --ask-vault-pass``


2. Lancer le playbook d'ansible :
``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire>  --ask-vault-pass``
(et renseigner le mot de passe demandé)
ou
``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


3. Pour déployer les extra seulement nécessaire pour le projet :

a. extra complet
``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire>  --ask-vault-pass``
(et renseigner le mot de passe demandé)
ou
``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


b. ihm-recette seulement

``ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<fichier d'inventaire>  --ask-vault-pass``
(et renseigner le mot de passe demandé)
ou
``ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``


4. Pour redéployer les rpm VITAM sans modification de configuration

``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_package_vitam``
et
``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_package_vitam``


5. Pour redéployer les keystores / truststores / grantedstores uniquement

``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_vitam_certificates``
et
``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_vitam_certificates``


6. Pour modifier uniquement la configuration JVM des composants VITAM

Modifier dans environments/<fichier d'inventaire> la directive memory_opts
Exemple:
memory_opts="-Xms384m -Xmx384m"
``ansible-playbook ansible-vitam/vitam.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_jvmoptions_vitam``
et
``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt --tags update_jvmoptions_vitam``

7. Automatisation du chargement de PRONOM

``ansible-playbook ansible-vitam-extra/init_pronom.yml -i environments/<fichier d'inventaire> --vault-password-file vault_pass.txt``

.. caution:: le playbook ne se termine pas correctement (code HTTP 403) si un référentiel PRONOM a déjà été chargé.
