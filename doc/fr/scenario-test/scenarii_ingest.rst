Scenarii pour l'ingest
######################

Cette partie décrit les scenarii de test pour l'ingest.

Liste des scenarii
==================

Ci-dessous est représentée la liste des différents scenarii de test exécutés dans le cadre de l'automatisation des tests.

.. csv-table:: Liste des scenarii pour l'ingest
   :header: "Nom du Sip", "Etat", "Code", "Nom du test", "Catégorie"
   :widths: 35, 5, 5, 35, 20
   
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Entrée en succès","EVT_STP_SANITY_CHECK_SIP"
   "SIP_KO/ZIP/KO_VIRUS_code2.zip","KO",200,"Échec du processus du contrôle sanitaire du SIP : fichier détecté comme infecté","EVT_STP_SANITY_CHECK_SIP"
   "SIP_KO/ZIP/KO_BORD_mauvais_format.zip","KO",200,"bordereau de versement au mauvais format","EVT_CHECK_SEDA"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification du bordereau de versement","EVT_CHECK_SEDA"
   "SIP_KO/ZIP/KO_BORD_non_conforme_seda.zip","KO",200,"Échec du processus de vérification du bordereau de versement : Bordreau non conforme au schéma SEDA 2.0","EVT_CHECK_SEDA"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification du bordereau de versement : Bordreau conforme au schéma SEDA","EVT_CHECK_SEDA"
   "SIP_KO/ZIP/KO_SIP_usages_errones.zip","KO",200,"liste des BinaryDataObject et PhysicalDataObject dont la valeur du champ Usage est non conforme","EVT_CHECK_MANIFEST_DATAOBJECT_VERSION"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification des usages des groupes d’objets","EVT_CHECK_MANIFEST_DATAOBJECT_VERSION"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus vérification du nombre d’Objets","EVT_CHECK_MANIFEST_OBJECTNUMBER"
   "SIP_KO/ZIP/KO_OBJT_nombresup_SEDA.zip","KO",200,"Échec du processus de vérification du nombre d’Objets","EVT_CHECK_MANIFEST_OBJECTNUMBER"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification de l’empreinte","EVT_OLD_CHECK_DIGEST"
   "SIP_KO/ZIP/KO_BORD_empreinteKO.zip","KO",200,"Échec du processus de vérification de l’empreinte : liste des objets du répertoire Content non conforme à l’empreinte déclarée dans le manifeste ","EVT_OLD_CHECK_DIGEST"
   "SIP_KO/ZIP/KO_OBJT_orphelins.zip","KO",200,"Échec du processus de contrôle métier et extraction du SIP des Objet, Groupes d'Objets et Unités Archivistiques","EVT_CHECK_MANIFEST"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de contrôle métier et extraction du SIP des Objet, Groupes d'Objets et Unités Archivistiques","EVT_CHECK_MANIFEST"
   "SIP_OK/ZIP/OK_ARBO_rateau.zip","OK",200,"Succès du processus de création du Journal de Cycle de Vie des Groupes d’Objets","EVT_CHECK_MANIFEST"
   "SIP_OK/ZIP/Format_ID_Different.zip","WARNING",206,"Identification des formats, FormatId différents","EVT_OG_OBJECTS_FORMAT_CHECK"
   "SIP_OK/ZIP/OK_FORMT_PUID_incoherent.zip","WARNING",206,"Avertissement lors du processus de vérification des formats","EVT_OG_OBJECTS_FORMAT_CHECK"
   "SIP_OK/ZIP/OK_ARBO_rateau.zip","OK",200,"Succès du processus de contrôle globale de l’entrée","EVT_STP_INGEST_CONTROL_SIP"
   "SIP_KO/ZIP/KO_BORD_mauvais_format.zip","KO",200,"Échec du processus de contrôle globale de l’entrée","EVT_STP_INGEST_CONTROL_SIP"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de contrôle et traitements des Unités Archivistiques","EVT_STP_UNIT_CHECK_AND_PROCESS"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification préalable à la prise en charge","EVT_STP_STORAGE_AVAILABILITY_CHECK"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de vérification de la disponibilité de l’offre de stockage","EVT_STORAGE_AVAILABILITY_CHECK"
   "SIP_OK/ZIP/OK_SIP_test_differentes_langues.zip","OK",200,"Succès de la sécurisation des métadonnées des Unités Archivistiques","EVT_UNIT_METADATA_STORAGE"
   "SIP_OK/ZIP/OK_SIP_test_differentes_langues.zip","OK",200,"Succès du processus d’indexation des métadonnées des Unités Archivistiques","EVT_UNIT_METADATA_INDEXATION"
   "SIP_OK/ZIP/OK_SIP_test_differentes_langues.zip","OK",200,"Succès du processus d’enregistrement du journal de cycle de vie des Unités Archivistiques","EVT_UNIT_LOGBOOK_STORAGE"
   "SIP_OK/ZIP/OK_ARBO_rateau.zip","OK",200,"Succès du processus de rangement des Unités Archivistiques","EVT_STP_UNIT_STORING"
   "SIP_OK/ZIP/OK_ARBO_rateau.zip","OK",200,"Succès du processus de rangement des Objets et groupes d’objets","EVT_OG_STORAGE"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processsus d’indexation des métadonnées des Objets et Groupes d’Objets","EVT_OG_METADATA_INDEXATION"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus d’enregistrement du journal de cycle de vie des Objets et Groupes d’Objets","EVT_OG_LOGBOOK_STORAGE"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de rangement des Objets et groupes d’objets","EVT_STP_OG_STORING"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de finalisation de l’entrée et de notification à l’opérateur de versement","EVT_STP_INGEST_FINALISATION"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus de notification à l’opérateur de versement","EVT_ATR_NOTIFICATION"
   "SIP_OK/ZIP/OK_SIP_2_GO.zip","OK",200,"Succès du processus d’alimentation du Registre des Fonds","EVT_ACCESSION_REGISTRATION"