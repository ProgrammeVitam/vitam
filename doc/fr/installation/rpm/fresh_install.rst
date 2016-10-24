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

.. literalinclude:: ../../../../deployment/environments-rpm/hosts.example
   :language: ini
   :linenos:

Pour chaque type de "host" (lignes 2 à 176), indiquer le(s) serveur(s) défini(s) pour chaque fonction. Une colocalisation de composants est possible.

.. warning:: indiquer les contre-indications !

Ensuite, dans la section ``hosts:vars`` (lignes 179 à 216), renseigner les valeurs comme décrit :

.. csv-table:: Définition des variables
   :header: "Clé", "Description","Valeur"
   :widths: 10, 10,10

   "ansible_ssh_user","Utilisateurs ansible sur les machines sur lesquelles VITAM sera déployé",""
   "ansible_become","Propriété interne à ansible pour passer root",""
   "local_user","En cas de déploiement en local",""
   "environnement","Suffixe",""
   "vitam_reverse_domain","Cas de la gestion d'un reverse proxy",""
   "consul_domain","nom de domaine consul",""
   "vitam_ihm_demo_external_dns","Déprécié ; ne pas utiliser",""
   "rpm_version","Version à installer",""
   "days_to_delete","Période de grâce des données sous Elastricsearch avant destruction (valeur en jours)",""
   "days_to_close","Période de grâce des données sous Elastricsearch avant fermeture des index (valeur en jours)",""
   "days_to_delete_topbeat","Période de grâce des données sous Elastricsearch  - index Topbeat - avant destruction (valeur en jours)",""
   "dns_server","Serveur DNS que Consul peut appeler s'il n'arrive pas à faire de résolution","172.16.1.21"


A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées sous |repertoire_inventory| ``/group_vars/all/all``, comme suit :

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/all
   :language: yaml
   :linenos:


Le ``vault.yml`` est également présent sous |repertoire_inventory| ``/group_vars/all/all`` et contient les secrets ; ce fichier est encrypté par ``ansible-vault`` et doit être paramétré avant le lancement de l'orchestration de déploiement.

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/vault.txt
   :language: ini
   :linenos:


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

.. warning:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.


Paramétrage de l'antivirus (ingest-externe)
-------------------------------------------

.. todo:: A rédiger plus correctement. L'idée est de créer un autre shell sous ``ansible-vitam-rpm/roles/vitam/templates/ingest-external`` ; prendre comme modèle le fichier ``scan-clamav.sh.j2``. Il faudra aussi modifier le fichier ``ansible-vitam-rpm/roles/vitam/templates/ingest-external/ingest-external.conf.j2`` en pointant sur le nouveau fichier.

Paramétrage des certificats (\*-externe)
-----------------------------------------

Se reporter à l'étape "PKI" du déploiement, décrite plus bas.


Déploiement
===========

PKI
---

1. paramétrer le fichier ``environnements-rpm/group_vars/all/vault.yml``et le fichier d'inventaire de la plate-forme sous ``environnements-rpm`` (se baser sur le fichier hosts.example)

2. Lancer le script ``pki-generate-ca.sh`` : en cas d'absence de PKI, il permet de générer  une PKI, ainsi que des certificats pour les échanges https entre composants. Se reporter au chapitre PKI si le client préfère utiliser sa propre PKI.

3. Lancer la script ``generate_certs.sh <environnement>`` . Basé sur le contenu du fichier ``vault.yml``, ce script  génère des certificats  nécessaires au bon fonctionnement de VITAM.

3. Lancer la script ``generate_stores.sh <environnement>`` . Basé sur le contenu du fichier ``vault.yml``, ce script  génère des stores nécessaires au bon fonctionnement de VITAM.

4. Lancer le script ``copie_fichiers_vitam.sh <environnement>`` pour recopier dans les bons répertoires d'ansiblerie les certificats et stores précédemment créés.


Déploiement
-------------

Une fois l'étape de PKI effectuée avec succès, le déploiement est à réaliser avec la commande suivante :

ansible-playbook |repertoire_playbook ansible|/vitam.yml -i |repertoire_inventory|/<ficher d'inventaire> --ask-vault-pass

Il sera alors demandé le mot de passe correspondant au fichier ``vault.yml``.

