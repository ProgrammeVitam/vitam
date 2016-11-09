Modèle de données Vitam
#######################

Objectif du document
====================

Ce document a pour objectif de présenter la struture générale des collections utilisées dans la solution logicielle Vitam.
Il est destiné principalement aux développeurs, afin de leur présenter la vision cible Vitam, ainsi qu'à tous les autres acteurs du programme pour leur permettre de connaître ce qui existe à l'état actuel.

Il explicite chaque champ, précise la relation avec les sources (manifeste conforme au standard SEDA v.2.0 ou référentiels Pronom et règles de gestions) et la structuration JSON stockée dans MongoDB.

Pour chacun des champs, cette documentation apporte :

- Une liste des valeurs licites
- La sémantique ou syntaxe du champ
- La codification en JSON

Il décrit aussi parfois une utilisation particulière faite à une itération donnée.
Cette indication diffère de la cible finale, auquel cas, le numéro de l'itération de cet usage est mentionné.

Collection LogbookOperation
===========================

Utilisation de la collection LogbookOperation
---------------------------------------------

La collection LogbookOperation comporte toutes les informations de traitement liées aux opérations effectuées dans la solution logicielle Vitam, chaque opération faisant l'objet d'un enregistrement distinct.

Ces opérations sont :

- Entrée (développé en bêta)
- Mise à jour (développé en bêta)
- Données de référence (développé en bêta)
- Audit (développé post-bêta)
- Elimination (développé post-bêta)
- Préservation (développé post-bêta)
- Vérification (développé post-bêta)

Exemple de JSON stocké en base
------------------------------

JSON correspondant à une opération d'entrée terminée avec succès.

::

  {
    "_id": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
    "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
    "evType": "PROCESS_SIP_UNITARY",
    "evDateTime": "2016-11-04T11:31:51.430",
    "evDetData": null,
    "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": null,
    "outMessg": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
    "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
    "agIdApp": null,
    "agIdAppSession": null,
    "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
    "agIdSubm": null,
    "agIdOrig": null,
    "obId": null,
    "obIdReq": null,
    "obIdIn": null,
    "events": [
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "STP_SANITY_CHECK_SIP",
            "evDateTime": "2016-11-04T11:31:51.460",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus du contrôle sanitaire du SIP",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq"
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "SANITY_CHECK_SIP",
            "evDateTime": "2016-11-04T11:31:51.466",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du contrôle sanitaire : aucun virus détecté",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "CHECK_CONTAINER",
            "evDateTime": "2016-11-04T11:31:51.470",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus du contrôle de format",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "STP_SANITY_CHECK_SIP",
            "evDateTime": "2016-11-04T11:31:51.474",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus du contrôle sanitaire du SIP : aucun virus détecté",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "STP_UPLOAD_SIP",
            "evDateTime": "2016-11-04T11:31:51.478",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de téléchargement du SIP",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "STP_UPLOAD_SIP",
            "evDateTime": "2016-11-04T11:31:51.600",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de téléchargement du SIP",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7qqyaaaaq",
            "evType": "STP_INGEST_CONTROL_SIP",
            "evDateTime": "2016-11-04T11:31:52.003",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de contrôle global de l’entrée du SIP",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7szaaaaaq",
            "evType": "CHECK_SEDA",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Extraction du bordereau réalisé avec succès Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7szaaaaba",
            "evType": "CHECK_MANIFEST_DATAOBJECT_VERSION",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Contrôle des versions réalisé avec succès Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7szaaaabq",
            "evType": "CHECK_MANIFEST_OBJECTNUMBER",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Contrôle du nombre des objets réalisé avec succès Detail=  OK:3",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7szaaaaca",
            "evType": "CHECK_CONSISTENCY",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de vérification de la cohérence entre Objets, Groupes d’Objets et Unités Archivistiques Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7szaaaacq",
            "evType": "CHECK_CONSISTENCY_POST",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de contrôle de la cohérence entre Objets, Groupes d’Objets et Unités Archivistiques Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7qqyaaaaq",
            "evType": "STP_INGEST_CONTROL_SIP",
            "evDateTime": "2016-11-04T11:31:52.292",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de contrôle globale de l’entrée du SIP",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": "Demo IT10",
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7s2iaaaaq",
            "evType": "STP_OG_CHECK_AND_TRANSFORME",
            "evDateTime": "2016-11-04T11:31:52.298",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de vérification et transformation des objets",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7ylyaaaaq",
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T11:31:53.007",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Contrôle de conformité des objets réalisé avec succès Detail=  OK:3",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7ylyaaaba",
            "evType": "OG_OBJECTS_FORMAT_CHECK",
            "evDateTime": "2016-11-04T11:31:53.007",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès de la vérification des formats Detail=  OK:3",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7s2iaaaaq",
            "evType": "STP_OG_CHECK_AND_TRANSFORME",
            "evDateTime": "2016-11-04T11:31:53.007",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès de l’étape de vérification et transformation des objets",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7ymyaaaaq",
            "evType": "STP_STORAGE_AVAILABILITY_CHECK",
            "evDateTime": "2016-11-04T11:31:53.011",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de vérification préalable à la prise en charge",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n7zhyaaaaq",
            "evType": "STORAGE_AVAILABILITY_CHECK",
            "evDateTime": "2016-11-04T11:31:53.120",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès de la vérification de la disponibilité de l’offre de stockage Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7ymyaaaaq",
            "evType": "STP_STORAGE_AVAILABILITY_CHECK",
            "evDateTime": "2016-11-04T11:31:53.120",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de vérification préalable à la prise en charge",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7zjaaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T11:31:53.124",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de rangement des objets et groupes d’objets",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n77naaaaaq",
            "evType": "OG_STORAGE",
            "evDateTime": "2016-11-04T11:31:53.908",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du rangement des Objets Detail=  OK:3",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4n77naaaaba",
            "evType": "OG_METADATA_INDEXATION",
            "evDateTime": "2016-11-04T11:31:53.908",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Index objectgroup réalisé avec succès Detail=  OK:3",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n7zjaaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T11:31:53.908",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de rangement des Objets et groupes d’objets",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n77oiaaaaq",
            "evType": "STP_UNIT_STORING",
            "evDateTime": "2016-11-04T11:31:53.913",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de rangement des Unités Archivistiques",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4oab7iaaaaq",
            "evType": "UNIT_METADATA_INDEXATION",
            "evDateTime": "2016-11-04T11:31:54.237",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Index unit réalisé avec succès Detail=  OK:4",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4n77oiaaaaq",
            "evType": "STP_UNIT_STORING",
            "evDateTime": "2016-11-04T11:31:54.237",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de rangement des Unités Archivistiques",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4oacaiaaaaq",
            "evType": "STP_ACCESSION_REGISTRATION",
            "evDateTime": "2016-11-04T11:31:54.241",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus dalimentation du registre des fonds",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4oacqqaaaaq",
            "evType": "ACCESSION_REGISTRATION",
            "evDateTime": "2016-11-04T11:31:54.306",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès denregistrement des archives prises en charge dans le registre des fonds Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4oacaiaaaaq",
            "evType": "STP_ACCESSION_REGISTRATION",
            "evDateTime": "2016-11-04T11:31:54.306",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus dalimentation du registre des fonds",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4oacryaaaaq",
            "evType": "STP_INGEST_FINALISATION",
            "evDateTime": "2016-11-04T11:31:54.311",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus de finalisation de l’entrée et de notification à lopérateur de versement",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaacaam7mxabezmakyf4oadiqaaaaq",
            "evType": "ATR_NOTIFICATION",
            "evDateTime": "2016-11-04T11:31:54.402",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Notification envoyée Detail=  OK:1",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aeaaaaaaaaaam7mxabezmakyf4oacryaaaaq",
            "evType": "STP_INGEST_FINALISATION",
            "evDateTime": "2016-11-04T11:31:54.402",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Succès du processus de finalisation de l’entrée et de notification à l’opérateur de versement",
            "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"PlatformId\":425367}",
            "agIdApp": null,
            "agIdAppSession": null,
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "agIdSubm": null,
            "agIdOrig": null,
            "obId": null,
            "obIdReq": null,
            "obIdIn": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evType": "PROCESS_SIP_UNITARY",
            "evDateTime": "2016-11-04T11:31:54.017",
            "evDetData": null,
            "evIdProc": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": null,
            "outMessg": "Entrée effectuée avec succès",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"PlatformId\":425367}",
            "evIdReq": "aedqaaaaacaam7mxaau56akyf4n3zzyaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        }
    ],
    "_tenant": 0
    }

Détail des champ du JSON stocké en base
---------------------------------------

Chaque entrée de cette collection est composée d'une structure auto-imbriquée : la structure possède une première instanciation "incluante", et contient un tableau de N structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.


"_id" : Identifiant unique donné par le système lors de l'initialisation de l'opération, constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire de l'opération dans la collection.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
     Il identifie l'entrée / le versement de manière unique dans la base.
     Cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par pair (début/fin).

     *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    Issu de la définition du workflow en json (fichier default-workflow.json).
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement.

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possible pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possible pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"agIdApp" (agent Identifier Application) : identifiant de l’application externe qui appelle Vitam pour effectuer l'opération

    *Utilisation à IT10 : la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe uniquement pour la structure incluante.*

"agIdAppSession" (agent Identifier Application Session) : identifiant donnée par l’application utilisatrice externe qui appelle Vitam à la session utilisée pour lancer l’opération
    L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.

    *Utilisation à IT10 : la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe pour les structures incluantes et incluses*

"evIdReq" (event Identifier Request) : identifiant de la requête déclenchant l’opération
    Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).

    *Ce champ existe pour les structures incluantes et incluses*

"agIdSubm" (agent Identifier Submission) : identifiant du service versant.
    Il s'agit du <SubmissionAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"agIdOrig" (agent Identifier Originating) : identifiant du service producteur.
    Il s'agit du <OriginatingAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
     Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc). Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini

     *Ce champ existe pour les structures incluantes et incluses*

"obIdReq" (object Identifier Request) : Identifiant de la requête caractérisant un lot d’objets auquel s’applique l’opération.
      Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.

      *Utilisation à IT10 : la valeur est toujours 'null'. Ce champ existe pour les structures incluantes et incluses*

"obIdIn" (ObjectIdentifierIncome) : Identifiant externe du lot d’objets auquel s’applique l’opération.
      Chaîne de caractère intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se reporte l'événement.
      Il s'agit le plus souvent soit du nom du SIP lui-même, soit du <MessageIdentifier> présent dans le manifeste.

      *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
      Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

      *Ce champ existe uniquement pour la structure incluante.*

"_tenant": identifiant du tenant
      *Ce champ existe uniquement pour la structure incluante.*

Collection LogbookLifeCycleUnit
===============================

Utilisation de la collection LogbookLifeCycleUnit
-------------------------------------------------

Le journal de cycle de vie d'une unité archivistique (ArchiveUnit) trace tous les événements qui impactent celle-ci dès sa prise en charge dans le système. Il doit être conservé aussi longtemps qu'elle est gérée par le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les ArchiveUnit qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des métadonnées OK, avant notification au service versant

Chaque Unit possède une et une seule entrée dans sa collection LogbookLifeCycleUnit.

Exemple de JSON stocké en base
------------------------------

::

  {
    "_id": "aeaaaaaaaaaam7mxaap44akyf6fv4sqaaaaq",
    "evId": "aedqaaaaacaam7mxaap44akyf6fv4syaaaaq",
    "evType": "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units",
    "evDateTime": "2016-11-04T13:33:32.619",
    "evIdProc": "aedqaaaaacaam7mxaau56akyf6fsepiaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": "STARTED",
    "outMessg": "Début de la vérification de la cohérence entre objets/groupes d’objets et ArchiveUnit.",
    "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
    "obId": "aeaaaaaaaaaam7mxaap44akyf6fv4sqaaaaq",
    "evDetData": null,
    "events": [
        {
            "evId": "aedqaaaaacaam7mxaap44akyf6fv4syaaaaq",
            "evType": "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units",
            "evDateTime": "2016-11-04T13:33:32.648",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf6fsepiaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet/groupe dobjet référencé par un ArchiveUnit.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf6fv4sqaaaaq",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf6fwjciaaaaq",
            "evType": "STP_UNIT_STORING",
            "evDateTime": "2016-11-04T13:33:34.217",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf6fsepiaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début de lindex unit.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf6fv4sqaaaaq",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf6fwjciaaaaq",
            "evType": "STP_UNIT_STORING",
            "evDateTime": "2016-11-04T13:33:34.253",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf6fsepiaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Index unit réalisé avec succès.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf6fv4sqaaaaq",
            "evDetData": null,
            "_tenant": 0
        }
    ],
    "_tenant": 0
    }

Détail des champ du JSON stocké en base
---------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal de cycle de vie, constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal de cycle de vie de l'unit.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal de cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possible fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possible pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement. Par exemple, l'historisation lors d'une modification de métadonnés se fait dans ce champ.

    *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
    Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

    *Ce champ existe uniquement pour la structure incluante*

"_tenant": identifiant du tenant
    *Ce champ existe pour les structures incluantes et incluses*

Collection LogbookLifeCycleObjectGroup
======================================

Utilisation de la collection LogbookLifeCycleObjectGroup
--------------------------------------------------------

Le journal de cycle de vie du groupe d'objets (ObjectGroup) trace tous les événements qui impactent le groupe d'objet (et les objets associés) dès sa prise en charge dans le système et doit être conservé aussi longtemps que les objets sont gérés dans le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les groupes d'objets et objets qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des MD OK, avant notification au service versant

Chaque groupe d'objet possède une et une seule entrée dans sa collection LogbookLifeCycleObjectGroup.

Exemple de JSON stocké en base
------------------------------

::

  {
    "_id": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
    "evId": "aedqaaaaacaam7mxaap44akyf7hurgaaaabq",
    "evType": "CHECK_CONSISTENCY",
    "evDateTime": "2016-11-04T14:47:43.512",
    "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": "STARTED",
    "outMessg": "Début de la vérification de la cohérence entre objets/groupes d’objets et ArchiveUnit.",
    "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
    "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
    "evDetData": null,
    "events": [
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hurgaaaabq",
            "evType": "CHECK_CONSISTENCY",
            "evDateTime": "2016-11-04T14:47:43.515",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet/groupe dobjet référencé par un ArchiveUnit.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "\"aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba\"",
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T14:47:45.132",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début de la vérification de lempreinte.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"MessageDigest\":\"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc1259056365aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc1259056365aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\"} ",
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaaq",
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T14:47:45.135",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début de la vérification de lempreinte.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a245b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\"} ",
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaaq",
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T14:47:45.140",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet reçu correspondant à lobjet attendu.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a245b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a245b668914a3364ee0def01ef8719eed5488e0e21020e\"} ",
            "_tenant": 0
        },
        {
            "evId": "\"aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba\"",
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T14:47:45.145",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet reçu correspondant à lobjet attendu.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a245b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a245b668914a3364ee0def01ef8719eed5488e0e21020e\"} ",
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hu57aaaaaq",
            "evType": "STP_OG_CHECK_AND_TRANSFORME",
            "evDateTime": "2016-11-04T14:47:45.148",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début de la vérification du format.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"diff\": \"+ PUID : 'fmt/18'\n+ FormatLitteral : 'Acrobat PDF 1.4 - Portable Document Format'\n+ MimeType : 'application/pdf'\"}",
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba.json",
            "evType": "STP_OG_CHECK_AND_TRANSFORME",
            "evDateTime": "2016-11-04T14:47:45.203",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Format de l’objet identifié, référencé dans le référentiel interne et avec des informations cohérentes entre le manifeste et le résultat de loutil didentification.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"diff\": \"+ PUID : 'fmt/18'\n+ FormatLitteral : 'Acrobat PDF 1.4 - Portable Document Format'\n+ MimeType : 'application/pdf'\"}",
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hvjgyaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T14:47:46.587",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début du stockage de lobjet.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaaq",
            "evType": "OG_STORAGE",
            "evDateTime": "2016-11-04T14:47:46.603",
            "evIdProc": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début du stockage de lobjet.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaaq",
            "evType": "OG_STORAGE",
            "evDateTime": "2016-11-04T14:47:46.647",
            "evIdProc": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Stockage de lobjet réalisé avec succès.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hvjwqaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T14:47:46.650",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Stockage de lobjet réalisé avec succès.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hvjxiaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T14:47:46.653",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début du stockage de lobjet.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hvjxiaaaaq",
            "evType": "STP_OG_STORING",
            "evDateTime": "2016-11-04T14:47:46.687",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Index objectgroup réalisé avec succès.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        }
    ],
    "_tenant": 0
    }
Détail des champ du JSON stocké en base
---------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal de cycle de vie, constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal de cycle de vie du groupe d'objet.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possible pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal de cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possible pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement.

    *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
    Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

    *Ce champ existe uniquement pour la structure incluante.*

"_tenant": identifiant du tenant
    *Ce champ existe pour les structures incluantes et incluses*

Collection Unit
===============

Utilisation de la collection Unit
---------------------------------

La colection unit contient les informations relatives aux ArchiveUnit.

Exemple de JSON
---------------

::

  {
    "_id": "aeaaaaaaaaaam7mxabigiakyiqyobzaaaaaq",
    "DescriptionLevel": "RecordGrp",
    "Title": "Europe orientale-sud",
    "Description": "C:\\Users\\XXX.XXX\\Desktop\\SIP arborescent\\Europe\\Europe\noccidentale",
    "StartDate": "2016-10-12T17:24:00",
    "EndDate": "2016-10-12T17:24:00",
    "_og": "",
    "_ops": [
        "aedqaaaaacaam7mxabr7iakyiqymbdqaaaaq"
    ],
    "_tenant": 0,
    "_max": 3,
    "_min": 1,
    "_up": [
        "aeaaaaaaaaaam7mxabigiakyiqyobxqaaaaq"
    ],
    "_nbc": 1,
    "_uds": [
        {
            "aeaaaaaaaaaam7mxabigiakyiqyobwqaaaaq": 2
        },
        {
            "aeaaaaaaaaaam7mxabigiakyiqyobxqaaaaq": 1
        }
    ],
    "_us": [
        "aeaaaaaaaaaam7mxabigiakyiqyobwqaaaaq",
        "aeaaaaaaaaaam7mxabigiakyiqyobxqaaaaq"
    ],
    "OriginatingAgency": {
        "OrganizationDescriptiveMetadata": "Issy"
    },
    }

Exemple de XML en entrée
------------------------

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour contribuer les champs du JSON. Il s'agit des informations situées entre les balises <ArchiveUnit>

::

  <DescriptiveMetadata>
    <ArchiveUnit id="ID8">
      <Content>
        <DescriptionLevel>RecordGrp</DescriptionLevel>
        <Title>Espagne</Title>
        <Description>C:\Users\XXX.XXX\Desktop\SIP arborescent\Europe\Europe occidentale\Espagne</Description>
        <StartDate>2016-10-12T17:24:00</StartDate>
        <EndDate>2016-10-12T17:24:00</EndDate>
      </Content>
        <ArchiveUnit id="ID11">
          <ArchiveUnitRefId>ID10</ArchiveUnitRefId>
        </ArchiveUnit>
      </ArchiveUnit>
    <DescriptiveMetadata>

Détail du JSON
--------------

La structure de la collection Unit est composée de la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau conforme au standard SEDA v.2.0., c'est-à-dire toutes les balises se rapportant aux ArchiveUnit. Cette transposition se fait comme suit :

*A noter: les champs préfixés par un '_' devraient être visibles via les API avec un code utilisant '#' en prefix. Mais il est possible que pour la version Bêta, le '_' reste visible.*

"_id" (#id): Identifiant unique de l'unit.
    Chaîne de 36 caractères.

"DescriptionLevel": La valeur de champ est une chaine de caractères. Il s'agit du niveau de description archivistique de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> dans le manifeste.

"Title": La valeur de ce champ est une chaine de caractères. Il s'agit du titre de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le manifest.

"Description": La valeur contenue dans ce champ est une chaîne de caractères.
    Ce champ est renseigné avec les informations situées entre les balises <description> de l'archiveUnit concernée dans le manifest.

"XXXXX" : Des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le boredereau SEDA au niveau du Content de chaque archive unit.
    (CF SEDA 2.0 descriptive pour connaître la liste des métadonnées facultatives)

"_og" (#object): identifiant du groupe d'objets référencé dans cette unit
    Chaîne de 36 caractères.

"_ops" (#operations): tableau contenant les identifiants d'opérations auxquelles ce Unit a participé

"_tenant" (#tenant): il s'agit de l'identifiant du tenant

"_max" (ne devrait pas être visible): profondeur maximale de l'unit par rapport à une racine
    Calculé, cette profondeur est le maximum des profondeurs, quelles que soient les racines concernées et les chemins possibles

"_min" (ne devrait pas être visible): profondeur minimum de l'unit par rapport à une racine
    Calculé, symétriquement le minimum des profondeurs, quelles que soient les racines concernées et les chemins possibles ;

"_up" (#unitups): est un tableau qui recense les _id des units parentes (parents immédiats)

"_nbc" (#nbunits): nombre d'enfants immédiats de l'unit

"_uds" (ne devrait pas être visible): tableau contenant la parentalité, non indexé et pas dans Elasticseatch exemple { GUID1 : depth1, GUID2 : depth2, ... } ; chaque depthN indique la distance relative entre le unit courant et le unit parent dont le GUID est précisé.

"_us" (#allunitups): tableau contenant la parentalité, indexé [ GUID1, GUID2, ... }

"OriginatingAgency": { "OrganizationDescriptiveMetadata": Métadonnées de description concernant le service producteur }

_type (#type): Type de document utilisé lors de l'entrée, correspond au ArchiveUnitProfile, le profil d'archivage utilisé lors de l'entrée

"_mgt" (#management): possède les balises reprises du bloc <Management> du bordereau pour cette unit

Collection ObjectGroup
======================

Utilisation de la collection ObjectGroup
----------------------------------------

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de Json stocké en base
------------------------------

::

   {
     "_id": "aeaaaaaaaaaam7mxab43iakye2cxhbaaaaaq",
     "_tenant": 0,
     "_type": "",
     "FileInfo": {},
     "_qualifiers": {
         "Thumbnail": {
             "_nbc": 1,
             "versions": [
                 {
                     "_id": "aeaaaaaaaaaam7mxab43iakye2cxiqyaaaaq",
                     "DataObjectGroupId": "aeaaaaaaaaaam7mxab43iakye2cxhbaaaaaq",
                     "DataObjectVersion": "Thumbnail_1",
                     "FormatIdentification": {
                         "FormatLitteral": "Portable Network Graphics",
                         "MimeType": "image/png",
                         "FormatId": "fmt/12"
                     },
                     "FileInfo": {
                         "Filename": "Vitam-Sensibilisation-API-V1.0.png",
                         "CreatingApplicationName": "LibreOffice/Impress",
                         "CreatingApplicationVersion": "5.0.5.2",
                         "CreatingOs": "Windows_X86_64",
                         "CreatingOsVersion": "10",
                         "LastModified": "2016-06-23T12:45:20"
                     },
                     "Metadata": {
                         "Image": null
                     },
                     "OtherMetadata": null,
                     "Size": 40740,
                     "Uri": "content/fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0.png",
                     "MessageDigest": "fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0",
                     "Algorithm": "SHA-512"
                 }
             ]
         },
         "TextContent": {
             "_nbc": 1,
             "versions": [
                 {
                     "_id": "aeaaaaaaaaaam7mxab43iakye2cxiriaaaaq",
                     "DataObjectGroupId": "aeaaaaaaaaaam7mxab43iakye2cxhbaaaaaq",
                     "DataObjectVersion": "TextContent_1",
                     "FormatIdentification": {
                         "FormatLitteral": "Plain Text File",
                         "MimeType": "text/plain",
                         "FormatId": "x-fmt/111",
                         "Encoding": "UTF-8"
                     },
                     "FileInfo": {
                         "Filename": "Vitam-Sensibilisation-API-V1.0.txt",
                         "LastModified": "2016-06-23T12:50:20"
                     },
                     "Metadata": {
                         "Text": null
                     },
                     "OtherMetadata": null,
                     "Size": 17120,
                     "Uri": "content/d156f4a4cc725cc6eaaafdcb7936c9441d25bdf033e4e2f1852cf540d39713446cfcd42f2ba087eb66f3f9dbfeca338180ca64bdde645706ec14499311d557f4.txt",
                     "MessageDigest": "d156f4a4cc725cc6eaaafdcb7936c9441d25bdf033e4e2f1852cf540d39713446cfcd42f2ba087eb66f3f9dbfeca338180ca64bdde645706ec14499311d557f4",
                     "Algorithm": "SHA-512"
                 }
             ]
         },
         "BinaryMaster": {
             "_nbc": 1,
             "versions": [
                 {
                     "_id": "aeaaaaaaaaaam7mxab43iakye2cxhaqaaaaq",
                     "DataObjectGroupId": "aeaaaaaaaaaam7mxab43iakye2cxhbaaaaaq",
                     "DataObjectVersion": "BinaryMaster_1",
                     "FormatIdentification": {
                         "FormatLitteral": "OpenDocument Presentation",
                         "MimeType": "application/vnd.oasis.opendocument.presentation",
                         "FormatId": "fmt/293"
                     },
                     "FileInfo": {
                         "Filename": "Vitam-Sensibilisation-API-V1.0.odp",
                         "CreatingApplicationName": "LibreOffice/Impress",
                         "CreatingApplicationVersion": "5.0.5.2",
                         "CreatingOs": "Windows_X86_64",
                         "CreatingOsVersion": "10",
                         "LastModified": "2016-05-05T20:45:20"
                     },
                     "Metadata": {
                         "Document": null
                     },
                     "OtherMetadata": null,
                     "Size": 100646,
                     "Uri": "content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp",
                     "MessageDigest": "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804",
                     "Algorithm": "SHA-512"
                 }
             ]
         },
         "Dissemination": {
             "_nbc": 1,
             "versions": [
                 {
                     "_id": "aeaaaaaaaaaam7mxab43iakye2cxiqaaaaaq",
                     "DataObjectGroupId": "aeaaaaaaaaaam7mxab43iakye2cxhbaaaaaq",
                     "DataObjectVersion": "Dissemination_1",
                     "FormatIdentification": {
                         "FormatLitteral": "Acrobat PDF 1.4 - Portable Document Format",
                         "MimeType": "application/pdf",
                         "FormatId": "fmt/18"
                     },
                     "FileInfo": {
                         "Filename": "Vitam-Sensibilisation-API-V1.0.pdf",
                         "CreatingApplicationName": "LibreOffice 5.0/Impress",
                         "CreatingApplicationVersion": "5.0.5.2",
                         "CreatingOs": "Windows_X86_64",
                         "CreatingOsVersion": "10",
                         "LastModified": "2016-05-05T20:45:32"
                     },
                     "Metadata": {
                         "Document": null
                     },
                     "OtherMetadata": null,
                     "Size": 186536,
                     "Uri": "content/f332ca3fd108067eb3500df34283485a1c35e36bdf8f4bd3db3fd9064efdb954.pdf",
                     "MessageDigest": "abead17e841c937187270cb95b0656bf3f7a9e71c8ca95e7fc8efa38cfffcab9889f353a95136fa3073a422d825175bf1bef24dc355bfa081f7e48b106070fd5",
                     "Algorithm": "SHA-512"
                 }
             ]
         }
     },
     "_up": [
         "aeaaaaaaaaaam7mxab43iakye2cxiryaaaaq"
     ],
     "_nbc": 0,
     "_ops": [
         "aedqaaaaacaam7mxabnmyakye2cun3iaaaaq"
     ]
     }

Exemple de XML
--------------

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour contribuer les champ du JSON

::

  <BinaryDataObject id="ID8">
      <DataObjectGroupReferenceId>ID4</DataObjectGroupReferenceId>
      <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
      <Uri>Content/ID8.txt</Uri>
      <MessageDigest algorithm="SHA-512">8e393c3a82ce28f40235d0870ca5b574ed2c90d831a73cc6bf2fb653c060c7f094fae941dfade786c826f8b124f09f989c670592bf7a404825346f9b15d155af</MessageDigest>
      <Size>30</Size>
      <FormatIdentification>
          <FormatLitteral>Plain Text File</FormatLitteral>
          <MimeType>text/plain</MimeType>
          <FormatId>x-fmt/111</FormatId>
      </FormatIdentification>
      <FileInfo>
          <Filename>BinaryMaster.txt</Filename>
          <LastModified>2016-10-18T21:03:30.000+02:00</LastModified>
      </FileInfo>
  </BinaryDataObject>

Détail des champ du JSON
------------------------

*A noter: les champs préfixés par un '_' devraient être visibles via les API avec un code utilisant '#' en prefix. Mais il est possible que pour la Beta, le '_' reste visible.*

"_id" (#id): identifiant du groupe d'objet. Il s'agit d'une chaîne de 36 caractères.
Cet id est ensuite reporté dans chaque structure inculse

"_tenant" (#tenant): identifiant du tenant

"_type" (#type): repris du nom de la balise présente dans le <Metadata> du <DataObjectPackage> du manifeste qui concerne le BinaryMaster.
Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...)

"FileInfo" : reprend le bloc FileInfo du BinaryMaster ; l'objet de cette copie est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait détruite (selon les règles de conservation), car ces informations ne sauraient être maintenues de manière garantie dans les futures versions.

"_qualifiers" (#qualifiers): est une structure qui va décrire les objets inclus dans ce groupe d'objet. Il est composé comme suit :

- [Usage de l'objet. Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>. C'est la valeur "BinaryMaster" qui est reportée.
  - "nb": nombre d'objets de cet usage
  - "versions" : tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du bordereau
    - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères.
    - "DataObjectGroupId" : Référence à l'identifiant objectGroup. Chaine de 36 caractères.
    - "DataObjectVersion" : version de l'objet par rapport à son usage.

    Par exemple, si on a *binaryMaster* sur l'usage, on aura au moins un objet *binarymaster_1*, *binaryMaster_2*. Ces champs sont renseignés avec les valeurs situées entre les balises <DataObjectVersion>.

    - "FormatIdentification": Contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel pronom et les valeurs que porte le document est faite.
      - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du XML
      - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du XML.
      - "FormatId" : PUID du format de l'objet. Il est défini par Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK).
    - "FileInfo"
      - "Filename" : nom de l'objet
      - "CreatingApplicationName": Chaîne de caractères. Contient le nom de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingApplicationVersion": Chaîne de caractères. Contient le numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOs": Chaîne de caractères. Contient le nom du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOsVersion": Chaîne de caractères. Contient le numéro de version du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnées correspondante portée par le fichier. *Ce champ et facultatif est n'est pas présent systématiquement*
      - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.

      - "Size": Ce champ contient un nombre entier. taille de l'objet (en octets).
    - "OtherMetadata": Contient une chaîne de caractères. Champ disponible pour ajouter d'autres métadonnées metier (Dublin Core, IPTC...). Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>. Ceci correspond à une extension du SEDA.
    - "Uri": localisation du fichier dans le SIP
    - "MessageDigest": empreinte du fichier. La valeur est calculé par Vitam.
    - "Algorithm": ce champ contient le nom de l'algorithme utilisé pour réaliser l'empreinte du document.

- "_up" (#up): [] : tableau d'identifiant des units parentes
- "_tenant" (#tenant): identifiant du tenant
- "_nbc" (#nbobjects): nombre d'objets dans ce groupe d'objet
- "_ops" (#operations): [] tableau des identifiants d'opérations pour lesquelles ce GOT a participé


Collection Formats
==================

Utilisation de la collection format
-----------------------------------

La collection format permet de stocker les différents formats de fichiers ainsi que leurs descriptions.

Exemple de JSON stocké en base
------------------------------

::

  {
     "_id": "aeaaaaaaaaaam7mxaahrkakyist5opaaahoa",
     "CreatedDate": "2016-09-27T15:37:53",
     "VersionPronom": "88",
     "Version": "2",
     "HasPriorityOverFileFormatID": [
         "fmt/714"
     ],
     "MIMEType": "audio/mobile-xmf",
     "Name": "Mobile eXtensible Music Format",
     "Group": "",
     "Alert": false,
     "Comment": "",
     "Extension": [
         "mxmf"
     ],
     "PUID": "fmt/961"
 }

Exemple de la description d'un format dans le XML d'entrée
----------------------------------------------------------

Ci-après, la portion d'un bordereau (DROID_SignatureFile_VXX.xml) utilisée pour renseigner les champ du JSON

::

   <FileFormat ID="105" MIMEType="application/msword" Name="Microsoft Word for Macintosh Document" PUID="x-fmt/64" Version="4.0">
     <InternalSignatureID>486</InternalSignatureID>
     <Extension>mcw</Extension>
   </FileFormat>

Détail des champ du JSON stocké en base
---------------------------------------

"_id": Il s'agit de l'identifiant unique du format dans VITAM.
    C'est une chaine de caractères composée de 36 signes.

"CreatedDate": Il s'agit la date de création de la version du fichier de signatures PRONOM utilisé pour alimenter l’enregistrement correspondant au format dans Vitam (balise DateCreated dans le fichier).
    Le format de la date correspond à la norme ISO 8601.

"VersionPronom": Il s'agit du numéro de version du fichier de signatures PRONOM utilisé.
    Ce chiffre est toujours un entier. Le numéro de version de pronom est à l'origine déclaré dans le XML au niveau de la balise <FFSignatureFile> au niveau de l'attribut "version ".

Dans cet exemple, le numéro de version est 88 :

::

 <FFSignatureFile DateCreated="2016-09-27T15:37:53" Version="88" xmlns="http://www.nationalarchives.gov.uk/pronom/SignatureFile">

"MIMEType": Ce champ contient le MIMEtype du format de fichier.
    C'est une chaine de caractères renseignée avec le contenu de l'attribut "MIMEType" de la balise <FileFormat>. Cet attribut est facultatif dans le XML.

"HasPriorityOverFileFormatID" : Liste des PUID des formats sur lesquels le format a la priorité.

::

  <HasPriorityOverFileFormatID>1121</HasPriorityOverFileFormatID>

Cet ID est ensuite utilisé dans Vitam pour retrouver le PUID correspondant.
    S'il existe plusieurs balises <HasPriorityOverFileFormatID> dans le xml pour un format donné, alors les PUID seront stocké dans le JSON sou la forme suivante :

::

  "HasPriorityOverFileFormatID": [
      "fmt/714",
      "fmt/715",
      "fmt/716"
  ],

"PUID": ce champ contient le PUID du format.
    Il s'agit de l'identifiant unique du format au sein du référentiel pronom. Il est issu du champ "PUID" de la balise <FileFormat>. La valeur est composée du préfixe fmt ou x-fmt, puis d'un nombre correspondant au numéro d'entrée du format dans le référentiel pronom. Les deuéléments sont séparés par un "/"

Par exemple

::

 x-fmt/64

Les PUID comportant un préfixe "x-fmt" indiquent que ces formats sont en cours de validation par The National Archives (UK). Ceux possédant un préfixe "fmt" sont validés.

"Version": Ce champ contient la version du format.
    Il s'agit d'une chaîne de caractère.

Exemples de formats :

::

 Version="3D Binary Little Endian 2.0"
 Version="2013"
 Version="1.5"

L'attribut "version" n'est pas obligatoire dans la balise <fileformat> du XML.

"Name": Il s'agit du nom du format.
    Le champ contient une chaîne de caractère. Le nom du format est issu de la valeur de l'attribut "Name" de la balise <FileFormat> du XML d'entrée.

"Extension" : Ce champ est un tableau.
    Il contient les valeurs situées entre les balises <Extension> elles-mêmes encapsulées entre les balises <FileFormat>. Le champ <Extension> peut-être multivalué. Dans ce cas, les différentes valeurs situées entre les différentes balises <Extensions> sont placées dans le tableau et séparées par une virgule.

Par exemple, pour le format PUID : fmt/918 on la XML suivant :

::

 <FileFormat ID="1723" Name="AmiraMesh" PUID="fmt/918" Version="3D ASCII 2.0">
     <InternalSignatureID>1268</InternalSignatureID>
     <Extension>am</Extension>
     <Extension>amiramesh</Extension>
     <Extension>hx</Extension>
   </FileFormat>

Les valeurs des balises extensions seront stockées de la façon suivante dans le JSON :

::

 "Extension": [
      "am",
      "amiramesh",
      "hx"
  ],

"Alert": Alerte sur l'obsolescence du format.
    C'est un booléen dont la valeur est par défaut placée à False.

"Comment": Ce champ n'est pas renseigné avec une valeur issue du XML.
    C'est un champ propre à VITAM qui contient une chaîne de caractère.

"Group": Ce champ n'est pas renseigné avec une valeur issue du XML.
    C'est un champ propre à VITAM qui contient une chaîne de caractère.

Collection Règles de gestion
============================

Utilisation de la collection règles de gestions
-----------------------------------------------

La collection règles de gestion permet de stocker unitairement les différentes règles de gestion du réferentiel.

Exemple de JSON stocké en base
------------------------------

::

 {
   "_id": "aeaaaaaaaaaam7mxabg7qakx65rhnkiaaada",
   "RuleId": "ACC-00004",
   "RuleType": "AccessRule",
   "RuleValue": "Communicabilité des informations portant atteinte à la monnaie et au crédit public (date du document)",
   "RuleDescription": "Durée de communicabilité applicable aux informations portant atteinte à la monnaie et au crédit public\nL’échéance est calculée à partir de la date du document ou du document le plus récent inclus dans le dossier",
   "RuleDuration": "25",
   "RuleMeasurement": "Année",
   "CreationDate": "2016-10-24",
   "UpdateDate": "2016-10-24"
   }

Colonne du csv comprenant les règles de gestion
-----------------------------------------------

================ ================= ======================= =========================== =============== ===============================
RuleId            RuleType          RuleValue               RuleDescription             RuleDuration     RuleMeasurement
---------------- ----------------- ----------------------- --------------------------- --------------- -------------------------------
Id de la règle    Type de règle     Intitulé de la règle    Description de la règle     Durée            Unité de mesure de la durée
================ ================= ======================= =========================== ===============  ===============================

Détail des champs
-----------------

"_id": Identifiant unique de la règle de gestion généré dans VITAM.
    C'est une chaîne de caractère composée de 36 caractères.

"RuleId": Il s'agit de l'identifiant de la règle dans le référentiel utilisé.
    Il est composé d'un Préfixe puis d'une nombre. Ces deux éléments sont séparés par un tiret

Par exemple :

::

 ACC-00027

Les préfixes indiquent le type de règle dont il s'agit. La liste des valeurs pouvant être utilisée comme préfixe ainsi que les types de règles auxquelles elles font référence sont disponibles en annexe.

"RuleType": Il s'agit du type de règle.
    Il correspond à la valeur située dans la colonne RuleType du fichier csv référentiel. Les valeurs possibles pour ce champ sont indiquées en annexe.

"RuleValue": Chaîne de caractères décrivant l'intitulé de la règle.
    Elle correspond à la valeur située dans la colonne RuleValue du fichier csv référentiel.

"RuleDescription": Chaîne de caractère permettant de décrire la règle.
    Elle correspond à la valeur située dans la colonne RuleDescriptionRule du fichier csv référentiel.

"RuleDuration": Chiffre entier compris entre 0 et 9999.
    Associé à la valeur "RuleMeasurement", il permet de décrire la durée d'application de la règle de gestion. Il correspond à la valeur située dans la colonne RuleDuration du fichier csv référentiel.

"RuleMeasurement": Correspond à l'unité de mesure de la durée décrite dans le champ "RuleDuration".

"CreationDate": Date de création de la règle

"UpdateDate": Date de mise à jour de la règle
       - Utilisation à IT10 : identique à la date de création. Ces deux dates sont mises à jour à chaque import de référentiel.

Collection AccessionRegisterSummary
===================================

Utilisation de la collection
----------------------------

Cette collection est utilisée pour l'affichage global du registre des fonds.

Exemple de JSON stocké en base
------------------------------

::

  {
    "_id": "aefaaaaaaaaam7mxaa4n4akyfm47sfqaaaaq",
    "_tenant": 0,
    "OriginatingAgency": "FRAN_NP_009734",
    "TotalObjects": {
        "Total": 1,
        "Deleted": 0,
        "Remained": 1
    },
    "TotalObjectGroups": {
        "Total": 244,
        "Deleted": 0,
        "Remained": 244
    },
    "TotalUnits": {
        "Total": 249,
        "Deleted": 0,
        "Remained": 249
    },
    "ObjectSize": {
        "Total": 55351,
        "Deleted": 0,
        "Remained": 55351
    },
    "creationDate": "2016-11-03T17:26:09.430"
    }

Exemple de la description dans le XML d'entrée
----------------------------------------------

Les seuls élements issus des bordereaux (manifest.xml), utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés entre les balisés <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

"_id": Identifiant unique. Il s'agit d'une chaine de 36 caractères.

"_tenant": 0,
"OriginatingAgency": La valeur de ce champ est une chaîne de caractère.
Ce champ est la clef primaire et sert de concaténation pour toutes les entrées effectuées sur ce producteur d'archives. Il est contenu entre les baslises <OriginatinAgencyIdentifier> du bordereau.

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état (total, deleted et remained)

    - "total": Nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état (total, deleted et remained)

    - "total": Nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état (total, deleted et remained)

    - "total": Nombre total d'unités archivistiques pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état (total, deleted et remained)

    - "total": Volume total en octets des fichiers pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Volume actualisé en octets des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.

"creationDate":  Date d'incription du producteur d'archives concerné dans le registre des fonds. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

Collection AccessionRegisterDetail
==================================

Utilisation de la collection
----------------------------

Cette collection a pour vocation de stocker l'ensemble des informations sur les opérations d'entrées réalisées pour un service producteur. A ce jour, il y a autant d'enregistrement que d'opérations d'entrées effectuées pour ce service producteur.

Exemple de JSON stocké en base
------------------------------

::

  {
    "_id": "aedqaaaaacaam7mxabnmyakye2ovpciaaaaq",
    "_tenant": 0,
    "OriginatingAgency": "FRAN_NP_005568",
    "SubmissionAgency": "FRAN_NP_005061",
    "EndDate": "2016-11-02T20:56:52.605+01:00",
    "StartDate": "2016-11-02T20:56:52.605+01:00",
    "Status": "STORED_AND_COMPLETED",
    "TotalObjectGroups": {
        "total": 3,
        "deleted": 0,
        "remained": 3
    },
    "TotalUnits": {
        "total": 4,
        "deleted": 0,
        "remained": 4
    },
    "TotalObjects": {
        "total": 1,
        "deleted": 0,
        "remained": 1
    },
    "ObjectSize": {
        "total": 579662,
        "deleted": 0,
        "remained": 579662
    }
    }

Exemple de la description dans le XML d'entrée
----------------------------------------------

Les seuls élements issus des bordereaux (manifest.xml) utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés entre les balisés <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

"_id": Identifiant unique.
    Il s'agit d'une chaine de 36 caractères.

"_tenant": 0, Identifiant du tenant
    *Utilisation post-béta*

"OriginatingAgency": Contient l'identifiant du service producteur du fonds.
    Il est contenu entre les baslises <OriginatinAgencyIdentifier>.

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314
La valeur est une chaîne de caractère.

"SubmissionAgency": Contient l'identifiant du service versant.
    Il est contenu entre les baslises <SubmissionAgencyIdentifier>.

Par exemple pour

::

  <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>

on récupère la valeur FRAN_NP_005761
La valeur est une chaîne de caractère.

Ce champ est facultatif dans le bordereau. Si elle est absente ou vide, alors la valeur contenue dans le champ <OriginatingAgencyIdentifier>. est reportée dans ce champ

"EndDate": date de la première opération d'entrée correspondant à l'enregistrement concerné. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00".
"StartDate": Date de la dernière opération d'entrée correspondant à l'enregistrement concerné. au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"
"Status": Indication sur l'état des archives concernées par l'enregistrement.
La liste des valeurs possibles pour ce champ se trouve en annexe

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état (total, deleted et remained)
    - "total": Nombre total de groupes d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre de groupes d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état (total, deleted et remained)
    - "total": Nombre total d'unités archivistiques pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'unités archivistiques conservées dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état (total, deleted et remained)
    - "total": Nombre total d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état (total, deleted et remained)
    - "total": Volume total en octet des fichiers pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Volume total en octets des fichiers conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

 Annexes
 =======

 Valeurs possibles pour le champ evType
 --------------------------------------

 ===================================== ========================================================================================== ======================================================================
 Code evtType                               Fr EventType Label                                                                         EN EventType Label
 ------------------------------------- ------------------------------------------------------------------------------------------ ----------------------------------------------------------------------
 STP_UPLOAD_SIP	                       Réception dans vitam	                                                                      Upload SIP
 UPLOAD_SIP	                           Tache de réception dans Vitam
 STP_SANITY_CHECK_SIP	                 Contrôles préalables à l’entrée	                                                          Sanity Check
 SANITY_CHECK_SIP	                     Contrôle sanitaire
 CHECK_CONTAINER	                     Contrôle du format du conteneur du SIP
 STP_INGEST_CONTROL_SIP	               Contrôle du bordereau	                                                                    Check Manifest
 CHECK_SEDA	Vérification               globale du SIP	                                                                            Check SIP – Manifest SEDA Consistency
 CHECK_MANIFEST_DATAOBJECT_VERSION	   Vérification des usage des groupes d’objets	                                              Check SIP – Manifest – DataObjectVersion
 CHECK_MANIFEST_OBJECTNUMBER	         Vérification du nombre d'objets	                                    c                       Check SIP – ObjectGroups - Objects Count
 CHECK_MANIFEST	                       Vérification de la cohérence du bordereau	                                                Check SIP – ObjectGroups – Lifecycle Logbook Creation
 CHECK_CONSISTENCY	                   Vérification de la cohérence entre objets, groupes d’objets et unités archivistiques
 OLD_CHECK_DIGEST	                     Vérification de l’empreinte	                                                              Check SIP – ObjectGroups – Digest
 STP_OG_CHECK_AND_TRANSFORME	         Contrôle et traitements des objets	                                                        Check and process objects
 CHECK_DIGEST	                         Vérification de l’intégrité des objets
 OG_OBJECTS_FORMAT_CHECK	             Identification des formats	                                                                File format check
 STP_STORAGE_AVAILABILITY_CHECK	       Préparation de la prise en charge	                                                        Check before storage
 STORAGE_AVAILABILITY_CHECK	           Vérification de la disponibilité de l’offre de stockage	                                  Storage availability check
 STP_UNIT_CHECK_AND_PROCESS	           Contrôle et traitements des Units	                                                        Check and process units
 UNITS_RULES_COMPUTE	                 Application des règles de gestion et calcul des échéances	                                Apply management rules and compute deadlines
 STP_UNIT_STORING	                     Rangement des Unites	                                                                      ArchiveUnit storing
 UNIT_METADATA_INDEXATION	             Indexation des metadonnées des Units	                                                      Units – Metadata Indexation
 STP_OG_STORING	                       Rangement des objets	                                                                      ObjectsGroups storing
 OG_STORAGE	                           Ecriture des objets sur l’offre de stockage	                                              ObjectGroups Storage
 OG_METADATA_INDEXATION	               Indexation des métadonnées des groupes d'objets	                                          ObjectGroups – Metadata Index
 STP_INGEST_FINALISATION	             Finalisation de l’entrée	                                                                  Ingest finalisation and transfer notification to the operator
 ATR_NOTIFICATION	                     Notification de la fin de l’opération d’entrée
 ACCESSION_REGISTRATION	               Alimentation du registre des fonds
 CHECK_PROFIL	                         Vérification du Profil
 UNIT_METADATA_STORAGE	               Sécurisation des métadonnées des Unités Archivistiques
 UNIT_LOGBOOK_STORAGE	                 Enregistrement du journal de cycle de vie des unités archivistiques
 OG_METADATA_STORAGE	                 Sécurisation des métadonnées des Objets et Groupes d’Objets
 OG_LOGBOOK_STORAGE	                   Enregistrement du journal du cycle de vie des Objets et Groupes d'objets
 ==================================== ========================================================================================== ======================================================================

 Valeurs possibles pour le champ evTypeProc
 ------------------------------------------

 =================================== ===================
 Process Type                          Valeur
 ----------------------------------- -------------------
 Ingest type process                   INGEST
 Audit type process                    AUDIT
 Destruction type process (v2)         DESTRUCTION
 Preservation type process             PRESERVATION
 Check type process                    CHECK
 Update process                        UPDATE
 Rules Manager process                 MASTERDATA
 =================================== ===================

 Prefixes possibles des RulesId
 -------------------------------

 ========= ============================== ===============================
 Prefixe    Type de règle correspondante   Description du type de règle
 --------- ------------------------------ -------------------------------
 ACC        AccessRule                     Règle d'accès
 APP        Appraisal                      Règle correspondant à la durée d'utilité administrative (DUA)/Durée de rétention
 STO        StorageRule                    Règle de Stockage
 DIS        DisseminationRule              Règle de diffusion
 REU        ReuseRule                      Règle d'utilisation
 CLASS      ClassificationRule             Règle de classification
 ========= ============================== ===============================

 Valeurs possibles pour le champ Status de la collection AccessionRegisterDetail
 -------------------------------------------------------------------------------

========================================== ======================
Status type                                Valeur
----------------------------------------- ----------------------
Le fonds est complet sauvegardé            STORED_AND_COMPLETED
Le fonds est mis à jour est sauvegardé     STORED_AND_UPDATED
Le fonds n'est pas sauvagerdé              UNSTORED
========================================== ======================
