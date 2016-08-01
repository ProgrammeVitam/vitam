Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le répertoire ``deployment`` Ils consistent en 2 parties :
 
 * le playbook ansible, présent dans le répertoire « ansible-vitam », qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le répertoire ``environments``

Pour configurer le déploiement, il est nécessaire de créer (dans n'importe quel répertoire en dehors du répertoire ``environments`` un nouveau fichier d'inventaire comportant les informations suivantes (les informations délimitées par les balises < > sont à compléter):

.. code-block:: bash

	[hosts]

	[hosts:children]
	hosts-ihm-demo
	hosts-mongodb
	hosts-logbook
	hosts-metadata
	hosts-workspace
	hosts-ingest

	[hosts-ihm-demo]
	<hostname du serveur où déployer le composant ihm-demo>

	[hosts-ingest-web]
	<hostname du serveur où déployer le composant ingest-web>

	[hosts-mongo-express]
	<hostname du serveur où déployer le composant ingest-web>

	[hosts-logbook]
	<hostname du serveur où déployer le composant logbook>

	[hosts-access]
	<hostname du serveur où déployer la base de données access>

	[hosts-metadata]
	<hostname du serveur où déployer le composant metadata>

	[hosts-workspace]
	<hostname du serveur où déployer le composant workspace>

	[hosts-processing]
	<hostname du serveur où déployer le composant processing>

	[hosts-mongodb]
	<hostname du serveur où déployer la base de données mongo>

	[hosts:vars]
	ansible_ssh_user=ansible
	ansible_become=true
	vitam_environment=rec
	vitam_folder_permission=0755
	vitam_conf_permission=0500
	pull_strategy=always
	local_user=vitam

.. note:: fichier d'exemple d'itération 5

../../../../deployment/environments-rpm/hosts.int2 est à réfléchir quant à un exemple lié à la version.


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

1. Test 
Pour tester le déploiement de VITAM, il faut se placer dans le répertoire ``deployment`` et entrer la commande suivante :

``ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<ficher d'inventaire> --check``

2. Déploiement

Si la commande de test se termine avec succès, le déploiement est à réaliser avec la commande suivante :

``ansible-vitam-rpm/vitam.yml -i environments-rpm/<ficher d'inventaire>``

