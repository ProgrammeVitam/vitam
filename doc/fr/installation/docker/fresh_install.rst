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
	hosts-ingest-web

	[hosts-ihm-demo]
	<hostname du serveur où déployer le composant ihm-demo>

	[hosts-ingest-web]
	<hostname du serveur où déployer le composant ingest-web>

	[hosts-mongo-express]
	<hostname du serveur où déployer le composant mongo-express>

	[hosts-metadata]
	<hostname du serveur où déployer le composant metadata>

	[hosts-logbook]
	<hostname du serveur où déployer le composant logbook>

	[hosts-workspace]
	<hostname du serveur où déployer le composant workspace>

	[hosts-processing]
	<hostname du serveur où déployer le composant processing>

	[hosts-metadata-mongodb]
	<hostname du serveur où déployer la base de données mongo>

	[hosts:vars]
	ansible_ssh_user=<nom de l'utilisateur admin (sudoer) configuré sur les serveurs cible>
	ansible_become=true
	vitam_environment=<nom court de l'environnement (ex : int, preprod, prod)>
	docker_registry_hostname=docker.programmevitam.fr
	vitam_mongodb_host=<hostname du serveur où est déployé la base de données mongo>
	vitam_logbook_host=<hostname du serveur où est déployé le serveur logbook>
	vitam_logbook_port=8204
	vitam_folder_permission=<permission des dossiers de stockage VITAM (conseillé : 0755)>
	vitam_conf_permission=<permission des dossiers de stockage VITAM (conseillé : 0500)>
	pull_strategy=always
	local_user=vitam


.. note:: fichier d'exemple d'itération 5

../../../../deployment/environments/hosts.int  (ou une arbo du genre...) est à réfléchir/afficher quant à un exemple réel lié à la version.


.. TODO:: faire également référence à la release note, si procédure supplémentaire particulière

Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

1. Test 

Pour tester le déploiement de VITAM, il faut se placer dans le répertoire ``deployment`` et entrer la commande suivante :

``ansible-playbook ansible-vitam/vitam.yml -i <path du fichier d'inventaire créé plus haut> --check``

2. Déploiement

Si la commande de test se termine avec succès, le déploiement est à réaliser avec la commande suivante :

``ansible-playbook ansible-vitam/vitam.yml -i <path du fichier d'inventaire créé plus haut>``

