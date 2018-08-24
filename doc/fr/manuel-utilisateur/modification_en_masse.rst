Traitements de masse
#####################

Cette partie décrit les fonctionnalités permettant d'effectuer des actions sur un grand nombre de métadonnées stockées dans la solution logicielle Vitam. 

Ces deux fonctionnalités sont accessibles via le panier.

Chaque mise à jour est suivie d'une journalisation dans le journal des opératons, et un rapport est disponible, afin d'avoir un compte rendu de toutes les modifications effectuées, et celles qui n'ont pas pu l'être, avec une explication sur la cause. 


Modification de métadonnées en masse 
====================================


Modification de métadonnées descriptives
-----------------------------------------

A partir d'unités archivistiques présentes dans le panier, il est possible de modifier les métadonnées descriptives: d'en ajouter, de les modifier, et de les effacer.

.. image:: images/meta_descriptives.png

Pour ajouter ou modifier une métadonnée : 
Il faut cliquer sur le boutons correspondant à "Ajouter/Modifier une métadonnée descriptives", remplir le nom du champ tel qu'il est enregistré dans la base et la nouvelle valeur , dans le champ "Nouvelle Valeur". Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 

Pour modifier une chaîne de caractères : 
Il faut cliquer sur le boutons correspondant à "Modifier une chaîne de caractères", remplir le nom du champ tel qu'il est enregistré dans la base, la chaîne de caractère actuelle et la nouvelle valeur, dans le champ "Nouvelle Valeur". Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 


Pour vider le champ d'une métadonnée : 
Il faut cliquer sur les boutons correspondants à "Vider une métadonnée descriptive", remplir le Nom du champ dont la valeur sera effacée, dans le champ "Nom du champ". Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 


Modification de métadonnées de gestion 
---------------------------------------

A partir d'unités archivistiques présentes dans le panier, il est possible de modifier les règles de gestion: d'en ajouter, de les modifier, et de les supprimer.

.. image:: images/meta_gestion.png

Chaque catégorie de règle de gestion est présente, il suffit de cliquer sur les boutons "Ajouter", "Modifier" ou "Supprimer" une règle, et d'ajouter les identifiants et les valeurs à modifier, à ajouter ou à supprimer, et ensuite de cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 

NOTE: les identifiants des règles de gestion, doivent être contenus dans le référentiel des règles de gestion. 


Nombre de modifications maximum
===============================

Il est possible de définir un seuil maximum de modifications dans le contexte de modifications en masse. Ce champ permet donc de définir le nombre maximum d'unités archivistiques du panier qui seront modifiées. 

Si le seuil est dépassé : la modification ne s'effectuera sur aucune des unités archivistiques ,et l'opération sera en échec dans le journal des opérations. 

NOTE: si aucune valeur n'est remplie dans le champ, la valeur par défaut est de 1000 modifications. 









