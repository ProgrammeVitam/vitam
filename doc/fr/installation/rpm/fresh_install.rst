Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le répertoire ``deployment`` Ils consistent en 2 parties :
 
 * le playbook ansible, présent dans le répertoire « ansible-vitam », qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le répertoire ``environments``

Pour configurer le déploiement, il est nécessaire de créer (dans n'importe quel répertoire en dehors du répertoire ``environments`` un nouveau fichier d'inventaire comportant les informations suivantes (les informations délimitées par les balises < > sont à compléter):

.. code-block:: bash

	[hosts]

	[hosts:children]
	hosts-ihm-demo
	hosts-ingest
	hosts-access
	hosts-mongodb
	hosts-logbook
	hosts-metadata
	hosts-workspace
	hosts-processing
	hosts-storageengine
	hosts-storageofferdefault
	hosts-ingest-external
	hosts-functional-administration

	[hosts-mongodb]
	vitam-iaas-mongodb-01.int2

	[hosts-logbook]
	vitam-iaas-logbook-01.int2

	[hosts-metadata]
	vitam-iaas-metadata-01.int2

	[hosts-workspace]
	vitam-iaas-workspace-01.int2

	[hosts-ingest]
	vitam-iaas-ingest-01.int2

	[hosts-ingest-external]
	vitam-iaas-ingest-02.int2

	[hosts-functional-administration]
	vitam-iaas-funcadm-01.int2

	[hosts-access]
	vitam-iaas-access-01.int2

	[hosts-processing]
	vitam-iaas-processing-01.int2

	[hosts-ihm-demo]
	vitam-iaas-ihm-demo-01.int2

	[hosts-storageengine]
	vitam-iaas-storage-01.int2

	[hosts-storageofferdefault]
	vitam-iaas-defoffer-01.int2


	[hosts:vars]
	ansible_ssh_user=centos
	ansible_become=true
	vitam_folder_permission=0755
	vitam_conf_permission=0640
	local_user=vitam
	vitam_ihm_demo_external_dns=int2.env.programmevitam.fr


.. note:: fichier d'exemple d'itération 6 du programme VITAM. Pour lamise en place, indiquer les noms corrects de machine.

.. info:: dans le cadre de l'itération 7, certains paramètres sont pré-définis dans le fichier ``environnements-rpm/group_vars/all``.


.. code-block:: yaml

	---
	vitam_folder_root: /vitam
	docker_registry_httponly: yes
	vitam_docker_tag: latest


	# Internal components communication configuration

	vitam_logbook_host: "{{ groups['hosts-logbook'][0] }}"
	vitam_logbook_port: 8082
	vitam_logbook_baseurl: "http://{{vitam_logbook_host}}:{{vitam_logbook_port}}"


	vitam_access_host: "{{ groups['hosts-access'][0] }}"
	vitam_access_port: 8082
	vitam_access_baseurl: "http://{{vitam_access_host}}:{{vitam_access_port}}"


	vitam_ingest_host: "{{ groups['hosts-ingest'][0] }}"
	vitam_ingest_port: 8082
	vitam_ingest_baseurl: "http://{{vitam_ingest_host}}:{{vitam_ingest_port}}"


	vitam_metadata_host: "{{ groups['hosts-metadata'][0] }}"
	vitam_metadata_port: 8082
	vitam_metadata_baseurl: "http://{{vitam_metadata_host}}:{{vitam_metadata_port}}"


	vitam_workspace_host: "{{ groups['hosts-workspace'][0] }}"
	vitam_workspace_port: 8082
	vitam_workspace_baseurl: "http://{{vitam_workspace_host}}:{{vitam_workspace_port}}"


	vitam_processing_host: "{{ groups['hosts-processing'][0] }}"
	vitam_processing_port: 8082
	vitam_processing_baseurl: "http://{{vitam_processing_host}}:{{vitam_processing_port}}"

	vitam_ihm_demo_host: "{{ groups['hosts-ihm-demo'][0] }}"
	vitam_ihm_demo_port: 8082
	vitam_ihm_demo_baseurl: /ihm-demo
	vitam_ihm_demo_static_content: webapp

	vitam_storageengine_host: "{{ groups['hosts-storageengine'][0] }}"
	vitam_storageengine_port: 8082
	vitam_storageengine_baseurl: "http://{{vitam_storageengine_host}}:{{vitam_storageengine_port}}"

	vitam_storageofferdefault_host: "{{ groups['hosts-storageofferdefault'][0] }}"
	vitam_storageofferdefault_port: 8082
	vitam_storageofferdefault_baseurl: "http://{{vitam_storageofferdefault_host}}:{{vitam_storageofferdefault_port}}"

	vitam_functional_administration_host: "{{ groups['hosts-functional-administration'][0] }}"
	vitam_functional_administration_port: 8082
	vitam_functional_administration_baseurl: "http://{{vitam_functional_administration_host}}:{{vitam_functional_administration_port}}"

	vitam_ingestexternal_host: "{{ groups['hosts-ingest-external'][0] }}"
	vitam_ingestexternal_port: 8082
	vitam_ingestexternal_baseurl: "http://{{vitam_ingestexternal_host}}:{{vitam_ingestexternal_port}}"

	vitam_mongodb_host: "{{ groups['hosts-mongodb'][0] }}"
	vitam_mongodb_port: 27017




.. TODO:: ../../../../deployment/environments-rpm/hosts.int2 est à réfléchir quant à un exemple lié à la version.


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné comme indiqué plus haut.

1. Test 
Pour tester le déploiement de VITAM, il faut se placer dans le répertoire ``deployment`` et entrer la commande suivante :

``ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<ficher d'inventaire> --check``

2. Déploiement

Si la commande de test se termine avec succès, le déploiement est à réaliser avec la commande suivante :

``ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<ficher d'inventaire> --become``

.. note:: le become est un "au cas où".

