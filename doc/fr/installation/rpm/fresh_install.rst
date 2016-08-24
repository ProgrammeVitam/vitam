Première installation
#####################


.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments-rpm``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam-rpm``


Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le sous-répertoire |repertoire_deploiement| . Ils consistent en 2 parties :
 
 * le playbook ansible, présent dans le répertoire |repertoire_inventory|, qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le répertoire |repertoire_inventory|

Pour configurer le déploiement, il est nécessaire de créer (dans n'importe quel répertoire en dehors du répertoire |repertoire_inventory| un nouveau fichier d'inventaire comportant les informations suivantes :

.. literalinclude:: ../../../../deployment/environments-rpm/hosts.int2
   :language: ini
   :linenos:

Pour chaque type de "host" (lignes 19 à 59), indiquer le serveur défini pour chaque fonction.

Ensuite, dans la section ``hosts:vars`` (lignes 62 à 71), renseigner les valeurs comme décrit :

.. csv-table:: Définition des variables
   :header: "Clé", "Description","Valeur"
   :widths: 10, 10,10

   "ansible_ssh_user","Utilisateurs ansible sur les machines sur lesquelles VITAM sera déployé",""
   "ansible_become","Propriété interne à ansible pour passer root",""
   "vitam_folder_permission","Droits Unix par défaut des arborescences créées pour VITAM",""
   "vitam_conf_permission","Droits sur les fichiers de configuration déployés pour VITAM",""
   "pull_strategy","Stratégie lorsdu docker pull",""
   "local_user","Utilisateur créé sur les hôtes des docker pour le mapping correct entre docker et hôte",""
   "vitam_docker_tag","Tag des conteeurs au téléchargement ; assimilable à la version",""
   "vitam_ihm_demo_external_dns","A revoir..",""
   "https_reverse_proxy","<nom ou IP>:<port>",""
	"proxy_host","Hôte proxy",""
	"proxy_port","Port du proxy",""


A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées sous |repertoire_inventory| ``/group_vars/all``, comme suit :

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all
   :language: ini
   :linenos:


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

1. Test du déploiement

Pour tester le déploiement de VITAM, il faut se placer dans le répertoire |repertoire_deploiement| et entrer la commande suivante :

ansible-playbook |repertoire_playbook ansible| /vitam.yml -i |repertoire_inventory| /<ficher d'inventaire> --check


2. Déploiement

Si la commande de test se termine avec succès, le déploiement est à réaliser avec la commande suivante :

ansible-playbook |repertoire_playbook ansible|/vitam.yml -i |repertoire_inventory|/<ficher d'inventaire> --become

.. note:: le become est un "au cas où".

