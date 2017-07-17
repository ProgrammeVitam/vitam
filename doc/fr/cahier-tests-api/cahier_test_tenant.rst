Cahier des API externes multi-tenant
####################################

Introduction
============

L'objectif de cette documentation est d‘élaborer le cahier de test multi-tenant des API external (ingest, referentiel, logbook, access et accession-registre) via postman. On va lister tous les APIs testés, les réponses, les pré-requis et les différents cas téstés avec les différents requêtes utilisés.

**A propos des pré-requis et stratégie de tests**



La manipulation des données dans Vitam pouvant être très impactant (par exemple lors de suppression et de remplacement du référentiel de gestion), il est nécessaire de garantir que la suite des tests se déroule dans de bonnes conditions opérationnelles.

Voici trois stratégies possibles concernant la spécification des pré-requis :

*1 - Le test remet la plateforme dans l’état dans laquelle il l’a trouvé*

Le test sauvegarde temporairement certaines données du système, exécute ses prérequis puis le test lui même. Ce dernier terminé, il supprime ses données issues du prérequis et restaure les données du système précédemment sauvegardées.
En revanche, pendant la durée du test, des utilisateurs effectuant des opérations sur le tenant pourront subir de forte perturbation d’utilisations (selon les données et le test effectué).

Illustration avec un test sur un référentiel de gestion spécifique pour l’occasion :

- 1/ Sauvegarde du référentiel en cours d’utilisation (données initiales)
- 2/ Purge du référentiel en cours (prérequis)
- 3/ Import d’un référentiel spécifique (prérequis)
- 4/ Exécution du test lui même (test)
- 5/ Purge du référentiel spécifique (remise en condition)
- 6 / Import du référentiel d’origine (remise en condition)

*2 - Les tests sont séquencés et les dépendances sont connues*

Dans ce cas, les pré et posts conditions sont connues et il sera possible de factoriser les tests par prérequis. Par exemple, si 5 tests requièrent un référentiel spécifique, ce référentiel ne sera importé qu’une seule fois et supprimé une seule fois au début et à la fin de ces 5 tests.

*3 - Chaque test est indépendant et chacun est garant de la mise en place de ses pré requis*

Chaque test doit effectuer l’ensemble des purges et importations nécessaires. Les tests sont indépendants et peuvent être lancés dans n’importe quel ordre, mais l’exécution des conditions est très coûteuse (on purgera et importera N fois le même référentiel)

Dans une démarche progressive, il est possible d’implémenter plusieurs tests suivant la démarche n°3, puis de factoriser les prérequis pour arriver à la démarche n°2

Dans un soucis de simplicité pour une premier jet, la stratégie n°3 est envisagée dans le reste de ce document.



Règles de gestion
=================

Importer des règles de gestion sur le tenant 0
----------------------------------------------


Code : #RG01

``API : {{accessServiceUrl}}/admin-external/v1/rules/``

Pré-requis(données de référence):

Utiliser sur le tenant 0 le jeu_donnees_OK_regles_CSV.csv contenant 22 règles de gestion. Ce jeu de données est le référentiel standard utilisé dans le reste des tests Vitam

Headers:  Accept : application/json ; Content-type : application/octet-stream ;X-Tenant-id : 0

Critères d'acceptance

- Le nombre de règle du référentiel est bien égal à 22. Pour s'en assurer, on effectue une recherche dans la base avec API en accompagnant des paramètres ci-dessous

Requête :

.. code-block:: json

    {
        "$roots":[],
        "$query":[],
        "$filter":{},
        "$projection":{}
    }


On doit retrouver :
- Dans la réponse : $hits.total = 22
- Pour chaque règle : _tenant = 0


Headers:
Accept : application/json ; Content-type : application/octet-stream ;X-Tenant-id : 0


Evolution du test :
Lorsque Vitam utilisera les CodeListVersions des référentiels dont celui des règles des gestions, celui ci pourra être intégré au test, possiblement à la place du _tenant.

Importer des règles de gestion avec tenant 1 (tenant de test)
-------------------------------------------------------------

**Code :** #RG02

**Pré-requis** :

Importer le référentiel de test : jeu_donnees_OK_regles_T1.csv, ou s'assurer qu'il s'agit bien du référentiel en cours d'utilisation. Ce référentiel contient 3 règles.

API : identique à #RG01

Critère d'acceptance :

On doit retrouver :

- Dans la réponse : $hits.total = 3
- Pour chaque règle : _tenant = 1

Rechercher une règle existante liée au tenant 0 par son identifiant via access-external
---------------------------------------------------------------------------------------



Code : #RG03

``API: {{accessServiceUrl}}/admin-external/v1/rules/{IdRule}``

Headers:
Accept : application/json ; X-Http-Method-Override : GET ;X-Tenant-id : 0


Rechercher une règle liée au tenant 1 via access-external non trouvé
--------------------------------------------------------------------

Code : #RG04

``API testé: {{accessServiceUrl}}/admin-external/v1/rules/{IdRule}``

Requête :

.. code-block:: json

 {
     "$roots":[],
     "$query":[],
     "$filter":{},
     "$projection":{}
 }

Headers:

Accept : application/json ; X-Http-Method-Override : GET ;X-Tenant-id : 1

La réponse: 500 INTERNAL_SERVER_ERROR

.. code-block:: json

     {
         "httpCode": 500,
         "code": "INTERNAL_SERVER_ERROR",
         "context": "ADMIN_EXTERNAL",
         "state": "code_vitam",
         "message": "Internal Server Error",
         "description": "Internal Server Error",
         "errors": []
     }





Ingest
======

Envoi d'un SIP ‘sip1’ (contenant au moins une unité d’archive valide) sur le *tenant 0*
---------------------------------------------------------------------------------------

Verser un SIP sur le tenant 0 (POST [ingest])
*********************************************


Code : #ING01

``API: {{ingestServiceUrl}}/ingest-external/v1/ingests``
Pré-requis(données de référence): OKSIP-v2-rules.zip

Headers:

Accept : application/json ; application/octet-stream ;X-Tenant-id : 0

La réponse: 200 OK

Rechercher une unité d’archive insérée dans la base  (POST [access-external])
*****************************************************************************

Code : #ING02

``API: {{accessServiceUrl}}/access-external/v1/units``

Headers

Accept : application/json ; application/octet-stream ;X-Tenant-id : 0

Critère d’acceptance :

- hits.total = 1 sur le tenant 0
- L’identifiant de l’unité d’archive est bien celui demandée


Requête

.. code-block:: json

    {
        "$hits": {
            "total": 1,
            "offset" : 1,
            "limit": 1,
            "size": 1
        },
        "$results": [
            {
                "_tenant" : 0
            }
        ],
        "$context":{}
    }

Verser un autre SIP ‘sip2’ ayant une unité d’archive valide sur le *tenant 1*
-----------------------------------------------------------------------------


Code : #ING03

Le test partage ses conditions avec #ING01, à ceci près qu’il utilise un SIP différent (OK_SIP_2_GO.zip) et vérifie que les données créées soient bien associées au tenant 1.



Logbook
=======

Rechercher l’opération, par son identifiant, correspondant au versement du SIP ‘sip1’ sur le tenant 0 (story #1650)
-------------------------------------------------------------------------------------------------------------------


Code : #OPLOG01

``API : {{accessServiceUrl}}/access-external/v1/operations``

Requête :

.. code-block:: json

    {
        "$query":{
            "$and":[
                {
                    "$eq":
                    {
                        "evTypeProc":"INGEST"
                    }
                }
            ]
        },
        "$filter":{},
        "$projection":{}
    }


Headers:

Accept :application/json ; Content-Type : application/json ; X-Http-Method-Override : GET ;  X-Tenant-Id : 0

La réponse: 200 OK et la valeur total dans la réponse soit 1

Critères d’acceptance :

- La réponse doit être 200 OK
- La valeur totale de la réponse doit être 1 hits.total = 1
- L’identifiant demandé doit bien être l’identifiant retourné

Rechercher l’opération, par son identifiant, correspondant au versement du SIP ‘sip2’ sur le tenant 1
-----------------------------------------------------------------------------------------------------


Code : #OPLO02

Ce test partage ses conditions avec #OPLOG01, à ceci près qu’il interroge l’identifiant de sip2 et vérifie que les données créées soient bien associées au tenant 1.

Rechercher les logbook opération avec le tenant 0, il retournera la liste des opérations, et chaque opération contiendra l'attribut "_tenant": 0
------------------------------------------------------------------------------------------------------------------------------------------------


Code : #OPLOG03

``API testé: {{accessServiceUrl}}/access-external/v1/operations``

Opérateurs du DSL:

.. code-block:: json

 {"$query":{},"$filter":{},"$projection":{}}

Headers:

+----------------------------+---------------------------+
| key                        | value                     |
+============================+===========================+
| Accept                     | application/json          |
+----------------------------+---------------------------+
| Content-Type               | application/json          |
+----------------------------+---------------------------+
| X-Http-Method-Override     | GET                       |
+----------------------------+---------------------------+
| X-Tenant-Id                | 0                         |
+----------------------------+---------------------------+

La réponse: 200 OK et la valeur total dans la réponse soit 3




Unités archivistiques et groupes d’objets
=========================================

Recherche des unités d’archive sur le "tenant 0" (POST [units]) via access-external
-----------------------------------------------------------------------------------


Code : #AUOG1

``API : {{accessServiceUrl}}/access-external/v1/units``

Requête :

.. code-block:: json

 {"$roots":[],"$query":[],"$filter":{},"$projection":{}}

Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0

La réponse: 200 OK. ET la valeur total qu'on a dans la réponse soit 1


Rechercher des unités d’archive sur le tenant 1
-----------------------------------------------

Code : #AUOG2

Ce test partage ses conditions et ses étapes avec #AUOG1, à la différence qu’il s’effectue sur le tenant 1.
La réponse doit être 200 OK et la valeur total dans la réponse soit 7.


Accès à une unité d’archive ajoutée par sip1 sur le tenant 0 (archive trouvée)
------------------------------------------------------------------------------

Code : #AUOG3

``API testé: {{accessServiceUrl}}/access-external/v1/units``

Requête :

.. code-block:: json

 {"$roots":[],"$query":[{"$match":{"Title":"Sensibilisation API"}}],"$filter":{},"$projection":{}}


Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0

La réponse doit être 200 OK avec résultat ci-dessous


Accès à une unité d’archive ajoutée par sip1 sur le tenant 1 (archive introuvable)
----------------------------------------------------------------------------------


Code : #AUOG4

Ce test partage ses conditions et ses étapes avec #AUOG1, à la différence qu’il s’effectue sur le tenant 1


Modification d'une unité d’archive versée par sip1 sur le *tenant 0* (archive trouvée et valide)
------------------------------------------------------------------------------------------------


Code : #AUOG5

``API : {{accessServiceUrl}}/access-external/v1/units/{idUnit}``

Requête :

.. code-block:: json

 {"$roots":["aeaaaaaaaaaam7mxabujeakzonzrepqaaaba"],"$query":[],"$filter":{},"$action":[{"$set":{"Title":"Demo Sensibilisation API"}}]}

Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0

La réponse doit être 200 OK avec résultat ci-dessous

.. code-block:: json

 {"$hits":{"total":1,"offset":0,"limit":1,"size":1},"$results":[{"#id":"aeaaaaaaaaaam7mxabujeakzonzrepqaaaba","#diff":"-  Title : Sensibilisation API\n+  Title : Demo Sensibilisation API\n-  #operations : [ aedqaaaaacaam7mxab5eeakzonzq74yaaaaq \n+  #operations : [ aedqaaaaacaam7mxab5eeakzonzq74yaaaaq, aecaaaaaacaam7mxabv7cakzoo5rahqaaaaq "}],"$context":{"$roots":["aeaaaaaaaaaam7mxabujeakzonzrepqaaaba"],"$query":[],"$filter":{},"$action":[{"$set":{"Title":"Demo Sensibilisation API"}},{"$push":{"#operations":{"$each":["aecaaaaaacaam7mxabv7cakzoo5rahqaaaaq"]}}}]}}


Modification d'un unité d’archive versée par sip1 sur le *tenant 1* (archive non trouvée)
-----------------------------------------------------------------------------------------


Code : #AUOG6

 Ce test partage ses conditions et ses étapes avec #AUOG5, à la différence qu’il s’effectue sur le tenant 1.


Accès au journal du cycle de vie d'une unité d’archive  versée par sip1 sur le tenant 0 (journal de l’unité trouvé)
-------------------------------------------------------------------------------------------------------------------


Code : #AUOG7

``API : {{accessServiceUrl}}/access-external/v1/unitlifecycles/{IdUnit}``

Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0 ; X-Http-Method-Override : GET

La réponse doit être 200 OK et le résultat doit renvoyer le journal du cycle de vie de l’unité d’archive.


Accès au journal du cycle de vie d'une unité d’archive versée par sip1 sur le tenant 0 (journal de l’unité introuvable)
-----------------------------------------------------------------------------------------------------------------------

Code : #AUOG8

Ce test partage ses conditions et ses étapes avec #AUOG7, à la différence qu’il s’effectue sur le tenant 1.

La réponse doit être  200 OK avec le résultat :

.. code-block:: json

 {"$hits":{"total":0,"offset":0,"limit":0,"size":0},"$results":[],"$context":{}}


Accès à un objet technique versé par le sip2 sur le tenant 1 (objet trouvé)
---------------------------------------------------------------------------


Code : #AUOG8

``API : {{accessServiceUrl}}/access-external/v1/objects/{IdObjectGroup}``


Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 1 ; X-Http-Method-Override : GET

La réponse doit être 200 OK et le résultat doit renvoyer les données relatives à l’objet demandé.


Accès à un objet technique versé par le sip2 sur le tenant 0 (objet introuvable)
--------------------------------------------------------------------------------


Code : #AUOG9

Ce test partage ses conditions et ses étapes avec #AUOG7, à la différence qu’il s’effectue sur le tenant 0.

La réponse doit être  200 OK avec le résultat :

.. code-block:: json

   {"$hits":{"total":0,"offset":0,"limit":0,"size":0},"$results":[],"$context":{}}


Accès au journal du cycle de vie d’un groupe d’objet, versé par sip2 sur le tenant 1 (journal de groupe d’objet trouvé)
-----------------------------------------------------------------------------------------------------------------------

Code : #AUOG10

``API testé: {{accessServiceUrl}}/access-external/v1/objectgrouplifecycles/IdObjectGroup}``

Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 1

La réponse doit être 200 OK et doit renvoyer une absence de résultat.



Accès au journal du cycle de vie d’un groupe d’objet, versé par sip2 sur le tenant 0 (journal de groupe d’objet introuvable)
----------------------------------------------------------------------------------------------------------------------------


Code : #AUOG11

Ce test partage ses conditions et ses étapes avec #AUOG10, à la différence qu’il s’effectue sur le tenant 0.


La réponse doit être 200 OK avec le résultat

.. code-block:: json

   {"$hits":{"total":0,"offset":0,"limit":0,"size":0},"$results":[],"$context":{}}


Registre des fonds
==================

Vérifier que le registre des fonds du tenant 0 renvoie les opérations liées au sip1
-----------------------------------------------------------------------------------


Code : #RFOND1

``API testé: {{accessServiceUrl}}/admin-external/v1/accession-registers``

Requête :

.. code-block:: json

   {"$roots":[],"$query":[],"$filter":{},"$projection":{}}


Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0 ; X-Http-Method-Override : GET

La réponse doit être 200 OK et renvoyer le registre des fonds relatif au tenant 0


Accès au détail du registre des fonds lié au *tenant 0* à partir de l'access-external (registre trouvé)
-------------------------------------------------------------------------------------------------------

Code : #RFOND2

``API testé: {{accessServiceUrl}}/access-external/v1/accession-register/{idAgency}/accession-register-detail``


.. code-block:: json

   {"$roots":[],"$query":[],"$filter":{},"$projection":{}}


Headers:

Accept : application/json ; Content-Type : application/json ; X-Tenant-Id : 0 ; X-Http-Method-Override : GET

La réponse doit être 200 OK et renvoyer le détail du registre des fonds relatif au tenant 0


Accès à un registre des fond lié au *deuxième tenant de test* à partir de l'access-external non trouvé
------------------------------------------------------------------------------------------------------

Code : #RFOND3

Ce test partage ses conditions et ses étapes avec #RFOND2, à la différence qu’il s’effectue sur le tenant 1.

On applique le scénario de test 2 en modifiant le X-Tenant-Id à 1.
La réponse doit être 200 OK et renvoyer une absence de résultat

.. code-block:: json

   {"$hits":{"total":0,"offset":0,"limit":0,"size":0},"$results":[],"$context":{}}
