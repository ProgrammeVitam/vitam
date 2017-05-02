Modèle de données Vitam
#######################

Objectif du document
====================

Ce document a pour objectif de présenter la structure générale des collections utilisées dans la solution logicielle Vitam.
Il est destiné principalement aux développeurs, afin de leur présenter la vision cible Vitam, ainsi qu'à tous les autres acteurs du programme pour leur permettre de connaître ce qui existe en l'état actuel.

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
- Sécurisation (développé en bêta)

Exemple de JSON stocké en base
------------------------------

JSON correspondant à une opération d'entrée terminée avec succès.

::

{
  "_id": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "evType": "PROCESS_SIP_UNITARY",
  "evDateTime": "2017-04-06T23:12:09.233",
  "evDetData": "{\"evDetDataType\":\"MASTER\",\"EvDetailReq\":\"Jeu de test avec arborescence complexe\",\"EvDateTimeReq\":\"2016-11-22T13:50:57\",\"ArchivalAgreement\":\"ArchivalAgreement0\",\"AgIfTrans\":\"Identifier5\"}",
  "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "evTypeProc": "INGEST",
  "outcome": "STARTED",
  "outDetail": null,
  "outMessg": "Début du processus d'entrée du SIP : aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
  "agIdApp": null,
  "agIdAppSession": null,
  "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "agIdSubm": null,
  "agIdOrig": null,
  "obId": null,
  "obIdReq": null,
  "obIdIn": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
  "events": [
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "STP_SANITY_CHECK_SIP",
          "evDateTime": "2017-04-06T23:12:09.234",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": null,
          "outMessg": "Début du processus des contrôles préalables à l'entrée",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "SANITY_CHECK_SIP",
          "evDateTime": "2017-04-06T23:12:09.980",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": null,
          "outMessg": "Début du contrôle sanitaire",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "SANITY_CHECK_SIP",
          "evDateTime": "2017-04-06T23:12:10.011",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": null,
          "outMessg": "Succès du contrôle sanitaire : aucun virus détecté",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "CHECK_CONTAINER",
          "evDateTime": "2017-04-06T23:12:10.012",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": null,
          "outMessg": "Début du contrôle de format du conteneur du SIP",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "CHECK_CONTAINER",
          "evDateTime": "2017-04-06T23:12:10.106",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": null,
          "outMessg": "Succès du contrôle de format du conteneur du SIP",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "STP_SANITY_CHECK_SIP",
          "evDateTime": "2017-04-06T23:12:10.106",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": null,
          "outMessg": "Succès du processus des contrôles préalables à l'entrée",
          "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "STP_UPLOAD_SIP",
          "evDateTime": "2017-04-06T23:12:09.408",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "PROCESS_SIP_UNITARY.STARTED",
          "outMessg": "Début du processus de téléchargement du SIP",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "STP_UPLOAD_SIP",
          "evDateTime": "2017-04-06T23:12:09.545",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "PROCESS_SIP_UNITARY.OK",
          "outMessg": "Succès du processus de téléchargement du SIP",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfbxwiaaaaq",
          "evType": "STP_INGEST_CONTROL_SIP",
          "evDateTime": "2017-04-06T23:12:10.713",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_INGEST_CONTROL_SIP.STARTED",
          "outMessg": "Début du processus du contrôle du bordereau",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aiaaaaq",
          "evType": "CHECK_SEDA",
          "evDateTime": "2017-04-06T23:12:11.265",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_SEDA.STARTED",
          "outMessg": "Début de la vérification globale du SIP",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aiaaaba",
          "evType": "CHECK_SEDA",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_SEDA.OK",
          "outMessg": "Succès de la vérification globale du SIP Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaaaq",
          "evType": "CHECK_MANIFEST_DATAOBJECT_VERSION",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_MANIFEST_DATAOBJECT_VERSION.STARTED",
          "outMessg": "Début de la vérification des usages des groupes d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaaba",
          "evType": "CHECK_MANIFEST_DATAOBJECT_VERSION",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_MANIFEST_DATAOBJECT_VERSION.OK",
          "outMessg": "Succès de la vérification des usages des groupes d'objets Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaabq",
          "evType": "CHECK_MANIFEST_OBJECTNUMBER",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_MANIFEST_OBJECTNUMBER.STARTED",
          "outMessg": "Début de la vérification du nombre d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaaca",
          "evType": "CHECK_MANIFEST_OBJECTNUMBER",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_MANIFEST_OBJECTNUMBER.OK",
          "outMessg": "Succès de la vérification du nombre d'objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaacq",
          "evType": "CHECK_MANIFEST",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_MANIFEST.STARTED",
          "outMessg": "Début du contrôle de cohérence du bordereau",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaada",
          "evType": "CHECK_MANIFEST",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": "{\"evDetDataType\":\"MASTER\",\"EvDetailReq\":\"Jeu de test avec arborescence complexe\",\"EvDateTimeReq\":\"2016-11-22T13:50:57\",\"ArchivalAgreement\":\"ArchivalAgreement0\",\"AgIfTrans\":\"Identifier5\"}",
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_MANIFEST.OK",
          "outMessg": "Succès du contrôle de cohérence du bordereau Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaadq",
          "evType": "CHECK_CONTRACT_INGEST",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_CONTRACT_INGEST.STARTED",
          "outMessg": "Début du contrôle de la validité du contrat d'entrée",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaaea",
          "evType": "CHECK_CONTRACT_INGEST",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_CONTRACT_INGEST.OK",
          "outMessg": "Succès du contrôle de la validité du contrat d'entrée Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaaeq",
          "evType": "CHECK_CONSISTENCY",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_CONSISTENCY.STARTED",
          "outMessg": "Début de la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaafa",
          "evType": "CHECK_CONSISTENCY",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_CONSISTENCY.OK",
          "outMessg": "Succès de la vérification de la cohérence entre objets, groupes d'objets et unités archivistiques Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb4aqaaafq",
          "evType": "STP_INGEST_CONTROL_SIP",
          "evDateTime": "2017-04-06T23:12:11.266",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_INGEST_CONTROL_SIP.OK",
          "outMessg": "Succès du processus du contrôle du bordereau",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfb5iaaaaaq",
          "evType": "STP_OG_CHECK_AND_TRANSFORME",
          "evDateTime": "2017-04-06T23:12:11.424",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_OG_CHECK_AND_TRANSFORME.STARTED",
          "outMessg": "Début du processus de vérification et de traitement des objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcbyyaaaaq",
          "evType": "CHECK_DIGEST",
          "evDateTime": "2017-04-06T23:12:12.003",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "CHECK_DIGEST.STARTED",
          "outMessg": "Début de la vérification de l'intégrité des objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcbyyaaaba",
          "evType": "CHECK_DIGEST",
          "evDateTime": "2017-04-06T23:12:12.003",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "CHECK_DIGEST.OK",
          "outMessg": "Succès de la vérification de l'intégrité des objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcbyyaaabq",
          "evType": "OG_OBJECTS_FORMAT_CHECK",
          "evDateTime": "2017-04-06T23:12:12.003",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "OG_OBJECTS_FORMAT_CHECK.STARTED",
          "outMessg": "Début de la vérification des formats",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcbyyaaaca",
          "evType": "OG_OBJECTS_FORMAT_CHECK",
          "evDateTime": "2017-04-06T23:12:12.004",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "OG_OBJECTS_FORMAT_CHECK.OK",
          "outMessg": "Succès de la vérification des formats Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcbzaaaaaq",
          "evType": "STP_OG_CHECK_AND_TRANSFORME",
          "evDateTime": "2017-04-06T23:12:12.004",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_OG_CHECK_AND_TRANSFORME.OK",
          "outMessg": "Succès de l'étape de vérification et de traitement des objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcciyaaaaq",
          "evType": "STP_UNIT_CHECK_AND_PROCESS",
          "evDateTime": "2017-04-06T23:12:12.067",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_UNIT_CHECK_AND_PROCESS.STARTED",
          "outMessg": "Début du processus de contrôle et traitements des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcf2yaaaaq",
          "evType": "UNITS_RULES_COMPUTE",
          "evDateTime": "2017-04-06T23:12:12.523",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "UNITS_RULES_COMPUTE.STARTED",
          "outMessg": "Début du calcul des dates d'échéance",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcf2yaaaba",
          "evType": "UNITS_RULES_COMPUTE",
          "evDateTime": "2017-04-06T23:12:12.523",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "UNITS_RULES_COMPUTE.OK",
          "outMessg": "Succès du calcul des dates d'échéance Detail=  OK:5",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcf2yaaabq",
          "evType": "STP_UNIT_CHECK_AND_PROCESS",
          "evDateTime": "2017-04-06T23:12:12.523",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_UNIT_CHECK_AND_PROCESS.OK",
          "outMessg": "Succès du processus de contrôle et traitements des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfcgoqaaaaq",
          "evType": "STP_STORAGE_AVAILABILITY_CHECK",
          "evDateTime": "2017-04-06T23:12:12.602",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_STORAGE_AVAILABILITY_CHECK.STARTED",
          "outMessg": "Début du processus de vérification préalable à la prise en charge",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfc3vaaaaaq",
          "evType": "STORAGE_AVAILABILITY_CHECK",
          "evDateTime": "2017-04-06T23:12:15.317",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STORAGE_AVAILABILITY_CHECK.STARTED",
          "outMessg": "Début de la vérification de la disponibilité de l'offre de stockage",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfc3viaaaaq",
          "evType": "STORAGE_AVAILABILITY_CHECK",
          "evDateTime": "2017-04-06T23:12:15.317",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STORAGE_AVAILABILITY_CHECK.OK",
          "outMessg": "Succès de la vérification de la disponibilité de l'offre de stockage Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfc3viaaaba",
          "evType": "STP_STORAGE_AVAILABILITY_CHECK",
          "evDateTime": "2017-04-06T23:12:15.317",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_STORAGE_AVAILABILITY_CHECK.OK",
          "outMessg": "Succès du processus de vérification préalable à la prise en charge",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfc4liaaaaq",
          "evType": "STP_OG_STORING",
          "evDateTime": "2017-04-06T23:12:15.405",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_OG_STORING.STARTED",
          "outMessg": "Début du processus de rangement des objets et groupes d'objets sur l'offre de stockage",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaaaq",
          "evType": "OG_STORAGE",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "OG_STORAGE.STARTED",
          "outMessg": "Début du rangement des objets et groupes d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaaba",
          "evType": "OG_STORAGE",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "OG_STORAGE.OK",
          "outMessg": "Succès du rangement des objets et groupes d'objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaabq",
          "evType": "OG_METADATA_INDEXATION",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "OG_METADATA_INDEXATION.STARTED",
          "outMessg": "Début de l'indexation des métadonnées des objets et groupes d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaaca",
          "evType": "OG_METADATA_INDEXATION",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "OG_METADATA_INDEXATION.OK",
          "outMessg": "Succès de l'indexation des métadonnées des objets et groupes d'objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaacq",
          "evType": "OG_METADATA_STORAGE",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "OG_METADATA_STORAGE.STARTED",
          "outMessg": "Début de l' enregistrement des métadonnées des groupes d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaada",
          "evType": "OG_METADATA_STORAGE",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "OG_METADATA_STORAGE.OK",
          "outMessg": "Succès de l' enregistrement des métadonnées des groupes d'objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaadq",
          "evType": "COMMIT_LIFE_CYCLE_OBJECT_GROUP",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "COMMIT_LIFE_CYCLE_OBJECT_GROUP.STARTED",
          "outMessg": "Début de la sécurisation des journaux du cycle de vie des groupes d'objets",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaaea",
          "evType": "COMMIT_LIFE_CYCLE_OBJECT_GROUP",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "COMMIT_LIFE_CYCLE_OBJECT_GROUP.OK",
          "outMessg": "Succès de la sécurisation des journaux du cycle de vie des groupes d'objets Detail=  OK:2",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdvxiaaaeq",
          "evType": "STP_OG_STORING",
          "evDateTime": "2017-04-06T23:12:18.653",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_OG_STORING.OK",
          "outMessg": "Succès du processus de rangement des objets et groupes d'objets sur l'offre de stockage",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfdwwiaaaaq",
          "evType": "STP_UNIT_STORING",
          "evDateTime": "2017-04-06T23:12:18.777",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_UNIT_STORING.STARTED",
          "outMessg": "Début du processus de rangement des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaaaq",
          "evType": "UNIT_METADATA_INDEXATION",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "UNIT_METADATA_INDEXATION.STARTED",
          "outMessg": "Début de l'indexation des métadonnées de l'unité archivistique",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaaba",
          "evType": "UNIT_METADATA_INDEXATION",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "UNIT_METADATA_INDEXATION.OK",
          "outMessg": "Succès de l'indexation des métadonnées de l'unité archivistique Detail=  OK:5",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaabq",
          "evType": "UNIT_METADATA_STORAGE",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "UNIT_METADATA_STORAGE.STARTED",
          "outMessg": "Début de l'enregistrement des métadonnées des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaaca",
          "evType": "UNIT_METADATA_STORAGE",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "UNIT_METADATA_STORAGE.OK",
          "outMessg": "Succès de l'enregistrement des métadonnées des unités archivistiques Detail=  OK:5",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaacq",
          "evType": "COMMIT_LIFE_CYCLE_UNIT",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "COMMIT_LIFE_CYCLE_UNIT.STARTED",
          "outMessg": "Début de la sécurisation du journal du cycle de vie des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaada",
          "evType": "COMMIT_LIFE_CYCLE_UNIT",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "COMMIT_LIFE_CYCLE_UNIT.OK",
          "outMessg": "Succès de la sécurisation du journal du cycle de vie des unités archivistiques Detail=  OK:5",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfetkqaaadq",
          "evType": "STP_UNIT_STORING",
          "evDateTime": "2017-04-06T23:12:22.442",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_UNIT_STORING.OK",
          "outMessg": "Succès du processus de rangement des unités archivistiques",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfeufaaaaaq",
          "evType": "STP_ACCESSION_REGISTRATION",
          "evDateTime": "2017-04-06T23:12:22.548",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_ACCESSION_REGISTRATION.STARTED",
          "outMessg": "Début du processus d'alimentation du registre des fonds",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfewhyaaaaq",
          "evType": "ACCESSION_REGISTRATION",
          "evDateTime": "2017-04-06T23:12:22.815",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "ACCESSION_REGISTRATION.STARTED",
          "outMessg": "Début de l'alimentation du registre des fonds",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfewhyaaaba",
          "evType": "ACCESSION_REGISTRATION",
          "evDateTime": "2017-04-06T23:12:22.815",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "ACCESSION_REGISTRATION.OK",
          "outMessg": "Succès de l'alimentation du registre des fonds Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfewhyaaabq",
          "evType": "STP_ACCESSION_REGISTRATION",
          "evDateTime": "2017-04-06T23:12:22.815",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_ACCESSION_REGISTRATION.OK",
          "outMessg": "Succès du processus d'alimentation du registre des fonds",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfexaiaaaaq",
          "evType": "STP_INGEST_FINALISATION",
          "evDateTime": "2017-04-06T23:12:22.913",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "STP_INGEST_FINALISATION.STARTED",
          "outMessg": "Début du processus de finalisation de l'entrée",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfe6tiaaaaq",
          "evType": "ATR_NOTIFICATION",
          "evDateTime": "2017-04-06T23:12:23.885",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "ATR_NOTIFICATION.STARTED",
          "outMessg": "Début de la notification de la fin de l'opération à l'opérateur de versement",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfe6tiaaaba",
          "evType": "ATR_NOTIFICATION",
          "evDateTime": "2017-04-06T23:12:23.885",
          "evDetData": "{\"FileName\":\"ATR_aedqaaaaache45hwaantmak3iwfbl6qaaaaq\", \"MessageDigest\": \"1fd78993b117d880dd59205b8ce39314e0aa8ea703f21ba0d23b3e6deae49015a013cc6285ed73d7bc8fe1fcfe760becde4dce48d950d3d44a1ec59bcf486a86\", \"Algorithm\": \"SHA512\"}",
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "ATR_NOTIFICATION.OK",
          "outMessg": "Succès de la notification de la fin de l'opération à l'opérateur de versement Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfe6tiaaabq",
          "evType": "ROLL_BACK",
          "evDateTime": "2017-04-06T23:12:23.885",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "ROLL_BACK.STARTED",
          "outMessg": "Début de la mise en cohérence des journaux du cycle de vie",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfe6tiaaaca",
          "evType": "ROLL_BACK",
          "evDateTime": "2017-04-06T23:12:23.885",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "ROLL_BACK.OK",
          "outMessg": "Succès de la mise en cohérence des journaux du cycle de vie Detail=  OK:1",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": null,
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaachgxr27aa73iak3iwfe6tiaaacq",
          "evType": "STP_INGEST_FINALISATION",
          "evDateTime": "2017-04-06T23:12:23.885",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "STP_INGEST_FINALISATION.OK",
          "outMessg": "Succès du processus de finalisation de l'entrée",
          "agId": "{\"Name\":\"vitam-iaas-app-03\",\"Role\":\"processing\",\"ServerId\":1047250783,\"SiteId\":1,\"GlobalPlatformId\":241944415}",
          "agIdApp": null,
          "agIdAppSession": null,
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "agIdSubm": null,
          "agIdOrig": null,
          "obId": null,
          "obIdReq": null,
          "obIdIn": "Jeu de test avec arborescence complexe",
          "_tenant": 0
      },
      {
          "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evType": "PROCESS_SIP_UNITARY",
          "evDateTime": "2017-04-06T23:12:22.935",
          "evDetData": null,
          "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "PROCESS_SIP_UNITARY.OK",
          "outMessg": "Entrée effectuée avec succès",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"ingest-internal\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
          "obId": null,
          "obIdReq": null,
          "obIdIn": null
      }
  ],
  "_tenant": 0
}

Détail des champs du JSON stocké en base
---------------------------------------

Chaque entrée de cette collection est composée d'une structure auto-imbriquée : la structure possède une première instanciation "incluante", et contient un tableau de N structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.


"_id" : Identifiant unique donné par le système lors de l'initialisation de l'opération
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire de l'opération dans la collection.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
     Il identifie l'entrée / le versement de manière unique dans la base.
     Cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par pair (début/fin).

     *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    Issu de la définition du workflow en json (fichier default-workflow.json).
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement.
    Par exemple, pour l'étape ATR_NOTIFICATION, ce champ détaille le nom de l'ATR, son empreinte et l'algorithme utilisé pour calculer l'empreinte.

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"agIdApp" (agent Identifier Application) : identifiant de l’application externe qui appelle Vitam pour effectuer l'opération

    *Utilisation à IT10 : la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe uniquement pour la structure incluante.*

"agIdAppSession" (agent Identifier Application Session) : identifiant donnée par l’application utilisatrice externe
    qui appelle Vitam à la session utilisée pour lancer l’opération
    L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.

    *Utilisation à IT10 : la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe pour les structures incluantes et incluses*

"evIdReq" (event Identifier Request) : identifiant de la requête déclenchant l’opération
    Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).

    *Ce champ existe pour les structures incluantes et incluses*

"agIdSubm" (agent Identifier Submission) : identifiant du service versant.
    Il s'agit du <SubmissionAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"agIdOrig" (agent Identifier Originating) : identifiant du service producteur.
    Il s'agit du <OriginatingAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
     Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc). Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini

     *Ce champ existe pour les structures incluantes et incluses*

"obIdReq" (object Identifier Request) : Identifiant de la requête caractérisant un lot d’objets auquel s’applique l’opération.
      Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.

      *Utilisation à IT10 : la valeur est toujours 'null'. Ce champ existe pour les structures incluantes et incluses*

"obIdIn" (ObjectIdentifierIncome) : Identifiant externe du lot d’objets auquel s’applique l’opération.
      Chaîne de caractère intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se reporte l'événement.
      Il s'agit le plus souvent soit du nom du SIP lui-même, soit du <MessageIdentifier> présent dans le manifeste.

      *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
      Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

      *Ce champ existe uniquement pour la structure incluante.*

"_tenant": identifiant du tenant
      *Ce champ existe uniquement pour la structure incluante.*

Détail des champs du JSON stocké en base spécifiques à une opération de Sécurisation
-----------------------------------------------------------------------------------

Exemple de données stockées :

::

  "evDetData":
  "{
  \"LogType\": \"operation\",
  \"StartDate\": \"2017-02-27T00:00:00.000\",
  \"EndDate\": \"2017-02-27T14:11:36.168\",
  \"PreviousLogbookTraceabilityDate\": \"2017-02-26T00:00:00.000\",
  \"MinusOneMonthLogbookTraceabilityDate\": \"2017-01-28T00:00:00.000\",
  \"MinusOneYearLogbookTraceabilityDate\": \"2016-02-28T00:00:00.000\",
  \"Hash\": \"cmKHRqv1HHB+Fd0JErOpztcdcV3BGlgcA0VAYxFjxjdEJO0+lOhhxNeK43mbrmgra6phNSuKBfVIXOE5i4877Q==\",
  \"TimeStampToken\": \"MIIEezAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEYAYJKoZIhvcNAQcCoIIEUTCCBE0CAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARAiTJZ9fQyplZfbRHe7j34JFw1iQlJMmwEn5\/oa9hha3oeJ7b7A+I0MOiz8n3lhajK5GWDMptybTI\/qyydRxRwqAIBARgPMjAxNzAxMjcxNDExMzdaMYIDsjCCA64CAQEwYzBdMQswCQYDVQQGEwJGUjEMMAoGA1UECBMDaWRmMQ4wDAYDVQQHEwVwYXJpczEPMA0GA1UEChMGVml0YW0uMR8wHQYDVQQDFBZDQV9zZXJ2ZXJfaW50ZXJtZWRpYXRlAgIAsDANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzAxMjcxNDExMzdaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQMa0fzRWvY0qJjOO4lO5aSfN3iW9xWwhSv24QSExqpp081WszJ0NIEP4gFOzAQIrE35Bz\/jgACNxVS8XXRda7\/AwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQAkVA\/7GPyjlbJC2NJJK+1ZY6k2vvEQls\/YcVrP9SV81nRL7fmrSw0mmia0Dj+kuu+qAun5hB6X9pzy4lbATsfEwDQYJKoZIhvcNAQENBQAEggIAgMAyrR6uTJYHxKqofV+HnPV+9fiykPb4DwNTWYKGEBOlu44yVfzep1P2GofDVBBguYQZHF0zCQ0vjktfGuVflh4GtiHsbhqKm6TMqeH+pdRv0MQvEYA3VK0ydA+\/36xb+tbOy8RBqUe3uXGpaafuqcrmlx0EYK4ey4I4sinvZKoB9c9kNCujlvpLxwPnL8teDe6\/jE4sWqvCHCSxorjXCXDN6aJTGvbFHepqa987eHRckDS5pdTiZ1a7V1IRjsX+bubA+ZYhWM5sA9L202msa8s\/zF5Nn+mmcApzpjiAkHu5u8QGuIe17jgHV0o73Zkv3Oranskz3Q3F3xXdNT8wblevU4mWFGQkW5wWhyyTfEKE97+z7+HTa5P4eLCEZkAgevkZPMo21PyEvNBUeXM3QIzfOKExX+wYpuL9k2\/5kg3ZmX3dMT1jxhZAr75puxp5pxOryuR+j0JFmeA8JI8a+XYsYZm75lV4uzSYl4QytMwNaSyxDwC4PBmZ9IGbPwRP8ttC8LSjeB+zwQug063kT0ZKmkCHzbZvVWHJlr3Iaew2UXjOabrWNIEijg6b6DBtze7sC9T8LXGHOlcAFFsW0kYfHb7MziVv22CCuUw4JyI5882I\/huPztjJqn+4bwzmAuWc8X\/OiyAbe2Iag23oaVJ36UU3QxzDLPhCg0TvNZg=\",
  \"NumberOfElement\": 366,
  \"Size\": 2554545,
  \"FileName\": \"0_LogbookOperation_20170127_141136.zip\"
  }"

Dans le cas d'un évènement final d'une opération de sécurisation du LogbookOperation, le champ **"evDetData"** est composé des champs suivants :

"LogType": type de logbook sécurisé.
      Type de la collection logbook sécurisée (LogbookOperation)
      ``Exemple : "operation"``

"StartDate": date de début.
      Date de début de la période de couverture de l'opération de sécurisation au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée par la précédente sécurisation)
      ``Exemple : "2016-08-17T08:26:04.227"``

"EndDate": date de fin.
      Date de fin de la période de couverture de l'opération de sécurisation  au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée)
      ``Exemple : "2016-08-17T08:26:04.227"``

"PreviousLogbookTraceabilityDate": date de la précédente sécurisation.
      Date de début de la précédente sécurisation du même type au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation précédente)
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneMonthLogbookTraceabilityDate": date de la sécurisation passée d'un mois.
      Date de début de la sécurisation un mois avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation passée d'un mois : logbook start 1 mois avant - logbookDate.mois(-1).suivant().sartDate)
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneMonthLogbookTraceabilityDate": date de la sécurisation passée d'un an.
     Date de début de la sécurisation un an avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation passée d'un an : logbook start 1 an avant - logbookDate.an(-1).suivant().sartDate)
     ``Exemple : "2016-08-17T08:26:04.227"``

"Hash": Empreinte racine.
      Empreinte de la racine de l'arbre de Merkle.

"TimeStampToken": Tampon d’horodatage.
      Tampon d’horodatage sûr du journal sécurisé.

"NumberOfElement": Nombre d'élèments.
      Nombre d'opérations sécurisées.

"Size": Taille du fichier.
      Taille du fichier sécurisé (en bytes).

"FileName": Identifiant du fichier.
      Nom du fichier sécurisé dans le stockage au format {tenant}_LogbookOperation_{AAAAMMJJ_HHMMSS}.zip.
      ``Exemple : "0_LogbookOperation_20170127_141136.zip"``


Collection LogbookLifeCycleUnit
===============================

Utilisation de la collection LogbookLifeCycleUnit
-------------------------------------------------

Le journal du cycle de vie d'une unité archivistique (ArchiveUnit) trace tous les événements qui impactent celle-ci dès sa prise en charge dans le système. Il doit être conservé aussi longtemps qu'elle est gérée par le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les ArchiveUnit qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des métadonnées OK, avant notification au service versant

Chaque unité archivistique possède une et une seule entrée dans sa collection LogbookLifeCycleUnit.

Exemple de JSON stocké en base
------------------------------

::

{
  "_id": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
  "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
  "evType": "LFC.LFC_CREATION",
  "evDateTime": "2017-04-10T12:39:37.933",
  "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
  "evTypeProc": "INGEST",
  "outcome": "STARTED",
  "outDetail": "LFC.LFC_CREATION.STARTED",
  "outMessg": "!LFC.LFC_CREATION.STARTED!",
  "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
  "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
  "evDetData": null,
  "events": [
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
          "evType": "LFC.CHECK_MANIFEST",
          "evDateTime": "2017-04-10T12:39:37.953",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "LFC.CHECK_MANIFEST.STARTED",
          "outMessg": "Début de la vérification de la cohérence du bordereau",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
          "evType": "LFC.CHECK_MANIFEST.LFC_CREATION",
          "evDateTime": "2017-04-10T12:39:37.953",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "LFC.CHECK_MANIFEST.LFC_CREATION.OK",
          "outMessg": "Succès de la création du journal du cycle de vie",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
          "evType": "LFC.CHECK_MANIFEST",
          "evDateTime": "2017-04-10T12:39:37.953",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "LFC.CHECK_MANIFEST.OK",
          "outMessg": "Succès de la vérification de la cohérence du bordereau",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhc5qaaaaq",
          "evType": "LFC.UNITS_RULES_COMPUTE",
          "evDateTime": "2017-04-10T12:39:38.614",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "LFC.UNITS_RULES_COMPUTE.STARTED",
          "outMessg": "Début du calcul des dates d'échéance",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhc5qaaaaq",
          "evType": "LFC.UNITS_RULES_COMPUTE",
          "evDateTime": "2017-04-10T12:39:38.661",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "LFC.UNITS_RULES_COMPUTE.OK",
          "outMessg": "Succès du calcul des dates d'échéance",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhuvaaaaaq",
          "evType": "LFC.UNIT_METADATA_INDEXATION",
          "evDateTime": "2017-04-10T12:39:40.884",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "LFC.UNIT_METADATA_INDEXATION.STARTED",
          "outMessg": "Début de l'indexation des métadonnées de l'unité archivistique",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhuvaaaaaq",
          "evType": "LFC.UNIT_METADATA_INDEXATION",
          "evDateTime": "2017-04-10T12:39:40.945",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "LFC.UNIT_METADATA_INDEXATION.OK",
          "outMessg": "Succès de l'indexation de l'unité archivistique",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhvfqaaaaq",
          "evType": "LFC.UNIT_METADATA_STORAGE",
          "evDateTime": "2017-04-10T12:39:40.950",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "STARTED",
          "outDetail": "LFC.UNIT_METADATA_STORAGE.STARTED",
          "outMessg": "Début de l'enregistrement des métadonnées de l'unité archivistique",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      },
      {
          "evId": "aedqaaaaaghbl62nabqkwak3k7qhvfqaaaaq",
          "evType": "LFC.UNIT_METADATA_STORAGE",
          "evDateTime": "2017-04-10T12:39:41.145",
          "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
          "evTypeProc": "INGEST",
          "outcome": "OK",
          "outDetail": "LFC.UNIT_METADATA_STORAGE.OK",
          "outMessg": "Succès de l'enregistrement des métadonnées de l'unité archivistique",
          "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
          "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
          "evDetData": null,
          "_tenant": 1
      }
  ],
  "_tenant": 1
}

Exemple avec une mise à jour de métadonnées

::

 {
   "_id": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
   "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
   "evType": "LFC.LFC_CREATION",
   "evDateTime": "2017-04-10T12:52:50.173",
   "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
   "evTypeProc": "INGEST",
   "outcome": "STARTED",
   "outDetail": "LFC.LFC_CREATION.STARTED",
   "outMessg": "!LFC.LFC_CREATION.STARTED!",
   "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
   "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
   "evDetData": null,
   "events": [
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
           "evType": "LFC.CHECK_MANIFEST",
           "evDateTime": "2017-04-10T12:52:50.205",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "STARTED",
           "outDetail": "LFC.CHECK_MANIFEST.STARTED",
           "outMessg": "Début de la vérification de la cohérence du bordereau",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
           "evType": "LFC.CHECK_MANIFEST.LFC_CREATION",
           "evDateTime": "2017-04-10T12:52:50.205",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.CHECK_MANIFEST.LFC_CREATION.OK",
           "outMessg": "Succès de la création du journal du cycle de vie",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
           "evType": "LFC.CHECK_MANIFEST",
           "evDateTime": "2017-04-10T12:52:50.205",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.CHECK_MANIFEST.OK",
           "outMessg": "Succès de la vérification de la cohérence du bordereau",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wir6iaaaaq",
           "evType": "LFC.UNITS_RULES_COMPUTE",
           "evDateTime": "2017-04-10T12:52:51.065",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "STARTED",
           "outDetail": "LFC.UNITS_RULES_COMPUTE.STARTED",
           "outMessg": "Début du calcul des dates d'échéance",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wir6iaaaaq",
           "evType": "LFC.UNITS_RULES_COMPUTE",
           "evDateTime": "2017-04-10T12:52:51.089",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.UNITS_RULES_COMPUTE.OK",
           "outMessg": "Succès du calcul des dates d'échéance",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wji3aaaaaq",
           "evType": "LFC.UNIT_METADATA_INDEXATION",
           "evDateTime": "2017-04-10T12:52:53.996",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "STARTED",
           "outDetail": "LFC.UNIT_METADATA_INDEXATION.STARTED",
           "outMessg": "Début de l'indexation des métadonnées de l'unité archivistique",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wji3aaaaaq",
           "evType": "LFC.UNIT_METADATA_INDEXATION",
           "evDateTime": "2017-04-10T12:52:54.064",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.UNIT_METADATA_INDEXATION.OK",
           "outMessg": "Succès de l'indexation de l'unité archivistique",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wjjniaaaaq",
           "evType": "LFC.UNIT_METADATA_STORAGE",
           "evDateTime": "2017-04-10T12:52:54.069",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "STARTED",
           "outDetail": "LFC.UNIT_METADATA_STORAGE.STARTED",
           "outMessg": "Début de l'enregistrement des métadonnées de l'unité archivistique",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wjjniaaaaq",
           "evType": "LFC.UNIT_METADATA_STORAGE",
           "evDateTime": "2017-04-10T12:52:54.301",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.UNIT_METADATA_STORAGE.OK",
           "outMessg": "Succès de l'enregistrement des métadonnées de l'unité archivistique",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaache45hwab3paak3k7x6zuaaaaaq",
           "evType": "LFC.UNIT_METADATA_UPDATE",
           "evDateTime": "2017-04-10T12:56:33.232",
           "evIdProc": "aecaaaaaache45hwab3paak3k7x6y7qaaaaq",
           "evTypeProc": "UPDATE",
           "outcome": "STARTED",
           "outDetail": "LFC.UNIT_METADATA_UPDATE.STARTED",
           "outMessg": "Début de la mise à jour des métadonnées de l'unité archivistique aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"access-internal\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaache45hwab3paak3k7x62zqaaaaq",
           "evType": "LFC.UNIT_METADATA_UPDATE",
           "evDateTime": "2017-04-10T12:56:33.382",
           "evIdProc": "aecaaaaaache45hwab3paak3k7x6y7qaaaaq",
           "evTypeProc": "UPDATE",
           "outcome": "OK",
           "outDetail": "LFC.UNIT_METADATA_UPDATE.OK",
           "outMessg": "Succès de la mise à jour des métadonnées de l'unité archivistique aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"access-internal\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": "{\n  \"diff\" : \"+  Description : Lorem ipsum\\n-  #operations : [ aedqaaaaache45hwabliwak3k7wim4qaaaaq \\n+  #operations : [ aedqaaaaache45hwabliwak3k7wim4qaaaaq, aecaaaaaache45hwab3paak3k7x6y7qaaaaq \"\n}",
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaache45hwab3paak3k7x63fyaaaaq",
           "evType": "LFC.UNIT_METADATA_STORAGE",
           "evDateTime": "2017-04-10T12:56:33.432",
           "evIdProc": "aecaaaaaache45hwab3paak3k7x6y7qaaaaq",
           "evTypeProc": "UPDATE",
           "outcome": "STARTED",
           "outDetail": "LFC.UNIT_METADATA_STORAGE.STARTED",
           "outMessg": "Début de l'enregistrement des métadonnées de l'unité archivistiqueaeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"access-internal\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaache45hwab3paak3k7x66cqaaaaq",
           "evType": "LFC.UNIT_METADATA_STORAGE",
           "evDateTime": "2017-04-10T12:56:33.802",
           "evIdProc": "aecaaaaaache45hwab3paak3k7x6y7qaaaaq",
           "evTypeProc": "UPDATE",
           "outcome": "OK",
           "outDetail": "LFC.UNIT_METADATA_STORAGE.OK",
           "outMessg": "Succès de l'enregistrement des métadonnées de l'unité archivistiqueaeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"access-internal\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": "{\n  \"diff\" : \"+  Description : Lorem ipsum\\n-  #operations : [ aedqaaaaache45hwabliwak3k7wim4qaaaaq \\n+  #operations : [ aedqaaaaache45hwabliwak3k7wim4qaaaaq, aecaaaaaache45hwab3paak3k7x6y7qaaaaq \"\n}",
           "_tenant": 0
       }
   ],
   "_tenant": 0
}

Détail des champs du JSON stocké en base
---------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal du cycle de vie.
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal du cycle de vie de l'unité archivistique.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possible fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
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



Détail des champs du JSON stocké en base spécifiques à une mise à jour
---------------------------------------------------------------------

Exemple de données stockées :

::

   "evDetData": "{\"diff\":\"-  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS)\\n+  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS) 222\\n-  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq \\n+  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq, aecaaaaaacaam7mxabjssak2dzsjniyaaaaq \"}"


Dans le cas d'une mise à jour de métadonnées d'une unité archivistique (ArchiveUnit), le champ **"evDetData"** de l'évènement final est composé des champs suivants :

"diff": historisation des modifications de métadonnées.
    Son contenu doit respecter la forme suivante : les anciennes valeurs sont précédées d'un "-" (``-champ1: valeur1``) et les nouvelles valeurs sont précédées d'un "+" (``+champ1: valeur2``)

    ``Exemple :
    -Titre: Discours du Roi \n+Titre: Discours du Roi Louis XVI \n-Description: Etat Généraux du 5 mai 1789 \n+Description: Etat Généraux du 5 mai 1789 au Château de Versailles``


Collection LogbookLifeCycleObjectGroup
======================================

Utilisation de la collection LogbookLifeCycleObjectGroup
--------------------------------------------------------

Le journal du cycle de vie du groupe d'objets (ObjectGroup) trace tous les événements qui impactent le groupe d'objets (et les objets associés) dès sa prise en charge dans le système et doit être conservé aussi longtemps que les objets sont gérés dans le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les groupes d'objets et objets qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des MD OK, avant notification au service versant

Chaque groupe d'objets possède une et une seule entrée dans sa collection LogbookLifeCycleObjectGroup.

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
            "evDetData": "{\"MessageDigest\":\"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc125905636
            5aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc125905636
            5aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\"} ",
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
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a2
            45b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\"} ",
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
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a
            245b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a2
            45b668914a3364ee0def01ef8719eed5488e0e21020e\"} ",
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
            "evDetData": "{\"MessageDigest\":\"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a2
            45b668914a3364ee0def01ef8719eed5488e0e21020e\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"a3077c531007f1ec5f8bc34bf4a7cf9c2c51ef83cb647cd5903d400bc1768b0fa0ca714e93be4bb9c5a
            245b668914a3364ee0def01ef8719eed5488e0e21020e\"} ",
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


Détail des champs du JSON stocké en base
---------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal du cycle de vie.
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal du cycle de vie du groupe d'objet.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    *Utilisation à IT10 : la valeur est toujours à 'null'. Il est censé être renseigné en IT11.*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
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
  "_id": "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaabq",
  "_og": "aebaaaaaaahbl62nabqkwak3k7zvpqaaaaba",
  "_mgt": {
      "StorageRule": [
          {
              "Rule": "R4",
              "StartDate": "9000-10-10",
              "FinalAction": "Copy",
              "EndDate": "9004-10-10"
          }
      ]
  },
  "DescriptionLevel": "Item",
  "Title": "AU4",
  "_ops": [
      "aedqaaaaache45hwabliwak3k7zvtfqaaaaq"
  ],
  "_tenant": 0,
  "_max": 4,
  "_min": 1,
  "_up": [
      "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaaba"
  ],
  "_nbc": 0,
  "_us": [
      "aeaqaaaaaahbl62nabqkwak3k7zvpuaaaaaq",
      "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaaaq",
      "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaaba"
  ],
  "_uds": [
      {
          "aeaqaaaaaahbl62nabqkwak3k7zvpuaaaaaq": 3
      },
      {
          "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaaaq": 2
      },
      {
          "aeaqaaaaaahbl62nabqkwak3k7zvpuiaaaba": 1
      }
  ]
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

"_id" (#id): Identifiant unique de l'unité archivistique.
    Chaîne de 36 caractères.

"DescriptionLevel": La valeur de champ est une chaine de caractères.
    Il s'agit du niveau de description archivistique de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> dans le manifeste.

"Title": La valeur de ce champ est une chaine de caractères. Il s'agit du titre de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le manifest.

"Description": La valeur contenue dans ce champ est une chaîne de caractères.
    Ce champ est renseigné avec les informations situées entre les balises <description> de l'archiveUnit concernée dans le manifest.

"XXXXX" : Des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le boredereau SEDA au niveau du Content de chaque unité archivistique.
    (CF SEDA 2.0 descriptive pour connaître la liste des métadonnées facultatives)

"_og" (#object): identifiant du groupe d'objets référencé dans cette unité archivistique
    Chaîne de 36 caractères.

"_ops" (#operations): tableau contenant les identifiants d'opérations auxquelles ce Unit a participé

"_tenant" (#tenant): il s'agit de l'identifiant du tenant

"_max" (ne devrait pas être visible): profondeur maximale de l'unité archivistique par rapport à une racine
    Calculé, cette profondeur est le maximum des profondeurs, quelles que soient les racines concernées et les chemins possibles

"_min" (ne devrait pas être visible): profondeur minimum de l'unité archivistique par rapport à une racine
    Calculé, symétriquement le minimum des profondeurs, quelles que soient les racines concernées et les chemins possibles ;

"_up" (#unitups): est un tableau qui recense les _id des unités archivistiques parentes (parents immédiats)

"_nbc" (#nbunits): nombre d'enfants immédiats de l'unité archivistique

"_uds" (ne devrait pas être visible): tableau contenant la parentalité, non indexé et pas dans Elasticseatch exemple { GUID1 : depth1, GUID2 : depth2, ... } ; chaque depthN indique la distance relative entre l'unité archivistique courante et l'unité archivistique parente dont le GUID est précisé.

"_us" (#allunitups): tableau contenant la parentalité, indexé [ GUID1, GUID2, ... }

"OriginatingAgency": { "OrganizationDescriptiveMetadata": Métadonnées de description concernant le service producteur }

_profil (#type): Type de document utilisé lors de l'entrée, correspond au ArchiveUnitProfile, le profil d'archivage utilisé lors de l'entrée

"_mgt" (#management): possède les balises reprises du bloc <Management> du bordereau (règles de gestion) pour cette unité archivistique ainsi que les dates d'échance calculées (endDate)

Collection ObjectGroup
======================

Utilisation de la collection ObjectGroup
----------------------------------------

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de Json stocké en base
------------------------------

::

{
 "_id": "aebaaaaaaahbl62nabejwak3lchc4baaaaaq",
 "_tenant": 0,
 "_profil": "Document",
 "FileInfo": {
     "Filename": "Vitam-Sensibilisation-API-V1.0.odp",
     "CreatingApplicationName": "LibreOffice/Impress",
     "CreatingApplicationVersion": "5.0.5.2",
     "CreatingOs": "Windows_X86_64",
     "CreatingOsVersion": 10,
     "LastModified": "2016-05-05T20:45:20"
 },
 "_qualifiers": {
     "BinaryMaster": {
         "_nbc": 1,
         "versions": [
             {
                 "_id": "aeaaaaaaaahbl62nabejwak3lchc4ayaaaaq",
                 "DataObjectGroupId": "aebaaaaaaahbl62nabejwak3lchc4baaaaaq",
                 "DataObjectVersion": "BinaryMaster_1",
                 "FormatIdentification": {
                     "FormatLitteral": "Acrobat PDF 1.4 - Portable Document Format",
                     "MimeType": "application/pdf",
                     "FormatId": "fmt/18"
                 },
                 "FileInfo": {
                     "Filename": "Vitam-Sensibilisation-API-V1.0.odp",
                     "CreatingApplicationName": "LibreOffice/Impress",
                     "CreatingApplicationVersion": "5.0.5.2",
                     "CreatingOs": "Windows_X86_64",
                     "CreatingOsVersion": 10,
                     "LastModified": "2016-05-05T20:45:20"
                 },
                 "Metadata": {
                     "Document": null
                 },
                 "OtherMetadata": null,
                 "Size": 29403,
                 "Uri": "content/MM4BBunhtApiMcDFGLfPPvtAK83qpz.pdf",
                 "MessageDigest": "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7",
                 "Algorithm": "SHA-512"
             }
         ]
     },
     "Dissemination": {
         "_nbc": 1,
         "versions": [
             {
                 "_id": "aeaaaaaaaahbl62nabejwak3lchc4diaaaaq",
                 "DataObjectGroupId": "aebaaaaaaahbl62nabejwak3lchc4baaaaaq",
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
                     "CreatingOsVersion": 10,
                     "LastModified": "2016-05-05T20:45:32"
                 },
                 "Metadata": {
                     "Document": null
                 },
                 "OtherMetadata": null,
                 "Size": 29403,
                 "Uri": "content/o3zT7xoCaeTVo1Em5d6FlAO1ZTclJT.pdf",
                 "MessageDigest": "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7",
                 "Algorithm": "SHA-512"
             }
         ]
     },
     "Thumbnail": {
         "_nbc": 1,
         "versions": [
             {
                 "_id": "aeaaaaaaaahbl62nabejwak3lchc4dqaaaba",
                 "DataObjectGroupId": "aebaaaaaaahbl62nabejwak3lchc4baaaaaq",
                 "DataObjectVersion": "Thumbnail_1",
                 "FormatIdentification": {
                     "FormatLitteral": "Acrobat PDF 1.4 - Portable Document Format",
                     "MimeType": "application/pdf",
                     "FormatId": "fmt/18"
                 },
                 "FileInfo": {
                     "Filename": "Vitam-Sensibilisation-API-V1.0.png",
                     "CreatingApplicationName": "LibreOffice/Impress",
                     "CreatingApplicationVersion": "5.0.5.2",
                     "CreatingOs": "Windows_X86_64",
                     "CreatingOsVersion": 10,
                     "LastModified": "2016-06-23T12:45:20"
                 },
                 "Metadata": {
                     "Image": null
                 },
                 "OtherMetadata": null,
                 "Size": 29403,
                 "Uri": "content/PC9p8TqGxK8bCoMEzVtBgo3baeSs0C.pdf",
                 "MessageDigest": "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7",
                 "Algorithm": "SHA-512"
             }
         ]
     },
     "TextContent": {
         "_nbc": 1,
         "versions": [
             {
                 "_id": "aeaaaaaaaahbl62nabejwak3lchc4dyaaaaq",
                 "DataObjectGroupId": "aebaaaaaaahbl62nabejwak3lchc4baaaaaq",
                 "DataObjectVersion": "TextContent_1",
                 "FormatIdentification": {
                     "FormatLitteral": "Acrobat PDF 1.4 - Portable Document Format",
                     "MimeType": "application/pdf",
                     "FormatId": "fmt/18"
                 },
                 "FileInfo": {
                     "Filename": "Vitam-Sensibilisation-API-V1.0.txt",
                     "LastModified": "2016-06-23T12:50:20"
                 },
                 "Metadata": {
                     "Text": null
                 },
                 "OtherMetadata": null,
                 "Size": 29403,
                 "Uri": "content/u2baSZmIVDCihTmWVpMPA0fLPttxUf.pdf",
                 "MessageDigest": "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7",
                 "Algorithm": "SHA-512"
             }
         ]
     }
 },
 "_up": [
     "aeaqaaaaaahbl62nabejwak3lchc4dyaaabq"
 ],
 "_nbc": 0,
 "_ops": [
     "aedqaaaaache45hwab23sak3lchc6vyaaaaq"
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
      <MessageDigest algorithm="SHA-512">8e393c3a82ce28f40235d0870ca5b574ed2c90d831a73cc6bf2fb653c060c7f094fae941dfade786c826
      f8b124f09f989c670592bf7a404825346f9b15d155af</MessageDigest>
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

Détail des champs du JSON
------------------------

*A noter: les champs préfixés par un '_' devraient être visibles via les API avec un code utilisant '#' en prefix. Mais il est possible que pour la Beta, le '_' reste visible.*

"_id" (#id): identifiant du groupe d'objet. Il s'agit d'une chaîne de 36 caractères.
Cet id est ensuite reporté dans chaque structure inculse

"_tenant" (#tenant): identifiant du tenant

"_profil" (#type): repris du nom de la balise présente dans le <Metadata> du <DataObjectPackage> du manifeste qui concerne le BinaryMaster.
Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...)

"FileInfo" : reprend le bloc FileInfo du BinaryMaster ; l'objet de cette copie est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait détruite (selon les règles de conservation), car ces informations ne sauraient être maintenues de manière garantie dans les futures versions.

"_qualifiers" (#qualifiers): est une structure qui va décrire les objets inclus dans ce groupe d'objet. Il est composé comme suit :

- [Usage de l'objet. Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>. C'est la valeur "BinaryMaster" qui est reportée.
    - "nb": nombre d'objets de cet usage
    - "versions" : tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du bordereau
        - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères.
        - "DataObjectGroupId" : Référence à l'identifiant objectGroup. Chaine de 36 caractères.
        - "DataObjectVersion" : version de l'objet par rapport à son usage.

    Par exemple, si on a *binaryMaster* sur l'usage, on aura au moins un objet *binarymaster_1*, *binaryMaster_2*. Ces champs sont renseignés avec les valeurs situées entre les balises <DataObjectVersion>.

    - "FormatIdentification": Contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel pronom et les valeurs que porte le document est faite.
      - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du XML
      - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du XML.
      - "FormatId" : PUID du format de l'objet. Il est défini par Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK).
    - "FileInfo"
      - "Filename" : nom de l'objet
      - "CreatingApplicationName": Chaîne de caractères. Contient le nom de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingApplicationVersion": Chaîne de caractères. Contient le numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOs": Chaîne de caractères. Contient le nom du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOsVersion": Chaîne de caractères. Contient le numéro de version du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnées correspondante portée par le fichier. *Ce champ et facultatif est n'est pas présent systématiquement*
      - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.

      - "Size": Ce champ contient un nombre entier. taille de l'objet (en octets).
    - "OtherMetadata": Contient une chaîne de caractères. Champ disponible pour ajouter d'autres métadonnées metier (Dublin Core, IPTC...). Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>. Ceci correspond à une extension du SEDA.
    - "Uri": localisation du fichier dans le SIP
    - "MessageDigest": empreinte du fichier. La valeur est calculé par Vitam.
    - "Algorithm": ce champ contient le nom de l'algorithme utilisé pour réaliser l'empreinte du document.

- "_up" (#unitup): [] : tableau d'identifiant des unités archivistiques parentes
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
  "_id": "aeaaaaaaaahbl62nabduoak3jc2zqciaadiq",
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

Détail des champs du JSON stocké en base
---------------------------------------

"_id": Il s'agit de l'identifiant unique du format dans VITAM.
    C'est une chaine de caractères composée de 36 signes.

"CreatedDate": Il s'agit la date de création de la version du fichier de signatures PRONOM.
    Il est utilisé pour alimenter l’enregistrement correspondant au format dans Vitam (balise DateCreated dans le fichier).
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
   "_id": "aeaaaaaaaahbl62nabduoak3jc4avsyaaaha",
   "_tenant": 0,
   "RuleId": "ACC-00011",
   "RuleType": "AccessRule",
   "RuleValue": "Communicabilité des informations portant atteinte au secret de la défense nationale",
   "RuleDescription": "Durée de communicabilité applicable aux informations portant atteinte au secret de la défense nationale\nL’échéance est calculée à partir de la date du document ou du document le plus récent inclus dans le dossier",
   "RuleDuration": "50",
   "RuleMeasurement": "YEAR",
   "CreationDate": "2017-04-07",
   "UpdateDate": "2017-04-07"
}

Colonne du csv comprenant les règles de gestion
-----------------------------------------------

================ ================= ======================= =========================== =============== ===============================
RuleId            RuleType          RuleValue               RuleDescription             RuleDuration     RuleMeasurement
---------------- ----------------- ----------------------- --------------------------- --------------- -------------------------------
Id de la règle    Type de règle     Intitulé de la règle    Description de la règle     Durée            Unité de mesure de la durée
================ ================= ======================= =========================== =============== ===============================

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

Collection IngestContract
=========================

Utilisation de la collection
----------------------------

La collection IngestContract permet de stocker unitairement les contrats d'entrée.

Exemple de JSON stocké en base
------------------------------

::

    {
    "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
    "_tenant": 0,
    "Name": "SIA archives nationales",
    "Description": "Contrat d'accès - SIA archives nationales",
    "Status": "ACTIVE",
    "CreationDate": "2017-04-10T11:30:33.798",
    "LastUpdate": "2017-04-10T11:30:33.798",
    "ActivationDate": "2017-04-10T11:30:33.798",
    "DesactivationDate": null
    }

Exemple de contrat d'entrée envoyé au format JSON
-------------------------------------------------

L'exemple suivant est un JSON contenant deux contrats d'entrée :

::

    [
        {
            "Name":"Contrat Archives Départementales",
            "Description":"Test entrée - Contrat Archives Départementales",
            "Status" : "ACTIVE",
            "CreationDate":"01/04/2017",
            "ActivationDate":"01/04/2017"
        },
        {
            "Name":"Contrat Archives Nationales",
            "Description":"Test entrée - Contrat Archives Nationales",
            "Status" : "INACTIVE",
            "CreationDate":"01/04/2017",
            "ActivationDate":"01/04/2017"
        }
    ]

Détail des champs
-----------------

"_id": identifiant unique. Il s'agit d'une chaîne de 36 caractères.

"_tenant": nom du tenant

"Name" : nom du contrat d'entrée. Il s'agit d'une chaîne de caractères.

"Description": description du contrat d'entrée. Il s'agit d'une chaîne de caractères.

"Status": statut du contrat. Peut être ACTIVE ou INACTIVE

"CreationDate": date de création du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"LastUpdate": date de dernière mise à jour du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"ActivationDate": date d'activation. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"DesactivationDate": date de désactivation du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

Collection AccessContract
=========================

Utilisation de la collection
----------------------------

La collection AccessContract permet de stocker unitairement les contrats d'accès.

Exemple de JSON stocké en base
------------------------------

::

    {
    "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
    "_tenant": 0,
    "Name": "SIA archives nationales",
    "Description": "Contrat d'accès - SIA archives nationales",
    "Status": "ACTIVE",
    "CreationDate": "2017-04-10T11:30:33.798",
    "LastUpdate": "2017-04-10T11:30:33.798",
    "ActivationDate": "2017-04-10T11:30:33.798",
    "DesactivationDate": null,
    "OriginatingAgencies":["FRA-56","FRA-47"]
    }

Exemple de contrat d'accès envoyé au format JSON
-------------------------------------------------

L'exemple suivant est un JSON contenant deux contrats d'accès :

::

    [
        {
            "Name":"Archives du Doubs",
            "Description":"Accès Archives du Doubs",
            "Status" : "ACTIVE",
            "LastUpdate":"10/12/2016",
            "CreationDate":"10/12/2016",
            "ActivationDate":"10/12/2016",
            "DeactivationDate":"10/12/2016",
            "OriginatingAgencies":["FRA-56","FRA-47"]
        },
        {
            "Name":"Archives du Calvados",
            "Description":"Accès Archives du Calvados",
            "Status" : "ACTIVE",
            "LastUpdate":"10/12/2016",
            "CreationDate":"10/12/2016",
            "ActivationDate":"10/12/2016",
            "DeactivationDate":"10/12/2016",
            "OriginatingAgencies":["FRA-54","FRA-64"]
        }
    ]

Détail des champs
-----------------

"_id": identifiant unique. Il s'agit d'une chaîne de 36 caractères.

"_tenant": nom du tenant

"Name" : nom du contrat d'accès. Il s'agit d'une chaîne de caractères.

"Description": description du contrat d'accès. Il s'agit d'une chaîne de caractères.

"Status": statut du contrat. Peut être ACTIVE ou INACTIVE

"CreationDate": date de création du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"LastUpdate": date de dernière mise à jour du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"ActivationDate": date d'activation. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"DesactivationDate": date de désactivation du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"OriginatingAgencies": tableau contenant les services producteurs auxquels le détenteur du contrat a accès. Il s'agit d'un tableau de chaînes de caractères.

Collection AccessionRegisterSummary
===================================

Utilisation de la collection
----------------------------

Cette collection est utilisée pour l'affichage global du registre des fonds.

Exemple de JSON stocké en base
------------------------------

::

{
  "_id": "aedqaaaaache45hwaantmak3iwffi2aaaaaq",
  "_tenant": 0,
  "OriginatingAgency": "FRAN_NP_009913",
  "SubmissionAgency": "FRAN_NP_009913",
  "ArchivalAgreement": "ArchivalAgreement0",
  "EndDate": "2017-04-07T01:12:31.772+02:00",
  "StartDate": "2017-04-07T01:12:31.772+02:00",
  "Status": "STORED_AND_COMPLETED",
  "LastUpdate": "2017-04-07T01:12:31.772+02:00",
  "TotalObjectGroups": {
      "total": 1,
      "deleted": 0,
      "remained": 1
  },
  "TotalUnits": {
      "total": 6,
      "deleted": 0,
      "remained": 6
  },
  "TotalObjects": {
      "total": 1,
      "deleted": 0,
      "remained": 1
  },
  "ObjectSize": {
      "total": 29403,
      "deleted": 0,
      "remained": 29403
  },
  "OperationIds": "[aedqaaaaache45hwaantmak3iwffi2aaaaaq]"
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

"_tenant": 0
"OriginatingAgency": La valeur de ce champ est une chaîne de caractère.
Ce champ est la clef primaire et sert de concaténation pour toutes les entrées effectuées sur ce producteur d'archives. Il est contenu entre les baslises <OriginatinAgencyIdentifier> du bordereau.

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état
    (total, deleted et remained)

    - "total": Nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état
    (total, deleted et remained)

    - "total": Nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état
    (total, deleted et remained)

    - "total": Nombre total d'unités archivistiques pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état
    (total, deleted et remained)

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

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état
    (total, deleted et remained)
    - "total": Nombre total de groupes d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre de groupes d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état
    (total, deleted et remained)
    - "total": Nombre total d'unités archivistiques pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'unités archivistiques conservées dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état
    (total, deleted et remained)
    - "total": Nombre total d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état
    (total, deleted et remained)
    - "total": Volume total en octet des fichiers pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Volume total en octets des fichiers conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

Annexes
=======

Valeurs possibles pour le champ evType logBook Operation
--------------------------------------------------------

  ===================================== ========================================================================================== ======================================================================
  Code evtType                          Fr EventType Label                                                                         EN EventType Label
  ------------------------------------- ------------------------------------------------------------------------------------------ ----------------------------------------------------------------------
  ACCESSION_REGISTRATION 	              Alimentation du registre des fonds
ATR_NOTIFICATION 	                      Notification de la fin de l’opération d’entrée
CHECK_CONSISTENCY 	                    Vérification de la cohérence entre objets, groupes d’objets et unités archivistiques
CHECK_CONTAINER 	                      Contrôle du format du conteneur du SIP
CHECK_DIGEST 	                          Vérification de l’intégrité des objets
CHECK_MANIFEST 	                        Vérification de la cohérence du bordereau                                                  Check SIP – ObjectGroups – Lifecycle Logbook Creation
CHECK_MANIFEST_DATAOBJECT_VERSION 	    Vérification des usage des groupes d’objets                                                Check SIP – Manifest – DataObjectVersion
CHECK_MANIFEST_OBJECTNUMBER 	          Vérification du nombre d'objets                                                             Check SIP – ObjectGroups - Objects Count
CHECK_PROFIL 	                          Vérification du Profil
CHECK_SEDA 	                            Vérification globale du SIP                                                                  Check SIP – Manifest SEDA Consistency
OG_LOGBOOK_STORAGE 	                    Enregistrement du journal du cycle de vie des Objets et Groupes d'objets
OG_METADATA_INDEXATION 	                Indexation des métadonnées des groupes d'objets                                              ObjectGroups – Metadata Index
OG_METADATA_STORAGE 	                  Sécurisation des métadonnées des Objets et Groupes d’Objets
OG_OBJECTS_FORMAT_CHECK 	              Identification des formats                                                                   File format check
OG_STORAGE 	                            Ecriture des objets sur l’offre de stockage                                                  ObjectGroups Storage
OLD_CHECK_DIGEST 	                      Vérification de l’empreinte                                                                Check SIP – ObjectGroups – Digest
SANITY_CHECK_SIP 	                      Contrôle sanitaire
STORAGE_AVAILABILITY_CHECK 	            Vérification de la disponibilité de l’offre de stockage                                      Storage availability check
STP_INGEST_CONTROL_SIP 	                Contrôle du bordereau                                                                      Check Manifest
STP_INGEST_FINALISATION 	              Finalisation de l’entrée                                                                   Ingest finalisation and transfer notification to the operator
STP_OG_CHECK_AND_TRANSFORME 	          Contrôle et traitements des objets                                                           Check and process objects
STP_OG_STORING 	                        Rangement des objets                                                                       ObjectsGroups storing
STP_SANITY_CHECK_SIP 	                  Contrôles préalables à l’entrée                                                            Sanity Check
STP_STORAGE_AVAILABILITY_CHECK 	        Préparation de la prise en charge                                                          Check before storage
STP_UNIT_CHECK_AND_PROCESS 	            Contrôle et traitements des Units                                                          Check and process units
STP_UNIT_STORING 	                      Rangement des Unites                                                                         ArchiveUnit storing
STP_UPLOAD_SIP 	                        Réception dans vitam                                                                       Upload SIP
UNIT_LOGBOOK_STORAGE 	                  Enregistrement du journal du cycle de vie des unités archivistiques
UNIT_METADATA_INDEXATION 	              Indexation des metadonnées des Units                                                         Units – Metadata Indexation
UNIT_METADATA_STORAGE 	                Sécurisation des métadonnées des Unités Archivistiques
UNITS_RULES_COMPUTE 	                  Application des règles de gestion et calcul des échéances                                  Apply management rules and compute deadlines
UPLOAD_SIP 	                            Tache de réception dans Vitam
  ===================================== ========================================================================================== ======================================================================

Valeurs possibles pour le champ evType logBook LifeCycle
--------------------------------------------------------

  ====================================== =========================================================================================================
  Code evtType                              Label de evtType
  -------------------------------------- ---------------------------------------------------------------------------------------------------------
  ACCESSION_REGISTRATION                      Alimentation du registre des fonds
  ATR_NOTIFICATION                          Notification de la fin de l’opération d’entrée
  CHECK_CONSISTENCY                         Vérification de la cohérence entre objets, groupes d’objets et unités archivistiques
  CHECK_CONTAINER                            Contrôle du format du conteneur du SIP
  CHECK_DIGEST                              Vérification de l’intégrité des objets
  CHECK_MANIFEST                              Vérification de la cohérence du bordereau
  CHECK_MANIFEST_DATAOBJECT_VERSION         Vérification des usage des groupes d’objets
  CHECK_MANIFEST_OBJECTNUMBER                 Vérification du nombre d'objets
  CHECK_SEDA                                  Vérification globale du SIP
  OG_METADATA_INDEXATION                      Indexation des métadonnées des groupes d'objets
  OG_OBJECTS_FORMAT_CHECK                     Identification des formats
  OG_STORAGE                                  Ecriture des objets sur l’offre de stockage
  OLD_CHECK_DIGEST                          Vérification de l’empreinte
  SANITY_CHECK_SIP                          Contrôle sanitaire
  STORAGE_AVAILABILITY_CHECK                  Vérification de la disponibilité de l’offre de stockage
  STP_ACCESSION_REGISTRATION                  Registre des Fonds
  STP_INGEST_CONTROL_SIP                      Contrôle du bordereau
  STP_INGEST_FINALISATION                     Finalisation de l’entrée
  STP_OG_CHECK_AND_TRANSFORME                 Contrôle et traitement des objets
  STP_OG_STORING                              Rangement des objets
  STP_SANITY_CHECK_SIP                      Contrôles préalables à l’entrée
  STP_STORAGE_AVAILABILITY_CHECK              Préparation de la prise en charge
  STP_UNIT_CHECK_AND_PROCESS                  Contrôle et traitement des Unités Archivistiques
  STP_UNIT_STORING                          Rangement des Unités Archivistiques
  STP_UPLOAD_SIP                              Réception dans vitam
  UNIT_METADATA_INDEXATION                  Indexation des metadonnées des Unités Archivistiques
  UNITS_RULES_COMPUTE                         Application des règles de gestion et calcule des échéances
  UPLOAD_SIP                                  Tache de réception dans Vitam
  ====================================== =========================================================================================================

Valeurs possibles pour le champ evTypeProc
------------------------------------------

 =================================== ===================
 Process Type                          Valeur
 ----------------------------------- -------------------

 Audit type process                    AUDIT
 Check type process                    CHECK
 Destruction type process (v2)         DESTRUCTION
 Ingest type process                   INGEST
 Preservation type process             PRESERVATION
 Rules Manager process                 MASTERDATA
 Traceability type process             TRACEABILITY
 Update process                        UPDATE
 =================================== ===================

Prefixes possibles des RulesId
------------------------------

 ========= ============================== ===================================================================================
 Prefixe    Type de règle correspondante   Description du type de règle
 --------- ------------------------------ -----------------------------------------------------------------------------------
 ACC        AccessRule                     Règle d'accès
 APP        Appraisal                      Règle correspondant à la durée d'utilité administrative (DUA)/Durée de rétention
 CLASS      ClassificationRule             Règle de classification
 DIS        DisseminationRule              Règle de diffusion
 REU        ReuseRule                      Règle d'utilisation
 STO        StorageRule                    Règle de Stockage
 ========= ============================== ===================================================================================

Valeurs possibles pour le champ Status de la collection AccessionRegisterDetail
-------------------------------------------------------------------------------

  ========================================== ======================
  Status type                                Valeur
  ------------------------------------------ ----------------------
  Le fonds est complet sauvegardé            STORED_AND_COMPLETED
  Le fonds est mis à jour est sauvegardé     STORED_AND_UPDATED
  Le fonds n'est pas sauvagerdé              UNSTORED
  ========================================== ======================
