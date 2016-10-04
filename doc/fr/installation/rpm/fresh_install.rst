Première installation
#####################


.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments-rpm``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam-rpm``


Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le sous-répertoire |repertoire_deploiement| . Ils consistent en 2 parties :
 
 * le playbook ansible, présent dans le sous-répertoire |repertoire_playbook ansible|, qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le sous-répertoire |repertoire_inventory|

 
Configuration du déploiement
============================

Informations "plate-forme"
--------------------------

Pour configurer le déploiement, il est nécessaire de créer (dans n'importe quel répertoire en dehors du répertoire |repertoire_inventory| un nouveau fichier d'inventaire comportant les informations suivantes :

.. literalinclude:: ../../../../deployment/environments-rpm/hosts.int
   :language: ini
   :linenos:

Pour chaque type de "host" (lignes 12 à 157), indiquer le(s) serveur(s) défini(s) pour chaque fonction. Une colocalisation de composants est possible.

.. warning:: indiquer les contre-indications !

Ensuite, dans la section ``hosts:vars`` (lignes 164 à 179), renseigner les valeurs comme décrit :

.. csv-table:: Définition des variables
   :header: "Clé", "Description","Valeur"
   :widths: 10, 10,10

   "ansible_ssh_user","Utilisateurs ansible sur les machines sur lesquelles VITAM sera déployé",""
   "ansible_become","Propriété interne à ansible pour passer root",""
   "vitam_folder_permission","Droits Unix par défaut des arborescences créées pour VITAM",""
   "vitam_conf_permission","Droits sur les fichiers de configuration déployés pour VITAM",""
   "local_user","En cas de déploiement en local",""
   "environnement","Suffixe",""
   "vitam_environnement","Comme environnement ; ATTENTION : le mot local est réservé pour cette directive aux seules installations en local",""
   "vitam_reverse_domain","Cas de la gestion d'un reverse proxy",""
   "consul_domain","nom de domaine consul",""
   "vitam_ihm_demo_external_dns","a vérifier ...",""
   "https_reverse_proxy","cas d'appel vers un proxy... Deprecated","10.220.23.1:3128"
   "proxy_host","cas d'appel vers un proxy (adresse IP)... Deprecated","10.220.23.1"
   "proxy_port","cas d'appel vers un proxy (port) ... Deprecated","3128"
   "rpm_version","Version à installer",""
   "days_to_delete","Période de grâce des données sous Elastricsearch avant destuction (valeur en jours)",""
   "dns_server","Serveur DNS que Consul peut appeler s'il n'arrive pas à faire de résolution","172.16.1.21"


A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées sous |repertoire_inventory| ``/group_vars/all/all``, comme suit :

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/all
   :language: yaml
   :linenos:


Le ``vault.yml`` est également présent sous |repertoire_inventory| ``/group_vars/all/all`` et contient les secrets ; ce fichier est encrypté par ``ansible-vault``.

Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

.. warning:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.


Paramétrage de l'antivirus (ingest-externe)
-------------------------------------------

.. todo:: A expliquer

Paramétrage des certificats (\*-externe)
-----------------------------------------

.. todo:: A expliquer



Test de la configuration
========================

Pour tester le déploiement de VITAM, il faut se placer dans le répertoire |repertoire_deploiement| et entrer la commande suivante :

``ansible-playbook`` |repertoire_playbook ansible| ``/vitam.yml -i`` |repertoire_inventory| ``/<ficher d'inventaire> --check``

.. note:: cette commande n'est pas recommandée, du fait de limitations de check.

Déploiement
===========

Pré-script
-------------
Le script suivant est à jour en premier ; il permet de générer ou recopier (selon le cas) une PKI, ainsi que des certificats pour les échanges https entre composants.

.. note:: ce script est en cours de mise au point.


Déploiement
-------------

Une fois le pré-script passé avec succès, le déploiement est à réaliser avec la commande suivante :

ansible-playbook |repertoire_playbook ansible|/vitam.yml -i |repertoire_inventory|/<ficher d'inventaire> --ask-vault-pass

