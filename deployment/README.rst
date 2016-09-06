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


Déploiement rpm
----------------

Pour tester le déploiement de VITAM : 
``ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<fichier d'inventaire> --check``

Pour le déployer : 
``ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<fichier d'inventaire>``

Pour déployer les extra seulement nécessaire pour le projet :
``ansible-playbook ansible-vitam-rpm-extra/extra.yml -i environments-rpm/<fichier d'inventaire>``
