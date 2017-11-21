Gestion des archives existantes
################################

Cette partie décrit les fonctionnalités de gestion et modification des archives dans la solution logicielle Vitam.


Audit
=====

La fonctionnalité d'audit permet de contrôler que les fichiers détenus par un service producteur ou présents sur un tenant sont toujours existants et intègres.

Pour réaliser des opérations d'audit, l'utilisateur survole le menu "Gestion des archives existantes", puis sélectionne "Audit".

.. image:: images/menu_audit.png

Sélection du type d'Audit
---------------------------------

Les audits peuvent être exécutés sur les fonds détenus par un service producteur ou sur l'ensemble d'un tenant. 

Pour exécuter un audit sur un tenant, l'utilisateur sélectionne "Service Producteur" dans le sélecteur "Sélectionner le type", puis choisi un service producteur dans le sélecteur "Sélectionner un service producteur".

Pour réaliser un audit sur l'ensemble des fonds pris en charge dans un tenant, l'utilisateur choisi "Tenant" dans le sélecteur "Sélectionner le type".

Le numéro du tenant s'affiche alors automatiquement et n'est pas modifiable.

.. image:: images/detail_audit.png


Sélection du périmètre de l'Audit
---------------------------------

Un audit porte à minima sur l'existence des fichiers et peut aller jusqu'au contrôle de leurs intégrité.

Ainsi, un utilisateur doit au moins cocher la case "Audit de l'existence des objets" pour pouvoir lancer un audit.

S'il coche la case "Audit de l'intégrité des objets", alors la case précédente est grisée et impossible à décocher.


Journalisation et rapport d'Audit
---------------------------------

La réalisation d'un audit donne lieu la création d'un journal d'opération et d'un rapport.

L'utilisateur peut télécharger le rapport de l'IHM depuis l'écran de consultation des journaux d'opération en choisissant affichant la colonne "Rapport", accessible par le sélecteur "Informations supplémentaires".

Un lien de téléchargement apparaît alors dans cette colonne pour les lignes affichant une opération d'audit.

L'utilisateur peut télécharger le rapport de l'opération en cliquant sur ce lien.