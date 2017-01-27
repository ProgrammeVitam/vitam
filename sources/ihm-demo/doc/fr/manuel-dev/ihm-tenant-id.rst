IHM Front - Requêtes HTTP et Tenant ID
######################################

Cette documentation décrit le process de récupération / sélection et communication du tenant ID depuis IHM-DEMO front vers les API publiques VITAM
--------------------------------------------------------------------------------------------------------------------------------------------------


Gestion du tenantId
*******************

Coté front
==========

Actuelement, le tenantID est sauvegardé dans le navigateur client sous forme de cookie au moment de la connexion.

Coté serveur d'app
==================

.. TODO

Création de requêtes HTTP utilisant un tenantID (front)
*******************************************************

Utilisation de ihmDemoClient
============================

Le service ihmDemoClient permet d'instancier un client HTTP préconfiguré pour dialoguer avec le serveur d'app IHM-DEMO.
Ce dernier contient entre autre:
 - La racine de l'url à appeler (Exemple: ihm-demo/v1/api)
 - Un intercepteur permettant d'ajouter le HEADER X-request-id à chaque requêtes.

Requêtes http personnalisées
============================

Si nécessaire il est possible d'utiliser $http ou un autre procédé pour faire votre requête HTTP.
Dans ce cas, il est possible de récupérrer la clé et la valeur du header via la ligne de code suivante:

.. code-block:: javascript

   var key = loadStaticValues.loadFromFile().then(function(response) {
     return response.data.headers;
   });
   var value = authVitamService.cookieValue(authVitamService.COOKIE_TENANT_ID);

NB: Les services authVitamService et loadStaticValues doivent avoir été injectés.
