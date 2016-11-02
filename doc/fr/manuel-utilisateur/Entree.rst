Entrée d'un SIP
###############

Cette partie décrit l'entrée manuelle d'archives dans Vitam, c'est-à-dire le processus de transfert du SIP via l'IHM minimale, ainsi que le suivi de l'ensemble des transferts via le journal des opérations d'entrées.

Le SIP est un fichier compressé comportant le bordereau de versement SEDA au format XML et les objets à archiver (se référer au livrable "Design SIP")

Transfert d'un SIP dans Vitam
=============================

Le transfert d'un SIP dans Vitam s'effectue depuis l'écran "Transfert" (Entrée/Transfert)

.. image:: images/menu_entree.jpg

Pour débuter une entrée, l’utilisateur doit sélectionner son lot d’archives (SIP) à transférer dans Vitam. Pour cela, il clique sur le bouton « Choisissez votre fichier », une nouvelle fenêtre s'ouvre dans laquelle il a la possibilité de sélectionner le SIP.

Une fois le SIP sélectionné, il apparaît sur l'écran "Transfert".

.. image:: images/upload_sip.jpg

Pour lancer le transfert du SIP, l’utilisateur clique sur le bouton « Importer ».

Les informations visibles à l'écran sont :

- Un tableau comportant les champs suivants :
  - Nom du fichier,
  - Taille : Affiche la taille du SIP en Ko, Mo ou Go en fonction de la taille arrondie au dixième près,
  - Statut representé sous forme d'icône, une croix si le transfert est en erreur, un check si le transfert est en succès.

- Une barre de progression affiche l’avancement du téléchargement du SIP dans Vitam (une barre de progression complète signifie que le téléchargement est achevé).

NB : Suite au téléchargement du SIP, un temps d'attente est necessaire pour traitement du SIP par le système avant affichage du statut.

Si l'utilisateur tente d'importer un SIP au format non conforme (s'il ne s'agit pas des formats ZIP, TAR, TAR.GZ, TAR.GZ2) alors le système empêche le téléchargement.
Une fenêtre pop-up s'ouvre indiquant les formats autorisés.

.. image:: images/upload_sip_KO.jpg

Toute opération d'entrée (succès ou échec) fait l'objet d'une écriture dans le journal des opérations.

Journal des opérations d'entrée
===============================


Le journal des opérations d'entrée est un extrait du journal des opérations.

Il propose deux visions, une vision globale des transferts effectués dans Vitam, et une vision plus détaillée de chaque transfert, explicitant toutes les étapes d'un processus d'entrée.
Il permet ainsi à l'utilisateur de savoir si son entrée est valide, c'est-à-dire si les données et objets contenus dans le SIP sont enregistrés dans Vitam. Dans le cas contraire, il lui permet d'identifier la ou les erreurs expliquant l'échec du transfert.

La consultation du journal des opérations d'entrée s'effectue depuis l'écran "Suivi des opérations d'entrée" (Entrée/Suivi des opérations d'entrée)

.. image:: images/menu_op_entree.jpg

Formulaire de recherche d'une entrée
------------------------------------

Par défaut, l'ensemble des opérations d'entrées est affiché sous le formulaire de recherche.

Pour effectuer une recherche précise, on utilise le champ "Lot d'archive" correspondant au nom du SIP porté par la balise <MessageIdentifier> dans le bordereau de versement SEDA.

Pour initier la recherche, l'utilisateur saisi le nom du SIP et clique sur le bouton "Rechercher".

La recherche s'effectue de façon stricte, c'est-à-dire que seul le nom exact de l'entrée comprenant strictement la chaîne de caractères saisie sera retourné. La recherche porte sur toutes les opérations d'entrées quel que soit leur statut (En cours, succès et échec)

.. image:: images/op_entree.jpg

Affichage des résultats
-----------------------

Le résultat de la recherche est affiché sous forme de tableau, comprenant les éléments suivants :

- Lot d'archive : correspond au nom du SIP porté par la balise <MessageIdentifier> du bordereau de versement SEDA
- Identifiant de l'entrée (référence donnée par le système)
- Date de début d'opération d'entrée
- Date de fin d'opération d'entrée
- Statut : Succès, Erreur, En cours

Par défaut, les colonnes sont triées par dates de versemment décroissantes.

.. image:: images/op_entree_liste.jpg

Depuis cette liste de résultats, l'utilisateur peut consulter le détail d'une opération d'entrée en cliquant sur la ligne de cette opération.


Consultation du détail
----------------------

Suite à la recherche d'une opération d'entrée, l'utilisateur peut choisir de consulter le détail des événements intervenus durant le processus d'entrée.
Pour cela, il clique sur la ligne de l'entrée voulue.
Une fenêtre de type "modale" s'ouvre, pour présenter le détail de cette entrée, sans perdre le focus sur la liste globale des résultats.

L'utilisateur peut consulter sur cet écran toutes les informations contenues dans le journal des opérations associées à cette entrée.
Le processus d'entrée est décrit étape par étape avec des messages correspondant au résultat de chaque événement.

Le détail est affiché sous forme de tableau comportant pour chaque événement les éléments suivants :

- Etape : nom de l'étape correspondante
- Date : date à laquelle l'étape a été effectuée
- Statut : statut final de l'étape
- Message : message expliquant le statut de cette étape

.. image:: images/op_entree_detail.jpg

Un clic sur le bouton "Close" ou hors de la fenêtre "modale" referme celle-ci.
