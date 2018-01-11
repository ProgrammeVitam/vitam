Outils de tests
###############

Divers outils ont été mis en place afin de vérifier chaque aspect de la solution logicielle VITAM :

  * Les tests manuels permettent de tester un large spectre de fonctionnalités de la solution logicielle Vitam lors des développements.

  * Les tests automatiques permettent de vérifier de manière régulière qu'une régression n'est pas survenue et que tout fonctionne correctement (chapitre 4).

Plusieurs documents complémentaires sont à disposition :
 - La documentation des Tests de Non Régression (TNR) se trouve dans : doc/fr/configuration-tnr/configuration.rst
 - Le manuel d'intégration applicative qui présente la manière d'interroger le DSL est présent dans ce même document  doc/fr/configuration-tnr/configuration.rst
 - Le tableau du cahier de tests manuels se trouve dans l'outil Jalios (espace livraison)


**Administration des collections**

L'administration des collections est accessible dans l'IHM recette via le menu éponyme. Cela permet de purger les référentiels, les journaux, les unités archvistiques et groupes d'objest et les contrats par collection (au sens MongoDB) ou pour la totalité des collections présentent dans cette IHM.


Tests Manuels
###############

Les tests manuels peuvent être effectués :
 * A l'aide du cahier de tests manuels
 * Via l'IHM recette, qui permet de lancer des requêtes DSL


Cahier de tests manuels
------------------------

 Le cahier de test se présente sous forme de tableur. Il répertorie méticuleusement chaque cas de test possible.

 Le tableau contient :

  - Le titre explicite du cas de test
  - L'itération à laquelle le test se raccroche
  - La liste des User Stories qui traitent ce cas de test
  - Le nom de l'activité, nom associé au code Story Map
  - Le Code Story Map, c'est-à-dire le code attribué à ce sujet (entrée, accès, stockage, etc.)
  - Le Use Case ou déroulement du test étape par étape
  - IHM / API, spécifie pour quelle interface le test est dédié
  - Le ou les jeux de tests associés

Requêtes DSL
---------------

Il est possible de lancer des requêtes DSL via l'IHM de recette depuis le menu "Tests / Tests requêtes DSL", sans besoin de certificat. Cela permet de tester de manière simple et rapide des requêtes DSL.

Un formulaire permet de gérer plusieurs variables. Un tenant doit être sélectionné aupréalable au niveau du menu.
Au niveau du formulaire, il faut choisir :

- contrat d'accès sur lequel lancer le test
- la collection relative à la requête
- l'action à tester
- un identifiant (facultatif)

La requête est ensuite écrite dans le champ texte de gauche. Le bouton "Valider JSON" permet de vérifier sa validité avant de l'envoyer. Un clic sur le bouton "Envoyer requête" affiche les résultats sous format JSON dans le champ texte de droite.

.. image:: images/RECETTE_requetesdsl_ecran_principal.png

.. image:: images/RECETTE_requetesdsl_boutons.png

.. image:: images/RECETTE_requetesdsl_ecran_reponse.png



Tests Automatisés
#################

Dans Vitam, les tests automatisés sont aussi appelés TNR. Pour avoir plus de détails sur leur écriture, se reporter à la section de la documentation associée aux TNR.

Tests fonctionnels
-------------------

**Cucumber**

Cucumber est un outil de tests fonctionnels, il est accessible via l'IHM de recette dans le menu "Tests / Tests fonctionnels". Ces tests sont effectués via des ordres écrits avec des phrases simples, ce qui offre une grande variété de combinaisons.

Il existe une liste de contextes et de fonctions disponibles. Il s'agit ensuite de les associer et les manipuler afin de créer son propre test.

Les résultats sont retournés sous forme de tableau

.. image:: images/RECETTE_test_fonctionnels_ecran_principal.png

.. image:: images/RECETTE_detail_tests.png

.. image:: images/RECETTE_detail_test_OK.png


Séquencement de tests
---------------------

Un fichier contient une liste des TNR qui seront lancés de manière séquencée afin de réaliser et tester un scénario complet.
