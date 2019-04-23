Spécificités des certificats
############################

Trois différents types de certificats sont nécessaires et utilisés dans Vitam:

* Certificats serveur
* Certificats client
* Certificats d'horodatage

Pour générer des certificats, il est possible de s'inspirer du fichier ``pki/config/crt-config``.
Il s'agit du fichier de configuration openssl utilisé par la PKI de test de Vitam.
Ce fichier dispose des 3 modes de configurations nécessaires pour générer les certificats de Vitam:

* extension_server: pour générer les certificats serveur
* extension_client: pour générer les certificats client
* extension_timestamping: pour générer les certificats d'horodatage

Cas des certificats serveur
---------------------------

Généralités
^^^^^^^^^^^

Les services vitam qui peuvent utiliser des certificats serveur sont ingest-external, access-external, offer (les seuls pouvant écouter en https).
Par défaut, offer n'écoute pas en https par soucis de performances.

Pour les certificats serveur, il est nécessaire de bien réfléchir au CN et subjectAltName qui vont être spécifiés.
Si par exemple le composant offer est paramétré pour fonctionner en https uniquement, il faudra que le CN ou un des subjectAltName de son certificat corresponde à son nom de service sur consul.

Noms dns des serveurs https Vitam
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Les noms dns résolus par consul seront ceux ci:

* <nom_service>.service.<domaine_consul> sur le datacenter local
* <nom_service>.service.<dc_consul>.<domaine_consul> sur n'importe quel datacenter

Rajouter le nom consul avec le nom du datacenter dedans peut par exemple servir si une installation multi-site de vitam est faite (appels storage -> offer inter dc)

Les variables pouvant impacter les noms d'hosts dns sur consul sont:

* ``consul_domain`` dans le fichier ``environments/group_vars/all/vitam_vars.yml`` --> <domain_consul>
* ``vitam_site_name`` dans le fichier d'inventaire ``environments/hosts`` (variable globale) --> <dc_consul>
* Service offer seulement: ``offer_conf`` dans le fichier d'inventaire ``environments/hosts`` (différente pour chaque instance du composant offer) --> <nom_service>

Exemples:

Avec ``consul_domain: consul``, ``vitam_site_name: dc2``, l'offre ``offer-fs-1`` sera résolue par

* ``offer-fs-1.service.consul`` depuis le dc2
* ``offer-fs-1.service.dc2.consul`` depuis n'importe quel dc

Avec ``consul_domain: preprod.vitam``, ``vitam_site_name: dc1``, les composants ingest-external et access-external seront résolu par

* ``ingest-external.service.preprod.vitam`` et ``access-external.service.preprod.vitam`` depuis le dc local
* ``ingest-external.service.dc1.preprod.vitam`` et ``access-external.service.dc1.preprod.vitam`` depuis n'importe quel dc

.. warning:: Si les composants ingest-external et access-external sont appelés via leur IP ou des records DNS autres que ceux de consul, il faut également ne pas oublier de les rajouter dans les subjectAltName.

Cas des certificats client
--------------------------

Les services qui peuvent utiliser des certificats client sont:

* N'importe quelle application utilisant les API Vitam exposées sur ingest-external et access-external
* Le service storage si le service offer est cofniguré en https
* Un certificat client nommé vitam-admin-int est obligatoire
    - Pour déployer vitam (nécessaire pour initialisation du fichier pronom)
    - Pour lancer certains actes d'exploitation

Cas des certificats d'horodatage
--------------------------------

Les services logbook et storage utilisent des certificats d'horodatage.
