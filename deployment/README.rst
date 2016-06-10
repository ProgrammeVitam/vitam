Déploiement de VITAM
====================

Pour tester le déploiement de VITAM : ``ansible-playbook ansible-vitam/vitam.yml -i environments/hosts.int --check``

Pour le déployer : ``ansible-playbook ansible-vitam/vitam.yml -i environments/hosts.int``

Pour une remise à zéro (encore en cours de développement) : ``ansible-playbook ansible-vitam/vitam-raz.yml  -i environments/hosts.int``
