Testeur de requêtes DSL
#######################

Le testeur de requêtes DSL met à disposition des administrateurs une interface graphique permettant de simplifier l'exécution de requêtes sur les API de la solution logicielle Vitam.

Celle-ci contient un formulaire composé de plusieurs champs.

Champs disponibles
==================

**Tenant** : champ obligatoire. Indique le tenant sur lequel la requête va être exécutée. Ce champs est contribué automatiquement avec le numéro du tenant sélectionné par l’administrateur.

**Contrat** : champ optionnel. Liste permettant de sélectionner un contrat d'accès qui sera associé à la requête.

**Collection** : champ obligatoire. Liste permettant de sélectionner la collection sur laquelle la requête va être exécutée.

**Action** : champ obligatoire. Liste permettant de sélectionner le type d'action à effectuer. Il est possible de sélectionner l'action "Rechercher" pour l'ensemble des collections.

Pour les collections suivantes, il est également possible de choisir l'action "Mise à jour" :

* Unit
* Contrat d'accès
* Contrat d'entrée
* Contexte
* Opération

**Identifiant** : champs optionnel. Permet de renseigner le GUID de l'objet ciblé dans la collection.

**Requête DSL** : champ obligatoire. Permet de saisir la requête DSL au format Json.

Réaliser une requête
====================

Pour réaliser une requête, l'administrateur rempli les champs du formulaire afin que leur contenu soit cohérent avec la requête qu'il souhaite exécuter. 

.. image:: images/DSL_envoyer_requete.png

Pour vérifier la validité du formatage du Json, l'administrateur clique sur bouton "Vérifier Json". Si le Json est valide, le texte est mis en forme et la mention "Json Valide" est affichée à gauche du bouton. Dans le cas contraire, la mention "Json non valide" est indiquée.

.. image:: images/DSL_Json_Invalide.png

Pour exécuter la requête, l'administrateur clique sur le bouton "Envoyer la requête". Une zone de résultat est alors affichée à droite de l'écran et contient le retour envoyé par la solution logicielle Vitam.

.. image:: images/DSL_requete_OK.png

Si la requête contient une erreur autre que le non-respect du formatage de la requête Json, le retour envoyé par la solution logicielle Vitam contiendra un code d’erreur et sera affiché de la façon suivante :

.. image:: images/DSL_requete_KO.png

Si la requête envoyée par l'administrateur ne respecte pas le formatage de la requête Json, l'endroit où se trouve l'erreur sera indiqué dans le retour de la façon suivante :

.. image:: images/DSl_requete_Json_KO.png