Traitements de masse
#####################

Cette partie décrit les fonctionnalités permettant d'effectuer des actions sur un grand nombre d'unités archivistiques stockées dans la solution logicielle Vitam. Les actions de traitement de masse sont accessibles après une recherche et une sélection des unités archivistiques via le panier. 
Le panier indique le nombre total d'unités archivistiques contenues dans le panier et/ou le nombre d'unités archivistiques séléctionnées. Les traitements de masse peuvent être effectués sur la totalité ou sur une partie du panier.  
Chaque mise à jour est suivie d'une entrée dans le journal des opérations et d'un rapport. Toutes les opérations sont indiquées dans le rapport, les cas de succès et les cas d'échecs. En cas d'échec, un message d'erreur indique la raison de la non application du traitement de masse. 


Modification de métadonnées en masse
************************************

Les modifications en masse concernent les métadonnées descriptives et les métadonnées de gestion. Pour accèder à cette fonctionnalité, cliquez dans la fenêtre "Sélectionner une action", puis dans le menu "Mise à jour de masse". 


1 - Modification de métadonnées descriptives
============================================

A partir d'unités archivistiques présentes dans le panier, il est possible de modifier, ajouter et effacer les métadonnées descriptives.

.. image:: images/meta_descriptives.png

Ajouter ou modifier une métadonnée : 
------------------------------------

Grâce à cette fonctionnalité, il est possible d'apporter des modifications à un champ. En cliquant sur cette option, un formulaire apparaît dans lequel le "Nom du champ" et la "Nouvelle valeur" doivent être complétés. 
 
* Si le champ existe pour ces unités archivistiques, les valeurs contenues dans ces unités archivistiques seront remplacées par la nouvelle valeur entrée dans le champ "Nouvelle Valeur" dans le formulaire. Il s'agit en définitive d'un annule et remplace. Pour pouvoir modifier la valeur du champ il est nécessaire de remplir le nom du champ tel qu'il est formulé par le SEDA, en anglais et en respectant la casse. Un vocabulaire externe peut également être modifié. Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection".

Ex : Pour modifier un titre : "Ecole nationale des Greffes" par le titre "Discours prononcé lors de la visite de l'Ecole nationale des Greffes". 

+-------------------------+----------------------------------------------------------------------------+       
|Nom de champ             |  Nouvelle Valeur                                                           |
+-------------------------+----------------------------------------------------------------------------+   
|Title (nom du SEDA,      |  Discours prononcé lors de la visite de l'Ecole nationale des Greffes      |
|en anglais,              |                                                                            |
|en respectant la casse,  |                                                                            |
|ou vocabulaire externe)  |                                                                            |
|                         |                                                                            |
+-------------------------+-----------------------------------+----------------------------------------+ 

* Si le champ n'existe pas pour ces unités archivistiques, en indiquant dans le "Nom du champ" un champ du SEDA concernant les métadonnées descriptives ou un vocabulaire externe ainsi qu'une valeur dans la rubrique "Nouvelle Valeur", ce nouveau champ complété sera créé pour cette sélection d'unités archivistiques. 

Ex : Pour ajouter un champ Langue des descriptions,

+-------------------------+----------------------------------------------------------------------------+       
|Nom de champ             |  Nouvelle Valeur                                                           |
+-------------------------+----------------------------------------------------------------------------+   
|DescriptionLanguage      |  Français ou Fr                                                            |
|(nom du SEDA, en anglais,|                                                                            |
|en respectant la casse,  |                                                                            |
|ou vocabulaire externe)  |                                                                            |
|                         |                                                                            |
+-------------------------+-----------------------------------+----------------------------------------+ 

Modifier une chaîne de caractères : 
-----------------------------------

Grâce à cette fonctionnalité, il est possible d'apporter des modifications sur une partie d'un champ, pour le corriger ou le compléter. 

* Ex : Corriger dans un titre le mot fracture par facture : 

+--------------------------+-----------------------------------+-------------------------+       
| Nom de champ             |   Chaîne de caractère actuelle    |      Nouvelle Valeur    |
+--------------------------+-----------------------------------+-------------------------+   
|Title (nom du SEDA,       |   fracture                        |          facture        |
|en anglais,               |                                   |                         |
|en respectant la casse,   |                                   |                         |
|ou vocabulaire externe)   |                                   |                         |
|                          |                                   |                         |
+--------------------------+-----------------------------------+-------------------------+ 

Il faut indiquer ensuite si la modification concerne toutes les unités archivistiques contenues dans le panier ou une selection. 

Vider une métadonnée descriptive :
----------------------------------
Il faut cliquer sur le bouton correspondant à "Vider une métadonnée descriptive", remplir le Nom du champ dont la valeur sera effacée, dans le champ "Nom du champ". Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 


2 - Modification de métadonnées de gestion 
==========================================

A partir d'unités archivistiques présentes dans le panier, il est possible de modifier les règles de gestion: d'en ajouter, de les modifier, et de les supprimer.

.. image:: images/meta_gestion.png

Chaque catégorie de règle de gestion est présente, il suffit de cliquer sur les boutons "Ajouter", "Modifier" ou "Supprimer" une règle, et d'ajouter les identifiants et les valeurs à modifier, à ajouter ou à supprimer, et ensuite de cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 

NOTE: les identifiants des règles de gestion, doivent être contenus dans le référentiel des règles de gestion. 


Nombre d'unités archivistiques maximum concernés par les modifications en masse 
===============================================================================

Il est possible de définir un seuil maximum de modifications dans le contexte de modifications en masse. Ce champ permet donc de définir le nombre maximum d'unités archivistiques du panier qui seront modifiées. 

Si le seuil est dépassé : la modification ne s'effectuera sur aucune des unités archivistiques et l'opération sera en échec dans le journal des opérations. 

NOTE: si aucune valeur n'est remplie dans le champ, la valeur de seuil par défaut  est de 1000 modifications. 









