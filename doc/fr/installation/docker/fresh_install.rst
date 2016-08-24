Première installation
#####################

Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le répertoire ``deployment`` Ils consistent en 2 parties :
 
 * le playbook ansible, présent dans le répertoire « ansible-vitam », qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le répertoire ``environments``

Pour configurer le déploiement, il est nécessaire de créer (dans n'importe quel répertoire en dehors du répertoire ``environments`` un nouveau fichier d'inventaire comportant les informations suivantes :


.. literalinclude:: ../../../../deployment/environments/hosts.int
   :language: ini
   :linenos:

Pour chaque type de "host" (lignes 16 à 54), indiquer le serveur défini pour chaque fonction.

Ensuite, dans la section ``hosts:vars``, renseigner comme suit :

.. csv-table:: Définition des variables
   :header: "Clé", "Description","Valeur"
   :widths: 10, 10,10

   "ansible_ssh_user","Utilisateurs ansible sur les machines sur lesquelles VITAM sera déployé",""
   "ansible_become","Propriété interne à ansible pour passer root",""
   "vitam_environment", "Environnement (int, pprd,prod, ...) ; suffixe les noms des docker",""
   "docker_registry_hostname","Repository des docker",""
   "vitam_folder_permission","Droits Unix par défaut des arborescences créées pour VITAM",""
   "vitam_conf_permission","Droits sur les fichiers de configuration déployés pour VITAM",""
   "pull_strategy","Stratégie lors du ``docker pull``",""
   "local_user","Utilisateur créé sur les hôtes des docker pour le mapping correct entre docker et hôte",""
   "vitam_docker_tag","Tag des *containers* au téléchargement ; assimilable à la version",""

Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

1. Test 

Pour tester le déploiement de VITAM, il faut se placer dans le répertoire ``deployment`` et entrer la commande suivante :

``ansible-playbook ansible-vitam/vitam.yml -i <path du fichier d'inventaire créé plus haut> --check``

2. Déploiement

Si la commande de test se termine avec succès, le déploiement est à réaliser avec la commande suivante :

``ansible-playbook ansible-vitam/vitam.yml -i <path du fichier d'inventaire créé plus haut>``

