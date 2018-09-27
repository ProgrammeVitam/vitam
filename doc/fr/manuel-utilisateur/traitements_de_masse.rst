Traitements de masse
#####################

Cette partie décrit les fonctionnalités permettant d'effectuer des actions sur un grand nombre d'unités archivistiques stockées dans la solution logicielle Vitam. Cette fonctionnalité nécessite d'avoir les droits requis pour pouvoir intervenir sur les métadonnées. Les actions de traitement de masse sont accessibles via le panier et peuvent être effectuées sur la totalités du panier ou une sélection (cf Chapitre 2.12 sur utilisation du panier).

Chaque traitement de masse donne lieu à une entrée dans le journal des opérations. lorsque le traitement génère un rapport, celui-ci est disponible dans le journal des opérations et détaille les cas de succès et les cas d'échec en explicitant la raison de la non application du traitement de masse. 


Mise à jour de métadonnées en masse
***********************************

Les modifications en masse portent sur les métadonnées descriptives et les métadonnées de gestion. Pour accèder à cette fonctionnalité, cliquez dans la fenêtre "Sélectionner une action", puis dans le menu "Mise à jour de masse". Deux blocs permettant de mettre à jour les métadonnées descriptives et les règles de gestion s'affichent. 


Modification de métadonnées descriptives
========================================

Il est possible de modifier, ajouter et effacer les métadonnées descriptives d'unités archivistiques présentes dans le panier. Pour cela il faut déplier le bloc "Mise à jour des métadonnées descriptives" en cliquant sur la flèche. Trois sous-menus sont proposés :
 - ajouter / modifier une métadonnée descriptive 
 - modifier une chaîne de caractère 
 - vider une métadonnées descriptive 
							

.. image:: images/meta_descriptives.png

Ajouter ou modifier une métadonnée
----------------------------------

Grâce à cette fonctionnalité, il est possible d'apporter des modifications à un champ. En cliquant sur cette option, un formulaire apparaît dans lequel le "Nom du champ" et la "Nouvelle valeur" doivent être complétés. Pour pouvoir modifier la valeur du champ, il est nécessaire de remplir le nom du champ tel qu'il est formulé par le SEDA, en anglais et en respectant la casse ou dans l'ontologie pour les vocabulaires externes. Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection".
 
* Si le champ existe pour ces unités archivistiques, les valeurs contenues dans les champs seront remplacées par la nouvelle valeur entrée dans le formulaire. Il s'agit en définitive d'un annule et remplace. 

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

* Si le champ n'existe pas pour ces unités archivistiques, en indiquant dans le "Nom du champ" un champ du SEDA concernant les métadonnées descriptives ou un vocabulaire externe ainsi qu'une valeur dans la rubrique "Nouvelle Valeur", ce nouveau champ complété sera créé pour ces unités archivistiques. 

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

Modifier une chaîne de caractères
---------------------------------

Grâce à cette fonctionnalité, il est possible d'apporter des modifications sur une partie d'un champ, pour le corriger ou le compléter. Pour pouvoir modifier la valeur du champ, il est nécessaire de remplir le nom du champ tel qu'il est formulé par le SEDA, en anglais et en respectant la casse.

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


Vider une métadonnée descriptive 
--------------------------------

Il faut cliquer sur le bouton correspondant à "Vider une métadonnée descriptive", remplir le Nom du champ dont la valeur sera effacée, dans le champ "Nom du champ",  il est nécessaire de remplir le nom du champ tel qu'il est formulé par le SEDA, en anglais et en respectant la casse. Il faut ensuite cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 


Modification de métadonnées de gestion 
======================================

Il est possible de modifier les règles de gestion des unités archivistiques portées dans le panier: d'en ajouter, de les modifier, et de les supprimer. Pour cela, il faut déplier le bloc "Mise à jour des règles de gestion" en cliquant sur la flèche. Des sous-menus sont proposés par catégorie de règles. 

.. image:: images/meta_gestion.png

Une fois sélectionnée une catégorie de règles, il suffit de cliquer sur les boutons "Ajouter", "Modifier" ou "Supprimer" une règle, et d'indiquer les identifiants et les valeurs à modifier, à ajouter ou à supprimer, et ensuite de cliquer sur un des deux boutons "Lancer la mise à jour de masse sur tout le panier", ou bien "Lancer la mise à jour de masse sur la sélection". 

.. note :: les identifiants des règles de gestion indiqués, doivent être contenus dans le référentiel des règles de gestion. 


Nombre d'unités archivistiques maximum concernés par les modifications en masse 
===============================================================================

Il est possible de définir un seuil maximum de modifications dans le contexte de modifications en masse. Ce champ permet donc de définir le nombre maximum d'unités archivistiques du panier auxquel le traitements de masse sera appliqué.

Si le seuil est dépassé : la modification s'effectuera mais l'opération sera déclarée en warning dans le journal des opérations. Il n'y a pas pour l'instant dans le cadre de cette release 8, de messages d'alerte ou d'indication concernant le temps de traitement de l'opération, ces fonctionnalités seront installés dans les release suivantes. Il est recommandé de respecter les seuils par défaut, prévus pour les traitements de masse. 

.. note:: 
    **Seuil par défaut d'unités archivistiques pour les traitements de masse**.
   
      - **100 000 unités archivistiques dans la phase d'analyse** (analyse des éliminables)
      - **10 000 unités archivistiques dans la phase d'action** (modification en masse, élimination)
      
.. image:: images/seuil_modification_masse.png





Elimination en masse
********************

Cette fonctionnalité permet de lancer des actions d'évaluation des unités archivistiques éliminables dans la solution Vitam et de procéder à leur élimination du système. 
Les actions d'élimination sont réalisables sur les unités archivistiques présentes dans le panier.
L'opération d'élimination en masse est une opération complexe, nécessitant différentes analyses, produisant différents temps de traitement : 

 - Analyse 1 / Unités archivistiques éliminables car la DUA est échue et le sort final déclaré est "Détruire" 
 - Analyse 2 / Unités archivistiques éliminables car il n'y a pas de conflits entre services producteurs pour détruire ces archives, la suppression d'une de ces unités archivistiques n'entraîne pas de conflits.  
 - Analyse 3 et action d'élimination / Unités archivistiques réellement éliminées car elles ne laissent pas d'unités archivistiques orphelines 

Analyse 1 / Rechercher des unités archivistiques potentiellement éliminables
============================================================================

La phase préalable à l'action d'élimination est la recherche d'archives éliminables. Dans l'onglet de recherche avancée il est possible de rechercher les unités archivistiques dont la durée d'utilité administrative est échue ou qui le sera prochainement.
Sélectionnez la durée d'utilité administrative, la date d'échéance et le sort final "Détruire", puis cliquez sur "Rechercher".
Vous pouvez appliquer à ces résultats les filtres de recherche et affiner la recherche par service producteur par exemple.
A l'obtention des résultats vous pouvez sélectionner tout ou une partie de la liste et l'ajouter au panier. 
Vous pouvez choisir d'ajouter l'unité archivistique seule, l'UA et sa descendance ou encore l'UA et son entrée.

Lorsque vous avez sélectionné les unités archivistiques, une pop-up vous informe que les unités archivistiques ont bien été ajoutées à votre panier.
Une fois cette opération terminée vous devez vous rendre dans votre panier.

.. image:: images/recherche_eliminables.png

Analyse 2 / Campagne d'évaluation des unités archivistiques éliminables 
=======================================================================

Cette phase d'analyse est destinée à évaluer les unités archivistiques pour décider de l'application de leur sort final et préparer les demandes d'autorisation. Dans le panier, sélectionnez les unités archivistiques qui ont été retenues par la recherche initiale ou celle que vous souhaitez traiter. Vous pouvez sélectionner la totalité du panier ou une partie.
Une fois la sélection faite, dans la rubrique "Sélectionner une action", cliquez sur Elimination.
Pour que l'analyse des unités archivistiques éliminables soit effectuée, vous devez indiquer une date d'échéance (passée ou future). Puis cliquez sur le bouton "Analyse".

.. image:: images/panier_elimination.png 


Accéder aux résultats de l'analyse
----------------------------------
 
Le résultat de l'analyse d'élimination est consultable dans la rubrique "Gestion des archives", "Résultats d'élimination". 

.. image:: images/resultats_analyse_elimination.png

Pour accéder à cet écran, vous devez : 

 -  aller dans le journal des opérations dans le menu administration
 -  consulter le journal des opérations
 -  récupérer le GUID de l'opération ELIMINATION
 -  puis aller dans la rubrique "Gestion des archives" et sélectionnez l'entrée "Résultats d'élimination".

En complétant l'entrée "Opération d'élimination" avec le GUID de l'opération et en cliquant sur rechercher vous accédez à une liste d'unités archivistiques éliminables. 

Votre opération d'analyse est indexée et enregistrée dans le système. A cette étape, le système va effectuer pour chaque UA une vérification des règles de gestion et d'héritage de celle-ci.  Lors de cette opération les unités archivistiques de statut "Destroy" (éliminables) et "Conflicts" (portant un conflit qui ne permet pas de déterminer le sort final applicable) vont être indexées en base de données. Les unités archivistiques qui sont réellement éliminables apparaissent dans l'écran d'affichage des résultats d'élimination :

 - les UA qui ont une DUA arrivée à échéance et dont le sort final est "détruire"
 - les UA pour lesquelles les services producteurs sont unanimes quant à leur élimination. 

Vous pouvez filter vos résultats en fonction des services producteurs éliminables et ceux qui ne le sont pas. 

.. image:: images/resultat_analyse_elimination_filtre.png


Analyse 3 et action  / Elimination des unités archivistiques éliminables 
========================================================================

Cette action peut être effectuée après la phase de recherche ou d'analyse, mais elle peut également être lancée indépendamment. En effet, vous pouvez décider de supprimer un lot d'unités archivistiques présent dans votre panier. 
Lors de cette opération, le système Vitam va effectuer une analyse analogue que celle décrite ci dessus dans la phase 2, mais ne va pas indexer les résultats concernant l'élimination sur les unités archivistiques en base de données. 
Lors de cette phase le système va s'assurer que l'élimination n'entraine pas de cas d'orphelinage. A l'issue de ces phases d'analyses si l'unité archivistique est réellement éliminable, elle va être supprimée du système.  Pour procéder à cette phase d'analyse et d'action d'élimination, vous devez : 
 
 - Sélectionner des unités archivistiques dans votre panier
 - Cliquer dans le menu déroulant sur l'action "Elimination"
 - Entrer la date du jour ou une date échue
 - Sélectionner le mode "Action"
 - Spécifier si l'action a lieu sur la selection ou le panier dans sa totalité. Au clic, une fenêtre modale vous confirme que l'action d'élimination est en cours.  

.. image:: images/analyse_action_elimination.png


.. image:: images/pop_action_elimination.png


Résultats de l'opération d'élimination
--------------------------------------

Le résultat de l'opération d'élimination est consultable dans le rapport de l'opération. Pour cela vous devez :

 - Aller dans le journal des opérations dans le menu administration
 - Consulter le journal des opérations
 - Cliquer sur l'opération élimination
 - Sélectionner le rapport dans informations complémentaires
 - Télécharger le rapport

Le rapport d'élimination spécifie :

Pour les unités archivistiques : 
 - GLOBAL_STATUS_KEEP tel que fait dans la phase 1 (la DUA n'est pas échue et le sort final spécifié est à conserver) 
 - GLOBAL_STATUS_CONFLICT tel que ou en phase 1 (les UA pour lesquelles les services producteurs ne sont pas unanimes quant à leur élimination, à la différence de la phase d'évaluation, lors de cette action il n'y a pas d'indexation mais des informations additionnelles sur les raisons de la non élimination)
 - DELETED les unités archivistiques ont été éliminées 
 - NOT_DESTROYABLE_HAS_CHILD_UNIT les unités archivistiques n'ont pas été éliminée car leur suppression provoquerait des cas d'orphelinage. 

Pour les GOT : 
 - DELETED les groupes d'objets ont été supprimés. 
 - PARTIAL_DETACHMENT les groupes d'objets ont été détachés des unités archivistiques concernées par l'élimination. 




