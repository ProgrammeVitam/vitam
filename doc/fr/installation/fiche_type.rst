Fiche type de déploiement VITAM
###############################

Fiche-type VITAM
================

.. caution:: cette liste a pour but d'évoluer et s'étoffer au fur et à mesure des mises à jour des composants et du contenu des fichiers de déploiement de VITAM.

.. csv-table:: Tableau récapitulatif des informations à renseigner pour VITAM
   :header: "Nom du omposant", "Descriptif", "Valeur d'exemple","Valeur choisie","Si HA ?"
   :widths: 10, 15, 10, 15, 5

   "IHM-demo machine","interface web","vitam-prod-app-1.internet.agri","",""
   "ingest-external machine","interface web","vitam-prod-app-1.internet.agri","",""
   "ingest-internal machine","interface web","vitam-prod-app-1.internet.agri","",""
   "access-external machine","interface web","vitam-prod-app-1.internet.agri","",""
   "access-internal machine","interface web","vitam-prod-app-1.internet.agri","",""
   "logbook machine","interface web","vitam-prod-app-1.internet.agri","",""
   "metadata machine","interface web","vitam-prod-app-1.internet.agri","",""
   "processing machine(s)","base de données","vitam-prod-app-1.internet.agri","",""
   "worker machine(s)","Traitement de fichiers","vitam-prod-wrk-1.internet.agri","",""
   "storage-engine machine(s)","xxxx","vitam-prod-app-1.internet.agri","",""
   "storage-offer-default machine(s)","implémentation de pilote de stockage","vitam-prod-app-1.internet.agri","",""
   "Consul servers","implémentation de Consul pour un DNS applicatif (nécessite 3 serveurs minimum ; règle (2*n+1) )","vitam-prod-app-1.internet.agri, vitam-prod-app-2.internet.agri, vitam-prod-app-3.internet.agri","",""
   "elasticsearch data machine(s)","Cluster ElasticSearch de données VITAM (3 machines)","vitam-prod-ela-1.internet.agri,vitam-prod-ela-2.internet.agri,vitam-prod-ela-3.internet.agri","",""
   "elasticsearch log machine(s)","Cluster ElasticSearch de log VITAM (3 machines)","vitam-prod-log-1.internet.agri,vitam-prod-log-2.internet.agri,vitam-prod-log-3.internet.agri","",""
   "mongo-s machine(s)","Cluster MongoDB de routage de data VITAM (3 machines)","vitam-prod-ms-1.internet.agri,vitam-prod-ms-2.internet.agri,vitam-prod-ms-3.internet.agri","",""  
   "mongo-c machine(s)","Cluster MongoDB de configuration des données VITAM (3 machines)","vitam-prod-mc-1.internet.agri,vitam-prod-mc-2.internet.agri,vitam-prod-mc-3.internet.agri","",""
   "mongo-d machine(s)","Cluster Mongo de données VITAM (3 machines)","vitam-prod-md-1.internet.agri,vitam-prod-md-2.internet.agri,vitam-prod-md-3.internet.agri","",""
   "log central machine(s)","Centralisation des logs","vitam-prod-log-1.internet.agri","",""

.. todo:: ajouter section issue du DAT sur les préconisations de colocalisation, ... et nombre de machines pour chaque composant.
