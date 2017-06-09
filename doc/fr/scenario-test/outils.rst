OUTILS DE TESTS
##########################################################

Afin de tester l'environnement de manière approfondie. Divers tests permettent, selon leur type, de vérifier chaque aspect de l'environnement.
Les tests manuels disposent d'une grande amplitude d'actions et les tests automatiquement permettent de vérifier de manière régulière qu'une régression n'est survenue et que tout fonctionne correctement.

Plusieurs documentations sont à disposition :
 - La documentation des Tests de Non Régression (TNR) se trouve ici : configuration-tnr/configuration-d-un-scenario.rst
 - Le manuel d'intégration applicative qui a vocation à présenter la manière d'interroger le DSL : configuration-tnr/configuration-d-un-scenario.rst
 - Le tableau du cahier de test :

 **Administration des collections**

L'administration des collections est accessible dans l'IHM recette via le menu éponyme. permet de purger les référentiels, les journaux et les objets de manière individuelle ou de tous les purger en même temps.


Tests Manuels
###############

Les tests manuels peuvent être effectués via :
 * Cahier de tests manuels, qui permettent
 * Postman, qui permet de lancer des tests sur l'API
 * IHM recette, qui permet de lancer des requêtes DSL


 Cahier de tests manuels
 ------------------------

 Le cahier de test se présente sous forme de tableur. Il répertorie méticuleusement chaque cas de test possible.

 Le tableau contient :

  - Le titre explicite du cas de test
  - L'itération à laquelle le test se raccroche
  - La nature du test (TNR ou Manuel)
  - La liste des User Stories qui traitent ce cas de test
  - Le Code Story Map, c'est-à-dire le code attribué à ce sujet (entrée, accès, stockage, etc.)
  - Le Use Case ou déroulement du test étape par étape
  - Si ce test est un test IHM ou API
  - Le ou les jeux de tests associés

Postman
---------

Postman se présente sous la forme de plugin qui peut être intégré au navigateur Chrome. Suite à l'installation d'un certificat, propre à VITAM, des requêtes DSL peuvent être lancées en GET ou POST.

.. image:: images/POSTMAN_requete.png

.. image:: images/POSTMAN_requete2.png

Les résultats seront ensuite retournés sous format JSON.

.. image:: images/POSTMAN_retour.png



Requêtes DSL
---------------

Il est possible de lancer des requêtes DSL via l'IHM de recette via le menu "Requêtes DSL", sans besoin de certificat. Cela permet de tester de manière simple et rapide  des requêtes DSL.

Il s'agit d'un formulaire permettant de gérer plusieurs variables, telles que le tenant, le contrat d'accès, la collection, l'action testée et un identifiant. La requête est ensuite placée dans un champ texte.

Il est possible de vérifier sa validité avant de la lancer. Les résultats sont ensuite retournés sous format JSON.

.. image:: images/RECETTE_requetesdsl_ecran_principal.png

.. image:: images/RECETTE_requetesdsl_boutons.png

.. image:: images/RECETTE_requetesdsl_ecran_reponse.png



Tests Automatisés
####################


Cucumber
---------

Cucumber est un outil de tests fonctionnels, il est accessible via l'IHM de recette dans le menu "Tests fonctionnels". Ces tests sont écrits à la manière de critère d'acceptances, ce qui offre une grande variété de combinaisons.

Il existe une liste de contextes et de fonctions disponibles. Il s'agit ensuite de les associer et les manipuler afin de créer son propre test.

Les résultats sont retournés sous forme detableau

.. image:: images/RECETTE_test_fonctionnels_ecran_principal.png

.. image:: images/RECETTE_detail_tests.png

.. image:: images/RECETTE_detail_test_OK.png

Tests de stockage
------------------

Ces tests permettent de vérifier qu'un objet est biens stockés plusieurs fois sur la plateforme afin d'assurer sa pérénnité.

Ce test vérifie :

 - Le tenant sur lequel est stocké l'objet
 - Le nom de l'objet stocké
 - La strategie de stockage
 - La liste des stratégies où est stocké l'objet
 - La présence de l'objet dans ces stratégies


Séquencement de tests
---------------------

Un fichier contient une liste des TNR qui seront lancés de manières séquencées afin de réaliser et tester un scénario complet.
