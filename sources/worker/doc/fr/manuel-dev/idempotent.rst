Idempotence
###########

Pour permettre une bonne résilience de l'application Vitam, il est important de s'assurer de l'idem-potence des plugins et handlers exécutés lors des différents Workflows.
L'idem-potence veut dire que le résultat pour une opération que l'on exécute plusieurs fois, doit être le même que le résultalt pour une opération exécutée unitairement.

Ici on parle donc des différentes actions et étapes lancées durant les différents processus.

Introduction
************

Pour pouvoir tester l'idem-potence du processus d'ingest, un test d'intégration a été mis en place et permet de lancer automatiquement un ingest en mode pas à pas.
Pour chaque étape, celle-ci sera relancée automatiquement. Son nombre d'exécution sera de 2.
Donc en toute logique, si un problème est rencontré (actuellement, il n'y a pas de problème) c'est que le développement en cours n'assure pas l'idem-potence.  

Modifications
*************

HandlerIO
=========

Dans le HandlerIO, classe permettant comme son nom l'indique de gérer les inputs et les outputs pour les différentes étapes, une méthode a été ajoutée : removeFolder.

Elle permet notamment de gérer le cas très précis de l'ExtractSeda.
Afin d'extraire du manifest, les différents ObjectGroup en une multitude de fichiers json dans un répertoire de travail commun, désormais on va tester l'existence de ce répertoire. Si ce répertoire existe déjà, cela signifie que cette étape a déjà été lancée (partiellement).
Pour garantir une bonne exécution de cette étape, on supprime le répertoire avec ce qu'il contient, afin de permettre de ne pas embarquer des morceaux de fichiers json faux qui auraient pu potentiellement être créés par une exécution précédente.

Handlers / plugins
==================

AccessionRegisterActionHandler
------------------------------

Afin de veiller à ne pas enregistrer plusieurs fois la même opération dans la collection AccessionRegisterDetail, un test a été ajouté pour vérifier la présence ou non d'un précédent enregistrement.

ExtractSedaActionHandler
------------------------
Pour ne pas dupliquer les fichiers générés lors de l'ExtractSeda, le répertoire contenant les outputs (fichiers json) est effacé au préalable, s'il existe déjà sur le Workspace.

IndexObjectGroupActionPlugin
----------------------------

Si l'on tente de sauvegarder plusieurs fois un même objectGroup dans Metadata, une exception est lancée par le composant Metadata.
Il convient dans ce cas de ne pas considérer cette exception comme FATAL pour le workflow. Un StatusCode particulier est retourné.

IndexUnitActionPlugin
---------------------

Si l'on tente de sauvegarder plusieurs fois un même objectGroup dans Metadata, une exception est lancée par le composant Metadata.
Il convient dans ce cas de ne pas considérer cette exception comme FATAL pour le workflow. Un StatusCode particulier est retourné.

StoreObjectGroupActionPlugin
----------------------------

Si l'on tente de sauvegarder plusieurs fois un même objectGroup dans le Storage, lors de la deuxième exécution (si la première s'est bien terminée) la partie work ne sera plus présente dans le Json présent dans le workspace.
Il convient dans ce cas de ne pas considérer cette exception comme FATAL pour le workflow. Un StatusCode particulier est retourné.

WorkerImpl
==========

Dans cette partie, on traite les retours des Handlers et des plugins. Si on se retrouve, dans le cadre d'actions distribuées, avec le StatusCode particulier (ALREADY_EXECUTED) alors on n'enregistre pas dans les LFC. 
Cela permet d'éviter les doublons dans les LFC Unit et ObjectGroup.