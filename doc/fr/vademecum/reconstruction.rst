
======================
Reconstruction Vitam
======================

.. sectnum::
.. contents::

Préambule
~~~~~~~~~~

Une plateforme Vitam complète en production comprend à minima 2 sites :
 - un site de référence
 - un site secondaire

Lors des installations le site de référence a une variable globale ``primary_site`` a « true », le site secondaire a cette même variable valorisée à « false ».

Lorsque la plateforme est en fonctionnement nominal les données des offres de stockages sont synchronisées au fil de l'eau entre les 2 sites.

Les opérations sur les offres de stockages du site primaire sont enregistrées en base de données (instances mongo-data) dans la base metadata.

Elles sont également synchronisées sur le site secondaire via des appels API.

Enfin, cette même base de données est enregistrée en partie dans l'offre de stockage.

Pour plus d'information se référer à la documentation d'exploitation.
http://www.programmevitam.fr/ressources/DocCourante/html/exploitation/topics/30-reconstruction.html
et
http://www.programmevitam.fr/ressources/DocCourante/html/archi/_images/dualsite-architecture.svg

Dans la suite du document nous allons voir pas à pas les étapes à suivre pour effectuer les scénarios de reconstruction.

Reconstruction du site primaire par rapport au site secondaire :
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Arrêter la solution Vitam afin qu'elle ne soit pas sollicitée et qu'il n'y ait pas d'écriture sur les offres de stockage et en base de données au niveau du site primaire.

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/stop_vitam.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Relancer le service Consul afin que les services concernés par la reconstruction soient accessibles par leur nom consul.

::
   ansible hosts_consul_server -i environments/<hostfile.site.primaire> -m shell -a "systemctl start vitam-consul.service" --vault-password-file vault_pass.txt

Relancer les services mongodb-data du site primaire :

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/start_mongodb.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Se connecter via ssh à ma machine hébergeant le service mongos de mongo-data sur le site primaire et visualiser les bases :

::
   mongo --host mongo-data-mongos.service.consul --port 27017 --username <vitamdb-admin> --password <password>

.. warning:: Le port 27017 est celui par défaut, il peut être différent selon le paramétrage de votre installation. Le username et password se trouvent dans le vault-vitam.yml.

::
   mongos> show dbs
    admin       0.000GB x
    config      0.002GB x
    identity    0.000GB x
    logbook     0.005GB
    masterdata  0.002GB
    metadata    0.000GB
    report      0.000GB

Effectuer une purge des datas :

::
   use logbook
   db.getCollectionNames().forEach(function(x) {db[x].remove({})});

   use masterdata
   db.getCollectionNames().forEach(function(x) {db[x].remove({})});

   use metadata
   db.getCollectionNames().forEach(function(x) {db[x].remove({})});

   use report
   db.getCollectionNames().forEach(function(x) {db[x].remove({})});

Arrêtez mongodb data :

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/stop_mongodb.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Purger les datas d'ElasticSearch-data le cas échéant :

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/start_elasticsearch_data.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Se connecter à la machine hébergeant elasticsearch-data

::
   curl http://<url-elasticsearch-data-vm>:9200/_cat/indices?v

Exemple de sortie :

::
  health status index                                     uuid                   pri rep docs.count docs.deleted store.size pri.store.size
green  open   unit_18_20220121_165121                   aqbCCro3RiepIEbw5q96aw   4   0          0            0       904b           904b
green  open   accesscontract_20220121_165612            EFabHAJ3QRuDkm72LsS9Hw   1   0         14            0     60.3kb         60.3kb
green  open   logbookoperation_16_20220121_165311       yCFFMaX3QryuWITQeDon7Q   3   0         89           10    702.8kb        702.8kb
green  open   unit_2_20220121_165101                    XmUIXsVzR7u41buTpgIOqA   3   0          0            0       678b           678b
green  open   logbookoperation_20_20220121_165314       hY6Dcn1LQU2HmDlvS8VC5A   3   0         89           12    598.8kb        598.8kb
green  open   unit_1_20220121_165058                    tfF9B4DCR4eF5TFucDPLYg   4   0          0            0       904b           904b
green  open   accessionregisterdetail_20220121_165610   Y5JvJDJ_QPKhahNbkjkqqA   1   0         31            0     92.1kb         92.1kb
green  open   profile_20220121_165612                   l3it7HulSV2zCkEjhv_nbQ   1   0          0            0       226b           226b
green  open   agencies_20220121_165614                  qN-h1rr6RsyTlv2ILOdZ9A   1   0         78            0     11.8kb         11.8kb
green  open   .apm-custom-link                          1hEYY-MzRjS_N6DscqbBcA 3   0          0            0       678b           678b
…

Vider chaque index via :

::
    curl -XPOST 'localhost:9200/*{index_name}*/_delete_by_query?conflicts=proceed&pretty'  -H 'Content-Type: application/json' -d '{ "query": { "match_all": {} } }'

ou faire appel au playbook dédié :

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/clean_indexes_es_data.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Stoppez Elasticsearch-data :
::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/stop_elasticsearch_data.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Changer la variable du site primaire  ``primary_site`` à false.

Lancer le playbook vitam.yml :

.. warning::  S'assurer d'avoir les mêmes certificats dans les sources de déploiement. Autrement, il faudra regénérer les ca et certificats, ainsi que les stores et lancer le playbook vitam.yml avec le tag update_vitam_certificates.

::
  ./pki/scripts/generate_ca.sh
  ./pki/scripts/generate_certs.sh environments/<hostfile.site.primaire>
  ./generate_stores.sh

Update vitam certificates pour avoir les certificats à jour avec ceux de l'ansiblerie

::
  ansible-playbook ansible-vitam/vitam.yml -i environments/<hostfile.site.primaire> --vault-password-file vault_pass.txt --tags update_vitam_certificates -e delete_security_profiles=yes

Prévoir cette action en amont (quand les services vitam sont actifs).

Désactiver le checkontologie car dépendant de elasticsearch-data éteint.

Dans le playbook ansible-vitam/vitam.yml, commentez les lignes suivantes :
::
    ### CHECK VITAM ONTOLOGY WHEN UPGRADE ###
    #- hosts: hosts_functional_administration
    #  gather_facts: no
    #  any_errors_fatal: true
    #  roles:
    #    - check_ontologies
    #  vars:
    #    vitam_struct: "{{ vitam.functional_administration }}"

Lancer enfin la commande :

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam/vitam.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Attendre la fin de l'exécution du playbook. Se connecter à la base de donnée mongo-data via son instance mongos.

::
   mongo --host mongo-data-mongos.service.consul --port 27017 --username <vitamdb-admin> --password <password>

.. warning:: Les username/password se trouvent dans le vault-vitam.yml. Le port 27017 est celui par défault, il peut être différent selon le paramétrafe de votre installation.

Controler la base metadata. Une collection Offset doit être désormais créée. C'est via cette dernière que la reconstruction peut être suivie ainsi que par le nombre de documents qu'on trouve dans les collections ``Unit`` et ``Objectgroup``.

::
   mongos>use metadata
   mongos> show collections
   ObjectGroup
   Offset
   Snapshot
   Unit

Effectuer le comptage des objets via le playbook ``ansible-vitam-exploitation/reconstruction_doc_count.yml`` :

::
   ansible-playbook -i environments/<hostfile.site> ansible-vitam-exploitation/reconstruction_doc_count.yml --vault-password-file vault_pass.txt

.. warning:: Pensez à générer les host_vars avant de lancer le playbook de comptage ci-dessus autrement les ips des machines ne seront pas connues par le playbook et l'execution tombera en erreur (playbook generate_hostvars_for_1_network_interface ou generate_hostvars_for_2_network_interface selon votre topologie réseau).

À l'issue de l'exécution, le fichier ``environments/unit_got_docs_count.<site_name>`` est généré. Il sera ainsi aisé de faire un diff entre votre site de référence et le site de reconstruction pour suivre l'état d'avancement de la reconstruction.

Exemple de rapport généré :
::
    ## Mongo-data Unit and ObjectGroup Doc count
    tenant 0 - Unit_DocCount=2564
    tenant 0 - ObjectGroup_DocCount=635
    tenant 1 - Unit_DocCount=12
    tenant 1 - ObjectGroup_DocCount=6
    tenant 2 - Unit_DocCount=0
    tenant 2 - ObjectGroup_DocCount=0
    tenant 3 - Unit_DocCount=0
    tenant 3 - ObjectGroup_DocCount=0
    ## Elasticsearch-data Unit and ObjectGroup Doc count
    unit_0_20220406_090103    2564
    unit_2_20220406_090106       0
    unit_5_20220406_090111       0
    unit_4_20220406_090110       0
    unit_3_20220406_090108       0
    unit_grp1_20220406_090113    0
    unit_6_20220406_090112       0
    unit_1_20220406_090105      12
    objectgroup_0_20220406_090104    1318
    objectgroup_1_20220406_090105      14
    objectgroup_2_20220406_090107       0
    objectgroup_3_20220406_090109       0
    objectgroup_4_20220406_090110       0
    objectgroup_5_20220406_090112       0
    objectgroup_6_20220406_090113       0
    objectgroup_grp1_20220406_090115    0

Il est également possible de compter les objets de manière manuelle. Il faut comptabiliser le nombre de documents côté Unit et ObjectGroup, soit le nombre d'élements de ces collections.

Remettre la variable ``primary_site`` à true et relancer le playbook vitam.yml pour reconfigurer le site primaire en tant que tel.

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam/vitam.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Redémarrer les extras :
::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/start_vitam_admin.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Contrôler le bon démarrage des services. Effectuer des démarrages manuels des services si nécessaire.

Reconstruction du site secondaire par rapport au site primaire :
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Arrêter la solution Vitam sur le site à reconstruire afin qu'elle ne soit pas sollicitée et qu'il n'y ait pas d'écriture sur les offres de stockage et en base de données au niveau du site primaire.

Pour ce faire, arrêter les externals sur le site primaire et le site secondaire.

::
   ansible-playbook -i environments/<hostfile.site.primaire> ansible-vitam-exploitation/stop_external.yml --vault-password-file vault_pass.txt

Sur le site Vitam secondaire, arrêtez Vitam en exécutant les commandes suivantes :

::
   ansible-playbook -i environments/<hostfile.site.secondaire> ansible-vitam-exploitation/stop_vitam.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Relancer le service Consul afin que les services concernés par la reconstruction soient accessibles par leur nom consul.

::
   ansible hosts_consul_server -i environments/<hostfile.site.secondaire> -m shell -a "systemctl start vitam-consul.service" --vault-password-file vault_pass.txt

Relancer les instances mongo-data.

::
   ansible-playbook -i environments/<hostfile.site.secondaire> ansible-vitam-exploitation/start_mongodb.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Effectuer une purge des données mongo de la même manière que dans la section précédente (reconstruction du site primaire).

Arrêter mongodata.
::
   ansible-playbook -i environments/<hostfile.site.secondaire> ansible-vitam-exploitation/stop_mongodb.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

Effectuer une purge des données elasticsearch data comme précédemment (voir reconstruction du site primaire).
et stopper eleasticsearch-data.

Redémarrer le site secondaire vitam :

::
   ansible-playbook -i environments/<hostfile.site.secondaire> ansible-vitam-exploitation/start_vitam.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml")

La reconstruction s'effectue alors au fil de l'eau. On peut observer la création des objets via mongo ou sur elasticsearch.

Le playbook ``ansible-vitam-exploitation/reconstruction_doc_count.yml`` peut être utilisé à cet usage.

Tant que le nombre de document Unit et ObjectGroup dans la base metadata n'est pas identique entre les 2 sites, la reconstruction n'est pas terminée.

Utilisation du playbook comme précédemment:

::
   ansible-playbook -i environments/<hostfile.site.primaire>  ansible-vitam-exploitation/reconstruction_doc_count.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra<_reference.yml") -e site="reference"

::
   ansible-playbook -i environments/<hostfile.site.secondaire>  ansible-vitam-exploitation/reconstruction_doc_count.yml --vault-password-file vault_pass.txt (--extra-vars="@environments/vitam_extra.yml") -e site="reconstruction"
